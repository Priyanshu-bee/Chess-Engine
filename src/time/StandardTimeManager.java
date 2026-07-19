package time;

import search.SearchConstraints;
import search.SearchProgress;
import search.Logger;
import core.Color;

public class StandardTimeManager implements TimeManager {
    private long allocatedTimeMs;
    private long softTimeLimit;
    private long hardTimeLimit;
    private boolean loggedSoftLimit;

    @Override
    public void init(SearchConstraints constraints, Color sideToMove) {
        this.loggedSoftLimit = false;

        if (constraints.depth > 0 || constraints.infinite || constraints.ponder) {
            this.allocatedTimeMs = -1;
            this.softTimeLimit = -1;
            this.hardTimeLimit = -1;
            return;
        }

        int timeRemaining = (sideToMove == Color.WHITE) ? constraints.wtime : constraints.btime;
        int increment = (sideToMove == Color.WHITE) ? constraints.winc : constraints.binc;
        int movesToGo = constraints.movestogo;

        if (timeRemaining > 0) {
            int moves = (movesToGo > 0) ? movesToGo : 40;
            this.allocatedTimeMs = (timeRemaining / Math.min(moves, 40)) + increment;
            
            // Soft limit: Ideal time minus 50ms buffer
            this.softTimeLimit = this.allocatedTimeMs - 50;
            if (this.softTimeLimit < 20) this.softTimeLimit = 20;

            // Hard limit: Max 1.5x soft limit, capped at remaining time minus buffer
            this.hardTimeLimit = (long) (this.softTimeLimit * 1.5);
            if (this.hardTimeLimit > timeRemaining - 50) {
                this.hardTimeLimit = timeRemaining - 50;
            }
            if (this.hardTimeLimit < this.softTimeLimit) {
                this.hardTimeLimit = this.softTimeLimit;
            }
        } else {
            this.allocatedTimeMs = -1;
            this.softTimeLimit = -1;
            this.hardTimeLimit = -1;
        }
    }

    @Override
    public long getAllocatedTimeMs() {
        return allocatedTimeMs;
    }

    @Override
    public boolean shouldStop(long elapsedMs, SearchProgress progress) {
        if (softTimeLimit <= 0) {
            return false;
        }

        // 1. Hard Cutoff: Abort immediately if we reach the hard ceiling
        if (elapsedMs >= hardTimeLimit) {
            Logger.log("info string [TimeManager] Hard limit reached (" + elapsedMs + "ms >= " + hardTimeLimit + "ms). Stopping search.");
            return true;
        }

        // 2. Soft Cutoff: Check if we should allow grace period or stop
        if (elapsedMs >= softTimeLimit) {
            boolean grantGrace = false;
            String graceReason = "";
            
            if (progress.currentDepth > 1) {
                // Grant grace if we are close to finishing the current depth layer
                if (progress.percentComplete > 0.85) {
                    grantGrace = true;
                    graceReason = "close to finishing depth (progress " + (int)(progress.percentComplete * 100) + "%)";
                }
                // Grant grace if the position search is highly volatile (unstable)
                else if (!progress.isStable) {
                    grantGrace = true;
                    graceReason = "volatile/unstable position (depth " + progress.currentDepth + ")";
                }
            }

            if (!grantGrace) {
                Logger.log("info string [TimeManager] Soft limit reached (" + elapsedMs + "ms >= " + softTimeLimit + "ms). No grace criteria met. Stopping search.");
                return true;
            } else if (!loggedSoftLimit) {
                loggedSoftLimit = true;
                Logger.log("info string [TimeManager] Soft limit reached (" + elapsedMs + "ms >= " + softTimeLimit + "ms). Grace period granted: " + graceReason);
            }
        }

        return false;
    }
}
