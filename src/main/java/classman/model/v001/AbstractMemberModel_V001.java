package classman.model.v001;

import java.util.List;

/** Base of the lines of the compartments of a class box: an attribute, or a method, or the marker for hidden ones. */
public abstract class AbstractMemberModel_V001 {
    /** Null if the source gives none. Notes and dependencies need it to attach to the member. */
    public String id;
    /** Null if no change set added or removed the member. */
    public ChangeModel_V001 change;
    /** Null if the source gives none. */
    public Visibility_V001 visibility;
    /** Null if no change set changed the visibility sign. A sign which was removed can't be marked. */
    public TextChangeModel_V001 visibilityChange;
    /** Null for the marker of hidden members. */
    public String name;
    /** Null if no change set changed the name. */
    public TextChangeModel_V001 nameChange;
    /** The type of an attribute, the return type of a method. Null if the source gives none. */
    public String type;
    /** Null if no change set changed the type. */
    public TextChangeModel_V001 typeChange;
    /** The items of the brackets, each as written and not interpreted. */
    public final List<InfoModel_V001> infos;
    /** True if the diagram doesn't show the member, which stays in the model. Not marked as a change. */
    public boolean hidden;
    /** True for the line "...": the model has more members which the diagram doesn't show. */
    public final boolean hiddenMarker;

    protected AbstractMemberModel_V001(Visibility_V001 visibility, String name, String type,
                                       List<InfoModel_V001> infos, boolean hiddenMarker) {
        this.visibility = visibility;
        this.name = name;
        this.type = type;
        this.infos = infos;
        this.hiddenMarker = hiddenMarker;
    }

    /** @return true if a change set added or removed the member or one of its infos or parameters, or changed a text of
     * them */
    public boolean hasMarks() {
        return firstChangeSet() != null;
    }

    /** @return the change set of the first mark of the member, in the order of the line, or null if it has none */
    public String firstChangeSet() {
        if (change != null) {
            return change.changeSet;
        }
        for (TextChangeModel_V001 textChange : new TextChangeModel_V001[] {visibilityChange, nameChange}) {
            if (textChange != null) {
                return textChange.changeSet;
            }
        }
        String parametersChangeSet = firstChangeSetOfParameters();
        if (parametersChangeSet != null) {
            return parametersChangeSet;
        }
        if (typeChange != null) {
            return typeChange.changeSet;
        }
        return firstChangeSetOf(infos);
    }

    /** @return the lists of list entries of the member in the order of the line: the parameters of a method, the infos */
    public List<List<InfoModel_V001>> entryLists() {
        return List.of(infos);
    }

    /** @return the change set of the first mark in the parameters, null for a member which has none */
    protected String firstChangeSetOfParameters() {
        return null;
    }

    protected static String firstChangeSetOf(List<InfoModel_V001> entries) {
        for (InfoModel_V001 entry : entries) {
            if (entry.change != null) {
                return entry.change.changeSet;
            }
            if (entry.textChange != null) {
                return entry.textChange.changeSet;
            }
        }
        return null;
    }
}
