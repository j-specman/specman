package classman.model.v001;

/**
 * Whether a compartment of a class box is shown. {@link #AUTOMATIC} shows it if it has anything to show and is the
 * state of a box whose source says nothing about it; the other two say the user's choice.
 */
public enum CompartmentVisibility_V001 {
    AUTOMATIC,
    SHOWN,
    HIDDEN
}
