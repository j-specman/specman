package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/** Model of one line in the attribute compartment of a class, or of the marker for hidden attributes. */
public class AttributeModel_V001 {
    /** Null if the source gives none. Notes need it to attach to the attribute. */
    public String id;
    /** Null if no change set added or removed the attribute. */
    public ChangeModel_V001 change;
    /** Null if the source gives none. */
    public Visibility_V001 visibility;
    /** Null if no change set changed the visibility sign. A sign which was removed can't be marked. */
    public TextChangeModel_V001 visibilityChange;
    /** Null for the marker of hidden attributes. */
    public String name;
    /** Null if no change set changed the name. */
    public TextChangeModel_V001 nameChange;
    /** Null if the source gives none. */
    public String type;
    /** Null if no change set changed the type. */
    public TextChangeModel_V001 typeChange;
    /** The items of the brackets, each as written and not interpreted. */
    public final List<InfoModel_V001> infos;
    /** True for the line "...": the model has more attributes which the diagram doesn't show. */
    public final boolean hiddenAttributesMarker;

    public AttributeModel_V001(Visibility_V001 visibility, String name, String type, List<InfoModel_V001> infos) {
        this(visibility, name, type, infos, false);
    }

    private AttributeModel_V001(Visibility_V001 visibility, String name, String type, List<InfoModel_V001> infos,
                                boolean hiddenAttributesMarker) {
        this.visibility = visibility;
        this.name = name;
        this.type = type;
        this.infos = infos;
        this.hiddenAttributesMarker = hiddenAttributesMarker;
    }

    /** @return true if a change set added or removed the attribute or one of its infos, or changed a text of them */
    public boolean hasMarks() {
        boolean marked = change != null || visibilityChange != null || nameChange != null || typeChange != null;
        for (InfoModel_V001 info : infos) {
            marked = marked || info.change != null || info.textChange != null;
        }
        return marked;
    }

    public static AttributeModel_V001 hiddenAttributesMarker() {
        return new AttributeModel_V001(null, null, null, new ArrayList<>(), true);
    }
}
