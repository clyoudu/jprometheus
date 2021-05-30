package github.clyoudu.jprometheus.config;

import github.clyoudu.jprometheus.api.dto.QueryErrorResultDto;
import github.clyoudu.jprometheus.exception.JprometheusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author leichen
 */
@ControllerAdvice
@Slf4j
public class JprometheusControllerAdvice {

    @ExceptionHandler({JprometheusException.class})
    @ResponseBody
    public QueryErrorResultDto errorHandler(JprometheusException e){
        log.error("error", e);
        return new QueryErrorResultDto(e.getMessage());
    }

}
