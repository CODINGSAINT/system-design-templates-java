# Rate limiting

*A limit protects the service from one noisy caller and tells clients when to come back. The algorithm decides how bursts are treated.*

## 1. Four algorithms

|   | Fixed window | Sliding window | Token bucket | Leaky bucket |
|---|---|---|---|---|
| How | Count per clock minute | Weighted count of this window and the last | Tokens refill steadily; a request takes one | A queue drains at a fixed rate |
| Bursts | Up to twice the limit across a boundary | Smoothed | Allowed, up to the bucket size | None: output is steady |
| State | One counter | Two counters | Tokens and a timestamp | A queue |
| Fits | Daily or monthly quotas | Public APIs | Most APIs | Protecting a slow dependency |

## 2. Pick one

- **Default: token bucket.** Short bursts pass; the average is capped.
- A fragile dependency that needs an even flow? **Leaky bucket.**
- A plan quota such as "1,000 calls a day"? **Fixed window.**
- Fixed windows per minute are fine for a start. Know the boundary burst.

## 3. Where, and by what key

- At the **gateway** for abuse; in the **service** for business rules.
- Key by **API key or user ID** first.
- Use the IP address only as a fallback: offices and mobile networks share one.
- Costly endpoints get their own, tighter limit. Login is not search.

## 4. With many app servers

- A counter in each server's memory multiplies the limit by the server count.
- Keep the counters in **Redis**, shared by all servers.
- Make check-and-decrement **one atomic step**, with a Lua script.
- If Redis is down: let most traffic through, and refuse on login and payments.

## 5. Tell the client

- Status **429 Too Many Requests**.
- Header `Retry-After: 2` so clients know when to return.
- Headers for the limit, what remains and when it resets.
- Clients should back off, not retry at once.

## 6. Token bucket, step by step

```java
tokens = min(capacity, tokens + (now - last_refill) * refill_per_second)   # top up for the time that passed
last_refill = now
if tokens >= 1:  tokens = tokens - 1;  allow
else:            reject with 429
# With Redis, run all of this as ONE Lua script, so two servers cannot spend the same token
```

## 7. In Spring Boot (Bucket4j)

```java
// bursts of up to 20, then 10 a second
Bucket bucket = Bucket.builder()
    .addLimit(limit -> limit.capacity(20).refillGreedy(10, Duration.ofSeconds(1)))
    .build();
if (!bucket.tryConsume(1)) {
    return ResponseEntity.status(429).header("Retry-After", "1").build();
}
```

One bucket per key. Bucket4j can keep buckets in Redis. Spring Cloud Gateway has a `RequestRateLimiter` filter; Resilience4j has `@RateLimiter`.

## 8. Mistakes with limits

- Counters in local memory behind a load balancer.
- Limiting by IP address only. One office gets blocked together.
- No limit on the expensive endpoint.
- A 429 with no `Retry-After`. Clients guess, and guess badly.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
