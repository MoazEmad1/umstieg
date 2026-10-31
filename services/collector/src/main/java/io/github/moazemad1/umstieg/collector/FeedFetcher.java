package io.github.moazemad1.umstieg.collector;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import io.github.moazemad1.umstieg.collector.CollectorProperties.Source;

public class FeedFetcher {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client;
    private final String userAgent;
    private final Clock clock;

    public FeedFetcher(HttpClient client, String userAgent, Clock clock) {
        this.client = client;
        this.userAgent = userAgent;
        this.clock = clock;
    }

    public FetchResult fetch(Source source, Optional<String> lastEtag) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(source.url())
                .timeout(TIMEOUT)
                .header("User-Agent", userAgent)
                .header("Accept-Encoding", "gzip");
        lastEtag.ifPresent(etag -> request.header("If-None-Match", etag));

        Instant start = clock.instant();
        HttpResponse<byte[]> response = client.send(request.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        Duration latency = Duration.between(start, clock.instant());

        return new FetchResult(
                source.name(),
                start,
                latency,
                response.statusCode(),
                response.headers().firstValue("ETag"),
                response.headers().firstValue("Content-Encoding"),
                response.body());
    }
}
