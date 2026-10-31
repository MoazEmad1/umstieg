# Decisions

Short records of why things are the way they are. I don't add a new tool or service without writing one first.

- [0001](0001-java-spring-boot-gradle.md) Java 21, Spring Boot and Gradle for ingestion; Python from Phase 2
- [0002](0002-official-vbb-gtfs-rt-feed.md) Use the official VBB GTFS-RT feed
- [0003](0003-raw-snapshot-collector-first.md) Build a raw snapshot collector first
- [0004](0004-kafka-one-record-per-trip-update.md) One Kafka record per TripUpdate
- [0005](0005-postgres-change-log-storage.md) Store changes, not every poll
- [0006](0006-archive-all-static-gtfs.md) Keep every static GTFS version
- [0007](0007-grafana-for-charts.md) Grafana for charts
- [0008](0008-staleness-alert.md) Alert when the feed goes stale
- [0010](0010-rclone-archive-sync.md) Copy raw files to object storage with rclone
