package github.clyoudu.jprometheus.storage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import github.clyoudu.jprometheus.config.JprometheusProperties;
import github.clyoudu.jprometheus.exception.JprometheusException;
import github.clyoudu.jprometheus.promql.plan.entity.LabelMatcher;
import github.clyoudu.jprometheus.storage.entity.Label;
import github.clyoudu.jprometheus.storage.entity.Metric;
import github.clyoudu.jprometheus.storage.entity.MetricData;
import github.clyoudu.jprometheus.storage.entity.Sample;
import github.clyoudu.jprometheus.util.DateTimeUtil;
import org.joda.time.Period;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author leichen
 */
public class PrometheusStorage implements Storage {

    private RestTemplate restTemplate = new RestTemplate();

    private JprometheusProperties properties;

    public PrometheusStorage(JprometheusProperties properties) {
        this.properties = properties;
    }

    @Override
    public Map<String, MetricData> queryVector(String metricName, List<LabelMatcher> labelMatcherList, Period window,
        Period offset, Long time) {
        try {
            UriComponentsBuilder uriComponentsBuilder =
                UriComponentsBuilder.fromHttpUrl(properties.getStorage().getQueryUrl() + "/query");
            uriComponentsBuilder.queryParam("query", URLEncoder
                .encode(getQuery(metricName, labelMatcherList, window, offset), StandardCharsets.UTF_8.name()))
                .queryParam("time", time / 1000D);
            return parsePrometheusJson(uriComponentsBuilder);
        } catch (Exception e) {
            throw new JprometheusException("Query prometheus error", e);
        }
    }

    private Map<String, MetricData> parsePrometheusJson(UriComponentsBuilder uriComponentsBuilder) {
        String resultContent = restTemplate.getForObject(uriComponentsBuilder.build(true).toUri(), String.class);
        JSONObject resultJson = JSON.parseObject(resultContent);
        String status = resultJson.getString("status");
        if (!"success".equalsIgnoreCase(status)) {
            throw new JprometheusException(resultJson.getString("error"));
        }
        return resultJson.getJSONObject("data").getJSONArray("result").stream().map(o -> (JSONObject) o)
            .collect(Collectors.toMap(o -> o.getString("metric"), o -> {
                MetricData data = new MetricData();
                data.setMetric(new Metric(o.getString("metric"), o.getJSONObject("metric").getString("__name__")));
                data.setLabels(new LinkedHashSet<>(o.getJSONObject("metric").entrySet().stream()
                    .map(entry -> new Label(entry.getKey(), (String) entry.getValue())).collect(Collectors.toSet())));
                JSONArray value = o.getJSONArray("value");
                if (value != null) {
                    ArrayList<Sample> samples = new ArrayList<>();
                    samples.add(new Sample((long) (value.getDouble(0) * 1000D), value.getDouble(1)));
                    data.setSamples(samples);
                } else {
                    data.setSamples(o.getJSONArray("values").stream().map(v -> (JSONArray) v)
                        .map(v -> new Sample((long) (v.getDouble(0) * 1000D), v.getDouble(1)))
                        .collect(Collectors.toList()));
                }
                return data;
            }));
    }

    private String getQuery(String metricName, List<LabelMatcher> labelMatcherList, Period window, Period offset) {
        String format = "%s{%s}%s%s";
        return String.format(format, metricName == null ? "" : metricName, getLabels(labelMatcherList),
            window == null ? "" : ("[" + DateTimeUtil.periodMills(window) + "ms]"),
            offset == null ? "" : ("offset " + DateTimeUtil.periodMills(offset) + "ms"));
    }

    private String getLabels(List<LabelMatcher> labelMatcherList) {
        return labelMatcherList.stream().map(m -> {
            switch (m.getMatchType()) {
                case EQ:
                    return m.getLabel() + "=\"" + m.getValue() + "\"";
                case NEQ:
                    return m.getLabel() + "!=\"" + m.getValue() + "\"";
                case RE:
                    return m.getLabel() + "=~\"" + m.getValue() + "\"";
                case NRE:
                    return m.getLabel() + "!~\"" + m.getValue() + "\"";
                default:
                    return "";
            }
        }).filter(s -> !s.isEmpty()).collect(Collectors.joining(","));
    }

    @Override
    public void scrapSample(Metric metric, LinkedHashSet<Label> labels, Sample sample) {

    }

    @Override
    public Map<String, MetricData> queryMatrix(String metricName, List<LabelMatcher> labelMatcherList, Period offset,
        long start, long end, long step) {
        try {
            UriComponentsBuilder uriComponentsBuilder =
                UriComponentsBuilder.fromHttpUrl(properties.getStorage().getQueryUrl() + "/query_range");
            uriComponentsBuilder.queryParam("query",
                URLEncoder.encode(getQueryRange(metricName, labelMatcherList, offset), StandardCharsets.UTF_8.name()))
                .queryParam("start", start / 1000D).queryParam("end", end / 1000D).queryParam("step", step / 1000);
            return parsePrometheusJson(uriComponentsBuilder);
        } catch (Exception e) {
            throw new JprometheusException("Query prometheus error", e);
        }
    }

    private String getQueryRange(String metricName, List<LabelMatcher> labelMatcherList, Period offset) {
        String format = "%s{%s}%s";
        return String.format(format, metricName == null ? "" : metricName, getLabels(labelMatcherList),
            offset == null ? "" : ("offset " + DateTimeUtil.periodMills(offset) + "ms"));
    }
}
