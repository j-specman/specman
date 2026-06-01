package specman.draganddrop;

import specman.view.AbstractSchrittView;
import specman.view.RelativeStepPosition;
import specman.view.SchrittSequenzView;

public class DropTarget {
    private final SchrittSequenzView sequence;
    private final AbstractSchrittView referenceStep;
    private final RelativeStepPosition position;

    public DropTarget(SchrittSequenzView sequence, AbstractSchrittView referenceStep, RelativeStepPosition position) {
        this.sequence = sequence;
        this.referenceStep = referenceStep;
        this.position = position;
    }

    public SchrittSequenzView sequence() { return sequence; }
    public AbstractSchrittView referenceStep() { return referenceStep; }
    public RelativeStepPosition position() { return position; }
}
