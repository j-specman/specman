package classman.model.v001;

/** One item in the brackets of an attribute or method, or one parameter of a method, shown as written. It is a list entry which can be added or removed, and a text which can be changed. */
public class InfoModel_V001 {
    public String text;
    /** Null if no change set added or removed the item. A removed item keeps its text and is shown struck through. */
    public ChangeModel_V001 change;
    /** Null if no change set changed the text. Not used for an item which is added or removed. */
    public TextChangeModel_V001 textChange;

    public InfoModel_V001(String text) {
        this.text = text;
    }
}
