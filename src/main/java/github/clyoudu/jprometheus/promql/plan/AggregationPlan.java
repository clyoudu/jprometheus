package github.clyoudu.jprometheus.promql.plan;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import com.google.common.math.Quantiles;
import github.clyoudu.jprometheus.api.vo.QueryRangeVo;
import github.clyoudu.jprometheus.api.vo.QueryVo;
import github.clyoudu.jprometheus.exception.JprometheusException;
import github.clyoudu.jprometheus.promql.result.MatrixResult;
import github.clyoudu.jprometheus.promql.result.NumericResult;
import github.clyoudu.jprometheus.promql.result.PromqlResult;
import github.clyoudu.jprometheus.promql.result.VectorResult;
import github.clyoudu.jprometheus.storage.Storage;
import github.clyoudu.jprometheus.storage.entity.Label;
import github.clyoudu.jprometheus.storage.entity.Metric;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import github.clyoudu.jprometheus.storage.entity.Sample;
import github.clyoudu.jprometheus.util.DateTimeUtil;
import github.clyoudu.jprometheus.util.MathUtil;
import github.clyoudu.jprometheus.util.MetricUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author leichen
 */
public class AggregationPlan extends VectorOperationPlan {

    private String aggregate;

    private List<JpromqlPlan> parameters;

    private Set<String> byLabels;

    private Set<String> withoutLabels;

    private boolean without;

    public AggregationPlan(String aggregate, List<JpromqlPlan> parameters, Set<String> byLabels,
        Set<String> withoutLabels, boolean without) {
        this.aggregate = aggregate;
        this.parameters = parameters;
        this.byLabels = byLabels;
        this.withoutLabels = withoutLabels;
        this.without = without;
    }

    @Override
    public PromqlResult eval(Storage storage, QueryVo vo) {
        switch (aggregate) {
            case "sum":
                return compute(storage, samples -> samples.stream().mapToDouble(Sample::getValue).sum(), vo);
            case "min":
                return compute(storage, samples -> samples.stream().mapToDouble(Sample::getValue).min().orElse(0D), vo);
            case "max":
                return compute(storage, samples -> samples.stream().mapToDouble(Sample::getValue).max().orElse(0D), vo);
            case "avg":
                return compute(storage, samples -> samples.stream().mapToDouble(Sample::getValue).average().orElse(0D),
                    vo);
            case "group":
                return compute(storage, samples -> 1D, vo);
            case "stddev":
                return compute(storage,
                    samples -> MathUtil.standardDeviation(samples.stream().mapToDouble(Sample::getValue).toArray())
                        .orElse(0D), vo);
            case "stdvar":
                return compute(storage,
                    samples -> MathUtil.standardVariance(samples.stream().mapToDouble(Sample::getValue).toArray())
                        .orElse(0D), vo);
            case "count":
                return compute(storage, samples -> (double) samples.stream().mapToDouble(Sample::getValue).count(), vo);
            case "count_values":
                return countValues(storage, vo);
            case "topk":
                return topBottomK(storage, true, vo);
            case "bottomk":
                return topBottomK(storage, false, vo);
            case "quantile":
                return quantile(storage, vo);
            default:
                throw new JprometheusException("Aggregation not supported: " + aggregate);
        }
    }

    private PromqlResult quantile(Storage storage, QueryVo vo) {
        checkTwoParam(LiteralPlan.class, "number");
        LiteralPlan literalPlan = (LiteralPlan) parameters.get(0);
        NumericResult numericResult = (NumericResult) literalPlan.eval(storage, vo);
        double p = numericResult.getValue();
        JpromqlPlan plan = parameters.get(1);

        Map<LinkedHashSet<Label>, List<Sample>> labelSamples = byWithoutLabels(storage, plan, vo);

        Map<String, MetricData> metricData = new HashMap<>(4);
        labelSamples.forEach((labelSet, samples) -> {
            if (samples != null && !samples.isEmpty()) {
                double value = quantile(p, samples.stream().mapToDouble(Sample::getValue).toArray());
                putMetricData(metricData, labelSet, value, vo);
            }
        });
        return new VectorResult(null, metricData);
    }

