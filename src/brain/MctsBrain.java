package brain;

import core.Board;
import core.Move;
import search.SearchProgress;
import java.util.List;

public class MctsBrain implements Brain {
    private volatile boolean stopSignal = false;
    private int simulationsRun = 0;
    private int maxSimulations = 100000;
    private Move bestMove = null;

    @Override
    public void init(Board board, long allocatedTimeMs, int targetDepth, long targetNodes, boolean isInfinite) {
        this.stopSignal = false;
        this.simulationsRun = 0;
        this.bestMove = null;
        
        // MCTS Tree Re-rooting:
        // if (mctsTree != null && mctsTree.hasNodeFor(board)) {
        //     mctsTree.setRoot(mctsTree.getNodeFor(board)); // Shift root to reuse simulations!
        // } else {
        //     mctsTree = new MctsTree(board); // Rebuild tree for new game
        // }

        if (targetNodes > 0) {
            this.maxSimulations = (int) targetNodes; // Fixed simulations limit (e.g. go nodes 50000)
        } else {
            this.maxSimulations = Integer.MAX_VALUE; // Search indefinitely under time controls
        }
    }

    @Override
    public Move think(Board board) {
        // MCTS Loop placeholder
        // while (simulationsRun < maxSimulations) {
        //     runSingleSimulation(board);
        //     simulationsRun++;
        // }

        List<Move> legalMoves = board.getLegalMoves();
        if (legalMoves.isEmpty()) {
            return null;
        }
        bestMove = legalMoves.get(0);
        return bestMove;
    }

    @Override
    public Move getBestMove() {
        return bestMove;
    }

    @Override
    public SearchProgress getProgress() {
        double percent = (maxSimulations > 0) ? ((double) simulationsRun / maxSimulations) : 0.0;
        return new SearchProgress(1, percent, true, 0, bestMove);
    }

    @Override
    public void stop() {
        this.stopSignal = true;
    }
}
