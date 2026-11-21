package classman.render;

/** Tells the layout which color a change set has. The host application knows its change sets, classman doesn't. */
public interface ChangeColors {
    /** No change set is known: all marks get the color for unknown change sets. */
    ChangeColors NONE = changeSet -> null;

    /** @return the strong color of the change set as #rrggbb, or null if the change set is unknown */
    String strongColorOf(String changeSet);

    /** @return the color of the frame around a selected element which the change set has marked, as #rrggbb, or null
     * if the usual selection color can be told from the color of the change set */
    default String selectionColorOf(String changeSet) {
        return null;
    }
}
