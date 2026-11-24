# 45-Minute High-Level System Design Answer Template

Use this exact sequence for any HLD interview or architecture design doc. Spend **no more than 5 minutes** on Step 1 before locking scope.

---

## 1. Scope & Non-Functional Requirements (Minutes 0–5)
- **Core user flows (pick top 2–3):**
  1. 
  2. 
  3. 
- **Out of scope (state explicitly):**
- **Scale & SLA targets:**
  - Read / Write ratio:
  - Peak QPS (Write / Read):
  - Latency target (p99):
  - Consistency model (Strong vs Eventual — where does money/inventory live?):
  - Durability / Availability target (e.g., 99.9% = 8.7 hrs/yr vs 99.99% = 52 mins/yr):

## 2. Back-of-the-Envelope Capacity Math (Minutes 5–9)
- **Traffic:** `DAU × actions/day ÷ 86,400 = avg QPS` → `Peak = 3x–5x avg`
- **Storage (5-year):** `writes/day × payload bytes × 365 × 5`
- **Bandwidth:** `Peak QPS × avg response KB`
- **Cache memory (80/20 rule):** `20% of daily reads × entry size`

## 3. Core API & Data Contracts (Minutes 9–15)
- **Endpoints / RPCs (include idempotency key on writes!):**
  - `POST /v1/...` (`Idempotency-Key` header, request/response JSON)
  - `GET /v1/...` (cursor pagination, never `OFFSET` on deep feeds)
- **Database Choice & Schema:**
  - Primary store + why (Postgres / Cassandra / DynamoDB / MongoDB)
  - Primary key, partition/shard key, critical secondary indexes

## 4. High-Level Architecture (Minutes 15–25)
- **Write Path:** Client → Edge/API Gateway (Auth + Rate Limit) → Stateless Service → DB / Event Outbox → Async Workers
- **Read Path:** Client → CDN (static/media) → API Gateway → Cache (Redis) → Read Replicas / Search Index

## 5. Deep Dive: Bottlenecks & Failure Modes (Minutes 25–38)
Pick the **2 hardest problems** in this specific system and defend your fix:
- [ ] **Hot key / Cache stampede:** Single-flight lock + jittered TTL + negative caching
- [ ] **Concurrency / Double-booking / Double-charge:** DB unique constraint / optimistic locking (`version`) / distributed lock + fencing token
- [ ] **Downstream slowness:** Bounded timeout + exponential backoff with jitter + circuit breaker + bulkhead
- [ ] **Async delivery:** Transactional outbox + Kafka/SQS consumer idempotency + Dead-Letter Queue (DLQ)
- [ ] **Backpressure / Spikes:** Token-bucket rate limiting at gateway + queue buffering

## 6. Observability, Trade-offs & Closing (Minutes 38–45)
- **Key metrics & alerts:** p99 latency, error rate (5xx), queue lag, cache hit ratio, DB connection pool saturation
- **What I traded away:** (e.g., accepted 200ms eventual consistency on read replicas to keep p99 < 50ms)
- **If traffic 10x's next year:** (next single bottleneck to split/shard)