    private MatrixResult quantileRange(Storage storage, QueryRangeVo vo) {
        checkTwoParam(LiteralPlan.class, "number");
        LiteralPlan literalPlan = (LiteralPlan) parameters.get(0);
        MatrixResult numericResult = literalPlan.evalRange(storage, vo);
        double p = numericResult.getMetricData().values().stream().findAny().get().getSamples().get(0).getValue();
        JpromqlPlan plan = (InstantOrRangeSelectorPlan) parameters.get(1);

        Map<LinkedHashSet<Label>, NavigableMap<Long, List<Double>>> labelSamples =
            byWithoutLabelsRange(storage, plan, vo);

        Map<String, MetricData> metricData = new HashMap<>(4);
        labelSamples.forEach((labelSet, samples) -> {
            if (samples != null && !samples.isEmpty()) {
                putRangeMetricData(metricData, labelSet, samples.entrySet().stream()
                    .map(e -> new Sample(e.getKey(), quantile(p, e.getValue().stream().mapToDouble(d -> d).toArray())))
                    .collect(Collectors.toList()));
            }
        });
        return new MatrixResult(metricData);
    }

    private Double quantile(Double p, double[] array) {
        double value;
        if (p < 0) {
            value = Double.NEGATIVE_INFINITY;
        } else if (p > 1) {
            value = Double.POSITIVE_INFINITY;
        } else {
            value = Quantiles.scale(100).index((int) (p * 100)).compute(array);
        }
        return value;
    }

    private PromqlResult topBottomK(Storage storage, boolean top, QueryVo vo) {
        checkTwoParam(LiteralPlan.class, "integer");
        LiteralPlan literalPlan = (LiteralPlan) parameters.get(0);
        NumericResult numericResult = (NumericResult) literalPlan.eval(storage, vo);
        int k = numericResult.getValue().intValue();
        JpromqlPlan plan = (InstantOrRangeSelectorPlan) parameters.get(1);
        if (plan instanceof InstantOrRangeSelectorPlan && ((InstantOrRangeSelectorPlan) plan).getWindow() != null) {
            throw throwVectorRangeException();
        }
        Map<LinkedHashSet<Label>, List<MetricData>> labelMetrics = byWithoutLabelsUseOriginLabel(storage, plan, vo);
        Map<String, MetricData> metricData = new HashMap<>(4);
        labelMetrics.forEach((labelSet, metricDataList) -> {
            if (metricDataList != null && !metricDataList.isEmpty()) {
                Comparator<Object> comparator;
                if (top) {
                    comparator = Comparator.comparingDouble(data -> ((MetricData) data).getSamples().get(0).getValue())
                        .reversed();
                } else {
                    comparator = Comparator.comparingDouble(data -> ((MetricData) data).getSamples().get(0).getValue());
                }

                List<MetricData> metrics =
                    metricDataList.stream().sorted(comparator).limit(k).collect(Collectors.toList());
                for (MetricData metric : metrics) {
                    metricData.put(metric.getMetric().getMetricId(), metric);
                }
            }
        });
        return new VectorResult(null, metricData);
    }

