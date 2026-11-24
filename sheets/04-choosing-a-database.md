# Choosing a database

*The brand matters less than the query. Write the three queries your screens run, then pick the store that answers them cheaply.*

## 1. Four stores, side by side

|   | Postgres / MySQL | MongoDB | Cassandra | Redis |
|---|---|---|---|---|
| Model | Tables, rows, joins | JSON-like documents | Wide rows under a partition key | Keys and values in memory |
| Best at | Transactions and any query later | Nested data read as a whole | Huge write volume, always on | Sub-millisecond lookups, counters |
| Weak at | Writes beyond one leader | Joins across collections | Ad hoc queries and joins | Data larger than memory |
| Consistency | Strong (ACID) | Strong on the primary | Tunable per query | Replicas can lag |
| Scaling | Read replicas; sharding is extra work | Built-in sharding | Add nodes; no leader | Cluster mode shards by key |
| Typical use | Orders, payments, users | Catalogues, content, profiles | Chat history, events, time series | Cache, sessions, rate limits |

## 2. Ask the query, not the brand

- Needs **joins or money**? Relational.
- Read **the whole thing by one ID**? Document.
- **Billions of rows**, written fast, read by one key and time? Wide-column.
- The **same answer** asked again and again? A cache in front.

## 3. Start here

**Default to Postgres.** Move a workload out only when you can name the query it cannot serve.

- `JSONB` columns cover many document needs.
- Add read replicas before anything more exotic.
- One well-indexed table handles hundreds of millions of rows.

## 4. Add-ons, not replacements

| Need | Add |
|---|---|
| Text search | Elasticsearch or OpenSearch |
| Analytics over history | A column store such as ClickHouse or BigQuery |
| Files and images | Object storage such as S3 |
| Vector search | `pgvector` first, a vector database later |

The relational database stays the source of truth. The add-on is fed from it.

## 5. SQL and NoSQL in one line each

- **SQL:** design the schema first; ask any question later.
- **NoSQL:** decide the questions first; shape the data to fit them.
- Changing the question later is cheap in SQL and costly in Cassandra.

## 6. Worked picks

| Workload | Pick | Because |
|---|---|---|
| Payments ledger | Postgres | Needs transactions and exact totals |
| Product catalogue with varied attributes | Postgres with `JSONB`, or MongoDB | Each product reads as one document |
| Chat messages, billions, by conversation and time | Cassandra | Write-heavy, one known query |
| Login sessions | Redis | Small, short-lived, read on every request |
| Search box with typos and ranking | Elasticsearch, fed from Postgres | Relevance search is its job |

## 7. Mistakes when choosing

- Choosing by popularity. "Big companies use it" says nothing about your queries.
- MongoDB for "no schema", then joins in application code.
- Cassandra before the queries are known. Its tables are built per query.
- Money in a store without transactions.
- A database per service on day one with three developers.
- No index on the column you filter by. Check that before blaming the database.

---
DigitalYogi · System design and AI for backend developers · learn.digitalyogi.tech/kb-pro
