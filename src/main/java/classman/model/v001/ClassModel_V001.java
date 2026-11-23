package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/** Model of a class box or an interface box, also used for the class of an association. */
public class ClassModel_V001 extends AbstractBoxModel_V001 {
    public final ClassKind_V001 kind;
    public String name;
    /** Null if no change set changed the name. */
    public TextChangeModel_V001 nameChange;
    /** The text between the angle brackets without them, null if there is no stereotype. */
    public String stereotype;
    /** Null if no change set changed the stereotype. */
    public TextChangeModel_V001 stereotypeChange;
    /** Null if the stereotype has no ID, or there is no stereotype. */
    public String stereotypeId;
    /** Null if no change set added or removed the stereotype. A removed stereotype keeps its text and is shown struck
     * through, like a removed info of an attribute. Not used for a text which is changed. */
    public ChangeModel_V001 stereotypeElementChange;
    public final List<TaggedValueModel_V001> taggedValues = new ArrayList<>();
    public final List<AttributeModel_V001> attributes = new ArrayList<>();
    /** True if the box doesn't show its attribute compartment, which keeps its attributes. */
    public boolean attributesHidden;

    /** @return true if a change set added or removed an attribute or one of its infos, or changed one of their texts */
    public boolean hasMarkedAttributes() {
        for (AttributeModel_V001 attribute : attributes) {
            if (attribute.hasMarks()) {
                return true;
            }
        }
        return false;
    }

    /** @return true if the box shows its attribute compartment: a class or enum does, unless the source hides it. The
     * marks of a change set must stay visible, so attributes with marks are shown whatever the source says. */
    public boolean showsAttributes() {
        return kind.hasAttributes() && (!attributesHidden || hasMarkedAttributes());
    }

    public ClassModel_V001(String name) {
        this(name, ClassKind_V001.CLASS);
    }

    public ClassModel_V001(String name, ClassKind_V001 kind) {
        this.name = name;
        this.kind = kind;
    }
}
