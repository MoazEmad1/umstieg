# 0001: Java 21, Spring Boot and Gradle for ingestion; Python from Phase 2

Status: Accepted · 2026-10-30

## Context
Phase 1 is plumbing: polling HTTP, decoding protobuf, writing to Kafka and Postgres. That's where I'm strongest, in Java and Spring Boot. Phase 2 is model training, where Python has the better libraries. A maintained Java binding for GTFS-RT exists (`org.mobilitydata:gtfs-realtime-bindings`).

## Decision
I use Java 21 and Spring Boot for the ingestion services, built with Gradle as a multi-module project. Python comes in only in Phase 2, for training and evaluation. The two sides share data through Postgres, not code.

## Alternatives
- All Python: one language, but a weaker result on the part I know best.
- Java 25: not needed yet, and easy to upgrade later.
- Maven: would work just as well. I prefer Gradle for multi-module builds.

## Consequences
From Phase 2 there are two languages and two CI jobs. The tables Python reads need to be documented.
