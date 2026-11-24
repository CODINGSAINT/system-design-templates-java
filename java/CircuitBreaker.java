import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Circuit breaker: closed -> open -> half-open -> closed.
 *
 * Companion to sheet 10 (timeouts, retries and circuit breakers). A timeout protects one call and a
 * retry recovers one call; a breaker stops calling a dependency that is already failing, so the
 * failure cannot spread to your own threads, pools and latency.
 *
 * No dependencies. Run with: java CircuitBreaker.java
 */
public final class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int windowSize;
    private final double failureRatio;
    private final int minimumCalls;
    private final Duration openDuration;
    private final Clock clock;

    private final Deque<Boolean> outcomes = new ArrayDeque<>();
    private State state = State.CLOSED;
    private Instant openedAt = Instant.EPOCH;
    private int halfOpenTrial = 0;

    public CircuitBreaker(int windowSize, double failureRatio, int minimumCalls, Duration openDuration, Clock clock) {
        if (windowSize < 1) throw new IllegalArgumentException("windowSize must be >= 1");
        if (failureRatio <= 0 || failureRatio > 1) throw new IllegalArgumentException("failureRatio must be in (0, 1]");
        if (minimumCalls < 1) throw new IllegalArgumentException("minimumCalls must be >= 1");
        this.windowSize = windowSize;
        this.failureRatio = failureRatio;
        this.minimumCalls = minimumCalls;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    public synchronized State state() {
        if (state == State.OPEN && clock.instant().isAfter(openedAt.plus(openDuration))) {
            state = State.HALF_OPEN;
            halfOpenTrial = 0;
        }
        return state;
    }

    /**
     * Runs the call behind the breaker.
     *
     * @throws IllegalStateException when the circuit is open, so the caller fails fast
     */
    public <T> T call(Supplier<T> action) {
        synchronized (this) {
            if (state() == State.OPEN) throw new IllegalStateException("circuit open: fail fast instead of queueing");
            if (state == State.HALF_OPEN && halfOpenTrial >= 1) throw new IllegalStateException("half-open: one trial at a time");
            if (state == State.HALF_OPEN) halfOpenTrial++;
        }
        try {
            T result = action.get();
            record(true);
            return result;
        } catch (RuntimeException e) {
            record(false);
            throw e;
        }
    }

    private synchronized void record(boolean success) {
        if (state == State.HALF_OPEN) {
            // One successful trial closes the circuit; one failure re-opens it for another window.
            if (success) {
                state = State.CLOSED;
                outcomes.clear();
            } else {
                trip();
            }
            halfOpenTrial = 0;
            return;
        }
        outcomes.addLast(success);
        while (outcomes.size() > windowSize) outcomes.removeFirst();
        if (outcomes.size() >= minimumCalls) {
            long failures = outcomes.stream().filter(ok -> !ok).count();
            if ((double) failures / outcomes.size() >= failureRatio) trip();
        }
    }

    private void trip() {
        state = State.OPEN;
        openedAt = clock.instant();
    }

    // ------------------------------------------------------------------ demo

    private static final class ManualClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public Instant instant() { return now; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    public static void main(String[] args) {
        ManualClock clock = new ManualClock();
        CircuitBreaker breaker = new CircuitBreaker(10, 0.5, 4, Duration.ofSeconds(30), clock);

        // Healthy calls keep the circuit closed.
        for (int i = 0; i < 5; i++) {
            if (!"ok".equals(breaker.call(() -> "ok"))) throw new AssertionError("healthy call failed");
        }
        System.out.println("after healthy calls: " + breaker.state());
        if (breaker.state() != State.CLOSED) throw new AssertionError("should be closed");

        // 4 failures in a 9-call window is 44%, below the 50% threshold, so it correctly stays closed.
        for (int i = 0; i < 4; i++) {
            try {
                breaker.call(() -> { throw new RuntimeException("downstream timeout"); });
            } catch (RuntimeException expected) {
                // the caller still sees the failure
            }
        }
        System.out.println("after 4 failures in 9 calls (44%): " + breaker.state());
        if (breaker.state() != State.CLOSED) throw new AssertionError("44% must not trip a 50% breaker");

        // Crossing 50% of the window opens it.
        try {
            breaker.call(() -> { throw new RuntimeException("downstream timeout"); });
        } catch (RuntimeException expected) {
            // ignore
        }
        System.out.println("after 5 failures in 10 calls (50%): " + breaker.state());
        if (breaker.state() != State.OPEN) throw new AssertionError("should be open");

        // While open, calls fail fast without touching the dependency.
        boolean fastFailed = false;
        try {
            breaker.call(() -> "should not run");
        } catch (IllegalStateException e) {
            fastFailed = true;
        }
        if (!fastFailed) throw new AssertionError("open breaker still called the dependency");

        // After the cool-down the breaker lets one trial through.
        clock.advance(Duration.ofSeconds(31));
        System.out.println("after 31 s: " + breaker.state());
        if (breaker.state() != State.HALF_OPEN) throw new AssertionError("should be half-open");

        // A failed trial re-opens the circuit immediately.
        try {
            breaker.call(() -> { throw new RuntimeException("still down"); });
        } catch (RuntimeException expected) {
            // ignore
        }
        if (breaker.state() != State.OPEN) throw new AssertionError("failed trial should re-open the circuit");

        // A successful trial closes it again.
        clock.advance(Duration.ofSeconds(31));
        breaker.call(() -> "recovered");
        System.out.println("after recovery: " + breaker.state());
        if (breaker.state() != State.CLOSED) throw new AssertionError("should be closed after a successful trial");

        System.out.println("CircuitBreaker OK");
    }
}
