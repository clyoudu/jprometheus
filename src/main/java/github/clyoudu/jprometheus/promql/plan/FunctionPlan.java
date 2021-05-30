package github.clyoudu.jprometheus.promql.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import github.clyoudu.jprometheus.api.vo.QueryVo;
import github.clyoudu.jprometheus.exception.JprometheusException;
import github.clyoudu.jprometheus.exception.JpromqlParseException;
import github.clyoudu.jprometheus.promql.plan.entity.LabelMatcher;
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
import org.joda.time.DateTime;

/**
 * @author leichen
 */
public class FunctionPlan extends VectorOperationPlan {

    private String function;

    private List<JpromqlPlan> parameters;

    public FunctionPlan(String function, List<JpromqlPlan> parameters) {
        this.function = function;
        this.parameters = parameters;
    }

    @Override
    public PromqlResult eval(Storage storage, QueryVo vo) {
        switch (function) {
            case "abs":
                return compute(storage, Math::abs, vo);
            case "absent":
                return absent(storage, vo);
            case "absent_over_time":
                return absentOverTime(storage, vo);
            case "ceil":
                return compute(storage, Math::ceil, vo);
            case "changes":
                return computeRange(storage, samples -> samples.stream().map(Sample::getValue).distinct().count(), vo);
            case "clamp":
                return clamp(storage, vo);
            case "clamp_max":
                return clampMax(storage, vo);
            case "clamp_min":
                return clampMin(storage, vo);
            case "day_of_month":
                return timeOf(storage, t -> new DateTime(t).dayOfMonth().get(), vo);
            case "day_of_week":
                return timeOf(storage, t -> new DateTime(t).dayOfWeek().get(), vo);
            case "days_in_month":
                return timeOf(storage, t -> new DateTime(t).dayOfMonth().getMaximumValue(), vo);
            case "delta":
                return computeRange(storage,
                    samples -> MathUtil.delta(samples.stream().mapToDouble(Sample::getValue).toArray()), vo);
            case "deriv":
                return computeRange(storage, samples -> MathUtil
                    .derivative(samples.stream().mapToDouble(Sample::getTimestamp).toArray(),
                        samples.stream().mapToDouble(Sample::getValue).toArray()), vo);
            case "exp":
                return compute(storage, sample -> Math.pow(Math.E, sample), vo);
            case "floor":
                return compute(storage, Math::floor, vo);
            case "histogram_quantile"://TODO
            case "holt_winters"://TODO
            case "hour":
                timeOf(storage, t -> new DateTime(t).hourOfDay().get(), vo);
            case "idelta"://TODO
            case "increase"://TODO
            case "irate"://TODO
            case "label_join"://TODO
            case "label_replace"://TODO
            case "ln":
                return compute(storage, Math::log, vo);
            case "log2":
                return compute(storage, MathUtil::log2, vo);
            case "log10":
                return compute(storage, Math::log10, vo);
            case "minute":
                return timeOf(storage, t -> new DateTime(t).minuteOfHour().get(), vo);
            case "month":
                return timeOf(storage, t -> new DateTime(t).monthOfYear().get(), vo);
            case "predict_linear"://TODO
            case "rate"://TODO
            case "resets"://TODO
            case "round"://TODO
            case "scalar"://TODO
            case "sgn":
                return compute(storage, Math::signum, vo);
            case "sort"://TODO
            case "sort_desc"://TODO
            case "sqrt":
                return compute(storage, Math::sqrt, vo);
            case "time"://TODO
            case "timestamp"://TODO
            case "vector"://TODO
            case "year":
                return timeOf(storage, t -> new DateTime(t).year().get(), vo);
            case "avg_over_time"://TODO
            case "min_over_time"://TODO
            case "max_over_time"://TODO
            case "sum_over_time"://TODO
            case "count_over_time"://TODO
            case "quantile_over_time"://TODO
            case "stddev_over_time"://TODO
            case "stdvar_over_time"://TODO
            case "last_over_time"://TODO
            default:
                throw new JprometheusException("Unsupported function: " + function);
        }
    }

    private PromqlResult computeRange(Storage storage, ToDoubleFunction<List<Sample>> function, QueryVo vo) {
        checkParam(1);
        InstantOrRangeSelectorPlan plan = (InstantOrRangeSelectorPlan) parameters.get(0);
        checkRangeSelector(plan);
        VectorResult result = (VectorResult) plan.eval(storage, vo);
        if (result != null && result.getMetricData() != null && !result.getMetricData().isEmpty()) {
            result.getMetricData().forEach((key, metric) -> {
                ArrayList<Sample> samples = new ArrayList<>();
                Long time = DateTimeUtil.timestampLong(vo.getTime());
                samples.add(new Sample(time, function.applyAsDouble(metric.getSamples())));
                metric.setSamples(samples);
            });
            result.setWindow(null);
        }
        return result;
    }

    private PromqlResult timeOf(Storage storage, ToIntFunction<Long> function, QueryVo vo) {
        if (parameters == null || parameters.isEmpty()) {
            HashMap<String, MetricData> metricData = new HashMap<>(1);
            MetricData data = new MetricData();
            data.setMetric(new Metric("", ""));
            data.setLabels(new LinkedHashSet<>());
            ArrayList<Sample> samples = new ArrayList<>();
            Long time = DateTimeUtil.timestampLong(vo.getTime());
            samples.add(new Sample(time, (double) function.applyAsInt(time)));
            data.setSamples(samples);
            metricData.put("", data);
            return new VectorResult(null, metricData);
        } else {
            return compute(storage, s -> function.applyAsInt((long) (s * 1000)), vo);
        }
    }

