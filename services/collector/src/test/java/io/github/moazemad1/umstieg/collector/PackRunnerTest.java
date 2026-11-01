package io.github.moazemad1.umstieg.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class PackRunnerTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T00:30:00Z"), ZoneOffset.UTC);

    @Test
    void yesterdayIsThePreviousUtcDay() {
        assertThat(PackRunner.resolveDay("yesterday", clock)).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void acceptsAnIsoDate() {
        assertThat(PackRunner.resolveDay("2026-10-31", clock)).isEqualTo(LocalDate.of(2026, 10, 31));
    }
}