    private MatrixResult topBottomKRange(Storage storage, boolean top, QueryRangeVo vo) {
        checkTwoParam(LiteralPlan.class, "integer");
        LiteralPlan literalPlan = (LiteralPlan) parameters.get(0);
        MatrixResult numericResult = literalPlan.evalRange(storage, vo);
        JpromqlPlan plan = parameters.get(1);
        if (plan instanceof InstantOrRangeSelectorPlan && ((InstantOrRangeSelectorPlan) plan).getWindow() != null) {
            throw throwVectorRangeException();
        }
        NavigableMap<Long, Map<LinkedHashSet<Label>, List<TimePointMetricData>>> labelMetrics =
            byWithoutLabelsUseOriginLabelRange(storage, plan, vo);
        int k =
            numericResult.getMetricData().values().stream().findAny().get().getSamples().get(0).getValue().intValue();
        Comparator<Object> comparator;
        if (top) {
            comparator =
                Comparator.comparingDouble(data -> ((TimePointMetricData) data).getSample().getValue()).reversed();
        } else {
            comparator = Comparator.comparingDouble(data -> ((TimePointMetricData) data).getSample().getValue());
        }
        labelMetrics.forEach((timestamp, labelMap) -> labelMap.forEach((labels, pointList) -> {
            labelMap.put(labels, pointList.stream().sorted(comparator).limit(k).collect(Collectors.toList()));
        }));

        Map<String, MetricData> metricData = new HashMap<>(4);

        for (Map.Entry<Long, Map<LinkedHashSet<Label>, List<TimePointMetricData>>> entry : labelMetrics.entrySet()) {
            for (Map.Entry<LinkedHashSet<Label>, List<TimePointMetricData>> labelPoint : entry.getValue().entrySet()) {
                for (TimePointMetricData point : labelPoint.getValue()) {
                    String metricId = point.getMetric().getMetricId();
                    List<Sample> samples;
                    if (metricData.containsKey(metricId)) {
                        samples = metricData.get(metricId).getSamples();
                    } else {
                        samples = new ArrayList<>();
                        MetricData data = new MetricData();
                        data.setMetric(point.getMetric());
                        data.setLabels(point.getLabels());
                        data.setSamples(samples);
                        metricData.put(metricId, data);
                    }
                    samples.add(point.getSample());
                }
            }
        }
        return new MatrixResult(metricData);
    }

    private JprometheusException throwVectorRangeException() {
        return new JprometheusException("Expected type instant vector in aggregation expression, got range vector");
    }

    private PromqlResult countValues(Storage storage, QueryVo vo) {
        checkTwoParam(StringPlan.class, "string");
        StringPlan stringPlan = (StringPlan) parameters.get(0);
        JpromqlPlan plan = parameters.get(1);
        if (plan instanceof InstantOrRangeSelectorPlan && ((InstantOrRangeSelectorPlan) plan).getWindow() != null) {
            throw throwVectorRangeException();
        }
        Map<LinkedHashSet<Label>, List<Sample>> labelSamples =
            byWithoutLabelsAndValue(storage, plan, stringPlan.getExpr(), vo);
        Map<String, MetricData> metricData = new HashMap<>(4);
        labelSamples.forEach((labelSet, samples) -> {
            if (samples != null && !samples.isEmpty()) {
                putMetricData(metricData, labelSet, (double) samples.stream().mapToDouble(Sample::getValue).count(),
                    vo);
            }
        });
        return new VectorResult(null, metricData);
    }

    private MatrixResult countValuesRange(Storage storage, QueryRangeVo vo) {
        checkTwoParam(StringPlan.class, "string");
        StringPlan stringPlan = (StringPlan) parameters.get(0);
        JpromqlPlan plan = parameters.get(1);
        if (plan instanceof InstantOrRangeSelectorPlan && ((InstantOrRangeSelectorPlan) plan).getWindow() != null) {
            throw throwVectorRangeException();
        }
        Map<LinkedHashSet<Label>, NavigableMap<Long, List<Double>>> labelSamples =
            byWithoutLabelsAndValueRange(storage, plan, stringPlan.getExpr(), vo);
        Map<String, MetricData> metricData = new HashMap<>(4);
        labelSamples.forEach((labelSet, samples) -> {
            if (samples != null && !samples.isEmpty()) {
                putRangeMetricData(metricData, labelSet, samples.entrySet().stream()
                    .map(e -> new Sample(e.getKey(), (double) e.getValue().stream().mapToDouble(s -> s).count()))
                    .collect(Collectors.toList()));
            }
        });
        return new MatrixResult(metricData);
    }

