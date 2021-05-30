package github.clyoudu.jprometheus.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import github.clyoudu.jprometheus.storage.InMemoryStorage;
import github.clyoudu.jprometheus.storage.PrometheusStorage;
import github.clyoudu.jprometheus.storage.Storage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @author leichen
 */
@Configuration
@EnableConfigurationProperties(JprometheusProperties.class)
public class JacksonConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ArrayList<Module> modules = new ArrayList<>();

        SimpleModule collectionTypeSerializerModule = new SimpleModule();
        collectionTypeSerializerModule.addSerializer(Double.class, new JprometheusDoubleSerializer());
        modules.add(collectionTypeSerializerModule);

        return Jackson2ObjectMapperBuilder.json().modules(modules).build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "jprometheus.storage.type", havingValue = "mempry", matchIfMissing = true)
    public Storage inMemoryStorage(JprometheusProperties jprometheusProperties) {
        return new InMemoryStorage(jprometheusProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "jprometheus.storage.type", havingValue = "prometheus")
    public Storage prometheusStorage(JprometheusProperties jprometheusProperties) {
        return new PrometheusStorage(jprometheusProperties);
    }

    public static class JprometheusDoubleSerializer extends JsonSerializer<Double> {

        @Override
        public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if ((value == null) || value.isNaN()) {
                gen.writeNull();
            } else {
                gen.writeNumber(new BigDecimal(value + "").toPlainString());
            }
        }
    }

}
