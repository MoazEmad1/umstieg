package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import io.github.moazemad1.umstieg.collector.CollectorProperties.Source;

class FeedFetcherTest {

    private static final String ETAG = "\"23-abc\"";

    private HttpServer server;
    private Source source;
    private final AtomicReference<Headers> lastRequestHeaders = new AtomicReference<>();
    private final FeedFetcher fetcher = new FeedFetcher(HttpClient.newHttpClient(), "umstieg-test (test@example.com)", Clock.systemUTC());

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/data", exchange -> {
            lastRequestHeaders.set(exchange.getRequestHeaders());
            if (ETAG.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                exchange.sendResponseHeaders(304, -1);
                exchange.close();
                return;
            }
            byte[] body = gzip("feed-bytes".getBytes());
            exchange.getResponseHeaders().add("ETag", ETAG);
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/limited", exchange -> {
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        server.start();
        source = sourceFor("/data");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsUserAgentAndAsksForGzip() throws Exception {
        fetcher.fetch(source, Optional.empty());

        assertThat(lastRequestHeaders.get().getFirst("User-Agent")).isEqualTo("umstieg-test (test@example.com)");
        assertThat(lastRequestHeaders.get().getFirst("Accept-Encoding")).isEqualTo("gzip");
        assertThat(lastRequestHeaders.get().containsKey("If-None-Match")).isFalse();
    }

    @Test
    void keepsBodyExactlyAsReceived() throws Exception {
        FetchResult result = fetcher.fetch(source, Optional.empty());

        assertThat(result.ok()).isTrue();
        assertThat(result.gzipped()).isTrue();
        assertThat(result.body()).isEqualTo(gzip("feed-bytes".getBytes()));
        assertThat(result.etag()).contains(ETAG);
        assertThat(result.source()).isEqualTo("test");
    }

    @Test
    void sendsLastEtagAndReportsNotModified() throws Exception {
        FetchResult result = fetcher.fetch(source, Optional.of(ETAG));

        assertThat(lastRequestHeaders.get().getFirst("If-None-Match")).isEqualTo(ETAG);
        assertThat(result.notModified()).isTrue();
        assertThat(result.body()).isEmpty();
    }

    @Test
    void passesErrorStatusThrough() throws Exception {
        FetchResult result = fetcher.fetch(sourceFor("/limited"), Optional.empty());

        assertThat(result.status()).isEqualTo(429);
        assertThat(result.ok()).isFalse();
    }

    private Source sourceFor(String path) {
        return new Source("test", URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path), Duration.ofSeconds(60));
    }

    private static byte[] gzip(byte[] data) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var gz = new GZIPOutputStream(out)) {
            gz.write(data);
        }
        return out.toByteArray();
    }
}
