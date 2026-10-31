package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"collector.user-agent=test-agent", "collector.polling-enabled=false"})
class CollectorApplicationTest {

    @Autowired
    CollectorProperties properties;

    @Test
    void bindsDefaultSources() {
        assertThat(properties.sources())
                .extracting(CollectorProperties.Source::name)
                .containsExactly("staging", "production");
    }
}
