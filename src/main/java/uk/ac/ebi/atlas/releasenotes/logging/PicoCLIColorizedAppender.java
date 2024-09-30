package uk.ac.ebi.atlas.releasenotes.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import picocli.CommandLine;

public class PicoCLIColorizedAppender extends ConsoleAppender<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent event) {
        String formattedMessage = new String(encoder.encode(event));
        String colorizedMessage = getColorizedMessage(event, formattedMessage);
        System.out.print(colorizedMessage);
    }

    private String getColorizedMessage(ILoggingEvent event, String formattedMessage) {
        String template = switch (event.getLevel().toInt()) {
            case Level.DEBUG_INT -> "@|blue %s|@"; // Blue for DEBUG
            case Level.INFO_INT -> "@|green %s|@"; // Green for INFO
            case Level.WARN_INT -> "@|yellow %s|@"; // Yellow for WARN
            case Level.ERROR_INT -> "@|red %s|@"; // Red for ERROR
            default -> "%s";
        };
        return CommandLine.Help.Ansi.AUTO.string(String.format(template, formattedMessage));
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
}
