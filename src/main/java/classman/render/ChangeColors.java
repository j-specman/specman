package classman.render;

/** Tells the layout which color a change set has. The host application knows its change sets, classman doesn't. */
public interface ChangeColors {
    /** No change set is known: all marks get the color for unknown change sets. */
    ChangeColors NONE = changeSet -> null;

    /** @return the strong color of the change set as #rrggbb, or null if the change set is unknown */
    String strongColorOf(String changeSet);
}
