package github.clyoudu.jprometheus.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import github.clyoudu.jprometheus.config.JprometheusProperties;
import github.clyoudu.jprometheus.exception.JprometheusException;
import github.clyoudu.jprometheus.promql.plan.entity.LabelMatcher;
import github.clyoudu.jprometheus.storage.entity.Label;
import github.clyoudu.jprometheus.storage.entity.Metric;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import github.clyoudu.jprometheus.storage.entity.Sample;
import github.clyoudu.jprometheus.util.DateTimeUtil;
import org.joda.time.Period;

/**
 * samples are stored in memory
 * <strong>Samples lost when jvm shutdown</strong>
 * @author leichen
 */
public class InMemoryStorage implements Storage {

    private static final Map<String, Metric> METRIC_MAP = new ConcurrentHashMap<>(512);

    private static final Map<String, Set<String>> METRIC_NAME_MAP = new ConcurrentHashMap<>(512);

    private static final Map<String, LinkedHashSet<Label>> METRIC_LABEL_MAP = new ConcurrentHashMap<>(512);

    private static final Map<String, TreeMap<Long, Double>> METRIC_SAMPLE_MAP = new ConcurrentHashMap<>(512);

    private JprometheusProperties jprometheusProperties;

    public InMemoryStorage(JprometheusProperties jprometheusProperties) {
        this.jprometheusProperties = jprometheusProperties;
    }

