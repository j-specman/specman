package classman.model.v001.change;

import classman.model.v001.ChangeType_V001;

/** What happens to the marks of a change set: they are accepted or rejected. */
public enum ChangeResolution_V001 {
    ACCEPT,
    REJECT;

    /** True if an element with this kind of mark goes away: removed ones when accepting, added ones when rejecting. */
    public boolean removesElementWith(ChangeType_V001 type) {
        return this == ACCEPT ? type == ChangeType_V001.REMOVED : type == ChangeType_V001.ADDED;
    }
}
