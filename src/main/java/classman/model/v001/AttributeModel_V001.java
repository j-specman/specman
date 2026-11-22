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
    /** True if the diagram doesn't show the attribute, which stays in the model. Not marked as a change. */
    public boolean hidden;
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
        return firstChangeSet() != null;
    }

    /** @return the change set of the first mark of the attribute, in the order of the line, or null if it has none */
    public String firstChangeSet() {
        if (change != null) {
            return change.changeSet;
        }
        for (TextChangeModel_V001 textChange : new TextChangeModel_V001[] {visibilityChange, nameChange, typeChange}) {
            if (textChange != null) {
                return textChange.changeSet;
            }
        }
        for (InfoModel_V001 info : infos) {
            if (info.change != null) {
                return info.change.changeSet;
            }
            if (info.textChange != null) {
                return info.textChange.changeSet;
            }
        }
        return null;
    }

    public static AttributeModel_V001 hiddenAttributesMarker() {
        return new AttributeModel_V001(null, null, null, new ArrayList<>(), true);
    }

    /** The marker which the diagram puts under the shown attributes if some are hidden. Its text is marked with the
     * change set, which is null for no mark. */
    public static AttributeModel_V001 hiddenAttributesMarker(String changeSet) {
        AttributeModel_V001 marker = hiddenAttributesMarker();
        marker.nameChange = changeSet == null ? null : new TextChangeModel_V001(changeSet, null);
        return marker;
    }
}
