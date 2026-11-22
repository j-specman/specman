package classman.model.v001;

/**
 * Where the user has put the label or a multiplicity of a relation, relative to the line of the relation so that the
 * text follows the line when its route changes. It says where the middle of the text stands.
 */
public class TextPlaceModel_V001 {
    /** For a label the percent of the length of the route from its start, for a multiplicity the distance along the
     * route from the end where the multiplicity belongs. */
    public final int along;
    /** The distance from the line, to its left seen from the source to the target if it is positive, to its right if it
     * is negative. */
    public final int across;

    public TextPlaceModel_V001(int along, int across) {
        this.along = along;
        this.across = across;
    }
}