    private PromqlResult clampMin(Storage storage, QueryVo vo) {
        checkParam(2);
        PromqlResult minResult = parameters.get(1).eval(storage, vo);
        if (!(minResult instanceof NumericResult)) {
            return throwClampParameterTypeError("second");
        }
        InstantOrRangeSelectorPlan plan = (InstantOrRangeSelectorPlan) parameters.get(0);
        VectorResult result = (VectorResult) plan.eval(storage, vo);
        result.getMetricData().forEach((key, metricData) -> {
            for (Sample sample : metricData.getSamples()) {
                sample.setValue(Math.max(((NumericResult) minResult).getValue(),
                    Math.min(Double.POSITIVE_INFINITY, sample.getValue())));
            }
        });
        return result;
    }

    private PromqlResult throwClampParameterTypeError(String index) {
        throw new JprometheusException(
            "Expected type scalar for the " + index + " parameter in call to function \"" + function + "\"");
    }

    private PromqlResult clampMax(Storage storage, QueryVo vo) {
        checkParam(2);
        PromqlResult maxResult = parameters.get(1).eval(storage, vo);
        if (!(maxResult instanceof NumericResult)) {
            throwClampParameterTypeError("second");
        }
        InstantOrRangeSelectorPlan plan = (InstantOrRangeSelectorPlan) parameters.get(0);
        VectorResult result = (VectorResult) plan.eval(storage, vo);
        result.getMetricData().forEach((key, metricData) -> {
            for (Sample sample : metricData.getSamples()) {
                sample.setValue(Math.max(Double.NEGATIVE_INFINITY,
                    Math.min(((NumericResult) maxResult).getValue(), sample.getValue())));
            }
        });
        return result;
    }

    private PromqlResult clamp(Storage storage, QueryVo vo) {
        checkParam(3);
        PromqlResult minResult = parameters.get(1).eval(storage, vo);
        PromqlResult maxResult = parameters.get(2).eval(storage, vo);
        if (!(minResult instanceof NumericResult)) {
            throwClampParameterTypeError("second");
        }
        if (!(maxResult instanceof NumericResult)) {
            throwClampParameterTypeError("third");
        }
        InstantOrRangeSelectorPlan plan = (InstantOrRangeSelectorPlan) parameters.get(0);
        VectorResult result = (VectorResult) plan.eval(storage, vo);
        result.getMetricData().forEach((key, metricData) -> {
            for (Sample sample : metricData.getSamples()) {
                sample.setValue(Math.max(((NumericResult) minResult).getValue(),
                    Math.min(((NumericResult) maxResult).getValue(), sample.getValue())));
            }
        });
        return result;
    }

    private PromqlResult absentOverTime(Storage storage, QueryVo vo) {
        checkParam(1);
        InstantOrRangeSelectorPlan plan = (InstantOrRangeSelectorPlan) parameters.get(0);
        checkRangeSelector(plan);
        return absentMetricData(storage, plan, vo);
    }

    private PromqlResult absent(Storage storage, QueryVo vo) {
        checkParam(1);
        InstantOrRangeSelectorPlan plan = (InstantOrRangeSelectorPlan) parameters.get(0);
        return absentMetricData(storage, plan, vo);
    }

    private PromqlResult absentMetricData(Storage storage, InstantOrRangeSelectorPlan plan, QueryVo vo) {
        VectorResult result = (VectorResult) plan.eval(storage, vo);
        if (result == null || result.getMetricData() == null || result.getMetricData().isEmpty()) {
            HashMap<String, MetricData> metricData = new HashMap<>(1);
            MetricData data = new MetricData();
            data.setMetric(new Metric("", ""));
            data.setLabels(new LinkedHashSet<>(
                plan.getLabelMatcherList().stream().filter(l -> l.getMatchType().equals(LabelMatcher.Type.EQ))
                    .map(l -> new Label(l.getLabel(), l.getValue())).collect(Collectors.toSet())));
            ArrayList<Sample> samples = new ArrayList<>();
            Long time = DateTimeUtil.timestampLong(vo.getTime());
            samples.add(new Sample(time, 1D));
            data.setSamples(samples);
            metricData.put("", data);
            return new VectorResult(null, metricData);
        } else {
            return new VectorResult(null, new HashMap<>());
        }
    }

    private PromqlResult compute(Storage storage, ToDoubleFunction<Double> function, QueryVo vo) {
        checkParam(1);
        InstantOrRangeSelectorPlan plan = (InstantOrRangeSelectorPlan) parameters.get(0);
        VectorResult result = (VectorResult) plan.eval(storage, vo);
        result.getMetricData().forEach((key, metricData) -> {
            for (Sample sample : metricData.getSamples()) {
                sample.setValue(function.applyAsDouble(sample.getValue()));
            }
        });
        return result;
    }

    private void checkParam(int paramLength) {
        if (parameters == null || parameters.size() != paramLength) {
            throw new JprometheusException(
                "Expected " + paramLength + " argument(s) in call to \"" + function + "\", got " +
                    (parameters == null ? 0 : parameters.size()));
        } else if (!(parameters.get(0) instanceof InstantOrRangeSelectorPlan)) {
            throw new JprometheusException("Expected type instant vector in call to function \"" + function + "\"");
        }
    }

    private void checkRangeSelector(InstantOrRangeSelectorPlan plan) {
        if (plan.getWindow() == null) {
            throw new JpromqlParseException(
                "Expected type range vector in call to function \"absent_over_time\", got" + " instant vector");
        }
    }
}
