package time;

import search.SearchConstraints;
import search.SearchProgress;

public interface TimeManager {
    void init(SearchConstraints constraints, core.Color sideToMove);
    long getAllocatedTimeMs(); // Exposes the calculated base move budget
    boolean shouldStop(long elapsedMs, SearchProgress progress);
}
