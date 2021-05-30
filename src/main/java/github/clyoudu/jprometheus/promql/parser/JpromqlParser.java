package github.clyoudu.jprometheus.promql.parser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import github.clyoudu.jprometheus.exception.JpromqlParseException;
import github.clyoudu.jprometheus.promql.antlr4.PromQLBaseVisitor;
import github.clyoudu.jprometheus.promql.antlr4.PromQLParser;
import github.clyoudu.jprometheus.promql.plan.AggregationPlan;
import github.clyoudu.jprometheus.promql.plan.BinaryOperationPlan;
import github.clyoudu.jprometheus.promql.plan.CompareOperationPlan;
import github.clyoudu.jprometheus.promql.plan.FunctionPlan;
import github.clyoudu.jprometheus.promql.plan.InstantOrRangeSelectorPlan;
import github.clyoudu.jprometheus.promql.plan.JpromqlPlan;
import github.clyoudu.jprometheus.promql.plan.LiteralPlan;
import github.clyoudu.jprometheus.promql.plan.StringPlan;
import github.clyoudu.jprometheus.promql.plan.entity.LabelMatcher;
import github.clyoudu.jprometheus.util.DateTimeUtil;
import org.antlr.v4.runtime.tree.RuleNode;
import org.joda.time.Period;

/**
 * @author leichen
 */
public class JpromqlParser extends PromQLBaseVisitor<JpromqlPlan> {

    @Override
    public JpromqlPlan visitVectorOperation(PromQLParser.VectorOperationContext ctx) {
        PromQLParser.LiteralContext literal = ctx.vector().literal();
        PromQLParser.ParensContext parens = ctx.vector().parens();
        PromQLParser.InstantOrRangeSelectorContext instantOrRangeSelectorContext =
            ctx.vector().instantOrRangeSelector();
        PromQLParser.AggregationContext aggregationContext = ctx.vector().aggregation();
        PromQLParser.FunctionContext functionContext = ctx.vector().function();
        if (literal != null) {
            return visitLiteral(literal);
        } else if (instantOrRangeSelectorContext != null) {
            return visitInstantOrRangeSelector(instantOrRangeSelectorContext);
        } else if (parens != null) {
            JpromqlPlan plan = visitVectorExpression(parens.vectorExpression());
            plan.setParens(true);
            return plan;
        } else if (aggregationContext != null) {
            return visitAggregation(aggregationContext);
        } else if (functionContext != null) {
            return visitFunction(functionContext);
        } else {
            throw new JpromqlParseException("Wrong vector context");
        }
    }

    @Override
    public JpromqlPlan visitFunction(PromQLParser.FunctionContext ctx) {
        String functionName = ctx.IDENTIFIER().getText();
        PromQLParser.ParameterListContext parameterListContext = ctx.parameterList();
        List<JpromqlPlan> params = new ArrayList<>();
        if (parameterListContext != null && parameterListContext.parameter() != null &&
            !parameterListContext.parameter().isEmpty()) {
            List<PromQLParser.ParameterContext> parameterContexts = parameterListContext.parameter();
            for (PromQLParser.ParameterContext parameterContext : parameterContexts) {
                params.add(visitParameter(parameterContext));
            }
        }
        return new FunctionPlan(functionName, params);
    }

    @Override
    public JpromqlPlan visitAggregation(PromQLParser.AggregationContext ctx) {
        String aggregate = ctx.AGGREGATION_OP().getText().toLowerCase();
        PromQLParser.ByContext byContext = ctx.by();
        PromQLParser.WithoutContext withoutContext = ctx.without();
        PromQLParser.ParameterListContext parameterListContext = ctx.parameterList();
        Set<String> byLabels = new LinkedHashSet<>();
        Set<String> withoutLabels = new LinkedHashSet<>();
        if (byContext != null && withoutContext != null) {
            throw new JpromqlParseException(
                "Wrong number of arguments for aggregate expression provided, expected 1, got 2");
        } else if (byContext != null && byContext.labelNameList() != null) {
            List<PromQLParser.LabelNameContext> labelNameContexts = byContext.labelNameList().labelName();
            if (labelNameContexts != null && !labelNameContexts.isEmpty()) {
                for (PromQLParser.LabelNameContext labelNameContext : labelNameContexts) {
                    byLabels.add(labelNameContext.IDENTIFIER().getText());
                }
            }
        } else if (withoutContext != null) {
            List<PromQLParser.LabelNameContext> labelNameContexts = withoutContext.labelNameList().labelName();
            if (labelNameContexts != null && !labelNameContexts.isEmpty()) {
                for (PromQLParser.LabelNameContext labelNameContext : labelNameContexts) {
                    withoutLabels.add(labelNameContext.IDENTIFIER().getText());
                }
            }
        }

        List<JpromqlPlan> parameterList = new ArrayList<>();
        if (parameterListContext != null && parameterListContext.parameter() != null) {
            List<PromQLParser.ParameterContext> parameterContexts = parameterListContext.parameter();
            for (PromQLParser.ParameterContext parameterContext : parameterContexts) {
                parameterList.add(visitParameter(parameterContext));
            }
        }
        return new AggregationPlan(aggregate, parameterList, byLabels, withoutLabels, withoutContext != null);
    }