    @PostConstruct
    public void init() {
        // TODO schedule evict logic
        // TODO delete test data
        scrapSample(new Metric("1", "process_uptime_seconds"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "chengdu"))),
            new Sample(1621849460000L, 247082.886D));
        scrapSample(new Metric("1", "process_uptime_seconds"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "chengdu"))),
            new Sample(1621849480000L, 247103.886D));

        scrapSample(new Metric("2", "process_uptime_seconds"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "shenzhen"))),
            new Sample(1621849460000L, 244305.772D));
        scrapSample(new Metric("2", "process_uptime_seconds"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "shenzhen"))),
            new Sample(1621849480000L, 244325.772D));

        scrapSample(new Metric("3", "order_cnt"), new LinkedHashSet<>(
                Arrays.asList(new Label("application", "amo"), new Label("area", "shenzhen"), new Label("gender",
                    "male"))),
            new Sample(1621849460000L, 100D));
        scrapSample(new Metric("3", "order_cnt"), new LinkedHashSet<>(
                Arrays.asList(new Label("application", "amo"), new Label("area", "shenzhen"), new Label("gender",
                    "male"))),
            new Sample(1621849480000L, 105D));
        scrapSample(new Metric("3", "order_cnt"), new LinkedHashSet<>(
                Arrays.asList(new Label("application", "amo"), new Label("area", "shenzhen"), new Label("gender",
                    "male"))),
            new Sample(1621849500000L, 107D));
        scrapSample(new Metric("3", "order_cnt"), new LinkedHashSet<>(
                Arrays.asList(new Label("application", "amo"), new Label("area", "shenzhen"), new Label("gender",
                    "male"))),
            new Sample(1621849520000L, 110D));

        scrapSample(new Metric("31", "order_cnt"), new LinkedHashSet<>(Arrays
                .asList(new Label("application", "amo"), new Label("area", "shenzhen"), new Label("gender", "female"))),
            new Sample(1621849460000L, 101D));
        scrapSample(new Metric("31", "order_cnt"), new LinkedHashSet<>(Arrays
                .asList(new Label("application", "amo"), new Label("area", "shenzhen"), new Label("gender", "female"))),
            new Sample(1621849480000L, 102D));
        scrapSample(new Metric("31", "order_cnt"), new LinkedHashSet<>(Arrays
                .asList(new Label("application", "amo"), new Label("area", "shenzhen"), new Label("gender", "female"))),
            new Sample(1621849500000L, 104D));
        scrapSample(new Metric("31", "order_cnt"), new LinkedHashSet<>(Arrays
                .asList(new Label("application", "amo"), new Label("area", "shenzhen"), new Label("gender", "female"))),
            new Sample(1621849520000L, 110D));

        scrapSample(new Metric("4", "order_cnt"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "chengdu"))),
            new Sample(1621849460000L, 99D));
        scrapSample(new Metric("4", "order_cnt"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "chengdu"))),
            new Sample(1621849480000L, 109D));
        scrapSample(new Metric("4", "order_cnt"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "chengdu"))),
            new Sample(1621849500000L, 130D));
        scrapSample(new Metric("4", "order_cnt"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "chengdu"))),
            new Sample(1621849520000L, 141D));

        scrapSample(new Metric("5", "order_cnt"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "beijing"))),
            new Sample(1621849460000L, 201D));
        scrapSample(new Metric("5", "order_cnt"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "beijing"))),
            new Sample(1621849480000L, 220D));
        scrapSample(new Metric("5", "order_cnt"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "beijing"))),
            new Sample(1621849500000L, 231D));
        scrapSample(new Metric("5", "order_cnt"),
            new LinkedHashSet<>(Arrays.asList(new Label("application", "amo"), new Label("area", "beijing"))),
            new Sample(1621849520000L, 241D));

    }

    @Override
    public Map<String, MetricData> queryVector(String metricName, List<LabelMatcher> labelMatcherList, Period window,
        Period offset, Long time) {
        HashMap<String, MetricData> result = new HashMap<>(4);

        boolean limit1 = false;
        if (window == null) {
            limit1 = true;
        }

        if (time == null) {
            time = System.currentTimeMillis();
        }

        if (offset != null) {
            time = time - DateTimeUtil.periodMills(offset);
        }

        Set<String> metricIdSet = queryMetricIdList(metricName, labelMatcherList);

        if (metricIdSet != null && !metricIdSet.isEmpty()) {
            queryMemory(window, time, result, limit1, metricIdSet);
        }

        return result;
    }

    private Set<String> queryMetricIdList(String metricName, List<LabelMatcher> labelMatcherList) {
        Set<String> metricIdSet;
        if (metricName != null) {
            metricIdSet = METRIC_NAME_MAP.get(metricName);
            if (labelMatcherList != null && !labelMatcherList.isEmpty()) {
                metricIdSet = metricIdSet.stream().filter(id -> {
                    Set<Label> labels = METRIC_LABEL_MAP.get(id);
                    if (labels == null || labels.isEmpty()) {
                        return false;
                    } else {
                        return labelMatch(labels, labelMatcherList);
                    }
                }).collect(Collectors.toSet());
            }
        } else if (labelMatcherList != null && !labelMatcherList.isEmpty()) {
            metricIdSet = METRIC_LABEL_MAP.keySet().stream().filter(id -> {
                Set<Label> labels = METRIC_LABEL_MAP.get(id);
                if (labels == null || labels.isEmpty()) {
                    return false;
                } else {
                    return labelMatch(labels, labelMatcherList);
                }
            }).collect(Collectors.toSet());
        } else {
            throw new JprometheusException("vector selector must contain at least one non-empty matcher");
        }
        return metricIdSet;
    }

    private void queryMemory(Period window, Long time, HashMap<String, MetricData> result, boolean limit1,
        Set<String> metricIdSet) {
        for (String id : metricIdSet) {
            TreeMap<Long, Double> samples = METRIC_SAMPLE_MAP.get(id);
            String metricName = METRIC_MAP.get(id).getMetricName();
            if (samples != null && !samples.isEmpty()) {
                if (limit1) {
                    query(metricName, time, result, id, samples);
                } else {
                    queryRange(metricName, window, time, result, id, samples);
                }
            }
        }
    }

    private void convertSamples(String metricName, HashMap<String, MetricData> result, String metricId,
        NavigableMap<Long, Double> samples) {
        if (samples != null && !samples.isEmpty()) {
            MetricData data = new MetricData();
            data.setMetric(new Metric(metricId, metricName));
            data.setSamples(samples.entrySet().stream().map(entry -> new Sample(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
            data.setLabels(METRIC_LABEL_MAP.get(metricId));
            result.put(metricId, data);
        }
    }

    private void queryRange(String metricName, Period window, Long time, HashMap<String, MetricData> result, String id,
        TreeMap<Long, Double> samples) {
        NavigableMap<Long, Double> entries = samples.subMap(time - DateTimeUtil.periodMills(window), true, time, true);
        convertSamples(metricName, result, id, entries);
    }

    private void query(String metricName, Long time, HashMap<String, MetricData> result, String id,
        TreeMap<Long, Double> samples) {
        Map.Entry<Long, Double> entry = samples.floorEntry(time);
        if (entry != null) {
            MetricData data = new MetricData();
            data.setMetric(new Metric(id, metricName));
            Sample sample = new Sample(entry.getKey(), entry.getValue());
            List<Sample> samplesList = new ArrayList<>();
            samplesList.add(sample);
            data.setSamples(samplesList);
            data.setLabels(METRIC_LABEL_MAP.get(id));
            result.put(id, data);
        }
    }

    private boolean labelMatch(Set<Label> labels, List<LabelMatcher> labelMatcherList) {
        for (LabelMatcher matcher : labelMatcherList) {
            LabelMatcher.Type matchType = matcher.getMatchType();
            switch (matchType) {
                case EQ:
                    if (labels.stream().noneMatch(
                        l -> l.getName().equals(matcher.getLabel()) && l.getValue().equals(matcher.getValue()))) {
                        return false;
                    }
                    break;
                case NEQ:
                    if (labels.stream().noneMatch(
                        l -> l.getName().equals(matcher.getLabel()) && !l.getValue().equals(matcher.getValue()))) {
                        return false;
                    }
                    break;
                case RE:
                    if (labels.stream().noneMatch(
                        l -> l.getName().equals(matcher.getLabel()) && l.getValue().matches(matcher.getValue()))) {
                        return false;
                    }
                    break;
                case NRE:
                    if (labels.stream().noneMatch(
                        l -> l.getName().equals(matcher.getLabel()) && !l.getValue().matches(matcher.getValue()))) {
                        return false;
                    }
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    @Override
    public void scrapSample(Metric metric, LinkedHashSet<Label> labels, Sample sample) {
        TreeMap<Long, Double> samples;
        Set<String> metricIdSet;
        String metricId = metric.getMetricId();
        String metricName = metric.getMetricName();
        METRIC_MAP.putIfAbsent(metricId, metric);
        if (!METRIC_LABEL_MAP.containsKey(metricId) || !METRIC_SAMPLE_MAP.containsKey(metricId)) {
            METRIC_LABEL_MAP.put(metricId, labels);
            samples = new TreeMap<>(Comparator.comparing(k -> k));
            METRIC_SAMPLE_MAP.put(metricId, samples);

        } else {
            samples = METRIC_SAMPLE_MAP.get(metricId);
        }
        if (!METRIC_NAME_MAP.containsKey(metricName)) {
            metricIdSet = new LinkedHashSet<>();
            METRIC_NAME_MAP.put(metricName, metricIdSet);
        } else {
            metricIdSet = METRIC_NAME_MAP.get(metricName);
        }
        samples.put(sample.getTimestamp(), sample.getValue());
        metricIdSet.add(metricId);
    }

    @Override
    public Map<String, MetricData> queryMatrix(String metricName, List<LabelMatcher> labelMatcherList, Period offset,
        long start, long end, long step) {
        HashMap<String, MetricData> result = new HashMap<>(4);
        if (offset != null) {
            start = start - DateTimeUtil.periodMills(offset);
            end = end - DateTimeUtil.periodMills(offset);
        }
        Set<String> metricIdSet = queryMetricIdList(metricName, labelMatcherList);
        if (metricIdSet != null && !metricIdSet.isEmpty()) {
            queryMemoryRange(start, end, step, result, metricIdSet);
        }

        return result;
    }

    private void queryMemoryRange(long start, long end, long step, HashMap<String, MetricData> result,
        Set<String> metricIdSet) {
        for (String id : metricIdSet) {
            TreeMap<Long, Double> samples = METRIC_SAMPLE_MAP.get(id);
            String metricName = METRIC_MAP.get(id).getMetricName();
            MetricData data = new MetricData();
            data.setMetric(new Metric(id, metricName));
            List<Sample> samplesList = new ArrayList<>();
            data.setSamples(samplesList);
            data.setLabels(METRIC_LABEL_MAP.get(id));
            if (samples != null && !samples.isEmpty()) {
                while (start <= end) {
                    Map.Entry<Long, Double> entry = samples.floorEntry(start);
                    if (entry != null) {
                        Sample sample = new Sample(entry.getKey(), entry.getValue());
                        samplesList.add(sample);
                    }
                    start += step;
                }
            }
            result.put(id, data);
        }
    }
}
