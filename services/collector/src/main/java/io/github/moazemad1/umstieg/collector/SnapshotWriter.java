package io.github.moazemad1.umstieg.collector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

import tools.jackson.databind.json.JsonMapper;

public class SnapshotWriter {

    private static final DateTimeFormatter DAY_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FILE_NAME = DateTimeFormatter.ofPattern("HHmmss").withZone(ZoneOffset.UTC);

    private final Path dataDir;
    private final JsonMapper json;

    public SnapshotWriter(Path dataDir, JsonMapper json) {
        this.dataDir = dataDir;
        this.json = json;
    }

    public Path write(FetchResult result, FeedSummary summary) throws IOException {
        Path base = freeBaseName(result.source(), result.fetchedAt());
        String snapshotFile = null;
        if (result.ok() && result.body().length > 0) {
            Path snapshot = withSuffix(base, ".pb.gz");
            writeAtomically(snapshot, result.gzipped() ? result.body() : gzip(result.body()));
            snapshotFile = snapshot.getFileName().toString();
        }
        return writeMetadata(base, SnapshotMetadata.of(result, summary, snapshotFile));
    }

    public Path writeFailure(String source, Instant fetchedAt, Exception error) throws IOException {
        Path base = freeBaseName(source, fetchedAt);
        return writeMetadata(base, SnapshotMetadata.failure(source, fetchedAt.toString(), error.toString()));
    }

    private Path writeMetadata(Path base, SnapshotMetadata metadata) throws IOException {
        Path file = withSuffix(base, ".json");
        writeAtomically(file, json.writeValueAsBytes(metadata));
        return file;
    }

    private Path freeBaseName(String source, Instant fetchedAt) throws IOException {
        Path dir = dataDir.resolve(source).resolve(DAY_DIR.format(fetchedAt));
        Files.createDirectories(dir);
        String name = FILE_NAME.format(fetchedAt);
        Path base = dir.resolve(name);
        for (int n = 2; Files.exists(withSuffix(base, ".json")); n++) {
            base = dir.resolve(name + "-" + n);
        }
        return base;
    }

    private static Path withSuffix(Path base, String suffix) {
        return base.resolveSibling(base.getFileName() + suffix);
    }

    private static void writeAtomically(Path target, byte[] content) throws IOException {
        Path tmp = withSuffix(target, ".tmp");
        Files.write(tmp, content);
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
    }

    private static byte[] gzip(byte[] data) {
        var out = new ByteArrayOutputStream();
        try (var gz = new GZIPOutputStream(out)) {
            gz.write(data);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
