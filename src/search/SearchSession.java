package search;

import core.Board;
import core.Move;
import core.Color;
import core.BoardListener;
import brain.Brain;
import time.TimeManager;
import time.StandardTimeManager;

public class SearchSession {
    private final Board board;
    private final SearchConstraints constraints;
    private final Brain brain;
    private final TimeManager timeManager;
    
    private Thread thread;
    private volatile boolean stopSignal = false;
    private long startTime;

    public SearchSession(Board board, SearchConstraints constraints, Brain brain) {
        this.board = board;
        this.constraints = constraints;
        this.brain = brain;
        this.timeManager = new StandardTimeManager();
        this.timeManager.init(constraints, board.getSideToMove());
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
        
        // Copy the board to protect the original state history on search aborts
        final Board searchBoard = board.copy();
        
        // Extract calculated budget and pass parameters directly to clean brain.init
        long allocatedTimeMs = timeManager.getAllocatedTimeMs();
        brain.init(searchBoard, allocatedTimeMs, constraints.depth, constraints.nodes, constraints.infinite);

        // Logging Search start
        Logger.log("info string [SearchSession] Starting search. Side: " + board.getSideToMove() +
                   ", allocatedTimeMs: " + (allocatedTimeMs > 0 ? allocatedTimeMs + "ms" : "unlimited") +
                   (constraints.ponder ? " (PONDER MODE)" : ""));

        thread = new Thread(() -> {
            // Attach throttled listener to check for stop signal once every 10,000 moves
            searchBoard.setListener(new BoardListener() {
                private int checkCounter = 0;

                @Override
                public void onMakeMove() {
                    if (++checkCounter >= 10000) {
                        checkCounter = 0;
                        if (stopSignal) {
                            throw new SearchAbortedException();
                        }
                    }
                }
            });

            Move best = null;
            try {
                best = brain.think(searchBoard); // Call the renamed think() method
            } catch (SearchAbortedException e) {
                best = brain.getBestMove();
            } finally {
                searchBoard.setListener(null);
            }

            if (best != null) {
                Move ponder = brain.getPonderMove();
                if (ponder != null) {
                    Logger.log("bestmove " + best.toUciString() + " ponder " + ponder.toUciString());
                } else {
                    Logger.log("bestmove " + best.toUciString());
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        // Start time monitoring if time controls are present and we are not pondering
        Color us = board.getSideToMove();
        int timeRemaining = (us == Color.WHITE) ? constraints.wtime : constraints.btime;
        if (!constraints.ponder && timeRemaining > 0 && constraints.depth <= 0 && constraints.nodes <= 0 && !constraints.infinite) {
            startMonitorThread();
        }
    }

    private void startMonitorThread() {
        Thread monitorThread = new Thread(() -> {
            while (thread.isAlive() && !stopSignal) {
                try {
                    Thread.sleep(10); // Poll every 10ms
                } catch (InterruptedException e) {
                    break;
                }

                long elapsed = System.currentTimeMillis() - startTime;
                
                // Let the abstract TimeManager make the decision based on time and search info!
                if (timeManager.shouldStop(elapsed, brain.getProgress())) {
                    stop();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void stop() {
        if (!this.stopSignal) {
            this.stopSignal = true;
            brain.stop();
            Logger.log("info string [SearchSession] Search stopped.");
        }
    }

    public void ponderHit() {
        // Transition from pondering to active play
        this.startTime = System.currentTimeMillis();
        
        // Recalculate limits with ponder disabled
        SearchConstraints activeConstraints = new SearchConstraints();
        activeConstraints.depth = constraints.depth;
        activeConstraints.nodes = constraints.nodes;
        activeConstraints.wtime = constraints.wtime;
        activeConstraints.btime = constraints.btime;
        activeConstraints.winc = constraints.winc;
        activeConstraints.binc = constraints.binc;
        activeConstraints.movestogo = constraints.movestogo;
        activeConstraints.infinite = constraints.infinite;
        activeConstraints.ponder = false; // Disable ponder mode

        timeManager.init(activeConstraints, board.getSideToMove());

        Logger.log("info string [SearchSession] Ponderhit received. Disabling ponder mode. Active allocatedTimeMs: " + timeManager.getAllocatedTimeMs() + "ms");

        // Start the monitor thread now that time controls are active
        Color us = board.getSideToMove();
        int timeRemaining = (us == Color.WHITE) ? activeConstraints.wtime : activeConstraints.btime;
        if (timeRemaining > 0 && activeConstraints.depth <= 0 && activeConstraints.nodes <= 0 && !activeConstraints.infinite) {
            startMonitorThread();
        }
    }

    public void join() {
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
}
