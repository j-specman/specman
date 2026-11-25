package classman.model.v001;

/** The mark which says that a change set added or removed a diagram element. */
public class ChangeModel_V001 {
    public final ChangeType_V001 type;
    /** The name of the change set, which the host application maps to a color. */
    public final String changeSet;

    public ChangeModel_V001(ChangeType_V001 type, String changeSet) {
        this.type = type;
        this.changeSet = changeSet;
    }
}
