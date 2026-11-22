package classman.model.v001;

/** What a class box stands for. The kind is fixed when the box is created. */
public enum ClassKind_V001 {
    CLASS("class", null, true),
    INTERFACE("interface", "interface", false),
    /** The values are attributes without a type. */
    ENUM("enum", "enumeration", true);

    /** The word which starts the box in the source. */
    public final String keyword;
    /** The stereotype which the layout shows above the name of the box without it being a stereotype of the box, null
     * if the kind shows none. */
    public final String derivedStereotype;
    private final boolean hasAttributes;

    /** @return true if an attribute of the box may have a type, which the values of an enum haven't */
    public boolean hasTypedAttributes() {
        return this == CLASS;
    }

    ClassKind_V001(String keyword, String derivedStereotype, boolean hasAttributes) {
        this.keyword = keyword;
        this.derivedStereotype = derivedStereotype;
        this.hasAttributes = hasAttributes;
    }

    /** @return true if the box has a part with attributes (the values, for an enum), which an interface hasn't */
    public boolean hasAttributes() {
        return hasAttributes;
    }
}
