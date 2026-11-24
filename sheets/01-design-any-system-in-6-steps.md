# Design any system in 6 steps

*A repeatable order for any design discussion: what to ask, what to estimate, what to draw, and what to check before you call it done.*

## 1. The six steps, in order

| Step | What you do | Say it out loud |
|---|---|---|
| 1. Scope | List the 3 to 5 features that matter. Park the rest | "Which actions must work on day one?" |
| 2. Numbers | Users, requests per second, data per year, reads against writes | "Roughly how many a day, and at the busiest hour?" |
| 3. API | The 3 to 5 calls: method, path, what goes in, what comes back | "This is what the client sends." |
| 4. Data | Entities, keys and the queries each screen runs. Then pick the store | "This query decides the database." |
| 5. Draw | Client, load balancer, service, cache, database, queue | "Let me follow one request through." |
| 6. Stress it | Find the bottleneck, the single point of failure and the hot key | "What breaks first at 10 times the load?" |

## 2. Questions to ask first

- Who uses it, and how many at the **busiest hour**?
- Mostly **reads** or mostly **writes**?
- How fresh must data be: a few seconds old, or exact?
- What must **never** happen? A double charge, a lost order.
- How long do we keep the data?

## 3. Name two targets

**Functional** is what the system does: post, search, pay. **Non-functional** is how well: speed, uptime, correctness, cost.

- Speed: "p99 under 200 ms" (99 in 100 requests are faster).
- Uptime: "99.9% available", which allows about 43 minutes down a month.

A design with no targets cannot be wrong, so it cannot be right.

## 4. The default first drawing

```
Client -> CDN                (static files)
Client -> Load balancer -> App   (stateless)
App -> Cache                 (Redis)
App -> Database              (leader, replicas)
App -> Queue -> Workers      (slow jobs)
```

Stateless servers let you add more. The cache takes repeated reads. The queue takes slow work. Replicas take reads.

## 5. Stress your own design

- What if **this box dies**? If everything stops, it is a single point of failure.
- What if traffic grows **10 times**? Which box fills first?
- What if **one key** gets most of the traffic?
- What if a message arrives **twice**, or a request is retried?
- What if the network between two services turns slow?

## 6. A 45-minute design discussion

| Minutes | Spend them on |
|---|---|
| 0 to 5 | Scope and questions |
| 5 to 10 | Numbers |
| 10 to 20 | API and data |
| 20 to 35 | The drawing, one request at a time |
| 35 to 45 | Stress it, and state the trade-offs |

## 7. What good sounds like

- You ask before you draw.
- Every box has a reason you can say in one line.
- You follow **one request** end to end, then a second.
- You find the weak point yourself, before anyone asks.
- You change your mind when a number says so.

## 8. The trade-off sentence

"I chose **X** over **Y** because we need **A** more than **B**. The cost is **C**. I would revisit it when **D**."

Example: "I chose a 60-second cache over always reading the database, because page speed matters more than exact stock on the listing. The cost is stale counts for a minute. Checkout still reads the database."

## 9. Mistakes that sink a design

- Drawing boxes before asking questions. You solve the wrong problem neatly.
- No numbers anywhere. Nobody can tell whether one server or a hundred is right.
- Picking the database before writing the queries.
- Adding Kafka, Redis and microservices with no stated reason.
- Designing for 100 million users when the brief said 10,000.
- Never saying a trade-off out loud. Every choice has a cost; name it.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
