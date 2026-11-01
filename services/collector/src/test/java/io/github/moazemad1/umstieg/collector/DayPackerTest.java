package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.luben.zstd.ZstdInputStream;

import tools.jackson.databind.json.JsonMapper;

class DayPackerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 1);

    @TempDir
    Path root;

    private final JsonMapper json = JsonMapper.builder().build();
    private Path dayDir;
    private DayPacker packer;

    @BeforeEach
    void setUp() throws IOException {
        dayDir = Files.createDirectories(root.resolve("raw/production/2026/11/01"));
        packer = new DayPacker(root.resolve("raw"), new PackProperties(null, root.resolve("archive"), 3, 27), json);
    }

    @Test
    void packsDayInOrderAndKeepsBytes() throws IOException {
        byte[] first = Feeds.feedWithTrips(Instant.parse("2026-11-01T10:00:00Z"), 3);
        byte[] second = Feeds.feedWithTrips(Instant.parse("2026-11-01T10:01:00Z"), 3);
        Files.write(dayDir.resolve("100100.pb.gz"), Feeds.gzip(second));
        Files.writeString(dayDir.resolve("100100.json"), "{\"status\":200}");
        Files.write(dayDir.resolve("100000.pb.gz"), Feeds.gzip(first));
        Files.writeString(dayDir.resolve("100000.json"), "{\"status\":200}");
        Files.writeString(dayDir.resolve("100200.json.tmp"), "partial");

        var manifest = packer.pack("production", DAY).orElseThrow();

        Map<String, byte[]> archived = read(root.resolve("archive/production/2026/production-2026-11-01.tar.zst"));
        assertThat(archived.keySet()).containsExactly(
                "production/2026/11/01/100000.json",
                "production/2026/11/01/100000.pb",
                "production/2026/11/01/100100.json",
                "production/2026/11/01/100100.pb");
        assertThat(archived.get("production/2026/11/01/100000.pb")).isEqualTo(first);
        assertThat(archived.get("production/2026/11/01/100100.pb")).isEqualTo(second);
        assertThat(manifest.files()).isEqualTo(4);
        assertThat(manifest.bytes()).isEqualTo(archived.values().stream().mapToLong(b -> b.length).sum());
        assertThat(root.resolve("archive/production/2026/production-2026-11-01.manifest.json")).exists();
        assertThat(root.resolve("archive/production/2026")).isDirectoryNotContaining("glob:**.tmp");
    }

    @Test
    void missingDayGivesNothing() throws IOException {
        assertThat(packer.pack("staging", DAY)).isEmpty();
        assertThat(packer.pack("production", DAY)).isEmpty();
    }

    @Test
    void neverOverwritesAnArchive() throws IOException {
        Files.writeString(dayDir.resolve("100000.json"), "{}");
        packer.pack("production", DAY);

        assertThatThrownBy(() -> packer.pack("production", DAY)).isInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    void verifyRejectsArchiveThatDoesNotMatch() throws IOException {
        Files.writeString(dayDir.resolve("100000.json"), "{}");
        var manifest = packer.pack("production", DAY).orElseThrow();
        Path archive = root.resolve("archive/production/2026/production-2026-11-01.tar.zst");
        var wrong = new DayPacker.Entry(manifest.entries().get(0).name(), 999, manifest.entries().get(0).sha256());

        assertThatThrownBy(() -> DayPacker.verify(archive, List.of(wrong))).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> DayPacker.verify(archive, List.of())).isInstanceOf(IOException.class);
    }

    private static Map<String, byte[]> read(Path archive) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (var tar = new TarArchiveInputStream(new ZstdInputStream(new BufferedInputStream(Files.newInputStream(archive))))) {
            for (TarArchiveEntry entry = tar.getNextEntry(); entry != null; entry = tar.getNextEntry()) {
                entries.put(entry.getName(), tar.readAllBytes());
            }
        }
        return entries;
    }
}
