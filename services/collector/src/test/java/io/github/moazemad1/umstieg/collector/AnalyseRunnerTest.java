package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.json.JsonMapper;

class AnalyseRunnerTest {

    @TempDir
    Path root;

    @Test
    void printsTheReportForAnArchive() throws Exception {
        var json = JsonMapper.builder().build();
        Instant fetched = Instant.parse("2026-11-02T08:00:00Z");
        Path dayDir = Files.createDirectories(root.resolve("raw/production/2026/11/02"));
        Files.write(dayDir.resolve("080000.pb.gz"), Feeds.gzip(Feeds.feedWithTrips(fetched, 2)));
        Files.write(dayDir.resolve("080000.json"), json.writeValueAsBytes(new SnapshotMetadata(
                "production", fetched.toString(), 200, 100L, null, "gzip", 50L, fetched.toString(), 2, "080000.pb.gz", null)));
        new DayPacker(root.resolve("raw"), new PackProperties(null, root.resolve("archive"), 3, 27), json)
                .pack("production", LocalDate.of(2026, 11, 2));

        var out = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        try {
            new AnalyseRunner(root.resolve("archive/production/2026/production-2026-11-02.tar.zst")).run(null);
        } finally {
            System.setOut(original);
        }

        assertThat(out.toString(StandardCharsets.UTF_8))
                .startsWith("# production-2026-11-02.tar.zst")
                .contains("Polls: 1.")
                .contains("P2 TripUpdates without start_date: 100.00% of 2 -> wrong");
    }
}
