package execution;

import core.*;
import brain.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class MyBot {
    private final Board board = new Board();

    public static void main(String[] args) {
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
                    System.out.println("uciok");
                    System.out.flush();
                } else if (line.equals("isready")) {
                    System.out.println("readyok");
                    System.out.flush();
                } else if (line.startsWith("position")) {
                    parsePosition(line);
                } else if (line.startsWith("go")) {
                    parseGo(line);
                } else if (line.equals("quit")) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        int depth = 5; // Default depth
        String[] parts = line.split("\\s+");
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].equals("depth") && i + 1 < parts.length) {
                depth = Integer.parseInt(parts[i + 1]);
            }
        }

        Search searcher = new Search();
        Move best = searcher.findBestMove(board, depth);
        if (best != null) {
            System.out.println("bestmove " + best.toUciString());
            System.out.flush();
        }
    }
}