package classman.model.v001;

/** Base of the relations between two classes. The classes are referred to by their IDs. */
public abstract class AbstractRelationModel_V001 extends AbstractDiagramElementModel_V001 {
    public final RelationType_V001 type;
    /** The ID of the source class: the one with the diamond or the subclass. The editor changes it when the user hangs
     * the start of the relation onto another class, outside of change tracking. */
    public String fromId;
    /** The ID of the target class: the one with the arrowhead, or the superclass or interface. The editor changes it
     * when the user hangs the end of the relation onto another class, outside of change tracking. */
    public String toId;
    /** Null if there is no label. Line breaks are real newline characters, not the two characters \n. */
    public String label;
    /** Null if no change set changed the label. A removed label is the empty string with the mark. */
    public TextChangeModel_V001 labelChange;
    /** Null if the relation is a straight line from center to center. */
    public RouteModel_V001 route;

    protected AbstractRelationModel_V001(RelationType_V001 type, String fromId, String toId) {
        this.type = type;
        this.fromId = fromId;
        this.toId = toId;
    }
}
