package io.github.moazemad1.umstieg.collector;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.github.luben.zstd.ZstdInputStream;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

import tools.jackson.databind.json.JsonMapper;

public record ArchivedSnapshot(String name, SnapshotMetadata metadata, Optional<FeedMessage> feed) {

    public Instant fetchedAt() {
        return Instant.parse(metadata.fetchedAt());
    }

    public static void read(Path archive, JsonMapper json, Consumer<ArchivedSnapshot> consumer) throws IOException {
        try (var tar = new TarArchiveInputStream(new ZstdInputStream(new BufferedInputStream(Files.newInputStream(archive))))) {
            String base = null;
            SnapshotMetadata metadata = null;
            FeedMessage feed = null;
            for (TarArchiveEntry entry = tar.getNextEntry(); entry != null; entry = tar.getNextEntry()) {
                String name = entry.getName();
                String entryBase = name.substring(0, name.lastIndexOf('.'));
                if (!entryBase.equals(base)) {
                    emit(base, metadata, feed, consumer);
                    base = entryBase;
                    metadata = null;
                    feed = null;
                }
                byte[] content = tar.readAllBytes();
                if (name.endsWith(".json")) {
                    metadata = json.readValue(content, SnapshotMetadata.class);
                } else if (name.endsWith(".pb")) {
                    feed = FeedMessage.parseFrom(content);
                }
            }
            emit(base, metadata, feed, consumer);
        }
    }

    private static void emit(String base, SnapshotMetadata metadata, FeedMessage feed, Consumer<ArchivedSnapshot> consumer) {
        if (base == null) {
            return;
        }
        if (metadata == null) {
            throw new IllegalStateException("No sidecar for " + base);
        }
        consumer.accept(new ArchivedSnapshot(base, metadata, Optional.ofNullable(feed)));
    }
}
