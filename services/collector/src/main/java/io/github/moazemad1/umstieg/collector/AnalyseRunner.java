package io.github.moazemad1.umstieg.collector;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import tools.jackson.databind.json.JsonMapper;

public class AnalyseRunner implements ApplicationRunner {

    private final Path archive;

    public AnalyseRunner(Path archive) {
        this.archive = archive;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String name = archive.getFileName().toString();
        String source = name.substring(0, name.length() - "-YYYY-MM-DD.tar.zst".length());
        var analysis = new DayAnalysis();
        ArchivedSnapshot.read(archive, JsonMapper.builder().build(), analysis::accept);
        System.out.print(DayReportText.format(source, name, Files.size(archive), analysis.report()));
    }
}
