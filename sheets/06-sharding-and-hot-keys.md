# Sharding and hot keys

*Sharding splits one database into several so writes can spread. It is the hardest change to undo, so it comes last.*

## 1. Before you shard

1. Add the missing **index**.
2. **Cache** the repeated reads.
3. Add **read replicas**.
4. Move to a **bigger machine**.
5. **Archive** old rows.
6. Then shard.

Each earlier step is cheaper and reversible.

## 2. Signs it is time

- Writes saturate the leader even after tuning.
- Indexes no longer fit in memory.
- Backup and restore take longer than you can afford.
- One tenant's load hurts every other tenant.

## 3. Three ways to split

|   | By range | By hash | By directory |
|---|---|---|---|
| How | Key ranges: A to M, or one shard a month | `hash(key)` picks the shard | A lookup table maps key to shard |
| Good | Range scans stay on one shard | Even spread | Move any tenant at any time |
| Bad | Hot ranges: the newest month takes every write | Range queries touch every shard | The lookup is an extra hop and must not fail |
| Use for | Time series with archiving | Users and orders by ID | Multi-tenant products |

## 4. Choosing the shard key

- It appears in **almost every query**.
- It has **many distinct values**.
- It spreads **writes** evenly.
- It rarely changes.
- Good: `customer_id` for orders. Weak: `status`, `country`, or a date on its own.

## 5. The hot key

One key takes most of the traffic: comments on one final-day match. **More shards do not help.** The same key still lands on one shard.

- **Split the key:** add a bucket, `match:42:0` to `match:42:9`, and merge on read.
- **Cache** the hot reads.
- Give the hot tenant **its own shard**.

Buckets spread the load, though not perfectly evenly, and a read now merges ten keys.

## 6. What gets harder

- **Joins across shards:** done in the app, or avoided by storing data together.
- **Transactions across shards:** avoid them, or use a saga.
- **Unique IDs:** use a generator that needs no central counter.
- **Adding a shard:** see consistent hashing, sheet 07.

## 7. Routing a query in Java

```java
int shard = Math.floorMod(Long.hashCode(customerId), SHARD_COUNT);   // same customer, same shard
DataSource ds = dataSources.get(shard);
// Spring: AbstractRoutingDataSource returns the DataSource for a key you hold per request
// Plain modulo moves most keys when a shard is added. Prefer a ring (sheet 07) or a directory
```

## 8. Mistakes with sharding

- Sharding at 50 GB. One node handles far more.
- Ranges on an auto-increment ID. The newest shard takes every write.
- A query without the shard key. It fans out to every shard.
- No plan for adding shards. You will need one.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
