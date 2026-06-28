package com.mrfermz.mcplugins.core.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link LogSink} that appends log lines to daily-rotated files under
 * {@code plugins/antitle/logs/} (e.g. {@code antitle-2026-06-28.log}).
 *
 * <p>One writer is kept open and reopened when the date rolls over. The
 * {@link LogService} guarantees single-threaded {@link #write} calls, so no
 * locking is needed here.
 */
public final class FileLogSink implements LogSink {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LINE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final File dir;
    private final ZoneId zone = ZoneId.systemDefault();
    private final Logger errorLog;

    private LocalDate openDate;
    private Writer writer;

    public FileLogSink(File logDir, Logger errorLog) {
        this.dir = logDir;
        this.errorLog = errorLog;
        if (!dir.isDirectory() && !dir.mkdirs()) {
            errorLog.warning("Could not create log directory " + dir);
        }
    }

    @Override
    public void write(List<LogEntry> batch) {
        try {
            for (LogEntry e : batch) {
                LocalDate date = Instant.ofEpochMilli(e.timestamp()).atZone(zone).toLocalDate();
                ensureWriter(date);
                if (writer == null) {
                    return; // couldn't open the file; reported already
                }
                writer.write(format(e));
                writer.write(System.lineSeparator());
                if (e.error() != null) {
                    writer.write(e.error());
                    writer.write(System.lineSeparator());
                }
            }
            if (writer != null) {
                writer.flush();
            }
        } catch (IOException ex) {
            errorLog.log(Level.SEVERE, "Failed to write log file in " + dir, ex);
        }
    }

    private String format(LogEntry e) {
        String when = LINE_TIME.format(Instant.ofEpochMilli(e.timestamp()).atZone(zone));
        return when + " [" + e.level() + "] [" + e.source() + "] " + e.message();
    }

    private void ensureWriter(LocalDate date) throws IOException {
        if (writer != null && date.equals(openDate)) {
            return;
        }
        closeWriter();
        File file = new File(dir, "antitle-" + FILE_DATE.format(date) + ".log");
        writer = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(file.toPath(),
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND),
                StandardCharsets.UTF_8));
        openDate = date;
    }

    @Override
    public void close() {
        closeWriter();
    }

    private void closeWriter() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ex) {
                errorLog.log(Level.SEVERE, "Failed to close log file in " + dir, ex);
            }
            writer = null;
            openDate = null;
        }
    }
}
