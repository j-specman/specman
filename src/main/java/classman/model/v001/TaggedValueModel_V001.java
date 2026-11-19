package classman.model.v001;

/** Model of a tagged value of a class, like TABLENAME=helpversion. */
public class TaggedValueModel_V001 {
    public final String key;
    /** Null if no change set changed the key. */
    public TextChangeModel_V001 keyChange;
    public final String value;
    /** Null if no change set changed the value. */
    public TextChangeModel_V001 valueChange;

    public TaggedValueModel_V001(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
