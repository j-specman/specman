package classman.model.v001;

/** Model of a tagged value of a class, like TABLENAME=helpversion. It is a list entry which can be added or removed, and a text which can be changed. */
public class TaggedValueModel_V001 {
    /** Null if no change set added or removed the tagged value. A removed one keeps its text and is shown struck through. */
    public ChangeModel_V001 change;
    public String key;
    /** Null if no change set changed the key. Not used for a tagged value which is added or removed. */
    public TextChangeModel_V001 keyChange;
    /** Null or empty if the tagged value only has a key. */
    public String value;
    /** Null if no change set changed the value. Not used for a tagged value which is added or removed. */
    public TextChangeModel_V001 valueChange;

    public TaggedValueModel_V001(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
