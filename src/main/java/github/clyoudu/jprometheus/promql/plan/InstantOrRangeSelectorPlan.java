package github.clyoudu.jprometheus.promql.plan;

import java.util.List;
import java.util.Map;

import github.clyoudu.jprometheus.api.vo.QueryRangeVo;
import github.clyoudu.jprometheus.api.vo.QueryVo;
import github.clyoudu.jprometheus.promql.plan.entity.LabelMatcher;
import github.clyoudu.jprometheus.promql.result.MatrixResult;
import github.clyoudu.jprometheus.promql.result.PromqlResult;
import github.clyoudu.jprometheus.promql.result.VectorResult;
import github.clyoudu.jprometheus.storage.Storage;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import github.clyoudu.jprometheus.util.DateTimeUtil;
import github.clyoudu.jprometheus.util.MetricUtil;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.Period;

/**
 * @author leichen
 */
@Getter
@Setter
public class InstantOrRangeSelectorPlan extends JpromqlPlan {

    private String metricName;

    private List<LabelMatcher> labelMatcherList;

    private Period window;

    private Period offset;

    public InstantOrRangeSelectorPlan(String metricName, List<LabelMatcher> labelMatcherList, Period window,
        Period offset) {
        this.metricName = metricName;
        this.labelMatcherList = labelMatcherList;
        this.window = window;
        this.offset = offset;
    }

    public InstantOrRangeSelectorPlan(String metricName, List<LabelMatcher> labelMatcherList) {
        this.metricName = metricName;
        this.labelMatcherList = labelMatcherList;
    }

    @Override
    public PromqlResult eval(Storage storage, QueryVo vo) {
        Long time = DateTimeUtil.timestampLong(vo.getTime());
        Map<String, MetricData> result = storage.queryVector(metricName, labelMatcherList, window, offset, time);
        return new VectorResult(window, result);
    }

    @Override
    public MatrixResult evalRange(Storage storage, QueryRangeVo vo) {
        long start = DateTimeUtil.timestampLong(vo.getStart());
        long end = DateTimeUtil.timestampLong(vo.getEnd());
        long step = DateTimeUtil.stepMills(vo.getStep());
        MetricUtil.checkRange(start, end, step);

        Map<String, MetricData> result = storage.queryMatrix(metricName, labelMatcherList, offset, start, end, step);

        return new MatrixResult(result);
    }
}
