package classman.model.v001.change;

import classman.model.v001.Visibility_V001;

import java.util.List;

/** What a line of an attribute, as the user types it, says: the visibility, the name, the type, and the infos. */
public class AttributeLine_V001 {
    /** Null if the line has no visibility sign. */
    public final Visibility_V001 visibility;
    public final String name;
    /** Null if the line has no type. */
    public final String type;
    /** The texts of the infos, without quotes. */
    public final List<String> infos;

    public AttributeLine_V001(Visibility_V001 visibility, String name, String type, List<String> infos) {
        this.visibility = visibility;
        this.name = name;
        this.type = type;
        this.infos = List.copyOf(infos);
    }
}
