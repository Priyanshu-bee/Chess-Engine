package search;

/**
 * Lightweight, stacktrace-free exception used to instantly collapse
 * the search recursion when a stop signal is received.
 */
public class SearchAbortedException extends RuntimeException {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this; // Disable stack trace generation for speed
    }
}
