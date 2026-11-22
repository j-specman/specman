package classman.model.v001.change;

/** The key and the value of a tagged value as the user typed them. The value is null if there is none. */
public class TaggedValueLine_V001 {
    public final String key;
    public final String value;

    TaggedValueLine_V001(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
