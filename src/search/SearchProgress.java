package search;

import core.Move;

public class SearchProgress {
    public final int currentDepth;
    public final double percentComplete;
    public final boolean isStable;
    public final int score;
    public final Move bestMove;

    public SearchProgress(int currentDepth, double percentComplete, boolean isStable, int score, Move bestMove) {
        this.currentDepth = currentDepth;
        this.percentComplete = percentComplete;
        this.isStable = isStable;
        this.score = score;
        this.bestMove = bestMove;
    }
}
