package github.clyoudu.jprometheus.storage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import github.clyoudu.jprometheus.promql.plan.entity.LabelMatcher;
import github.clyoudu.jprometheus.storage.entity.Label;
import github.clyoudu.jprometheus.storage.entity.Metric;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import github.clyoudu.jprometheus.storage.entity.Sample;
import org.joda.time.Period;

/**
 * @author leichen
 */
public interface Storage {

    /**
     * query vector(instant or range)
     * @param metricName metric name
     * @param labelMatcherList metric label matcher
     * @param window time window(range vector)
     * @param offset offset
     * @param time current time
     * @return metric samples map
     */
    Map<String, MetricData> queryVector(String metricName, List<LabelMatcher> labelMatcherList, Period window,
        Period offset, Long time);

    /**
     * scrap sample
     * @param metric metric
     * @param labels labels
     * @param sample sample
     */
    void scrapSample(Metric metric, LinkedHashSet<Label> labels, Sample sample);

    /**
     * query range vector
     * @param metricName metric name
     * @param labelMatcherList label list
     * @param offset offset
     * @param start start time
     * @param end end time
     * @param step step
     * @return metric samples map
     */
    Map<String, MetricData> queryMatrix(String metricName, List<LabelMatcher> labelMatcherList, Period offset,
        long start, long end, long step);
}
