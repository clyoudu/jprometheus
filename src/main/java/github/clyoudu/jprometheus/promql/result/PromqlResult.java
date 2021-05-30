package github.clyoudu.jprometheus.promql.result;

import github.clyoudu.jprometheus.api.dto.QueryResultDto;

/**
 * Promql result interface.
 *
 * @author leichen
 * @since 0.0.1, 2021/5/20 5:39 下午
 */
public interface PromqlResult {

    /**
     * convert to api query result
     * @param time time point
     * @return query api result
     */
    default QueryResultDto toQueryResult(String time) {
        return null;
    }

    /**
     * convert to api query range result
     * @return query_range api result
     */
    default QueryResultDto toQueryRangeResult() {
        return null;
    }

}
