package github.clyoudu.jprometheus.promql.plan.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author leichen
 */
@Data
@AllArgsConstructor
public class LabelMatcher {

    private String label;

    private String value;

    private Type matchType;

    public enum Type {
        /**
         * equal
         */
        EQ,
        /**
         * not equal
         */
        NEQ,
        /**
         * regex like
         */
        RE,
        /**
         * regex not like
         */
        NRE;
    }

}
