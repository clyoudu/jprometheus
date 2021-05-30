package github.clyoudu.jprometheus.promql.plan;

import lombok.Getter;

/**
 * @author leichen
 */
public class StringPlan extends JpromqlPlan {

    @Getter
    private String expr;

    public StringPlan(String expr) {
        this.expr = expr;
    }
}
