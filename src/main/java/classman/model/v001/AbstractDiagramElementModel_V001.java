package classman.model.v001;

/** Base of everything which is drawn in a class diagram on its own: classes, relations and notes. */
public abstract class AbstractDiagramElementModel_V001 {
    /** 8 lower-case hex characters like a Specman step ID, null if the source doesn't give one. */
    public String id;
    /** Null if no change set added or removed the element. */
    public ChangeModel_V001 change;
    /** The look of the element. Never null, the standard look is a style without any part. Which parts an element can
     * have depends on its kind, see {@link StyleProperty_V001}: classes and notes all of them, relations all but the
     * fill. The others keep the standard style. */
    public ElementStyleModel_V001 style = new ElementStyleModel_V001();
}
