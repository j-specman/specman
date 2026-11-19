package classman.model.v001;

/** Base of everything which is drawn in a class diagram on its own: classes, relations and notes. */
public abstract class AbstractDiagramElementModel_V001 {
    /** 8 lower-case hex characters like a Specman step ID, null if the source doesn't give one. */
    public String id;
    /** Null if no change set added or removed the element. */
    public ChangeModel_V001 change;
}
