package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

import io.github.moazemad1.umstieg.collector.CollectorProperties.Source;
import tools.jackson.databind.json.JsonMapper;

class SourcePollerTest {

    private static final String ETAG = "\"v1\"";

    @TempDir
    Path dataDir;

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/data", exchange -> {
            requests.incrementAndGet();
            if (ETAG.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                exchange.sendResponseHeaders(304, -1);
            } else {
                byte[] body = Feeds.gzip(Feeds.feedWithTrips(Instant.now(), 3));
                exchange.getResponseHeaders().add("ETag", ETAG);
                exchange.getResponseHeaders().add("Content-Encoding", "gzip");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void secondPollUsesEtagAndBothAreRecorded() throws IOException {
        SourcePoller poller = pollerFor(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/data"));

        poller.run();
        poller.run();

        List<Path> sidecars = files(".json");
        assertThat(sidecars).hasSize(2);
        assertThat(files(".pb.gz")).hasSize(1);
        assertThat(sidecars.stream().map(this::status)).containsExactlyInAnyOrder(200, 304);
        assertThat(poller.freshness().stale()).isFalse();
    }

    @Test
    void unreachableSourceWritesFailureRecord() throws IOException {
        SourcePoller poller = pollerFor(URI.create("http://127.0.0.1:1/data"));

        poller.run();

        List<Path> sidecars = files(".json");
        assertThat(sidecars).hasSize(1);
        assertThat(json.readTree(sidecars.get(0).toFile()).get("error").asString()).isNotBlank();
    }

    private SourcePoller pollerFor(URI url) {
        Clock clock = Clock.systemUTC();
        var source = new Source("test", url, Duration.ofSeconds(60));
        var fetcher = new FeedFetcher(HttpClient.newHttpClient(), "umstieg-test", clock);
        var freshness = new FreshnessTracker(new FreshnessProperties(Duration.ofMinutes(5), Duration.ofMinutes(15)), clock.instant());
        return new SourcePoller(source, fetcher, new SnapshotWriter(dataDir, json), freshness, clock);
    }

    private List<Path> files(String suffix) throws IOException {
        try (Stream<Path> walk = Files.walk(dataDir)) {
            return walk.filter(p -> p.getFileName().toString().endsWith(suffix)).sorted().toList();
        }
    }

    private int status(Path sidecar) {
        return json.readTree(sidecar.toFile()).get("status").asInt();
    }
}
