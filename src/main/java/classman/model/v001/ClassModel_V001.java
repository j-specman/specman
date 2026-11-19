package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/** Model of a class box, also used for the class of an association. */
public class ClassModel_V001 extends AbstractDiagramElementModel_V001 {
    public String name;
    /** Null if no change set changed the name. */
    public TextChangeModel_V001 nameChange;
    /** Null if the source gives no position, the layout places the class then. */
    public PositionModel_V001 position;
    /** Null if the content decides about the width. The height is always computed. */
    public Integer width;
    /** The text between the angle brackets without them, null if there is no stereotype. */
    public String stereotype;
    /** Null if no change set changed the stereotype. */
    public TextChangeModel_V001 stereotypeChange;
    /** Null if the stereotype has no ID, or there is no stereotype. */
    public String stereotypeId;
    public final List<TaggedValueModel_V001> taggedValues = new ArrayList<>();
    public final List<AttributeModel_V001> attributes = new ArrayList<>();

    public ClassModel_V001(String name) {
        this.name = name;
    }
}
