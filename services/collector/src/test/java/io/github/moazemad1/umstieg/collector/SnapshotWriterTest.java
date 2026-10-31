package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SnapshotWriterTest {

    private static final Instant FETCHED_AT = Instant.parse("2026-10-30T18:07:21.500Z");

    @TempDir
    Path dataDir;

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void storesGzippedBodyUnchangedWithSidecar() throws IOException {
        byte[] body = Feeds.gzip(Feeds.feedWithTrips(FETCHED_AT, 2));
        var result = Feeds.ok("staging", FETCHED_AT, body, true);

        Path sidecar = new SnapshotWriter(dataDir, json).write(result, FeedSummary.of(result));

        Path dir = dataDir.resolve("staging/2026/10/30");
        assertThat(sidecar).isEqualTo(dir.resolve("180721.json"));
        assertThat(Files.readAllBytes(dir.resolve("180721.pb.gz"))).isEqualTo(body);

        JsonNode meta = json.readTree(sidecar.toFile());
        assertThat(meta.get("status").asInt()).isEqualTo(200);
        assertThat(meta.get("entityCount").asInt()).isEqualTo(2);
        assertThat(meta.get("etag").asString()).isEqualTo("\"etag-1\"");
        assertThat(meta.get("snapshotFile").asString()).isEqualTo("180721.pb.gz");
        assertThat(meta.get("fetchedAt").asString()).isEqualTo("2026-10-30T18:07:21.500Z");
    }

    @Test
    void gzipsPlainBodyBeforeStoring() throws IOException {
        byte[] plain = Feeds.feedWithTrips(FETCHED_AT, 1);
        var result = Feeds.ok("staging", FETCHED_AT, plain, false);

        new SnapshotWriter(dataDir, json).write(result, FeedSummary.of(result));

        byte[] stored = Files.readAllBytes(dataDir.resolve("staging/2026/10/30/180721.pb.gz"));
        assertThat(stored).isEqualTo(Feeds.gzip(plain));
    }

    @Test
    void notModifiedWritesOnlySidecar() throws IOException {
        var result = Feeds.status("staging", FETCHED_AT, 304);

        new SnapshotWriter(dataDir, json).write(result, FeedSummary.of(result));

        assertThat(filesIn(dataDir.resolve("staging/2026/10/30"))).containsExactly("180721.json");
    }

    @Test
    void failureWritesSidecarWithError() throws IOException {
        Path sidecar = new SnapshotWriter(dataDir, json)
                .writeFailure("production", FETCHED_AT, new IOException("connection reset"));

        JsonNode meta = json.readTree(sidecar.toFile());
        assertThat(meta.get("error").asString()).contains("connection reset");
        assertThat(meta.get("status").isNull()).isTrue();
    }

    @Test
    void neverOverwritesWithinTheSameSecond() throws IOException {
        var writer = new SnapshotWriter(dataDir, json);
        var result = Feeds.status("staging", FETCHED_AT, 304);

        writer.write(result, FeedSummary.NONE);
        writer.write(result, FeedSummary.NONE);

        assertThat(filesIn(dataDir.resolve("staging/2026/10/30"))).containsExactly("180721-2.json", "180721.json");
    }

    private static List<String> filesIn(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }
}
