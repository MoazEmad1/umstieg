package io.github.moazemad1.umstieg.collector;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("collector.pack")
public record PackProperties(
        String date,
        @DefaultValue("data/archive") Path archiveDir,
        @DefaultValue("19") int level,
        @DefaultValue("27") int windowLog) {
}
