# Consistent hashing

*Add or remove a server and move only a small slice of keys, instead of nearly all of them.*

## 1. The problem with modulo

```
server = hash(key) % N
```

Go from 4 servers to 5 and about **4 in 5 keys** map to a different server.

- A cache goes cold and the database takes every read.
- A sharded store has to move most of its data.
- In general, N to N+1 moves about N in N+1 keys.

## 2. The ring

- Hash servers **and** keys onto the same circle.
- A key belongs to the **first server clockwise** from it.
- Add a server: only the keys between it and the previous server move. From 4 to 5, about **1 in 5**.
- Remove a server: only its keys move, to the next one along.

## 3. Modulo against the ring

|   | hash % N | Ring |
|---|---|---|
| Keys moved, 4 to 5 servers | ~80% | ~20% |
| Lookup | One calculation | Binary search over sorted positions |
| Even spread | Yes | Needs virtual nodes |
| Fits | A fixed pool that never changes | Caches and stores that grow and shrink |

## 4. Virtual nodes

- With one position each, servers get uneven slices.
- Give each server **many positions**, often 100 to 200.
- Slices become close to even. Not perfect; close.
- A bigger machine can take more positions.
- When a server leaves, its load spreads over many servers, not one neighbour.

## 5. Copies on the ring

- Store each key on the next **R distinct servers** clockwise. R = 3 is common.
- Lose one server and the next still has the key.
- Skip virtual nodes of the same machine: copies must sit on different machines.
- Found in Cassandra's token ring, Dynamo-style stores and cache clients.

## 6. A ring in Java

```java
TreeMap<Long, String> ring = new TreeMap<>();

void add(String server) {
    for (int i = 0; i < 150; i++) ring.put(hash(server + "#" + i), server);     // 150 virtual nodes
}

String serverFor(String key) {
    Map.Entry<Long, String> e = ring.ceilingEntry(hash(key));                   // first position clockwise
    return (e != null ? e : ring.firstEntry()).getValue();                      // wrap around past the end
}
```

Use a well-mixed hash such as MurmurHash. `String.hashCode()` clusters.

## 7. Where you meet it

| System | How it places keys |
|---|---|
| Cassandra | A token ring; each node owns several ranges of it |
| Redis Cluster | 16,384 fixed hash slots moved between nodes: a different mechanism, the same aim |
| Cache client libraries | A ring inside the client, so every app server agrees where a key lives |
| Load balancers | A hash of the user or session, so one user keeps the same server |

## 8. Mistakes with rings

- No virtual nodes. One server ends up with twice the load of another.
- A weak hash. Positions bunch together on the ring.
- Copies on the same machine. One failure takes all of them.
- Expecting zero movement. The ring limits how much moves; it does not remove it.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