    @Override
    public JpromqlPlan visitInstantOrRangeSelector(PromQLParser.InstantOrRangeSelectorContext ctx) {
        String metricName = null;
        PromQLParser.MetricNameContext metricNameContext = ctx.instantSelector().metricName();
        if (metricNameContext != null) {
            metricName = metricNameContext.getText();
        }

        List<LabelMatcher> labelMatchers;
        PromQLParser.LabelMatcherListContext labelMatcherListContext = ctx.instantSelector().labelMatcherList();
        if (labelMatcherListContext == null) {
            if (metricName == null) {
                throw new JpromqlParseException("vector selector must contain at least one non-empty matcher");
            }
            labelMatchers = new ArrayList<>();
        } else {
            List<PromQLParser.LabelMatcherContext> labelMatcherContexts = labelMatcherListContext.labelMatcher();
            labelMatchers = labelMatcherContexts.stream().map(c -> {
                String labelName = c.STRING().getText();
                if (labelName.length() > 2) {
                    labelName = labelName.substring(1, labelName.length() - 1);
                }
                if (c.labelMatcherOp().EQ() != null) {
                    return new LabelMatcher(c.labelName().getText(), labelName, LabelMatcher.Type.EQ);
                } else if (c.labelMatcherOp().NE() != null) {
                    return new LabelMatcher(c.labelName().getText(), labelName, LabelMatcher.Type.NEQ);
                } else if (c.labelMatcherOp().RE() != null) {
                    return new LabelMatcher(c.labelName().getText(), labelName, LabelMatcher.Type.RE);
                } else if (c.labelMatcherOp().NRE() != null) {
                    return new LabelMatcher(c.labelName().getText(), labelName, LabelMatcher.Type.NRE);
                }
                return null;
            }).collect(Collectors.toList());
        }
        Period window = null;
        if (ctx.window() != null) {
            window = DateTimeUtil.parsePeriod(ctx.window().DURATION().getText());
        }
        Period offset = null;
        if (ctx.offset() != null) {
            offset = DateTimeUtil.parsePeriod(ctx.offset().DURATION().getText());
        }
        return new InstantOrRangeSelectorPlan(metricName, labelMatchers, window, offset);
    }

