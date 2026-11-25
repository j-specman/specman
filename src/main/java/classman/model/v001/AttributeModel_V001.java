package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/** Model of one line in the attribute compartment of a class, or of the marker for hidden attributes. */
public class AttributeModel_V001 extends AbstractMemberModel_V001 {

    public AttributeModel_V001(Visibility_V001 visibility, String name, String type, List<InfoModel_V001> infos) {
        this(visibility, name, type, infos, false);
    }

    private AttributeModel_V001(Visibility_V001 visibility, String name, String type, List<InfoModel_V001> infos,
                                boolean hiddenMarker) {
        super(visibility, name, type, infos, hiddenMarker);
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
