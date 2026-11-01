package io.github.moazemad1.umstieg.collector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

import tools.jackson.databind.json.JsonMapper;

public class DayPacker {

    private static final DateTimeFormatter DAY_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public record Entry(String name, long bytes, String sha256) { }

    public record Manifest(String source, String date, int files, long bytes, long archiveBytes, List<Entry> entries) { }

    private final Path dataDir;
    private final PackProperties properties;
    private final JsonMapper json;

    public DayPacker(Path dataDir, PackProperties properties, JsonMapper json) {
        this.dataDir = dataDir;
        this.properties = properties;
        this.json = json;
    }

    public Optional<Manifest> pack(String source, LocalDate day) throws IOException {
        String dayPath = DAY_DIR.format(day);
        List<Path> files = snapshotFiles(dataDir.resolve(source).resolve(dayPath));
        if (files.isEmpty()) {
            return Optional.empty();
        }

        Path outDir = properties.archiveDir().resolve(source).resolve(String.valueOf(day.getYear()));
        String baseName = source + "-" + day;
        Path archive = outDir.resolve(baseName + ".tar.zst");
        Path manifestFile = outDir.resolve(baseName + ".manifest.json");
        if (Files.exists(archive) || Files.exists(manifestFile)) {
            throw new FileAlreadyExistsException(archive.toString());
        }
        Files.createDirectories(outDir);

        Path tmp = outDir.resolve(baseName + ".tar.zst.tmp");
        try {
            List<Entry> entries = write(tmp, source + "/" + dayPath + "/", files);
            verify(tmp, entries);
            long bytes = entries.stream().mapToLong(Entry::bytes).sum();
            var manifest = new Manifest(source, day.toString(), entries.size(), bytes, Files.size(tmp), entries);
            Path manifestTmp = outDir.resolve(baseName + ".manifest.json.tmp");
            Files.write(manifestTmp, json.writeValueAsBytes(manifest));
            Files.move(tmp, archive, StandardCopyOption.ATOMIC_MOVE);
            Files.move(manifestTmp, manifestFile, StandardCopyOption.ATOMIC_MOVE);
            return Optional.of(manifest);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private List<Entry> write(Path target, String prefix, List<Path> files) throws IOException {
        List<Entry> entries = new ArrayList<>();
        try (var zstd = new ZstdOutputStream(new BufferedOutputStream(Files.newOutputStream(target)))
                .setLevel(properties.level())
                .setLong(properties.windowLog())
                .setChecksum(true);
             var tar = new TarArchiveOutputStream(zstd)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (Path file : files) {
                String name = file.getFileName().toString();
                byte[] content = Files.readAllBytes(file);
                if (name.endsWith(".gz")) {
                    content = gunzip(content);
                    name = name.substring(0, name.length() - ".gz".length());
                }
                var entry = new TarArchiveEntry(prefix + name);
                entry.setSize(content.length);
                entry.setModTime(Files.getLastModifiedTime(file));
                tar.putArchiveEntry(entry);
                tar.write(content);
                tar.closeArchiveEntry();
                entries.add(new Entry(entry.getName(), content.length, sha256(content)));
            }
            tar.finish();
        }
        return entries;
    }

    static void verify(Path archive, List<Entry> expected) throws IOException {
        List<Entry> actual = new ArrayList<>();
        try (var tar = new TarArchiveInputStream(new ZstdInputStream(new BufferedInputStream(Files.newInputStream(archive))))) {
            for (TarArchiveEntry entry = tar.getNextEntry(); entry != null; entry = tar.getNextEntry()) {
                byte[] content = tar.readAllBytes();
                actual.add(new Entry(entry.getName(), content.length, sha256(content)));
            }
        }
        if (!actual.equals(expected)) {
            throw new IOException("Archive check failed for " + archive + ": expected " + expected.size()
                    + " entries, read " + actual.size());
        }
    }

    private static List<Path> snapshotFiles(Path dayDir) throws IOException {
        if (!Files.isDirectory(dayDir)) {
            return List.of();
        }
        try (Stream<Path> list = Files.list(dayDir)) {
            return list.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".tmp"))
                    .sorted()
                    .toList();
        }
    }

    private static byte[] gunzip(byte[] data) throws IOException {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return in.readAllBytes();
        }
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
