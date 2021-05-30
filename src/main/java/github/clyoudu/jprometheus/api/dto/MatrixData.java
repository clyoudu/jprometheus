package github.clyoudu.jprometheus.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import github.clyoudu.jprometheus.api.enums.ResultType;
import github.clyoudu.jprometheus.storage.entity.Label;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import github.clyoudu.jprometheus.storage.entity.Sample;
import lombok.Getter;
import lombok.Setter;

/**
 * @author leichen
 */
@Getter
@Setter
public class MatrixData extends ResultData {

    private List<MetricMatrix> result = new ArrayList<>();

    public MatrixData(Collection<MetricData> metrics) {
        super(ResultType.MATRIX.name().toLowerCase());
        for (MetricData metric : metrics) {
            MetricMatrix m = new MetricMatrix();
            Map<String, String> metricLabels =
                metric.getLabels().stream().collect(Collectors.toMap(Label::getName, Label::getValue));
            m.setMetric(metricLabels);
            List<Sample> samples = metric.getSamples();
            for (Sample sample : samples) {
                Object[] value = new Object[2];
                value[0] = sample.getTimestamp() / 1000D;
                if (sample.getValue().equals(Double.NEGATIVE_INFINITY)) {
                    value[1] = "-Inf";
                } else if (sample.getValue().equals(Double.POSITIVE_INFINITY)) {
                    value[1] = "+Inf";
                } else if (sample.getValue().equals(Double.NaN)) {
                    value[1] = "NaN";
                } else {
                    value[1] = new BigDecimal(sample.getValue() + "").toPlainString();
                }
                m.getValues().add(value);
            }
            result.add(m);
        }
    }

    @Getter
    @Setter
    public static class MetricMatrix {

        private Map<String, String> metric;

        private List<Object[]> values = new ArrayList<>();
    }

}
