package classman.model.v001;

/** Base of the relations between two classes. The classes are referred to by their names. */
public abstract class AbstractRelationModel_V001 extends AbstractDiagramElementModel_V001 {
    public final RelationType_V001 type;
    /** The source class: the one with the diamond or the subclass. */
    public final String from;
    /** The target class: the one with the arrowhead, or the superclass or interface. */
    public final String to;
    /** Null if there is no label. Line breaks are real newline characters, not the two characters \n. */
    public String label;
    /** Null if the relation is a straight line from center to center. */
    public RouteModel_V001 route;

    protected AbstractRelationModel_V001(RelationType_V001 type, String from, String to) {
        this.type = type;
        this.from = from;
        this.to = to;
    }
}
