package specman.draganddrop;

import specman.view.ZweigSchrittSequenzView;

public class BranchHeadingZone {
    public final ZweigSchrittSequenzView branch;
    public final int xOffset;

    public BranchHeadingZone(ZweigSchrittSequenzView branch, int xOffset) {
        this.branch = branch;
        this.xOffset = xOffset;
    }
}
