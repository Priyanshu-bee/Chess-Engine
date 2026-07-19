package brain;

import core.Board;
import core.Move;
import search.SearchProgress;

public interface Brain {
    void init(Board board, long allocatedTimeMs, int targetDepth, long targetNodes, boolean isInfinite);
    Move think(Board board); // Active computation method
    Move getBestMove(); // Returns the best move found from the last completed layer
    default Move getPonderMove() { return null; }
    SearchProgress getProgress();
    void stop();
}
