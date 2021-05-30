package github.clyoudu.jprometheus.api;

import javax.validation.Valid;

import github.clyoudu.jprometheus.api.dto.QueryResultDto;
import github.clyoudu.jprometheus.api.vo.QueryRangeVo;
import github.clyoudu.jprometheus.api.vo.QueryVo;
import github.clyoudu.jprometheus.promql.antlr4.PromQLLexer;
import github.clyoudu.jprometheus.promql.antlr4.PromQLParser;
import github.clyoudu.jprometheus.promql.parser.JpromqlErrorListener;
import github.clyoudu.jprometheus.promql.parser.JpromqlParser;
import github.clyoudu.jprometheus.promql.plan.JpromqlPlan;
import github.clyoudu.jprometheus.storage.Storage;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author leichen
 */
@RequestMapping("api/v1")
@RestController
@Validated
public class ApiController {

    @Autowired
    private Storage storage;

    @GetMapping("query")
    @PostMapping("query")
    public QueryResultDto query(@Valid QueryVo vo) {
        if (StringUtils.isBlank(vo.getTime())) {
            vo.setTime(System.currentTimeMillis() / 1000D + "");
        }
        JpromqlParser jpromqlParser = new JpromqlParser();
        PromQLLexer lexer = new PromQLLexer(CharStreams.fromString(vo.getQuery()));
        PromQLParser parser = new PromQLParser(new CommonTokenStream(lexer));
        parser.addErrorListener(new JpromqlErrorListener());
        JpromqlPlan plan = jpromqlParser.visit(parser.expression());
        return plan.eval(storage, vo).toQueryResult(vo.getTime());
    }

    @GetMapping("query_range")
    @PostMapping("query_range")
    public QueryResultDto queryRange(@Valid QueryRangeVo vo) {
        JpromqlParser jpromqlParser = new JpromqlParser();
        PromQLLexer lexer = new PromQLLexer(CharStreams.fromString(vo.getQuery()));
        PromQLParser parser = new PromQLParser(new CommonTokenStream(lexer));
        parser.addErrorListener(new JpromqlErrorListener());
        JpromqlPlan plan = jpromqlParser.visit(parser.expression());
        return plan.evalRange(storage, vo).toQueryRangeResult();
    }

}
