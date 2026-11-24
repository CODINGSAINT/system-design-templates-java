# Caching with Redis

*A cache speeds up reads of data that many people ask for and that may be a little old. It does nothing for writes, and it must never decide money.*

## 1. When a cache helps

| Helps | Does not help |
|---|---|
| Many people **reading** the same thing: a product page, a profile, config | Many people **writing** at once: checkout, counters |
| Data that may be a few seconds old | Data that must be exact now: price at payment, stock, balance |
| Slow or costly queries repeated often | Data each user reads once |

## 2. Read: cache-aside

```
value = redis.GET(key)
if value != null: return value   # hit
value = db.query(key)            # miss
redis.SET(key, value, EX = 60)   # copy + expiry
return value
```

The app talks to Redis and to the database. Clients never talk to Redis directly.

## 3. Write: update, then delete

```
db.update(key, value)
redis.DEL(key)        # the next read refills it
```

- **Delete**, do not overwrite: it avoids storing a value another update just replaced.
- Always keep a **TTL** as the safety net for a missed delete.

## 4. Memory full: which key goes?

| Policy | Removes |
|---|---|
| `allkeys-lru` | The key unused for longest |
| `allkeys-lfu` | The key used least often |
| `volatile-ttl` | The key closest to expiry |
| `noeviction` | Nothing. Writes fail. This is the default |

LRU can push out a popular key during a burst of one-off reads. LFU keeps what is used most.

## 5. Many misses at once

- A popular key expires and thousands of requests rebuild it together.
- Let **one** request rebuild it; the rest wait or get the old copy.
- Add random **jitter** to TTLs so keys do not expire together.
- Warm the cache **before** a sale, not during it.

## 6. In Spring Boot

```java
@Cacheable(cacheNames = "product", key = "#id")                    // read through the cache
public Product find(long id) { return repository.findById(id).orElseThrow(); }

@CacheEvict(cacheNames = "product", key = "#product.id")            // delete the copy on change
public Product updatePrice(Product product) { return repository.save(product); }

# application.properties:  spring.cache.redis.time-to-live=60s
```

## 7. Picking an expiry

| Data | Illustrative TTL |
|---|---|
| Product page | ~60 seconds |
| User profile | ~5 to 15 minutes |
| Feature flags, config | ~1 to 5 minutes |
| Price at payment, stock, balance | Do not cache. Read the database |

Ask: how old can this be before a user is harmed? That is the TTL.

## 8. Other patterns, in one line

- **Read-through:** the cache library loads from the database for you.
- **Write-through:** every write goes to the cache and the database together.
- **Write-behind:** write to the cache now, the database later. Fast, and a crash can lose data.
- Cache-aside stays the default: simplest, and safe when Redis is down.

## 9. Mistakes with caches

- Caching what must be exact. At payment, check the price and reserve stock in one database step.
- No expiry. One missed delete then serves old data forever.
- Caching "not found" forever. Cache it briefly, or a missing ID hits the database on every call.
- Huge values under one key. A 5 MB value makes every read slow.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
