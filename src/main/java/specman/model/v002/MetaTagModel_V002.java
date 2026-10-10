package specman.model.v002;

/** Model of a meta tag assigned to a step. It refers to its configuration by name. */
public class MetaTagModel_V002 {
    public final String configName;
    /** Null for a tag without freetext. */
    public final String freetext;

    public MetaTagModel_V002(String configName, String freetext) {
        this.configName = configName;
        this.freetext = freetext;
    }
}
