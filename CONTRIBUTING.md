# Contributing

Thanks for reading the sheets.

## Found a mistake?

Open an issue with:

- the sheet number and the heading you were reading,
- what the sheet says,
- what it should say, and why.

Corrections to facts, numbers, Java APIs or Spring Boot behaviour are the most valuable thing you can
send. Please include a source or a short reproduction when the claim is testable.

## Improving the Java

The three files in `java/` must stay **dependency-free and runnable with a single command** on Java 21:

```bash
java java/TokenBucket.java
```

Any pull request that changes them should keep that true, keep the self-checking `main`, and print
the property it verifies. No Maven, no Gradle, no Spring on the classpath — those would stop the files
from being copy-pasteable.

## Translating a sheet

Translations are welcome as a new file, for example `sheets/09-rate-limiting.hi.md`, keeping the
original attribution line. The English sheet stays the source of truth.

## What will be declined

- Re-selling the sheets or packing them into a paid product.
- Rewriting a sheet in a different voice or structure (the pages are designed as a set; see
  `LICENSE-CONTENT.md`).
- Adding a build system or third-party dependency to `java/`.

## Licence of contributions

By opening a pull request you agree that

- code you add to `java/` is contributed under the MIT licence, and
- sheet text you add is contributed under CC BY-NC-ND 4.0, with DigitalYogi Lab (OPC) Private
  Limited as the attributed publisher.