    @Override
    public JpromqlPlan visitBinaryOperation(PromQLParser.BinaryOperationContext ctx) {
        PromQLParser.PowOpContext powOpContext = ctx.powOp();
        // TODO
        PromQLParser.AddOpContext addOpContext = ctx.addOp();
        PromQLParser.MultOpContext multOpContext = ctx.multOp();
        //TODO
        PromQLParser.CompareOpContext compareOpContext = ctx.compareOp();
        // TODO
        PromQLParser.AndUnlessOpContext andUnlessOpContext = ctx.andUnlessOp();
        // TODO
        PromQLParser.OrOpContext orOpContext = ctx.orOp();

        BinaryOperationPlan result;
        if (powOpContext != null) {
            result = new BinaryOperationPlan(visit(ctx.vectorExpression(0)), visit(ctx.vectorExpression(1)),
                powOpContext.getText());
        } else if (addOpContext != null) {
            result = new BinaryOperationPlan(visit(ctx.vectorExpression(0)), visit(ctx.vectorExpression(1)),
                addOpContext.getText());
        } else if (multOpContext != null) {
            result = new BinaryOperationPlan(visit(ctx.vectorExpression(0)), visit(ctx.vectorExpression(1)),
                multOpContext.getText());
        } else if (compareOpContext != null) {
            String compareOp = compareOpContext.getText().toLowerCase();
            if (compareOp.contains("bool")) {
                result = new CompareOperationPlan(visit(ctx.vectorExpression(0)), visit(ctx.vectorExpression(1)),
                    compareOp.replace("bool", "".trim()), true);
            } else {
                result = new CompareOperationPlan(visit(ctx.vectorExpression(0)), visit(ctx.vectorExpression(1)),
                    compareOp.replace("bool", "".trim()), false);
            }

        } else {
            // TODO more
            throw new JpromqlParseException("Unsupported binary operation: " + ctx.getText());
        }

        PromQLParser.GroupingContext grouping = ctx.grouping();
        if (grouping != null) {
            PromQLParser.OnContext on = grouping.on();
            PromQLParser.IgnoringContext ignoring = grouping.ignoring();
            PromQLParser.GroupLeftContext groupLeft = grouping.groupLeft();
            PromQLParser.GroupRightContext groupRight = grouping.groupRight();
            if (on != null && on.labelNameList() != null && on.labelNameList().labelName() != null &&
                !on.labelNameList().labelName().isEmpty()) {
                result.setOnLabels(on.labelNameList().labelName().stream().map(c -> c.IDENTIFIER().getText())
                    .collect(Collectors.toSet()));
            }
            if (ignoring != null && ignoring.labelNameList() != null && ignoring.labelNameList().labelName() != null &&
                !ignoring.labelNameList().labelName().isEmpty()) {
                result.setIgnoreLabels(ignoring.labelNameList().labelName().stream().map(c -> c.IDENTIFIER().getText())
                    .collect(Collectors.toSet()));
            }
            if (groupLeft != null) {
                result.setGroupLeft(true);
            }
            if (groupRight != null) {
                result.setGroupRight(true);
            }
        }

        return result;
    }

    @Override
    public JpromqlPlan visitParameter(PromQLParser.ParameterContext ctx) {
        PromQLParser.LiteralContext literal = ctx.literal();
        PromQLParser.VectorExpressionContext vectorExpressionContext = ctx.vectorExpression();
        if (literal != null) {
            return visitLiteral(literal);
        } else if (vectorExpressionContext != null) {
            return visitVectorExpression(vectorExpressionContext);
        }
        return super.visitParameter(ctx);
    }

    public JpromqlPlan visitVectorExpression(PromQLParser.VectorExpressionContext ctx) {
        if (ctx instanceof PromQLParser.BinaryOperationContext) {
            return visitBinaryOperation((PromQLParser.BinaryOperationContext) ctx);
        } else if (ctx instanceof PromQLParser.UnaryOperationContext) {
            return visitUnaryOperation((PromQLParser.UnaryOperationContext) ctx);
        } else if (ctx instanceof PromQLParser.SubqueryOperationContext) {
            return visitSubqueryOperation((PromQLParser.SubqueryOperationContext) ctx);
        } else if (ctx instanceof PromQLParser.VectorOperationContext) {
            return visitVectorOperation((PromQLParser.VectorOperationContext) ctx);
        } else {
            return visit(ctx);
        }
    }

    @Override
    public JpromqlPlan visitLiteral(PromQLParser.LiteralContext ctx) {
        if (ctx.STRING() != null) {
            String text = ctx.STRING().getText();
            if (text.length() > 2) {
                return new StringPlan(text.substring(1, text.length() - 1));
            } else {
                throw new JpromqlParseException("Empty string");
            }
        } else if (ctx.NUMBER() != null) {
            return new LiteralPlan(ctx.NUMBER().getText());
        } else {
            throw new JpromqlParseException("Unexpected expression: " + ctx.getText());
        }
    }

    @Override
    public JpromqlPlan visitUnaryOperation(PromQLParser.UnaryOperationContext ctx) {
        JpromqlPlan plan = visitVectorExpression(ctx.vectorExpression());
        if (ctx.unaryOp().SUB() != null) {
            return new BinaryOperationPlan(new LiteralPlan("-1"), plan, "*");
        } else if (ctx.unaryOp().ADD() != null) {
            return plan;
        }
        return super.visitUnaryOperation(ctx);
    }

    @Override
    protected boolean shouldVisitNextChild(RuleNode node, JpromqlPlan currentResult) {
        if (currentResult != null) {
            return false;
        }
        return true;
    }
}
