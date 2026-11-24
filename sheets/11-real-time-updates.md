# Real-time updates: polling, SSE or WebSocket

*Most "live" screens only need the server to push. Pick the lightest option that meets the delay users will accept.*

## 1. Four ways to get fresh data

|   | Short polling | Long polling | Server-Sent Events | WebSocket |
|---|---|---|---|---|
| How | Ask every N seconds | The request waits until there is news | One long response; the server pushes events | One upgraded connection; both sides send |
| Direction | Client pulls | Client pulls | Server to client | Both ways |
| Delay | Up to N seconds | Near instant | Near instant | Near instant |
| Cost | Many empty requests | One held request per client | One open connection per client | One open connection per client, with state |
| Fits | Dashboards refreshed every 30 s | A fallback | Live scores, order tracking, alerts | Chat, games, shared editing |

## 2. Pick one

- Only the **server pushes**? **SSE.** Plain HTTP, and the browser reconnects by itself.
- **Both sides** send, often? **WebSocket.**
- A 30-second delay is fine? **Poll**, and stop there.
- Server to server? A **webhook**: they call your URL. Verify the signature; expect repeats.

## 3. Scaling open connections

- A connection pins the user to **one server**.
- To reach a user on another server, publish through a shared channel such as Redis Pub/Sub or Kafka.
- Send a **heartbeat** about every 30 seconds, or proxies close idle connections.
- After a deploy every client reconnects at once. Spread it with random delay.

## 4. Do not lose events

- Give every event an **ID**.
- SSE: on reconnect the browser sends `Last-Event-ID`. Replay from there.
- WebSocket: the client sends the last ID it saw after reconnecting.
- Keep a short buffer of recent events to replay from.

## 5. Webhooks, briefly

- Answer fast with 200, then do the work from a queue.
- The sender **retries**, so the same event can arrive twice.
- Events can arrive out of order. Use the event's own timestamp or version.
- Check the signature header before trusting the body.

## 6. Worked picks

| Screen | Pick | Because |
|---|---|---|
| Live cricket score | SSE | Only the server has news; millions only listen |
| Food order tracking | SSE | One-way updates every few seconds |
| Chat | WebSocket | Both sides send, and often |
| Payment result after checkout | Webhook, then SSE to the browser | The payment provider tells your server first |

## 7. Server-Sent Events in Spring Boot

```java
@GetMapping(path = "/orders/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter track(@PathVariable String id) {
    SseEmitter emitter = new SseEmitter(0L);              // 0 = no timeout; send heartbeats yourself
    tracker.subscribe(id, update -> send(emitter, SseEmitter.event().id(update.id()).data(update)));
    return emitter;                                       // send(...) handles IOException
}
```

## 8. Mistakes with live updates

- WebSocket for one-way updates. SSE is simpler to run.
- Polling every second from a million clients.
- No heartbeat. Connections die silently behind a proxy.
- Assuming a webhook arrives once and in order.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
