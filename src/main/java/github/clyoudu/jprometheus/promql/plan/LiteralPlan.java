package github.clyoudu.jprometheus.promql.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import github.clyoudu.jprometheus.api.enums.ResultType;
import github.clyoudu.jprometheus.api.vo.QueryRangeVo;
import github.clyoudu.jprometheus.api.vo.QueryVo;
import github.clyoudu.jprometheus.promql.result.MatrixResult;
import github.clyoudu.jprometheus.promql.result.NumericResult;
import github.clyoudu.jprometheus.promql.result.PromqlResult;
import github.clyoudu.jprometheus.storage.Storage;
import github.clyoudu.jprometheus.storage.entity.Metric;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import github.clyoudu.jprometheus.storage.entity.Sample;
import github.clyoudu.jprometheus.util.DateTimeUtil;
import github.clyoudu.jprometheus.util.MetricUtil;
import lombok.Getter;

/**
 * @author leichen
 */
public class LiteralPlan extends VectorOperationPlan {

    private static final String INF = "Inf";

    private static final String POSITIVE_INF = "+Inf";

    private static final String NEGATIVE_INF = "-Inf";

    private static final String HEX_PREFIX = "0x";

    private static final String HEX_PREFIX_UP = "0X";

    @Getter
    private String expr;

    public LiteralPlan(String expr) {
        this.expr = expr;
    }

    @Override
    public PromqlResult eval(Storage storage, QueryVo vo) {
        return new NumericResult(eval());
    }

    private Double eval() {
        if (expr.equalsIgnoreCase(INF) || expr.equalsIgnoreCase(POSITIVE_INF)) {
            return Double.POSITIVE_INFINITY;
        } else if (expr.equalsIgnoreCase(NEGATIVE_INF)) {
            return Double.NEGATIVE_INFINITY;
        } else if (expr.startsWith(HEX_PREFIX) || expr.startsWith(HEX_PREFIX_UP)) {
            return (double) Integer.parseInt(expr.substring(2, expr.length()), 16);
        }
        return Double.parseDouble(expr);
    }

    @Override
    public MatrixResult evalRange(Storage storage, QueryRangeVo vo) {
        long start = DateTimeUtil.timestampLong(vo.getStart());
        long end = DateTimeUtil.timestampLong(vo.getEnd());
        long step = DateTimeUtil.stepMills(vo.getStep());
        MetricUtil.checkRange(start, end, step);

        Double value = eval();

        HashMap<String, MetricData> metricData = new HashMap<>(1);
        MetricData data = new MetricData();
        data.setMetric(new Metric("", ""));
        data.setLabels(new LinkedHashSet<>());
        List<Sample> samples = new ArrayList<>();
        for (long i = start; i <= end; i += step) {
            samples.add(new Sample(i, value));
        }
        data.setSamples(samples);
        metricData.put("", data);
        return new MatrixResult(ResultType.SCALAR, metricData);
    }
}
