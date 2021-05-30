package github.clyoudu.jprometheus.api.vo;

import javax.validation.constraints.NotBlank;

import lombok.Data;

/**
 * @author leichen
 */
@Data
public class QueryVo {

    @NotBlank(message = "Query must be not null or empty")
    private String query;

    /**
     * rfc3339 | unix_timestamp
     * 2015-07-01T20:10:51.781Z | 1621561367.722
     */
    private String time;

    /**
     * duration: 0ms 1s 2m 3d 4h 5w 6y
     */
    private String timeout;

}
