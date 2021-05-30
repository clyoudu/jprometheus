package github.clyoudu.jprometheus.storage.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author leichen
 */
@Data
@AllArgsConstructor
public class Label {

    /**
     * label name
     */
    private String name;

    /**
     * label value
     */
    private String value;

    @Override
    public int hashCode() {
        return (name + "@" + value).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Label && name.equals(((Label) obj).getName()) && value.equals(((Label) obj).getValue());
    }

}
