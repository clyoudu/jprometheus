package github.clyoudu.jprometheus.storage.entity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import lombok.Data;

/**
 * metric
 * @author leichen
 */
@Data
public class MetricData {

    private Metric metric;

    private LinkedHashSet<Label> labels = new LinkedHashSet<>(4);

    private List<Sample> samples = new ArrayList<>();
}
