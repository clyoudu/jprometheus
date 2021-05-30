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
public class VectorData extends ResultData {

    private List<MetricVector> result = new ArrayList<>();

    public VectorData(Collection<MetricData> metrics) {
        super(ResultType.VECTOR.name().toLowerCase());
        for (MetricData metric : metrics) {
            Map<String, String> metricLabels =
                metric.getLabels().stream().collect(Collectors.toMap(Label::getName, Label::getValue));
            MetricVector m = new MetricVector();
            m.setMetric(metricLabels);
            Sample sample = metric.getSamples().get(0);
            m.getValue()[0] = sample.getTimestamp() / 1000D;
            if (sample.getValue().equals(Double.NEGATIVE_INFINITY)) {
                m.getValue()[1] = "-Inf";
            } else if (sample.getValue().equals(Double.POSITIVE_INFINITY)) {
                m.getValue()[1] = "+Inf";
            } else if (sample.getValue().equals(Double.NaN)) {
                m.getValue()[1] = "NaN";
            } else {
                m.getValue()[1] = new BigDecimal(sample.getValue() + "").toPlainString();
            }
            result.add(m);
        }
    }

    @Getter
    @Setter
    public static class MetricVector {

        private Map<String, String> metric;

        private Object[] value = new Object[2];
    }

}
