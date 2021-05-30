package github.clyoudu.jprometheus.api.dto;

import github.clyoudu.jprometheus.api.enums.ResultStatus;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author leichen
 */
@Data
@RequiredArgsConstructor
public class QueryErrorResultDto {

    private String status = ResultStatus.ERROR.name().toLowerCase();

    private String errorType = "bad_data";

    @NonNull
    private String error;

}
