# Numbers for capacity estimates

*Rough numbers are enough to choose between one server and fifty. Round hard, say your assumptions, and keep moving.*

## 1. How long things take, roughly

| Operation | Roughly | So |
|---|---|---|
| Read from memory | ~100 ns | Effectively free |
| SSD random read | ~0.1 ms | About 1,000 times slower than memory |
| Round trip inside one data centre | ~0.5 ms | Every extra service call costs about this |
| Redis GET over the network | ~0.5 to 1 ms | The network is the cost, not Redis |
| Indexed database query | ~1 to 10 ms | A cache hit skips this |
| Round trip across continents | ~150 to 250 ms | Put data near users, or cache at the edge |

Orders of magnitude, not benchmarks. Measure your own system before you promise a number.

## 2. Time and traffic

- 1 day = 86,400 seconds. Use **100,000** for quick sums.
- 1 million requests a day is about **12 a second**.
- 100 million a day is about **1,200 a second**.
- The peak is often **2 to 5 times** the average. Say which you assumed.

## 3. Sizes worth remembering

| Thing | Size |
|---|---|
| `long`, timestamp | 8 bytes |
| UUID | 16 bytes (36 as text) |
| A typical row | ~200 bytes to 1 KB |
| A compressed photo | ~200 KB to 2 MB |
| 1 TB | 1,000 GB = a billion 1 KB rows |

## 4. Worked example: a URL shortener

1. **Writes:** 100 million new links a month, over 2.6 million seconds, is about **40 a second**.
2. **Reads:** 100 reads for every write is about **4,000 a second**. That is a caching problem, not a write problem.
3. **Storage:** 500 bytes a link times 100 million is **50 GB a month**, 600 GB a year, **3 TB in 5 years**.
4. **Code length:** 7 characters of base 62 give about **3.5 trillion** codes. Five years needs 6 billion. Seven is plenty.

## 5. Uptime, in minutes

| Target | Allowed downtime a year |
|---|---|
| 99% | 3.65 days |
| 99.9% | 8.8 hours |
| 99.99% | 53 minutes |
| 99.999% | 5 minutes |

Two services in a chain multiply: 99.9% times 99.9% is 99.8%.

## 6. What one box can do

- Thread-per-request server: **threads divided by seconds per request**. 200 threads at 100 ms each is at most 2,000 requests a second.
- One Redis node: ~100,000 simple operations a second.
- Connections cap you too: a pool of 20 at 10 ms a query serves ~2,000 queries a second.

## 7. Powers of two

| Power | About | Name |
|---|---|---|
| 2^10 | 1 thousand | KB |
| 2^20 | 1 million | MB |
| 2^30 | 1 billion | GB |
| 2^40 | 1 trillion | TB |

A Java `int` ID runs out near 2 billion. Use a `long`.

## 8. From numbers to a design

- **40 writes a second:** one database leader is plenty.
- **4,000 reads a second, same few keys:** cache, then read replicas.
- **3 TB in 5 years:** fits one large node; plan archiving, not sharding.
- **Bandwidth:** 4,000 reads times 500 bytes is 2 MB a second. Not a concern.

## 9. Mistakes with numbers

- False precision. "38.58 writes a second" hides the point. Say 40.
- Average only. Systems fall over at the peak, not at the average.
- Storage without growth. Estimate year one and year five.
- Silent assumptions. Say "I am assuming 100 reads per write" so it can be corrected.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
