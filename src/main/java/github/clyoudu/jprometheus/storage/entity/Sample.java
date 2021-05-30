package github.clyoudu.jprometheus.storage.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * sample
 * @author leichen
 */
@Data
@AllArgsConstructor
public class Sample {

    /**
     *
     */
    private Long timestamp;

    private Double value;

}
