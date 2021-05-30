package github.clyoudu.jprometheus.config;

import lombok.Data;
import org.joda.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author leichen
 */
@Data
@ConfigurationProperties(prefix = "jprometheus")
public class JprometheusProperties {

    /**
     * storage configuration
     */
    private Storage storage = new Storage();

    @Data
    public static class Storage {

        /**
         * storage type
         */
        private Type type = Type.MEMORY;

        /**
         * data evict interval, default 3min, required when storage type is memory/database
         */
        private Period evictInterval = Period.minutes(3);

        /**
         * data retention duration, default 3hr, required when storage type is memory/database
         */
        private Period retention = Period.hours(3);

        /**
         * prometheus query url, required when storage type is prometheus
         */
        private String queryUrl = "http://localhost:9090/api/v1";

        public enum Type {
            /**
             * in memory storage
             */
            MEMORY,
            /**
             * database storage
             */
            DATABASE,
            /**
             * use prometheus directly
             */
            PROMETHEUS;
        }

    }

}
