package github.clyoudu.jprometheus.api.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @author leichen
 */
@Getter
@Setter
public abstract class ResultData {

    private String resultType;

    public ResultData(String resultType) {
        this.resultType = resultType;
    }
}

