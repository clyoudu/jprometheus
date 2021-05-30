package github.clyoudu.jprometheus.api.dto;

import github.clyoudu.jprometheus.api.enums.ResultType;
import lombok.Getter;
import lombok.Setter;

/**
 * @author leichen
 */
@Getter
@Setter
public class ScalarData extends ResultData {

    private Object[] result = new Object[2];

    public ScalarData(double time, String value) {
        super(ResultType.SCALAR.name().toLowerCase());
        result[0] = time;
        result[1] = value;
    }

}
