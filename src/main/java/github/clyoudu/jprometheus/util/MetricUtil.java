package github.clyoudu.jprometheus.util;

import java.util.Set;
import java.util.stream.Collectors;

import github.clyoudu.jprometheus.exception.JprometheusException;
import github.clyoudu.jprometheus.storage.entity.Label;
import org.apache.commons.text.StringEscapeUtils;

/**
 * @author leichen
 */
public final class MetricUtil {

    private final static int MAX_POINT = 11000;

    private MetricUtil() {
    }

    /**
     * labels set to prometheus exposure format
     * input: [a=b,c=d]
     * output: {a="b",c="d"}
     *
     * @param labels labels
     * @return prometheus exposure format
     */
    public static String labelsToPromExposureFormat(Set<Label> labels) {
        return "{" + labels.stream()
            .map(label -> label.getName() + "=\"" + StringEscapeUtils.escapeJava(label.getValue()) + "\"")
            .collect(Collectors.joining(",")) + "}";
    }

    public static void checkRange(long start, long end, long step) {
        if ((end - start) / step > MAX_POINT) {
            throw new JprometheusException("Exceeded maximum resolution of " + MAX_POINT +
                " points per timeseries. Try decreasing the query resolution (?step=XX)");
        }
    }

}
