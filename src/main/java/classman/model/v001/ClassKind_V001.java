package classman.model.v001;

/** What a class box stands for. The kind is fixed when the box is created. */
public enum ClassKind_V001 {
    CLASS("class", null, true, CompartmentVisibility_V001.AUTOMATIC),
    INTERFACE("interface", "interface", false, CompartmentVisibility_V001.SHOWN),
    /** The values are attributes without a type. */
    ENUM("enum", "enumeration", true, CompartmentVisibility_V001.AUTOMATIC);

    /** The word which starts the box in the source. */
    public final String keyword;
    /** The stereotype which the layout shows above the name of the box without it being a stereotype of the box, null
     * if the kind shows none. */
    public final String derivedStereotype;
    private final boolean hasAttributes;
    private final CompartmentVisibility_V001 initialMethodsVisibility;

    /** @return true if an attribute of the box may have a type, which the values of an enum haven't */
    public boolean hasTypedAttributes() {
        return this == CLASS;
    }

    ClassKind_V001(String keyword, String derivedStereotype, boolean hasAttributes,
                   CompartmentVisibility_V001 initialMethodsVisibility) {
        this.keyword = keyword;
        this.derivedStereotype = derivedStereotype;
        this.hasAttributes = hasAttributes;
        this.initialMethodsVisibility = initialMethodsVisibility;
    }

    /** @return true if the box has a part with attributes (the values, for an enum), which an interface hasn't */
    public boolean hasAttributes() {
        return hasAttributes;
    }

    /** @return the visibility of the method compartment of a box which is created: a class and an enum start without
     * methods and so without the compartment, for an interface the methods are all it has */
    public CompartmentVisibility_V001 initialMethodsVisibility() {
        return initialMethodsVisibility;
    }
}
