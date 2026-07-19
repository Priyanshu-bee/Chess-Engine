package execution;

import core.*;
import brain.*;
import search.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class MyBot {
    private final Board board = new Board();
    private final Brain brain = new NegamaxBrain(); // Persistent brain instance
    private SearchSession activeSession = null;

    public static void main(String[] args) {
        Logger.clear();
        MyBot bot = new MyBot();
        bot.startUciLoop();
    }

    public void startUciLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("uci")) {
                    System.out.println("id name BitChess");
                    System.out.println("id author ArtificialMagic");
                    System.out.println("option name Ponder type check default false");
                    System.out.println("uciok");
                    System.out.flush();
                } else if (line.equals("isready")) {
                    System.out.println("readyok");
                    System.out.flush();
                } else if (line.startsWith("position")) {
                    stopSearchSession();
                    parsePosition(line);
                } else if (line.startsWith("go")) {
                    parseGo(line);
                } else if (line.equals("stop")) {
                    if (activeSession != null) {
                        activeSession.stop();
                    }
                } else if (line.equals("ponderhit")) {
                    if (activeSession != null) {
                        activeSession.ponderHit();
                    }
                } else if (line.equals("quit")) {
                    stopSearchSession();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void stopSearchSession() {
        if (activeSession != null) {
            activeSession.stop();
            activeSession.join();
            activeSession = null;
        }
    }

    private void parsePosition(String line) {
        String[] parts = line.split("\\s+");
        int movesIdx = -1;

        if (parts[1].equals("startpos")) {
            board.loadFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            for (int i = 2; i < parts.length; i++) {
                if (parts[i].equals("moves")) {
                    movesIdx = i + 1;
                    break;
                }
            }
        } else if (parts[1].equals("fen")) {
            StringBuilder sb = new StringBuilder();
            int i = 2;
            while (i < parts.length && !parts[i].equals("moves")) {
                sb.append(parts[i]).append(" ");
                i++;
            }
            board.loadFen(sb.toString().trim());
            if (i < parts.length && parts[i].equals("moves")) {
                movesIdx = i + 1;
            }
        }

        if (movesIdx != -1) {
            for (int i = movesIdx; i < parts.length; i++) {
                Move m = parseMoveToken(parts[i]);
                if (m != null) {
                    board.makeMove(m);
                }
            }
        }
    }

    private Move parseMoveToken(String token) {
        List<Move> legalMoves = board.getLegalMoves();
        for (Move m : legalMoves) {
            if (m.toUciString().equals(token)) {
                return m;
            }
        }
        return null;
    }

    private void parseGo(String line) {
        stopSearchSession();

        SearchConstraints constraints = new SearchConstraints();
        String[] parts = line.split("\\s+");
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].equals("depth") && i + 1 < parts.length) {
                constraints.depth = Integer.parseInt(parts[i + 1]);
                i++;
            } else if (parts[i].equals("nodes") && i + 1 < parts.length) {
                constraints.nodes = Long.parseLong(parts[i + 1]);
                i++;
            } else if (parts[i].equals("wtime") && i + 1 < parts.length) {
                constraints.wtime = Integer.parseInt(parts[i + 1]);
                i++;
            } else if (parts[i].equals("btime") && i + 1 < parts.length) {
                constraints.btime = Integer.parseInt(parts[i + 1]);
                i++;
            } else if (parts[i].equals("winc") && i + 1 < parts.length) {
                constraints.winc = Integer.parseInt(parts[i + 1]);
                i++;
            } else if (parts[i].equals("binc") && i + 1 < parts.length) {
                constraints.binc = Integer.parseInt(parts[i + 1]);
                i++;
            } else if (parts[i].equals("movestogo") && i + 1 < parts.length) {
                constraints.movestogo = Integer.parseInt(parts[i + 1]);
                i++;
            } else if (parts[i].equals("ponder")) {
                constraints.ponder = true;
            } else if (parts[i].equals("infinite")) {
                constraints.infinite = true;
            }
        }

        // Reuse the persistent brain instance
        activeSession = new SearchSession(board, constraints, brain);
        activeSession.start();
    }
}