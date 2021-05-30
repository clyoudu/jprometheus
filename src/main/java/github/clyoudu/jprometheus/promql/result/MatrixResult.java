package github.clyoudu.jprometheus.promql.result;

import java.util.Map;

import github.clyoudu.jprometheus.api.dto.MatrixData;
import github.clyoudu.jprometheus.api.dto.QueryResultDto;
import github.clyoudu.jprometheus.api.enums.ResultStatus;
import github.clyoudu.jprometheus.api.enums.ResultType;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import lombok.Getter;

/**
 * @author leichen
 */
public class MatrixResult implements PromqlResult {

    @Getter
    private ResultType originalDataType = ResultType.MATRIX;

    @Getter
    private Map<String, MetricData> metricData;

    public MatrixResult(Map<String, MetricData> metricData) {
        this.metricData = metricData;
    }

    public MatrixResult(ResultType originalDataType, Map<String, MetricData> metricData) {
        this.originalDataType = originalDataType;
        this.metricData = metricData;
    }

    @Override
    public QueryResultDto toQueryRangeResult() {
        QueryResultDto result = new QueryResultDto();
        result.setStatus(ResultStatus.SUCCESS.name().toLowerCase());
        result.setData(new MatrixData(metricData.values()));
        return result;
    }
}
