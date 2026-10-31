package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.moazemad1.umstieg.collector.CollectorProperties.Source;

class CollectorPropertiesTest {

    private static final Source STAGING =
            new Source("staging", URI.create("https://example.test/data"), Duration.ofSeconds(60));

    @Test
    void rejectsMissingUserAgent() {
        assertThatThrownBy(() -> new CollectorProperties(" ", Path.of("data"), List.of(STAGING)))
                .hasMessageContaining("FEED_USER_AGENT");
    }

    @Test
    void rejectsEmptySources() {
        assertThatThrownBy(() -> new CollectorProperties("ua", Path.of("data"), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSourceNameThatIsNotPathSafe() {
        assertThatThrownBy(() -> new Source("../x", URI.create("https://example.test"), Duration.ofSeconds(1)))
                .hasMessageContaining("[a-z0-9-]+");
    }

    @Test
    void acceptsValidConfig() {
        var props = new CollectorProperties("ua", Path.of("data"), List.of(STAGING));
        assertThat(props.sources()).containsExactly(STAGING);
    }
}
