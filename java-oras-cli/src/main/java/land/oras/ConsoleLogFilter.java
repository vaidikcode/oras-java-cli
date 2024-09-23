package land.oras;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;

public class ConsoleLogFilter extends ThresholdFilter {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (Main.DEBUG) {
            setLevel(Level.DEBUG.levelStr);
        }
        else {
            setLevel(Level.INFO.levelStr);
        }
        return super.decide(event);
    }
}
