package classman.model.v001;

/** Model of a tagged value of a class, like TABLENAME=helpversion. */
public class TaggedValueModel_V001 {
    public final String key;
    public final String value;

    public TaggedValueModel_V001(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
