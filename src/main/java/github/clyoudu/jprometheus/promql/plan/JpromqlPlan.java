package github.clyoudu.jprometheus.promql.plan;

import github.clyoudu.jprometheus.api.vo.QueryRangeVo;
import github.clyoudu.jprometheus.api.vo.QueryVo;
import github.clyoudu.jprometheus.promql.result.MatrixResult;
import github.clyoudu.jprometheus.promql.result.PromqlResult;
import github.clyoudu.jprometheus.storage.Storage;
import lombok.Getter;
import lombok.Setter;

/**
 * Plan interface.
 *
 * @author leichen
 * @since 0.0.1, 2021/5/20 5:36 下午
 */
public abstract class JpromqlPlan {

    @Setter
    @Getter
    private boolean parens = false;

    /**
     * eval result
     * @param storage storage for query
     * @param vo 查询条件
     * @return PromqlResult
     */
    public PromqlResult eval(Storage storage, QueryVo vo) {
        throw new UnsupportedOperationException("Eval not supported");
    }

    /**
     * eval range result
     * @param storage storage for query
     * @param vo 查询条件
     * @return PromqlResult
     */
    public MatrixResult evalRange(Storage storage, QueryRangeVo vo) {
        throw new UnsupportedOperationException("Eval range not supported");
    }

}
