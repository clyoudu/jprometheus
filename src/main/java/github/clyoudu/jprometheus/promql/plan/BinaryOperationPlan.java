package github.clyoudu.jprometheus.promql.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import github.clyoudu.jprometheus.api.enums.ResultType;
import github.clyoudu.jprometheus.api.vo.QueryRangeVo;
import github.clyoudu.jprometheus.api.vo.QueryVo;
import github.clyoudu.jprometheus.exception.JprometheusException;
import github.clyoudu.jprometheus.promql.result.MatrixResult;
import github.clyoudu.jprometheus.promql.result.NumericResult;
import github.clyoudu.jprometheus.promql.result.PromqlResult;
import github.clyoudu.jprometheus.promql.result.VectorResult;
import github.clyoudu.jprometheus.storage.Storage;
import github.clyoudu.jprometheus.storage.entity.Label;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import github.clyoudu.jprometheus.storage.entity.Sample;
import github.clyoudu.jprometheus.util.DateTimeUtil;
import github.clyoudu.jprometheus.util.MetricUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * @author leichen
 */
@Getter
public class BinaryOperationPlan extends JpromqlPlan {

    private JpromqlPlan left;

    private JpromqlPlan right;

    private String operator;

    @Setter
    private Set<String> onLabels;

    @Setter
    private Set<String> ignoreLabels;

    @Setter
    private boolean groupLeft = false;

    @Setter
    private boolean groupRight = false;

