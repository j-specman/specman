package classman.model.v001;

/** One item in the brackets of an attribute, shown as written. */
public class InfoModel_V001 {
    public String text;
    /** Null if no change set changed the text. */
    public TextChangeModel_V001 change;

    public InfoModel_V001(String text) {
        this.text = text;
    }
}
