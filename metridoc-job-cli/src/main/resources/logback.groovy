import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import metridoc.cli.MetridocMain

import static ch.qos.logback.classic.Level.ERROR
import static ch.qos.logback.classic.Level.INFO

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        def logPattern
        if (MetridocMain.EXTENDED_LOG) {
            logPattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
        }
        else {
            logPattern = "%d{HH:mm:ss} %-5level - %msg%n"
        }
        pattern = owner.formatPattern(logPattern)
    }

    withJansi = !MetridocMain.PLAIN_TEXT
}

appender("SIMPLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = owner.formatPattern("%msg%n")
    }

    withJansi = !MetridocMain.PLAIN_TEXT
}

if(MetridocMain.LEVEL == null) {
    logger("metridoc", INFO)
    root(ERROR, ["STDOUT"])
}
else {
    root(MetridocMain.LEVEL, ["STDOUT"])
}

logger("metridoc.simple", INFO, ["SIMPLE"], false)

private String formatPattern(String logPattern) {
    if (MetridocMain.PLAIN_TEXT) {
        return logPattern
    }
    else {
        return "%highlight($logPattern)"
    }
}