    public BinaryOperationPlan(JpromqlPlan left, JpromqlPlan right, String operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public PromqlResult eval(Storage storage, QueryVo vo) {
        PromqlResult lv = left.eval(storage, vo);
        PromqlResult rv = right.eval(storage, vo);
        checkGrouping(lv, rv);
        if (lv instanceof NumericResult && rv instanceof NumericResult) {
            return numOpNum((NumericResult) lv, (NumericResult) rv);
        } else if (lv instanceof NumericResult && rv instanceof VectorResult) {
            return numOpVector((NumericResult) lv, (VectorResult) rv);
        } else if (lv instanceof VectorResult && rv instanceof NumericResult) {
            return vectorOpNum((VectorResult) lv, (NumericResult) rv);
        } else {
            // 1. left labels
            // 2. right labels
            // 3. matching test
            // 4.
            // TODO
        }

        return null;
    }

    private void checkGrouping(PromqlResult lv, PromqlResult rv) {
        if ((!(lv instanceof VectorResult) || !(rv instanceof VectorResult)) &&
            (onLabels != null || ignoreLabels != null)) {
            throw new JprometheusException("Vector matching only allowed between instant vectors");
        }
    }

    private void checkGrouping(MatrixResult lv, MatrixResult rv) {
        if ((!(lv.getOriginalDataType().equals(ResultType.MATRIX)) ||
            !(rv.getOriginalDataType().equals(ResultType.MATRIX))) && (onLabels != null || ignoreLabels != null)) {
            throw new JprometheusException("Vector matching only allowed between instant vectors");
        }
    }

    @Override
    public MatrixResult evalRange(Storage storage, QueryRangeVo vo) {
        long start = DateTimeUtil.timestampLong(vo.getStart());
        long end = DateTimeUtil.timestampLong(vo.getEnd());
        long step = DateTimeUtil.stepMills(vo.getStep());
        MetricUtil.checkRange(start, end, step);

        MatrixResult lv = left.evalRange(storage, vo);
        MatrixResult rv = right.evalRange(storage, vo);

        checkGrouping(lv, rv);

        Map<String, MetricData> lData = lv.getMetricData();
        Map<String, MetricData> rData = rv.getMetricData();
        if (lv.getOriginalDataType().equals(ResultType.SCALAR) && rv.getOriginalDataType().equals(ResultType.SCALAR)) {
            // scalar op scalar
            HashMap<String, MetricData> metricData = new HashMap<>(1);
            lData.forEach((key, metric) -> {
                MetricData data = new MetricData();
                List<Sample> samples = new ArrayList<>();
                List<Sample> lSamples = metric.getSamples();
                List<Sample> rSamples = rData.get(key).getSamples();
                double value = compute(lSamples.get(0).getValue(), rSamples.get(0).getValue());
                for (Sample lSample : lSamples) {
                    samples.add(new Sample(lSample.getTimestamp(), value));
                }
                data.setSamples(samples);
                data.setLabels(new LinkedHashSet<>());
                metricData.put("", data);
            });
            return new MatrixResult(ResultType.SCALAR, metricData);

        } else if (lv.getOriginalDataType().equals(ResultType.SCALAR) &&
            rv.getOriginalDataType().equals(ResultType.MATRIX)) {
            // scalar op matrix
            HashMap<String, MetricData> metricData = new HashMap<>(1);
            List<Sample> lSamples = lData.values().toArray(new MetricData[0])[0].getSamples();
            rData.forEach((key, metric) -> {
                MetricData data = new MetricData();
                List<Sample> samples = new ArrayList<>();
                List<Sample> rSamples = metric.getSamples();
                computeRange(data, samples, lSamples, rSamples, left.isParens());
                LinkedHashSet<Label> labels = new LinkedHashSet<>(metric.getLabels());
                labels.removeIf(l -> "__name__".equals(l.getName()));
                data.setLabels(labels);
                metricData.put(metric.getMetric().getMetricId(), data);
            });
            return new MatrixResult(metricData);
        } else if (lv.getOriginalDataType().equals(ResultType.MATRIX) &&
            rv.getOriginalDataType().equals(ResultType.SCALAR)) {
            //matrix op scalar
            HashMap<String, MetricData> metricData = new HashMap<>(1);
            List<Sample> rSamples = rData.values().toArray(new MetricData[0])[0].getSamples();
            lData.forEach((key, metric) -> {
                MetricData data = new MetricData();
                List<Sample> samples = new ArrayList<>();
                List<Sample> lSamples = metric.getSamples();
                computeRange(data, samples, lSamples, rSamples, left.isParens());
                LinkedHashSet<Label> labels = new LinkedHashSet<>(metric.getLabels());
                labels.removeIf(l -> "__name__".equals(l.getName()));
                data.setLabels(labels);
                metricData.put(metric.getMetric().getMetricId(), data);
            });
            return new MatrixResult(metricData);
        } else {
            // TODO
            return new MatrixResult(new HashMap<>(1));
        }
    }

    private void computeRange(MetricData data, List<Sample> samples, List<Sample> lSamples, List<Sample> rSamples,
        boolean lParens) {
        for (int i = 0; i < lSamples.size(); i++) {
            samples.add(new Sample(lSamples.get(i).getTimestamp(),
                compute(lSamples.get(i).getValue(), rSamples.get(i).getValue())));
        }
        data.setSamples(samples);
    }

    private PromqlResult vectorOpNum(VectorResult lv, NumericResult rv) {
        for (MetricData data : lv.getMetricData().values()) {
            if (data != null && data.getSamples() != null && !data.getSamples().isEmpty()) {
                for (Sample sample : data.getSamples()) {
                    sample.setValue(compute(sample.getValue(), rv.getValue()));
                }
            }
        }
        return lv;
    }

    private PromqlResult numOpVector(NumericResult lv, VectorResult rv) {
        for (MetricData data : rv.getMetricData().values()) {
            if (data != null && data.getSamples() != null && !data.getSamples().isEmpty()) {
                for (Sample sample : data.getSamples()) {
                    sample.setValue(compute(lv.getValue(), sample.getValue()));
                }
            }
        }
        return rv;
    }

    private PromqlResult numOpNum(NumericResult lv, NumericResult rv) {
        return new NumericResult(compute(lv.getValue(), rv.getValue()));
    }

    private JprometheusException opNotSupported() {
        return new JprometheusException(operator + " is not supported for binary operations");
    }

    private Double compute(Double l, Double r) {
        switch (operator) {
            case "^":
                if (!left.isParens() && l < 0) {
                    return -1 * Math.pow(l, r);
                }
                return Math.pow(l, r);
            case "+":
                return l + r;
            case "-":
                return l - r;
            case "*":
                return l * r;
            case "/":
                return l / r;
            case "%":
                return l % r;
            default:
                throw opNotSupported();
        }
    }
}