    private PromqlResult compute(Storage storage, ToDoubleFunction<List<Sample>> function, QueryVo vo) {
        checkSingleParam();
        JpromqlPlan plan = parameters.get(0);
        if (plan instanceof InstantOrRangeSelectorPlan && ((InstantOrRangeSelectorPlan) plan).getWindow() != null) {
            throw throwVectorRangeException();
        }
        Map<LinkedHashSet<Label>, List<Sample>> labelSamples = byWithoutLabels(storage, plan, vo);

        Map<String, MetricData> metricData = new HashMap<>(4);
        labelSamples.forEach((labelSet, samples) -> {
            if (samples != null && !samples.isEmpty()) {
                putMetricData(metricData, labelSet, function.applyAsDouble(samples), vo);
            }
        });
        return new VectorResult(null, metricData);
    }

    private MatrixResult computeRange(Storage storage,
        Function<NavigableMap<Long, List<Double>>, List<Sample>> function, QueryRangeVo vo) {
        checkSingleParam();
        JpromqlPlan plan = parameters.get(0);
        if (plan instanceof InstantOrRangeSelectorPlan && ((InstantOrRangeSelectorPlan) plan).getWindow() != null) {
            throw throwVectorRangeException();
        }
        Map<LinkedHashSet<Label>, NavigableMap<Long, List<Double>>> labelSamples =
            byWithoutLabelsRange(storage, plan, vo);

        Map<String, MetricData> metricData = new HashMap<>(4);
        labelSamples.forEach((labelSet, samples) -> {
            if (samples != null && !samples.isEmpty()) {
                putRangeMetricData(metricData, labelSet, function.apply(samples));
            }
        });
        return new MatrixResult(metricData);
    }

    private void putMetricData(Map<String, MetricData> metricData, LinkedHashSet<Label> labelSet, Double value,
        QueryVo vo) {
        String metricName = MetricUtil.labelsToPromExposureFormat(labelSet);
        MetricData data = new MetricData();
        data.setMetric(new Metric(metricName, ""));
        data.setLabels(labelSet);
        Long time = DateTimeUtil.timestampLong(vo.getTime());
        Sample sample = new Sample(time, value);
        List<Sample> sampleList = new ArrayList<>();
        sampleList.add(sample);
        data.setSamples(sampleList);
        metricData.put(metricName, data);
    }

    private void putRangeMetricData(Map<String, MetricData> metricData, LinkedHashSet<Label> labelSet,
        List<Sample> samples) {
        String metricName = MetricUtil.labelsToPromExposureFormat(labelSet);
        MetricData data = new MetricData();
        data.setMetric(new Metric(metricName, metricName));
        data.setLabels(labelSet);
        data.setSamples(samples);
        metricData.put(metricName, data);
    }

    private void checkSingleParam() {
        if (parameters == null || parameters.isEmpty()) {
            throw new JprometheusException("No arguments for aggregate expression provided.");
        } else if (parameters.size() != 1) {
            throw new JprometheusException(
                "Wrong number of arguments for aggregate expression(" + aggregate + ") provided, expected 1, got " +
                    parameters.size());
        } else if (!(parameters.get(0) instanceof InstantOrRangeSelectorPlan) &&
            !(parameters.get(0) instanceof AggregationPlan)) {
            throw new JprometheusException("Expected type instant vector in aggregation expression(" + aggregate + ")");
        } else if (byLabels != null && !byLabels.isEmpty() && withoutLabels != null && !withoutLabels.isEmpty()) {
            throw new JprometheusException(
                "<by> or <without> can be specified only one in aggregation expression(" + aggregate + ")");
        }
    }

    private void checkTwoParam(Class<? extends JpromqlPlan> firstPlanClass, String firstParamType) {
        if (parameters == null || parameters.isEmpty()) {
            throw new JprometheusException("No arguments for aggregate expression provided.");
        } else if (parameters.size() != 2) {
            throw new JprometheusException(
                "Wrong number of arguments for aggregate expression(" + aggregate + ") provided, expected 2, got " +
                    parameters.size());
        } else if (!firstPlanClass.isInstance(parameters.get(0))) {
            throw new JprometheusException(
                "Expected type " + firstParamType + " in first aggregation parameter(" + aggregate + ")");
        } else if (!(parameters.get(1) instanceof InstantOrRangeSelectorPlan) &&
            !(parameters.get(1) instanceof AggregationPlan)) {
            throw new JprometheusException(
                "Expected type instant vector in second aggregation expression(" + aggregate + ")");
        } else if (byLabels != null && !byLabels.isEmpty() && withoutLabels != null && !withoutLabels.isEmpty()) {
            throw new JprometheusException(
                "<by> or <without> can be specified only one in aggregation expression(" + aggregate + ")");
        }
    }

