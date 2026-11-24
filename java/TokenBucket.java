import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe token bucket.
 *
 * Companion to sheet 09 (rate limiting). Tokens refill continuously at a fixed rate, so a short
 * burst passes while the long-run average stays capped. Compare with a fixed window, which allows
 * up to twice the limit across a window boundary.
 *
 * The clock is injected so behaviour can be tested without sleeping. In production pass
 * {@link Clock#systemUTC()} and share one bucket per key (user ID or API key) in Redis or Caffeine.
 *
 * No dependencies. Run with: java TokenBucket.java
 */
public final class TokenBucket {

    private final double capacity;
    private final double refillPerSecond;
    private final Clock clock;

    private double tokens;
    private Instant lastRefill;

    public TokenBucket(double capacity, double refillPerSecond, Clock clock) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        if (refillPerSecond <= 0) throw new IllegalArgumentException("refillPerSecond must be > 0");
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefill = clock.instant();
    }

    /** Takes one token if one is available. */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * Takes {@code cost} tokens atomically.
     *
     * @return true when the request may proceed
     */
    public synchronized boolean tryAcquire(double cost) {
        if (cost <= 0) throw new IllegalArgumentException("cost must be > 0");
        refill();
        if (tokens < cost) return false;
        tokens -= cost;
        return true;
    }

    /** Seconds a caller should wait before this request would fit, or 0 when it fits now. */
    public synchronized double retryAfterSeconds(double cost) {
        refill();
        if (tokens >= cost) return 0;
        return (cost - tokens) / refillPerSecond;
    }

    public synchronized double available() {
        refill();
        return tokens;
    }

    private void refill() {
        Instant now = clock.instant();
        double elapsed = Duration.between(lastRefill, now).toNanos() / 1_000_000_000.0;
        if (elapsed <= 0) return;
        tokens = Math.min(capacity, tokens + elapsed * refillPerSecond);
        lastRefill = now;
    }

    // ------------------------------------------------------------------ demo

    /** A clock the test can advance by hand. */
    private static final class ManualClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public Instant instant() { return now; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    public static void main(String[] args) throws Exception {
        ManualClock clock = new ManualClock();

        // 20 burst, then 10 per second sustained.
        TokenBucket bucket = new TokenBucket(20, 10, clock);

        int burst = 0;
        while (bucket.tryAcquire()) burst++;
        System.out.println("burst allowed: " + burst);
        if (burst != 20) throw new AssertionError("expected a burst of exactly 20, got " + burst);
        if (bucket.tryAcquire()) throw new AssertionError("bucket should be empty");
        if (bucket.retryAfterSeconds(1) <= 0) throw new AssertionError("expected a Retry-After hint");

        // Half a second of refill at 10/s gives 5 tokens, not a full reset.
        clock.advance(Duration.ofMillis(500));
        if (Math.abs(bucket.available() - 5) > 0.001) throw new AssertionError("refill math is wrong: " + bucket.available());
        int afterHalfSecond = 0;
        while (bucket.tryAcquire()) afterHalfSecond++;
        System.out.println("after 500 ms: " + afterHalfSecond + " more allowed");
        if (afterHalfSecond != 5) throw new AssertionError("expected 5, got " + afterHalfSecond);

        // Idle long enough and tokens cap at capacity, never above it.
        clock.advance(Duration.ofHours(1));
        if (bucket.available() != 20) throw new AssertionError("bucket overflowed: " + bucket.available());

        // Concurrency: 40 threads racing for 20 tokens -> exactly 20 winners, never 21.
        ManualClock fixed = new ManualClock();
        TokenBucket shared = new TokenBucket(20, 1, fixed);
        AtomicInteger winners = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        Thread[] threads = new Thread[40];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                if (shared.tryAcquire()) winners.incrementAndGet();
            });
            threads[i].start();
        }
        start.countDown();
        for (Thread t : threads) t.join();
        System.out.println("concurrent winners: " + winners.get() + " of 40");
        if (winners.get() != 20) throw new AssertionError("token bucket allowed " + winners.get() + " requests");

        System.out.println("TokenBucket OK");
    }
}
