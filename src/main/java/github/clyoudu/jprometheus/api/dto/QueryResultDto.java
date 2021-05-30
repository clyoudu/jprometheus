package github.clyoudu.jprometheus.api.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * abstract result
 * @author leichen
 */
@Getter
@Setter
public class QueryResultDto {

    private String status;

    private ResultData data;

}
