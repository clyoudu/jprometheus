package github.clyoudu.jprometheus.storage.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author leichen
 */
@Data
@AllArgsConstructor
public class Metric {

    /**
     * metrics id(md5 for metric name and labels)
     */
    private String metricId;

    /**
     * metric name
     */
    private String metricName;

    @Override
    public int hashCode() {
        return metricId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Metric && metricId.equals(((Metric) obj).getMetricId());
    }
}
