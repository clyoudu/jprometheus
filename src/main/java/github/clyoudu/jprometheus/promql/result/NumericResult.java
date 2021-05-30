package github.clyoudu.jprometheus.promql.result;

import github.clyoudu.jprometheus.api.dto.QueryResultDto;
import github.clyoudu.jprometheus.api.dto.ScalarData;
import github.clyoudu.jprometheus.api.enums.ResultStatus;
import github.clyoudu.jprometheus.util.DateTimeUtil;
import lombok.Data;

/**
 * Numbric result.
 *
 *
 * @author leichen
 * @since 0.0.1, 2021/5/20 5:44 下午
 */
@Data
public class NumericResult implements PromqlResult {

    private Double value;

    public NumericResult(Double value) {
        this.value = value;
    }

    @Override
    public QueryResultDto toQueryResult(String time) {
        QueryResultDto result = new QueryResultDto();
        result.setStatus(ResultStatus.SUCCESS.name().toLowerCase());
        result.setData(new ScalarData(DateTimeUtil.timestampDouble(time), getStringValue()));
        return result;
    }

    private String getStringValue() {
        return value.toString();
    }
}
