package classman.model.v001;

import java.util.ArrayList;
import java.util.List;

/**
 * Model of one line in the method compartment of a class, or of the marker for hidden methods: [visibility]
 * name(parameter, ...) : return type [infos]. A parameter is a list entry like an info: shown as written, and it can be
 * added or removed, or its text can be changed. The return type is the type of the member.
 */
public class MethodModel_V001 extends AbstractMemberModel_V001 {
    /** The parameters in their order, each as written (name: Type or just Type). */
    public final List<InfoModel_V001> parameters;

    public MethodModel_V001(Visibility_V001 visibility, String name, List<InfoModel_V001> parameters,
                            String returnType, List<InfoModel_V001> infos) {
        this(visibility, name, parameters, returnType, infos, false);
    }

    private MethodModel_V001(Visibility_V001 visibility, String name, List<InfoModel_V001> parameters,
                             String returnType, List<InfoModel_V001> infos, boolean hiddenMarker) {
        super(visibility, name, returnType, infos, hiddenMarker);
        this.parameters = parameters;
    }

    @Override
    public List<List<InfoModel_V001>> entryLists() {
        return List.of(parameters, infos);
    }

    @Override
    protected String firstChangeSetOfParameters() {
        return firstChangeSetOf(parameters);
    }

    /** The marker which the diagram puts under the shown methods if some are hidden. Its text is marked with the
     * change set, which is null for no mark. */
    public static MethodModel_V001 hiddenMethodsMarker(String changeSet) {
        MethodModel_V001 marker = new MethodModel_V001(null, null, new ArrayList<>(), null, new ArrayList<>(), true);
        marker.nameChange = changeSet == null ? null : new TextChangeModel_V001(changeSet, null);
        return marker;
    }
}
