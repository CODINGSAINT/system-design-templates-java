# REST vs gRPC vs GraphQL

*Three ways for programs to talk. Choose by who the caller is: the public, your own services, or a screen that needs many things at once.*

## 1. Side by side

|   | REST | gRPC | GraphQL |
|---|---|---|---|
| Shape | Resources and HTTP verbs | Remote method calls | One endpoint; the client writes the query |
| Format | JSON over HTTP | Protobuf (binary) over HTTP/2 | JSON over HTTP |
| Contract | OpenAPI, optional | A `.proto` file, required; generates code | A typed schema, required |
| Streaming | Limited | Built in, in both directions | Subscriptions |
| From a browser | Native | Needs gRPC-Web and a proxy | Native |
| Caching | HTTP caching and CDNs work | None built in | Harder: most calls are POST to one URL |
| Fits | Public APIs and CRUD | Calls between your own services | Screens that need many resources at once |

## 2. Pick one

- **Public API**, or outside developers? **REST.**
- **Service to service**, where speed and a strict contract matter? **gRPC.**
- Web and mobile clients each needing **different fields** from many resources? **GraphQL.**
- Mixing is normal: REST or GraphQL at the edge, gRPC inside.

## 3. REST essentials

- Nouns for resources, HTTP verbs for actions.
- PUT and DELETE are idempotent. POST is not, without an idempotency key.
- Status codes that mean it: 201 created, 400 bad input, 401 and 403, 404, 409 conflict, 429, 503.
- Page large lists with a **cursor**, not an offset.

## 4. gRPC essentials

- Four call types: unary, server stream, client stream, two-way stream.
- **Never reuse or renumber a field number.** Add fields; do not change types.
- Set a **deadline** on every call and pass it down the chain.
- Connections are long-lived, so load balancing must work per request.

## 5. GraphQL essentials

- **N+1:** one extra query per item. Batch with a DataLoader.
- Limit query **depth and cost**, or one query can stall the server.
- Errors come back with HTTP 200. Check the `errors` field.
- Persisted queries help caching and keep unknown queries out.

## 6. Change without breaking callers

- **Add** fields. Never remove or rename one that callers read.
- A new required input is a new version.
- Announce removals with a date, and watch who still calls the old one.
- Keep a contract test per consumer, so a breaking change fails your build.

## 7. The same read, three ways, in Spring

```java
@GetMapping("/orders/{id}")  Order get(@PathVariable long id)  { return orders.find(id); }   // REST: Spring MVC
@QueryMapping                Order order(@Argument long id)     { return orders.find(id); }   // Spring for GraphQL
public void getOrder(OrderRequest req, StreamObserver<OrderReply> out) {                      // gRPC service
    out.onNext(toReply(orders.find(req.getId())));
    out.onCompleted();
}
```

## 8. Mistakes with APIs

- GraphQL for simple CRUD with one client.
- gRPC called straight from a browser.
- Twelve REST calls to draw one screen. Add an endpoint shaped for the screen.
- 200 OK with an error in the body.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
