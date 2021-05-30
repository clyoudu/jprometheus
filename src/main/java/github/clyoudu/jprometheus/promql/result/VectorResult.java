package github.clyoudu.jprometheus.promql.result;

import java.util.Map;

import github.clyoudu.jprometheus.api.dto.MatrixData;
import github.clyoudu.jprometheus.api.dto.QueryResultDto;
import github.clyoudu.jprometheus.api.dto.VectorData;
import github.clyoudu.jprometheus.api.enums.ResultStatus;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import lombok.Data;
import org.joda.time.Period;

/**
 * @author leichen
 */
@Data
public class VectorResult implements PromqlResult {

    private Period window;

    private Map<String, MetricData> metricData;

    public VectorResult(Period window, Map<String, MetricData> metricData) {
        this.window = window;
        this.metricData = metricData;
    }

    @Override
    public QueryResultDto toQueryResult(String time) {
        QueryResultDto result = new QueryResultDto();
        result.setStatus(ResultStatus.SUCCESS.name().toLowerCase());
        if (window != null) {
            result.setData(new MatrixData(metricData.values()));
        } else {
            result.setData(new VectorData(metricData.values()));
        }
        return result;
    }
}
