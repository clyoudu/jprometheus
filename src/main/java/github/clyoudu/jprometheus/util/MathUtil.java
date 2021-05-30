package github.clyoudu.jprometheus.util;

import java.util.OptionalDouble;

import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * @author leichen
 */
public final class MathUtil {

    private MathUtil() {

    }

    /**
     * compute standard deviation
     * @param values values array
     * @return standard deviation OptionalDouble
     */
    public static OptionalDouble standardDeviation(double[] values) {
        OptionalDouble optionalDouble = standardVariance(values);
        return optionalDouble.isPresent() ? OptionalDouble.of(Math.sqrt(optionalDouble.getAsDouble())) : OptionalDouble
            .empty();

    }

    public static OptionalDouble standardVariance(double[] values) {
        double sum = 0.0;
        double standardDeviation = 0.0;
        int length = values.length;

        for (double num : values) {
            sum += num;
        }

        double mean = sum / length;

        for (double num : values) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return length > 0 ? OptionalDouble.of(standardDeviation / length) : OptionalDouble.empty();
    }

    public static double delta(double[] ascArray) {
        if (ascArray == null || ascArray.length == 0) {
            return 0D;
        } else if (ascArray.length == 1) {
            return ascArray[0];
        } else {
            return ascArray[ascArray.length - 1] - ascArray[0];
        }
    }

    public static double derivative(double[] x, double[] y) {
        SimpleRegression regression = new SimpleRegression();
        int minLength = Math.min(x.length, y.length);
        for (int i = 0; i < minLength; i++) {
            regression.addData(x[i], y[i]);
        }
        return regression.getSlope();
    }

    public static double log2(double value) {
        return Math.log(value) / Math.log(2);
    }
}
