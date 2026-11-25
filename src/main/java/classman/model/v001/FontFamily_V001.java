package classman.model.v001;

/** The fonts a class box can have: the three fonts which Specman ships. */
public enum FontFamily_V001 {
    SITKA("SitkaDisplay"),
    ROBOTO("Roboto"),
    COURIER("CourierPrime");

    /** The family name without blanks, as the source writes it and as the PDF knows it. */
    public final String sourceName;

    FontFamily_V001(String sourceName) {
        this.sourceName = sourceName;
    }

    /** @return the font with the name, null if there is none */
    public static FontFamily_V001 fromSourceName(String sourceName) {
        for (FontFamily_V001 family : values()) {
            if (family.sourceName.equals(sourceName)) {
                return family;
            }
        }
        return null;
    }
}
