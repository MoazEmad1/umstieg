package io.github.moazemad1.umstieg.collector;

import java.util.Locale;

import io.github.moazemad1.umstieg.collector.DayAnalysis.Report;

public final class DayReportText {

    private DayReportText() {
    }

    public static String format(String source, String archiveName, long archiveBytes, Report r) {
        var out = new StringBuilder();
        line(out, "# %s", archiveName);
        line(out, "");
        line(out, "Polls: %d. Bytes as received: %d (%.2f GB). Archive: %d bytes (%.1f MB), %.1fx smaller.",
                r.polls(), r.receivedBytes(), r.receivedBytes() / 1e9, archiveBytes, archiveBytes / 1e6,
                archiveBytes == 0 ? 0 : (double) r.receivedBytes() / archiveBytes);
        line(out, "");

        if (source.equals("staging")) {
            line(out, "- P7 staging under 0.2 GB: %s", verdict(r.receivedBytes() < 0.2e9));
            return out.toString();
        }
        if (!source.equals("production")) {
            return out.toString();
        }

        double kept = r.share(r.passedKept(), r.passedObservations());
        String p1 = kept >= 0.90 ? "kept" : kept <= 0.10 ? "dropped" : "mixed";
        line(out, "- P1 passed stops: %s of %d observations still present -> %s -> %s",
                pct(kept), r.passedObservations(), p1, verdict(p1.equals("kept")));
        double changed = r.share(r.settledStopsChanged(), r.settledStops());
        line(out, "- P1 extra, settled stops that still changed: %s of %d -> %s",
                pct(changed), r.settledStops(), verdict(r.settledStops() > 0 && changed < 0.05));
        double noDate = r.share(r.tripUpdatesWithoutStartDate(), r.tripUpdates());
        line(out, "- P2 TripUpdates without start_date: %s of %d -> %s",
                pct(noDate), r.tripUpdates(), verdict(noDate < 0.01));
        double noSeq = r.share(r.stopUpdatesWithoutSequence(), r.stopUpdates());
        line(out, "- P3 stop updates without stop_sequence: %s of %d -> %s",
                pct(noSeq), r.stopUpdates(), verdict(noSeq < 0.05));
        double ok = r.share(r.ok(), r.polls());
        double failed = r.share(r.failed(), r.polls());
        line(out, "- P4 polls 200 with trips: %s, failed: %s -> %s",
                pct(ok), pct(failed), verdict(ok >= 0.98 && failed < 0.02));
        double notModified = r.share(r.notModified(), r.polls());
        line(out, "- P5 not modified (304): %s -> %s", pct(notModified), verdict(notModified < 0.01));
        double stale = r.share(r.stale(), r.polls());
        boolean alert = r.longestWithoutFresh().compareTo(DayAnalysis.ALERT_AFTER) >= 0;
        line(out, "- P6 stale or failed polls: %s, longest stretch without a fresh snapshot: %d min, alert would fire: %s -> %s",
                pct(stale), r.longestWithoutFresh().toMinutes(), alert ? "yes" : "no", verdict(stale < 0.02 && !alert));
        double ratio = archiveBytes == 0 ? 0 : (double) r.receivedBytes() / archiveBytes;
        boolean sizeOk = r.receivedBytes() >= 1.2e9 && r.receivedBytes() <= 2.2e9;
        line(out, "- P7 production %.2f GB as received, archive %.1fx smaller -> %s",
                r.receivedBytes() / 1e9, ratio, verdict(sizeOk && ratio >= 8 && ratio <= 20));
        line(out, "- P8 packing time: read from journalctl, not from the archive");
        return out.toString();
    }

    private static String pct(double share) {
        return String.format(Locale.ROOT, "%.2f%%", share * 100);
    }

    private static String verdict(boolean right) {
        return right ? "right" : "wrong";
    }

    private static void line(StringBuilder out, String format, Object... args) {
        out.append(String.format(Locale.ROOT, format, args)).append('\n');
    }
}
