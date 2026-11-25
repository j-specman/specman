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
    public final List<MethodModel_V001> methods = new ArrayList<>();
    /** True if the box doesn't show its attribute compartment, which keeps its attributes. */
    public boolean attributesHidden;
    /** Whether the box shows its method compartment: by default if it has methods. It keeps its methods when hidden. */
    public CompartmentVisibility_V001 methodsVisibility = CompartmentVisibility_V001.AUTOMATIC;

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

    /** @return true if a change set added or removed a method, one of its parameters or infos, or changed one of their
     * texts */
    public boolean hasMarkedMethods() {
        for (MethodModel_V001 method : methods) {
            if (method.hasMarks()) {
                return true;
            }
        }
        return false;
    }

    /** @return true if the box shows its method compartment: it has methods and the source doesn't hide it, or the
     * source shows it. The marks of a change set must stay visible, so methods with marks are shown whatever the
     * source says. */
    public boolean showsMethods() {
        return switch (methodsVisibility) {
            case SHOWN -> true;
            case HIDDEN -> hasMarkedMethods();
            case AUTOMATIC -> !methods.isEmpty();
        };
    }

    /** @return the attributes or the methods of the box */
    public List<? extends AbstractMemberModel_V001> members(Compartment_V001 compartment) {
        return compartment == Compartment_V001.ATTRIBUTES ? attributes : methods;
    }

    /** @return the attributes and the methods of the box, in this order, each in the order of the source */
    public List<AbstractMemberModel_V001> allMembers() {
        List<AbstractMemberModel_V001> all = new ArrayList<>(attributes);
        all.addAll(methods);
        return all;
    }

    public ClassModel_V001(String name) {
        this(name, ClassKind_V001.CLASS);
    }

    public ClassModel_V001(String name, ClassKind_V001 kind) {
        this.name = name;
        this.kind = kind;
    }
}
