package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/** Model of one line in the attribute compartment of a class, or of the marker for hidden attributes. */
public class AttributeModel_V001 {
    /** Null if the source gives none. Notes need it to attach to the attribute. */
    public String id;
    /** Null if the source gives none. */
    public final Visibility_V001 visibility;
    /** Null for the marker of hidden attributes. */
    public final String name;
    /** Null if the source gives none. */
    public final String type;
    /** The items of the brackets, each as written and not interpreted. */
    public final List<String> infos;
    /** True for the line "...": the model has more attributes which the diagram doesn't show. */
    public final boolean hiddenAttributesMarker;

    public AttributeModel_V001(Visibility_V001 visibility, String name, String type, List<String> infos) {
        this(visibility, name, type, infos, false);
    }

    private AttributeModel_V001(Visibility_V001 visibility, String name, String type, List<String> infos,
                                boolean hiddenAttributesMarker) {
        this.visibility = visibility;
        this.name = name;
        this.type = type;
        this.infos = infos;
        this.hiddenAttributesMarker = hiddenAttributesMarker;
    }

    public static AttributeModel_V001 hiddenAttributesMarker() {
        return new AttributeModel_V001(null, null, null, new ArrayList<>(), true);
    }
}
