package io.github.moazemad1.umstieg.collector;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class PackRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PackRunner.class);

    private final DayPacker packer;
    private final CollectorProperties collector;
    private final LocalDate day;

    public PackRunner(DayPacker packer, CollectorProperties collector, PackProperties pack, Clock clock) {
        this.packer = packer;
        this.collector = collector;
        this.day = resolveDay(pack.date(), clock);
    }

    static LocalDate resolveDay(String date, Clock clock) {
        if ("yesterday".equals(date)) {
            return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).minusDays(1);
        }
        return LocalDate.parse(date);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (CollectorProperties.Source source : collector.sources()) {
            Optional<DayPacker.Manifest> manifest = packer.pack(source.name(), day);
            if (manifest.isPresent()) {
                var m = manifest.get();
                log.info("{} {} packed files={} bytes={} archiveBytes={}",
                        m.source(), m.date(), m.files(), m.bytes(), m.archiveBytes());
            } else {
                log.info("{} {} has no files", source.name(), day);
            }
        }
    }
}
