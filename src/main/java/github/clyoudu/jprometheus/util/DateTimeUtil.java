package github.clyoudu.jprometheus.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import github.clyoudu.jprometheus.exception.JprometheusException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * @author leichen
 */
@Slf4j
public final class DateTimeUtil {

    /**
     * RFC_3339 time format
     */
    private static final String RFC_3339_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final PeriodFormatter PERIOD_FORMATTER =
        new PeriodFormatterBuilder().appendYears().appendSuffix("y").appendWeeks().appendSuffix("w").appendDays()
            .appendSuffix("d").appendHours().appendSuffix("h").appendMinutes().appendSuffix("m").appendSeconds()
            .appendSuffix("s").appendMillis().appendSuffix("ms").toFormatter();

    private DateTimeUtil() {
    }

    /**
     * convert time string to double timestamp(s)
     * @param prometheusTime time string
     * @return double timestamp(s)
     */
    public static double timestampDouble(String prometheusTime) {
        try {
            long time = ((long) (Double.parseDouble(prometheusTime) * 1000));
            return time / 1000D;
        } catch (NumberFormatException e) {
            DateFormat format = new SimpleDateFormat(RFC_3339_FORMAT);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                return format.parse(prometheusTime).getTime() / 1000D;
            } catch (ParseException ex) {
                log.error("parse time error: {}", prometheusTime);
                throw new JprometheusException("parse time error: " + prometheusTime, ex);
            }
        }
    }

    /**
     * convert time string to long timestamp(ms)
     * @param prometheusTime time string
     * @return double timestamp(s)
     */
    public static long timestampLong(String prometheusTime) {
        try {
            return ((long) (Double.parseDouble(prometheusTime) * 1000));
        } catch (NumberFormatException e) {
            DateFormat format = new SimpleDateFormat(RFC_3339_FORMAT);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                return format.parse(prometheusTime).getTime();
            } catch (ParseException ex) {
                log.error("parse time error: {}", prometheusTime);
                throw new JprometheusException("parse time error: " + prometheusTime, ex);
            }
        }
    }

    /**
     * parse period
     * @param text
     * @return
     */
    public static Period parsePeriod(String text) {
        try {
            return PERIOD_FORMATTER.parsePeriod(text);
        } catch (Exception e) {
            throw new JprometheusException(
                "Time durations are specified as a number, followed immediately by one of the units[ms|s|m|h|d|w|y], " +
                    "Time" +
                    " durations can be combined, by concatenation. Units must be ordered from the longest to the " +
                    "shortest" + ".", e);
        }
    }

    /**
     * period mills
     * @param period period
     * @return period mills
     */
    public static long periodMills(Period period) {
        long offset = 0L;
        offset += period.getMillis();
        offset += period.getSeconds() * 1000L;
        offset += period.getMinutes() * 60 * 1000L;
        offset += period.getHours() * 60 * 60 * 1000L;
        offset += period.getDays() * 24 * 60 * 60 * 1000L;
        offset += period.getWeeks() * 7 * 24 * 60 * 60 * 1000L;
        offset += period.getYears() * 365 * 24 * 60 * 60 * 1000L;
        return offset;
    }

    public static long stepMills(String step) {
        if (StringUtils.isNumeric(step)) {
            step += "s";
        }
        return periodMills(parsePeriod(step));
    }

}