    private Map<LinkedHashSet<Label>, List<Sample>> byWithoutLabels(Storage storage, JpromqlPlan plan, QueryVo vo) {
        VectorResult vector = (VectorResult) plan.eval(storage, vo);
        Map<LinkedHashSet<Label>, List<Sample>> labelSamples = new HashMap<>(4);
        if (byLabels != null && !byLabels.isEmpty()) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                for (Label label : entry.getLabels()) {
                    if (byLabels.contains(label.getName())) {
                        key.add(label);
                    }
                }
                if (labelSamples.containsKey(key)) {
                    labelSamples.get(key).addAll(entry.getSamples());
                } else {
                    labelSamples.put(key, entry.getSamples());
                }
            }
        } else if (withoutLabels != null && !withoutLabels.isEmpty()) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                for (Label label : entry.getLabels()) {
                    if (!withoutLabels.contains(label.getName())) {
                        key.add(label);
                    }
                }
                key.removeIf(k -> "__name__".equals(k.getName()));
                if (labelSamples.containsKey(key)) {
                    labelSamples.get(key).addAll(entry.getSamples());
                } else {
                    labelSamples.put(key, entry.getSamples());
                }
            }
        } else if (without) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>(entry.getLabels());
                key.removeIf(k -> "__name__".equals(k.getName()));
                if (labelSamples.containsKey(key)) {
                    labelSamples.get(key).addAll(entry.getSamples());
                } else {
                    labelSamples.put(key, entry.getSamples());
                }
            }
        } else {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                if (labelSamples.containsKey(key)) {
                    labelSamples.get(key).addAll(entry.getSamples());
                } else {
                    labelSamples.put(key, entry.getSamples());
                }
            }
        }
        return labelSamples;
    }

    private Map<LinkedHashSet<Label>, NavigableMap<Long, List<Double>>> byWithoutLabelsRange(Storage storage,
        JpromqlPlan plan, QueryRangeVo vo) {
        MatrixResult vector = plan.evalRange(storage, vo);
        Map<LinkedHashSet<Label>, NavigableMap<Long, List<Double>>> labelSamples = new HashMap<>(4);
        if (byLabels != null && !byLabels.isEmpty()) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                for (Label label : entry.getLabels()) {
                    if (byLabels.contains(label.getName())) {
                        key.add(label);
                    }
                }
                collectRange(labelSamples, entry, key);
            }
        } else if (withoutLabels != null && !withoutLabels.isEmpty()) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                for (Label label : entry.getLabels()) {
                    if (!withoutLabels.contains(label.getName())) {
                        key.add(label);
                    }
                }
                key.removeIf(k -> "__name__".equals(k.getName()));
                collectRange(labelSamples, entry, key);
            }
        } else if (without) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>(entry.getLabels());
                key.removeIf(k -> "__name__".equals(k.getName()));
                collectRange(labelSamples, entry, key);
            }
        } else {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                collectRange(labelSamples, entry, key);
            }
        }
        return labelSamples;
    }

    private void collectRange(Map<LinkedHashSet<Label>, NavigableMap<Long, List<Double>>> labelSamples,
        MetricData entry, LinkedHashSet<Label> key) {
        if (labelSamples.containsKey(key)) {
            NavigableMap<Long, List<Double>> value = labelSamples.get(key);
            for (Sample sample : entry.getSamples()) {
                if (value.containsKey(sample.getTimestamp())) {
                    value.get(sample.getTimestamp()).add(sample.getValue());
                } else {
                    List<Double> samples = new ArrayList<>();
                    samples.add(sample.getValue());
                    value.put(sample.getTimestamp(), samples);
                }
            }
        } else {
            TreeMap<Long, List<Double>> value = new TreeMap<>(Comparator.comparingLong(s -> s));
            for (Sample sample : entry.getSamples()) {
                List<Double> samples = new ArrayList<>();
                samples.add(sample.getValue());
                value.put(sample.getTimestamp(), samples);
            }
            labelSamples.put(key, value);
        }
    }

    private Map<LinkedHashSet<Label>, List<MetricData>> byWithoutLabelsUseOriginLabel(Storage storage, JpromqlPlan plan,
        QueryVo vo) {
        VectorResult vector = (VectorResult) plan.eval(storage, vo);
        Map<LinkedHashSet<Label>, List<MetricData>> labelSamples = new HashMap<>(4);
        if (byLabels != null && !byLabels.isEmpty()) {
            for (Map.Entry<String, MetricData> entry : vector.getMetricData().entrySet()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                for (Label label : entry.getValue().getLabels()) {
                    if (byLabels.contains(label.getName())) {
                        key.add(label);
                    }
                }
                putLabelMetricData(labelSamples, entry, key);
            }
        } else if (withoutLabels != null && !withoutLabels.isEmpty()) {
            for (Map.Entry<String, MetricData> entry : vector.getMetricData().entrySet()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                for (Label label : entry.getValue().getLabels()) {
                    if (!withoutLabels.contains(label.getName())) {
                        key.add(label);
                    }
                }
                key.removeIf(k -> "__name__".equals(k.getName()));
                putLabelMetricData(labelSamples, entry, key);
            }
        } else if (without) {
            for (Map.Entry<String, MetricData> entry : vector.getMetricData().entrySet()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>(entry.getValue().getLabels());
                key.removeIf(k -> "__name__".equals(k.getName()));
                putLabelMetricData(labelSamples, entry, key);
            }
        } else {
            for (Map.Entry<String, MetricData> entry : vector.getMetricData().entrySet()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                putLabelMetricData(labelSamples, entry, key);
            }
        }
        return labelSamples;
    }

    @Data
    @AllArgsConstructor
    private static class TimePointMetricData {

        private Metric metric;

        private LinkedHashSet<Label> labels;

        private Sample sample;
    }

    private LinkedHashSet<Label> labelKey(Map.Entry<String, MetricData> entry) {
        return labelKey(entry.getValue());

    }

    private LinkedHashSet<Label> labelKey(MetricData data) {
        LinkedHashSet<Label> key = new LinkedHashSet<>();
        if (byLabels != null && !byLabels.isEmpty()) {
            for (Label label : data.getLabels()) {
                if (byLabels.contains(label.getName())) {
                    key.add(label);
                }
            }
        } else if (withoutLabels != null && !withoutLabels.isEmpty()) {
            for (Label label : data.getLabels()) {
                if (!withoutLabels.contains(label.getName())) {
                    key.add(label);
                }
            }
            key.removeIf(k -> "__name__".equals(k.getName()));
        } else if (without) {
            key.addAll(data.getLabels());
            key.removeIf(k -> "__name__".equals(k.getName()));
        }

        return key;

    }

    private NavigableMap<Long, Map<LinkedHashSet<Label>, List<TimePointMetricData>>> byWithoutLabelsUseOriginLabelRange(
        Storage storage, JpromqlPlan plan, QueryRangeVo vo) {
        MatrixResult vector = plan.evalRange(storage, vo);

        NavigableMap<Long, Map<LinkedHashSet<Label>, List<TimePointMetricData>>> timeMetricData =
            new TreeMap<>(Comparator.comparingLong(k -> k));
        vector.getMetricData().forEach((id, metric) -> {
            LinkedHashSet<Label> key = labelKey(metric);
            for (Sample sample : metric.getSamples()) {
                TimePointMetricData timePointMetricData =
                    new TimePointMetricData(metric.getMetric(), metric.getLabels(), sample);

                Map<LinkedHashSet<Label>, List<TimePointMetricData>> map;
                Long timestamp = sample.getTimestamp();
                if (!timeMetricData.containsKey(timestamp)) {
                    map = new HashMap<>();
                    timeMetricData.put(timestamp, map);
                } else {
                    map = timeMetricData.get(timestamp);
                }

                List<TimePointMetricData> list;
                if (!map.containsKey(key)) {
                    list = new ArrayList<>();
                    map.put(key, list);
                } else {
                    list = map.get(key);
                }
                list.add(timePointMetricData);

            }
        });

        return timeMetricData;
    }

    private void putLabelMetricData(Map<LinkedHashSet<Label>, List<MetricData>> labelSamples,
        Map.Entry<String, MetricData> entry, LinkedHashSet<Label> key) {
        List<MetricData> metricDataList;
        if (labelSamples.containsKey(key)) {
            metricDataList = labelSamples.get(key);
        } else {
            metricDataList = new ArrayList<>();
            labelSamples.put(key, metricDataList);
        }
        metricDataList.add(entry.getValue());
    }

    private Map<LinkedHashSet<Label>, List<Sample>> byWithoutLabelsAndValue(Storage storage, JpromqlPlan plan,
        String valueLabel, QueryVo vo) {
        VectorResult vector = (VectorResult) plan.eval(storage, vo);
        Map<LinkedHashSet<Label>, List<Sample>> labelSamples = new HashMap<>(4);
        if (byLabels != null && !byLabels.isEmpty()) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                for (Label label : entry.getLabels()) {
                    if (byLabels.contains(label.getName())) {
                        key.add(label);
                    }
                }
                key.add(
                    new Label(valueLabel, new BigDecimal(entry.getSamples().get(0).getValue() + "").toPlainString()));
                if (labelSamples.containsKey(key)) {
                    labelSamples.get(key).addAll(entry.getSamples());
                } else {
                    labelSamples.put(key, entry.getSamples());
                }
            }
        } else if (withoutLabels != null && !withoutLabels.isEmpty()) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                for (Label label : entry.getLabels()) {
                    if (!withoutLabels.contains(label.getName())) {
                        key.add(label);
                    }
                }
                key.removeIf(k -> "__name__".equals(k.getName()));
                key.add(
                    new Label(valueLabel, new BigDecimal(entry.getSamples().get(0).getValue() + "").toPlainString()));
                if (labelSamples.containsKey(key)) {
                    labelSamples.get(key).addAll(entry.getSamples());
                } else {
                    labelSamples.put(key, entry.getSamples());
                }
            }
        } else if (without) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>(entry.getLabels());
                key.removeIf(k -> "__name__".equals(k.getName()));
                key.add(
                    new Label(valueLabel, new BigDecimal(entry.getSamples().get(0).getValue() + "").toPlainString()));
                if (labelSamples.containsKey(key)) {
                    labelSamples.get(key).addAll(entry.getSamples());
                } else {
                    labelSamples.put(key, entry.getSamples());
                }
            }
        } else {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                key.add(
                    new Label(valueLabel, new BigDecimal(entry.getSamples().get(0).getValue() + "").toPlainString()));
                if (labelSamples.containsKey(key)) {
                    labelSamples.get(key).addAll(entry.getSamples());
                } else {
                    labelSamples.put(key, entry.getSamples());
                }
            }
        }
        return labelSamples;
    }

    private Map<LinkedHashSet<Label>, NavigableMap<Long, List<Double>>> byWithoutLabelsAndValueRange(Storage storage,
        JpromqlPlan plan, String valueLabel, QueryRangeVo vo) {
        MatrixResult vector = plan.evalRange(storage, vo);
        Map<LinkedHashSet<Label>, NavigableMap<Long, List<Double>>> labelSamples = new HashMap<>(4);
        if (byLabels != null && !byLabels.isEmpty()) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                for (Label label : entry.getLabels()) {
                    if (byLabels.contains(label.getName())) {
                        key.add(label);
                    }
                }
                key.add(
                    new Label(valueLabel, new BigDecimal(entry.getSamples().get(0).getValue() + "").toPlainString()));
                collectRange(labelSamples, entry, key);
            }
        } else if (withoutLabels != null && !withoutLabels.isEmpty()) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                for (Label label : entry.getLabels()) {
                    if (!withoutLabels.contains(label.getName())) {
                        key.add(label);
                    }
                }
                key.removeIf(k -> "__name__".equals(k.getName()));
                key.add(
                    new Label(valueLabel, new BigDecimal(entry.getSamples().get(0).getValue() + "").toPlainString()));
                collectRange(labelSamples, entry, key);
            }
        } else if (without) {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>(entry.getLabels());
                key.removeIf(k -> "__name__".equals(k.getName()));
                key.add(
                    new Label(valueLabel, new BigDecimal(entry.getSamples().get(0).getValue() + "").toPlainString()));
                collectRange(labelSamples, entry, key);
            }
        } else {
            for (MetricData entry : vector.getMetricData().values()) {
                LinkedHashSet<Label> key = new LinkedHashSet<>();
                key.add(
                    new Label(valueLabel, new BigDecimal(entry.getSamples().get(0).getValue() + "").toPlainString()));
                collectRange(labelSamples, entry, key);
            }
        }
        return labelSamples;
    }

    @Override
    public MatrixResult evalRange(Storage storage, QueryRangeVo vo) {
        long start = DateTimeUtil.timestampLong(vo.getStart());
        long end = DateTimeUtil.timestampLong(vo.getEnd());
        long step = DateTimeUtil.stepMills(vo.getStep());
        MetricUtil.checkRange(start, end, step);

        switch (aggregate) {
            case "sum":
                return computeRange(storage, samples -> samples.entrySet().stream()
                    .map(e -> new Sample(e.getKey(), e.getValue().stream().mapToDouble(s -> s).sum()))
                    .collect(Collectors.toList()), vo);
            case "min":
                return computeRange(storage, samples -> samples.entrySet().stream()
                    .map(e -> new Sample(e.getKey(), e.getValue().stream().mapToDouble(s -> s).min().orElse(0D)))
                    .collect(Collectors.toList()), vo);
            case "max":
                return computeRange(storage, samples -> samples.entrySet().stream()
                    .map(e -> new Sample(e.getKey(), e.getValue().stream().mapToDouble(s -> s).max().orElse(0D)))
                    .collect(Collectors.toList()), vo);
            case "avg":
                return computeRange(storage, samples -> samples.entrySet().stream()
                    .map(e -> new Sample(e.getKey(), e.getValue().stream().mapToDouble(s -> s).average().orElse(0D)))
                    .collect(Collectors.toList()), vo);
            case "group":
                return computeRange(storage,
                    samples -> samples.keySet().stream().map(doubles -> new Sample(doubles, 1D))
                        .collect(Collectors.toList()), vo);
            case "stddev":
                return computeRange(storage, samples -> samples.entrySet().stream().map(e -> new Sample(e.getKey(),
                    MathUtil.standardDeviation(e.getValue().stream().mapToDouble(s -> s).toArray()).orElse(0D)))
                    .collect(Collectors.toList()), vo);
            case "stdvar":
                return computeRange(storage, samples -> samples.entrySet().stream().map(e -> new Sample(e.getKey(),
                    MathUtil.standardVariance(e.getValue().stream().mapToDouble(s -> s).toArray()).orElse(0D)))
                    .collect(Collectors.toList()), vo);
            case "count":
                return computeRange(storage, samples -> samples.entrySet().stream()
                    .map(e -> new Sample(e.getKey(), (double) e.getValue().stream().mapToDouble(s -> s).count()))
                    .collect(Collectors.toList()), vo);
            case "count_values":
                return countValuesRange(storage, vo);
            case "topk":
                return topBottomKRange(storage, true, vo);
            case "bottomk":
                return topBottomKRange(storage, false, vo);
            case "quantile":
                return quantileRange(storage, vo);
            default:
                throw new JprometheusException("Aggregation not supported: " + aggregate);
        }
    }
}
