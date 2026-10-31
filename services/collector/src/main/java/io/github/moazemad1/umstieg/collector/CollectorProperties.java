package io.github.moazemad1.umstieg.collector;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("collector")
public record CollectorProperties(String userAgent, Path dataDir, List<Source> sources) {

    public CollectorProperties {
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("collector.user-agent must be set (FEED_USER_AGENT)");
        }
        Objects.requireNonNull(dataDir, "collector.data-dir must be set");
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("collector.sources must not be empty");
        }
        sources = List.copyOf(sources);
    }

    public record Source(String name, URI url, Duration interval) {

        public Source {
            Objects.requireNonNull(name, "source name");
            Objects.requireNonNull(url, "source url");
            Objects.requireNonNull(interval, "source interval");
            if (!name.matches("[a-z0-9-]+")) {
                throw new IllegalArgumentException("source name must match [a-z0-9-]+: " + name);
            }
        }
    }
}
