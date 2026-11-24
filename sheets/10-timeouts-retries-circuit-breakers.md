# Timeouts, retries and circuit breakers

*A slow dependency is worse than a dead one: it holds your threads. These four habits keep one failing service from taking the rest down.*

## 1. Timeouts come first

- Every network call needs two: **connect** (about 1 second) and **read** (from the callee's slow-case time).
- With no timeout, one slow service holds your threads until the pool is empty.
- Several Java HTTP clients wait **forever** by default. Set both.
- The caller's timeout must be longer than the callee's.

## 2. Retry safely

- Retry only what is **safe to repeat**: GET, PUT, DELETE, or POST with an idempotency key.
- Retry on timeouts, 5xx and 429. **Never on 4xx.**
- **Back off with jitter:** about 100 ms, 200 ms, 400 ms, each plus a random part.
- Cap attempts at 3 and retry at **one layer only**. Three layers of 3 make 27 calls.

## 3. Circuit breaker states

| State | What happens | Moves when |
|---|---|---|
| Closed | Calls go through. Failures are counted | The failure rate passes the threshold, e.g. half of the last 20 calls |
| Open | Calls fail at once, or return a fallback. The sick service gets no load | The wait time ends, e.g. 30 seconds |
| Half-open | A few trial calls go through | They pass: closed. They fail: open again |

## 4. Which failures to retry

| You got | Retry? | Then |
|---|---|---|
| A timeout, 500, 502, 503 | Yes, if the call is safe to repeat | Back off, at most 3 attempts |
| 429 Too Many Requests | Yes, after `Retry-After` | Slow down; do not hammer |
| 400, 401, 403, 404, 409, 422 | No | The request is wrong. Fix it, or sign in again |

## 5. Idempotency for POST

- The client sends a unique `Idempotency-Key` with the request.
- The server stores key and result under a **unique constraint**, in the same transaction as the change.
- A repeat with the same key returns the stored result, so a lost response never becomes a second charge.

## 6. Bulkheads

- Give each dependency its **own pool** of threads or connections.
- A slow fare service then uses up its own pool, not the pool login needs.
- Shed load early: a fast "try later" beats a slow failure.

## 7. In Spring Boot (Resilience4j)

```java
@CircuitBreaker(name = "fare", fallbackMethod = "standardFare")   // opens when calls keep failing
@Retry(name = "fare")                                             // attempts and backoff: application.yml
public Fare estimate(Trip trip) { return fareClient.get(trip); }  // the HTTP client sets both timeouts
Fare standardFare(Trip trip, Throwable cause) { return Fare.standard(); }

# application.yml
resilience4j:
  circuitbreaker.instances.fare: { slidingWindowSize: 20, failureRateThreshold: 50,
                                   waitDurationInOpenState: 30s }
  retry.instances.fare: { maxAttempts: 3, waitDuration: 100ms, enableExponentialBackoff: true,
                          enableRandomizedWait: true }
```

## 8. Mistakes with failure handling

- Retrying a POST that is not idempotent. That is a double charge.
- Retrying with no backoff. Your retries finish off a service that was only slow.
- A breaker with no fallback. Decide what the user sees when it opens.
- The same timeout at every layer. The outer call gives up just as the inner one answers.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
