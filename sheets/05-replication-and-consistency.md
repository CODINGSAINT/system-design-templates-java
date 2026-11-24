# Replication and consistency

*Copies keep you online and spread reads. They also mean a user can read data that is a moment behind. Decide where that is acceptable.*

## 1. Three ways to keep copies

|   | Leader and followers | Several leaders | Leaderless (quorum) |
|---|---|---|---|
| Writes go to | One leader | Any leader | Any node |
| Write conflicts | None: one writer | Possible; needs a merge rule | Possible; resolved by version |
| Read your own write | Yes, from the leader | Not guaranteed | Yes when W + R > N |
| Found in | Postgres, MySQL, MongoDB | Multi-region set-ups | Cassandra, Dynamo-style stores |

## 2. Wait for the copy, or not

- **Synchronous:** the leader waits for a follower. No write is lost on failover. Writes are slower, and a slow follower slows everyone.
- **Asynchronous:** the leader does not wait. Fast. A crash can lose the last writes, and followers lag.
- Common middle: **one** synchronous follower, the rest asynchronous.

## 3. The lag bug users notice

A user saves a new address. The page reloads from a follower. The **old** address shows.

- Read from the **leader** for a few seconds after that user's write.
- Or pin that user's reads to the leader for the session.
- Or pass the write's position and wait until the follower has it.

## 4. The quorum rule

**N** copies. A write needs **W** acknowledgements. A read asks **R** copies.

If **W + R > N**, every read overlaps the newest write.

- N = 3, W = 2, R = 2 is the usual choice.
- W = 3: nothing is lost, but one node down blocks writes.
- W = 1: fast, and a read can be stale.

## 5. Failover

- Detect a dead leader by timeout. Too short gives false alarms; too long gives downtime.
- Promote the follower that is **most up to date**.
- Guard against **two leaders** at once: fence the old one before it accepts writes.
- With asynchronous copies, the newest writes may be gone. Decide if that is acceptable.

## 6. Consistency levels, in plain words

| Level | What a reader sees | Fits |
|---|---|---|
| Strong | Always the newest write | Balances, stock, bookings |
| Read-your-writes | Your own changes at once; others' a moment later | Profiles, settings, posts |
| Eventual | Copies agree once writes stop | Likes, view counts, feeds |

## 7. In Spring Boot

```java
@Transactional(readOnly = true)                        // may be routed to a replica: fine for lists and search
public List<Order> history(long customerId) { return orders.findByCustomer(customerId); }

@Transactional                                         // primary: every write, and any read that follows a write
public Order place(OrderRequest request) { return orders.save(Order.from(request)); }
```

## 8. CAP, in a form you can use

When the network splits, a node must choose: **refuse** the request, or **answer with data that may be old**. Money and stock: refuse. Feeds, likes and view counts: answer. With no split, the trade is speed against freshness.

## 9. Mistakes with replicas

- Reading from a replica straight after a write.
- Treating replicas as backups. A wrong `DELETE` is copied too.
- No alert on lag. You learn about it from users.
- Failover that was never rehearsed.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
