package io.github.moazemad1.umstieg.collector;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public record FetchResult(
        String source,
        Instant fetchedAt,
        Duration latency,
        int status,
        Optional<String> etag,
        Optional<String> contentEncoding,
        byte[] body) {

    public boolean notModified() {
        return status == 304;
    }

    public boolean ok() {
        return status == 200;
    }

    public boolean gzipped() {
        return contentEncoding.map("gzip"::equalsIgnoreCase).orElse(false);
    }
}
