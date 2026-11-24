package classman.model.v001;

/** The two parts of a class box which hold lines: the attributes (the values of an enum) and the methods. */
public enum Compartment_V001 {
    ATTRIBUTES,
    METHODS;

    /** @return true if a box of the kind has the compartment: an interface has no attributes */
    public boolean existsIn(ClassKind_V001 kind) {
        return this == METHODS || kind.hasAttributes();
    }

    /** @return true if the box shows the compartment: it has it, and the source doesn't hide it, or marks of a change
     * set must be seen in it */
    public boolean isShownIn(ClassModel_V001 box) {
        return this == ATTRIBUTES ? box.showsAttributes() : box.showsMethods();
    }

    /** Hides the compartment, or shows it: the attribute compartment is shown unless it is hidden, the method
     * compartment is shown or hidden for good, whether it has methods or not.
     * @return true if that changed the box */
    public boolean setHidden(ClassModel_V001 box, boolean hidden) {
        if (this == ATTRIBUTES) {
            boolean changes = box.attributesHidden != hidden;
            box.attributesHidden = hidden;
            return changes;
        }
        CompartmentVisibility_V001 visibility = hidden ? CompartmentVisibility_V001.HIDDEN
            : CompartmentVisibility_V001.SHOWN;
        boolean changes = box.methodsVisibility != visibility;
        box.methodsVisibility = visibility;
        return changes;
    }

    /** @return true if a change set marked a member of the compartment */
    public boolean hasMarkedMembersIn(ClassModel_V001 box) {
        return this == ATTRIBUTES ? box.hasMarkedAttributes() : box.hasMarkedMethods();
    }

    /** @return the marker which the diagram puts under the shown members if some are hidden, marked with the change
     * set, null for no mark */
    public AbstractMemberModel_V001 hiddenMarker(String changeSet) {
        return this == ATTRIBUTES ? AttributeModel_V001.hiddenAttributesMarker(changeSet)
            : MethodModel_V001.hiddenMethodsMarker(changeSet);
    }
}
