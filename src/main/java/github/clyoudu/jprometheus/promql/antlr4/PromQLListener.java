// Generated from PromQL.g4 by ANTLR 4.7.1
package github.clyoudu.jprometheus.promql.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PromQLParser}.
 */
public interface PromQLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PromQLParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(PromQLParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(PromQLParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryOperation}
	 * labeled alternative in {@link PromQLParser#vectorExpression}.
	 * @param ctx the parse tree
	 */
	void enterBinaryOperation(PromQLParser.BinaryOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryOperation}
	 * labeled alternative in {@link PromQLParser#vectorExpression}.
	 * @param ctx the parse tree
	 */
	void exitBinaryOperation(PromQLParser.BinaryOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unaryOperation}
	 * labeled alternative in {@link PromQLParser#vectorExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryOperation(PromQLParser.UnaryOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unaryOperation}
	 * labeled alternative in {@link PromQLParser#vectorExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryOperation(PromQLParser.UnaryOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code vectorOperation}
	 * labeled alternative in {@link PromQLParser#vectorExpression}.
	 * @param ctx the parse tree
	 */
	void enterVectorOperation(PromQLParser.VectorOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code vectorOperation}
	 * labeled alternative in {@link PromQLParser#vectorExpression}.
	 * @param ctx the parse tree
	 */
	void exitVectorOperation(PromQLParser.VectorOperationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryOperation}
	 * labeled alternative in {@link PromQLParser#vectorExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryOperation(PromQLParser.SubqueryOperationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryOperation}
	 * labeled alternative in {@link PromQLParser#vectorExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryOperation(PromQLParser.SubqueryOperationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#unaryOp}.
	 * @param ctx the parse tree
	 */
	void enterUnaryOp(PromQLParser.UnaryOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#unaryOp}.
	 * @param ctx the parse tree
	 */
	void exitUnaryOp(PromQLParser.UnaryOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#powOp}.
	 * @param ctx the parse tree
	 */
	void enterPowOp(PromQLParser.PowOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#powOp}.
	 * @param ctx the parse tree
	 */
	void exitPowOp(PromQLParser.PowOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#multOp}.
	 * @param ctx the parse tree
	 */
	void enterMultOp(PromQLParser.MultOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#multOp}.
	 * @param ctx the parse tree
	 */
	void exitMultOp(PromQLParser.MultOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#addOp}.
	 * @param ctx the parse tree
	 */
	void enterAddOp(PromQLParser.AddOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#addOp}.
	 * @param ctx the parse tree
	 */
	void exitAddOp(PromQLParser.AddOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#compareOp}.
	 * @param ctx the parse tree
	 */
	void enterCompareOp(PromQLParser.CompareOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#compareOp}.
	 * @param ctx the parse tree
	 */
	void exitCompareOp(PromQLParser.CompareOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#andUnlessOp}.
	 * @param ctx the parse tree
	 */
	void enterAndUnlessOp(PromQLParser.AndUnlessOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#andUnlessOp}.
	 * @param ctx the parse tree
	 */
	void exitAndUnlessOp(PromQLParser.AndUnlessOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#orOp}.
	 * @param ctx the parse tree
	 */
	void enterOrOp(PromQLParser.OrOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#orOp}.
	 * @param ctx the parse tree
	 */
	void exitOrOp(PromQLParser.OrOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#vector}.
	 * @param ctx the parse tree
	 */
	void enterVector(PromQLParser.VectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#vector}.
	 * @param ctx the parse tree
	 */
	void exitVector(PromQLParser.VectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#parens}.
	 * @param ctx the parse tree
	 */
	void enterParens(PromQLParser.ParensContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#parens}.
	 * @param ctx the parse tree
	 */
	void exitParens(PromQLParser.ParensContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#instantOrRangeSelector}.
	 * @param ctx the parse tree
	 */
	void enterInstantOrRangeSelector(PromQLParser.InstantOrRangeSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#instantOrRangeSelector}.
	 * @param ctx the parse tree
	 */
	void exitInstantOrRangeSelector(PromQLParser.InstantOrRangeSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#instantSelector}.
	 * @param ctx the parse tree
	 */
	void enterInstantSelector(PromQLParser.InstantSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#instantSelector}.
	 * @param ctx the parse tree
	 */
	void exitInstantSelector(PromQLParser.InstantSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#window}.
	 * @param ctx the parse tree
	 */
	void enterWindow(PromQLParser.WindowContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#window}.
	 * @param ctx the parse tree
	 */
	void exitWindow(PromQLParser.WindowContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#offset}.
	 * @param ctx the parse tree
	 */
	void enterOffset(PromQLParser.OffsetContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#offset}.
	 * @param ctx the parse tree
	 */
	void exitOffset(PromQLParser.OffsetContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#subquery}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(PromQLParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#subquery}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(PromQLParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#labelMatcher}.
	 * @param ctx the parse tree
	 */
	void enterLabelMatcher(PromQLParser.LabelMatcherContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#labelMatcher}.
	 * @param ctx the parse tree
	 */
	void exitLabelMatcher(PromQLParser.LabelMatcherContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#labelMatcherOp}.
	 * @param ctx the parse tree
	 */
	void enterLabelMatcherOp(PromQLParser.LabelMatcherOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#labelMatcherOp}.
	 * @param ctx the parse tree
	 */
	void exitLabelMatcherOp(PromQLParser.LabelMatcherOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#labelMatcherList}.
	 * @param ctx the parse tree
	 */
	void enterLabelMatcherList(PromQLParser.LabelMatcherListContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#labelMatcherList}.
	 * @param ctx the parse tree
	 */
	void exitLabelMatcherList(PromQLParser.LabelMatcherListContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#function}.
	 * @param ctx the parse tree
	 */
	void enterFunction(PromQLParser.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#function}.
	 * @param ctx the parse tree
	 */
	void exitFunction(PromQLParser.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(PromQLParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(PromQLParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void enterParameterList(PromQLParser.ParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void exitParameterList(PromQLParser.ParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#aggregation}.
	 * @param ctx the parse tree
	 */
	void enterAggregation(PromQLParser.AggregationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#aggregation}.
	 * @param ctx the parse tree
	 */
	void exitAggregation(PromQLParser.AggregationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#by}.
	 * @param ctx the parse tree
	 */
	void enterBy(PromQLParser.ByContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#by}.
	 * @param ctx the parse tree
	 */
	void exitBy(PromQLParser.ByContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#without}.
	 * @param ctx the parse tree
	 */
	void enterWithout(PromQLParser.WithoutContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#without}.
	 * @param ctx the parse tree
	 */
	void exitWithout(PromQLParser.WithoutContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#grouping}.
	 * @param ctx the parse tree
	 */
	void enterGrouping(PromQLParser.GroupingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#grouping}.
	 * @param ctx the parse tree
	 */
	void exitGrouping(PromQLParser.GroupingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#on}.
	 * @param ctx the parse tree
	 */
	void enterOn(PromQLParser.OnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#on}.
	 * @param ctx the parse tree
	 */
	void exitOn(PromQLParser.OnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#ignoring}.
	 * @param ctx the parse tree
	 */
	void enterIgnoring(PromQLParser.IgnoringContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#ignoring}.
	 * @param ctx the parse tree
	 */
	void exitIgnoring(PromQLParser.IgnoringContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#groupLeft}.
	 * @param ctx the parse tree
	 */
	void enterGroupLeft(PromQLParser.GroupLeftContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#groupLeft}.
	 * @param ctx the parse tree
	 */
	void exitGroupLeft(PromQLParser.GroupLeftContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#groupRight}.
	 * @param ctx the parse tree
	 */
	void enterGroupRight(PromQLParser.GroupRightContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#groupRight}.
	 * @param ctx the parse tree
	 */
	void exitGroupRight(PromQLParser.GroupRightContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#metricName}.
	 * @param ctx the parse tree
	 */
	void enterMetricName(PromQLParser.MetricNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#metricName}.
	 * @param ctx the parse tree
	 */
	void exitMetricName(PromQLParser.MetricNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#metricKeyword}.
	 * @param ctx the parse tree
	 */
	void enterMetricKeyword(PromQLParser.MetricKeywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#metricKeyword}.
	 * @param ctx the parse tree
	 */
	void exitMetricKeyword(PromQLParser.MetricKeywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#labelName}.
	 * @param ctx the parse tree
	 */
	void enterLabelName(PromQLParser.LabelNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#labelName}.
	 * @param ctx the parse tree
	 */
	void exitLabelName(PromQLParser.LabelNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#labelNameList}.
	 * @param ctx the parse tree
	 */
	void enterLabelNameList(PromQLParser.LabelNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#labelNameList}.
	 * @param ctx the parse tree
	 */
	void exitLabelNameList(PromQLParser.LabelNameListContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#labelKeyword}.
	 * @param ctx the parse tree
	 */
	void enterLabelKeyword(PromQLParser.LabelKeywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#labelKeyword}.
	 * @param ctx the parse tree
	 */
	void exitLabelKeyword(PromQLParser.LabelKeywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PromQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(PromQLParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link PromQLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(PromQLParser.LiteralContext ctx);
}