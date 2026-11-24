# Message queues: Kafka vs RabbitMQ vs SQS

*Three tools that all "send messages". They keep, order and deliver them very differently. Pick by what happens to a message after it is read.*

## 1. Side by side

|   | Kafka | RabbitMQ | Amazon SQS |
|---|---|---|---|
| What it is | A distributed, append-only **log** | A **broker**: exchanges and queues | A fully managed **queue** |
| After a message is read | Stays until retention ends. **Replayable** | Removed once acknowledged | Removed once deleted |
| Order | Per partition | Per queue, with one consumer | FIFO queues: per message group |
| Several independent readers | Built in: an offset per consumer group | A queue per reader (fanout exchange) | A queue per reader (through SNS) |
| Routing | The key picks the partition | Direct, topic, fanout, headers | None |
| Scale and operate | Add partitions. You run a cluster | Add consumers. You run a broker | Automatic. Nothing to run |
| Reach for it when | Event streams, feeds, CDC, analytics | Task queues, routing, priorities, delays | Background jobs on AWS |

## 2. Pick one in 20 seconds

- Need **replay**, or several teams reading the same events? **Kafka.**
- Need **routing, priorities or delayed retries** with little code? **RabbitMQ.**
- On AWS and want **no servers to run**? **SQS.** Use FIFO when order matters per customer.
- One service handing jobs to one worker pool? Any of the three. Use what your team already runs.

## 3. Delivery guarantees

| Guarantee | Meaning |
|---|---|
| At most once | May lose a message. Never repeats one |
| At least once | Never loses one. **May repeat** one. Design for this |
| Exactly once | Holds only inside one system. A database write or an API call is outside it |

**Rule:** make the consumer idempotent. Save the message ID under a unique key in the **same transaction** as the work, and skip IDs you have seen.

## 4. Order costs throughput

- **Kafka:** same key, same partition, same order. Parallelism is capped by the partition count.
- **RabbitMQ:** strict order needs a single active consumer on the queue.
- **SQS FIFO:** order holds inside one `MessageGroupId`. Use the customer or order ID.
- Ask first: does order matter **globally**, or only **per customer**? It is almost always per customer.

## 5. When processing fails

- Acknowledge **after** the work is done, never before.
- Retry with backoff, then move the message to a **dead-letter queue** after N attempts.
- SQS: set the visibility timeout **longer** than your slowest job, or the job runs twice.
- Kafka: alert on **consumer lag**. A growing lag means consumers cannot keep up.

## 6. In Spring Boot

```java
@KafkaListener(topics = "orders", groupId = "billing")   // the group id decides who shares the work
void onOrder(OrderEvent e) { billing.charge(e); }
@RabbitListener(queues = "email.send")                    // the exchange and binding fill this queue
void onEmail(EmailJob job) { mailer.send(job); }
@SqsListener("invoice-jobs")                              // Spring Cloud AWS: deleted only on a normal return
void onInvoice(InvoiceJob job) { invoices.build(job); }
```

## 7. Mistakes that cost a weekend

- Adding a queue to hide a slow database. The backlog only grows. Fix the consumer's speed first.
- Assuming Kafka keeps global order. Order holds inside one partition only.
- No dead-letter queue. One bad message is retried forever and blocks the rest.
- A consumer that is not idempotent. At-least-once delivery then means a double charge or a double email.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
