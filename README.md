# System Design Templates (Java)

Twelve one-page system design cheat sheets for backend engineers, plus three dependency-free Java
reference implementations of the mechanisms the sheets describe.

Each sheet is a decision template: the question to ask, the numbers to quote, the trade-off to say
out loud, and the Spring Boot / Java 21 code that follows from it. Read one before an interview or a
design review. Nothing here is a 40-hour course; every sheet fits on one A4 page.

**Java 21 · no build tool required · no dependencies in the code samples.**

---

## The twelve sheets

| # | Sheet | What it settles |
|---|---|---|
| 01 | [Design any system in 6 steps](sheets/01-design-any-system-in-6-steps.md) | The order to answer in: scope, numbers, API, data, drawing, stress |
| 02 | [Numbers for capacity estimates](sheets/02-numbers-for-capacity-estimates.md) | Latency, QPS and storage arithmetic you can do in your head |
| 03 | [Caching with Redis](sheets/03-caching-with-redis.md) | Cache-aside, writes, eviction, expiry and stampedes |
| 04 | [Choosing a database](sheets/04-choosing-a-database.md) | Postgres vs MongoDB vs Cassandra vs Redis, driven by the query |
| 05 | [Replication and consistency](sheets/05-replication-and-consistency.md) | Leaders, followers, lag, quorums and failover |
| 06 | [Sharding and hot keys](sheets/06-sharding-and-hot-keys.md) | When to shard, how to pick the key, what to do with one hot key |
| 07 | [Consistent hashing](sheets/07-consistent-hashing.md) | Why modulo breaks, the ring, virtual nodes, ten lines of Java |
| 08 | [Message queues: Kafka vs RabbitMQ vs SQS](sheets/08-message-queues-kafka-rabbitmq-sqs.md) | What happens to a message after it is read |
| 09 | [Rate limiting](sheets/09-rate-limiting.md) | Four algorithms, where to enforce, token bucket in Spring Boot |
| 10 | [Timeouts, retries and circuit breakers](sheets/10-timeouts-retries-circuit-breakers.md) | Four habits that stop one slow service taking the rest down |
| 11 | [Real-time updates](sheets/11-real-time-updates.md) | Polling, SSE, WebSocket or webhook, and when each fits |
| 12 | [REST vs gRPC vs GraphQL](sheets/12-rest-vs-grpc-vs-graphql.md) | Which API style for which caller |

Printable PDFs of every sheet, plus the combined 13-page pack, are in [`pdf/`](pdf).

## Runnable Java

The sheets stay short by showing only the decisive lines. These three files are the full, tested
versions — the ones you would actually put in a service.

| File | Sheet | What it shows |
|---|---|---|
| [`java/ConsistentHashRing.java`](java/ConsistentHashRing.java) | 07 | Ring with virtual nodes; measures that adding a node moves ~25% of keys, not 75% |
| [`java/TokenBucket.java`](java/TokenBucket.java) | 09 | Thread-safe token bucket with an injectable clock and a `Retry-After` hint |
| [`java/CircuitBreaker.java`](java/CircuitBreaker.java) | 10 | Closed / open / half-open with a sliding failure ratio |

No Maven, no Gradle, no dependencies. Java 21 runs a single file directly, and each file asserts its
own behaviour:

```bash
java java/ConsistentHashRing.java
java java/TokenBucket.java
java java/CircuitBreaker.java
```

Each prints the numbers it measured and ends with `... OK`. If an assertion fails, the printed
ratios tell you which property broke.

## How to use these

1. Skim the six steps in sheet 01 before any design round; they keep you from drawing before asking.
2. Pull the two or three sheets whose component your design actually needs.
3. Say the trade-off out loud in the sentence from sheet 01 §8 — "I chose X over Y because A matters
   more than B; the cost is C; I would revisit it when D."
4. For practice, write your own answer first and compare it with a reference design afterwards.

## Who made this

DigitalYogi Lab (OPC) Private Limited builds interview practice for working Java and Spring Boot
backends — DSA patterns, HLD, LLD and Spring AI engineering, with AI grading against senior model
answers. The sheets are the reference; the practice app is at
[kb.digitalyogi.tech](https://kb.digitalyogi.tech/).

## Licence

- **Code** in `java/`: [MIT](LICENSE).
- **Sheets and PDFs** in `sheets/` and `pdf/`: [CC BY-NC-ND 4.0](LICENSE-CONTENT.md) — free to read,
  share, print and teach with, with attribution. Not for resale or rebranding.

Corrections are welcome; see [CONTRIBUTING.md](CONTRIBUTING.md).
