package github.clyoudu.jprometheus.promql.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import github.clyoudu.jprometheus.storage.Storage;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import github.clyoudu.jprometheus.storage.entity.Sample;
import github.clyoudu.jprometheus.util.DateTimeUtil;
import github.clyoudu.jprometheus.util.MetricUtil;

/**
 * @author leichen
 */
public class CompareOperationPlan extends BinaryOperationPlan {

    private boolean bool;

    public CompareOperationPlan(JpromqlPlan left, JpromqlPlan right, String operator, boolean bool) {
        super(left, right, operator);
        this.bool = bool;
    }

    @Override
    public PromqlResult eval(Storage storage, QueryVo vo) {
        PromqlResult lv = getLeft().eval(storage, vo);
        PromqlResult rv = getRight().eval(storage, vo);
        if (lv instanceof NumericResult && rv instanceof NumericResult) {
            if (!bool) {
                throw new JprometheusException("Comparisons between scalars must use BOOL modifier");
            }
            return numCompareNum((NumericResult) lv, (NumericResult) rv);
        }
        //TODO
        return super.eval(storage, vo);
    }

    private PromqlResult numCompareNum(NumericResult lv, NumericResult rv) {
        Double l = lv.getValue();
        Double r = rv.getValue();
        return new NumericResult(compute(l, r));
    }

    private double compute(Double l, Double r) {
        switch (getOperator()) {
            case "==":
                return l.equals(r) ? 1D : 0D;
            case "!=":
                return !l.equals(r) ? 1D : 0D;
            case ">=":
                return l >= r ? 1D : 0D;
            case ">":
                return l > r ? 1D : 0D;
            case "<=":
                return l <= r ? 1D : 0D;
            case "<":
                return l < r ? 1D : 0D;
            default:
                throw new JprometheusException("Unsupported compare operation: " + getOperator());
        }
    }

    @Override
    public MatrixResult evalRange(Storage storage, QueryRangeVo vo) {
        long start = DateTimeUtil.timestampLong(vo.getStart());
        long end = DateTimeUtil.timestampLong(vo.getEnd());
        long step = DateTimeUtil.stepMills(vo.getStep());
        MetricUtil.checkRange(start, end, step);

        MatrixResult lv = getLeft().evalRange(storage, vo);
        MatrixResult rv = getRight().evalRange(storage, vo);
        Map<String, MetricData> lData = lv.getMetricData();
        Map<String, MetricData> rData = rv.getMetricData();
        if (lv.getOriginalDataType().equals(ResultType.SCALAR) && rv.getOriginalDataType().equals(ResultType.SCALAR)) {
            // scalar compare scalar
            if (!bool) {
                throw new JprometheusException("Comparisons between scalars must use BOOL modifier");
            }
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
            // scalar compare matrix
            List<Sample> lSamples = lData.values().toArray(new MetricData[0])[0].getSamples();
            if (bool) {
                rData.forEach((key, metric) -> {
                    Iterator<Sample> iteratorR = metric.getSamples().iterator();
                    Iterator<Sample> iteratorL = lSamples.iterator();
                    while (iteratorL.hasNext() && iteratorR.hasNext()) {
                        Sample rSample = iteratorR.next();
                        Sample lSample = iteratorL.next();
                        rSample.setValue(compute(lSample.getValue(), rSample.getValue()));
                        metric.getLabels().removeIf(k -> "__name__".equals(k.getName()));
                    }
                });
            } else {
                Set<String> idToDelete = new HashSet<>();
                rData.forEach((key, metric) -> {
                    Iterator<Sample> iteratorR = metric.getSamples().iterator();
                    Iterator<Sample> iteratorL = lSamples.iterator();
                    while (iteratorL.hasNext() && iteratorR.hasNext()) {
                        Sample rSample = iteratorR.next();
                        Sample lSample = iteratorL.next();
                        double v = compute(lSample.getValue(), rSample.getValue());
                        if (v <= 0) {
                            iteratorR.remove();
                        }
                    }
                    if (metric.getSamples().isEmpty()) {
                        idToDelete.add(metric.getMetric().getMetricId());
                    }
                });
                idToDelete.forEach(id -> rv.getMetricData().remove(id));
            }
            return rv;
        } else if (lv.getOriginalDataType().equals(ResultType.MATRIX) &&
            rv.getOriginalDataType().equals(ResultType.SCALAR)) {
            // matrix compare scalar
            List<Sample> rSamples = rData.values().toArray(new MetricData[0])[0].getSamples();
            if (bool) {
                lData.forEach((key, metric) -> {
                    Iterator<Sample> iteratorL = metric.getSamples().iterator();
                    Iterator<Sample> iteratorR = rSamples.iterator();
                    while (iteratorL.hasNext() && iteratorR.hasNext()) {
                        Sample rSample = iteratorR.next();
                        Sample lSample = iteratorL.next();
                        lSample.setValue(compute(lSample.getValue(), rSample.getValue()));
                        metric.getLabels().removeIf(k -> "__name__".equals(k.getName()));
                    }
                });
            } else {
                Set<String> idToDelete = new HashSet<>();
                lData.forEach((key, metric) -> {
                    Iterator<Sample> iteratorL = metric.getSamples().iterator();
                    Iterator<Sample> iteratorR = rSamples.iterator();
                    while (iteratorL.hasNext() && iteratorR.hasNext()) {
                        Sample rSample = iteratorR.next();
                        Sample lSample = iteratorL.next();
                        double v = compute(lSample.getValue(), rSample.getValue());
                        if (v <= 0) {
                            iteratorL.remove();
                        }
                    }
                    if (metric.getSamples().isEmpty()) {
                        idToDelete.add(metric.getMetric().getMetricId());
                    }
                });
                idToDelete.forEach(id -> lv.getMetricData().remove(id));
            }
            return lv;
        } else {
            //TODO
        }

        return super.evalRange(storage, vo);
    }
}
