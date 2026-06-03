package specman.draganddrop;

import specman.view.AbstractSchrittView;
import specman.view.CatchBereich;
import specman.view.CatchUeberschrift;
import specman.view.RelativeStepPosition;
import specman.view.SchrittSequenzView;

import java.awt.*;

import static specman.view.RelativeStepPosition.After;
import static specman.view.RelativeStepPosition.Before;

public class DropTarget {
    private final SchrittSequenzView sequence;
    private final AbstractSchrittView referenceStep;
    private final RelativeStepPosition position;
    private final CatchUeberschrift referenceCatch;
    private final Component dropZoneComponent;

    public DropTarget(SchrittSequenzView sequence, AbstractSchrittView referenceStep, CatchUeberschrift referenceCatch, Component dropZoneComponent, RelativeStepPosition position) {
        this.sequence = sequence;
        this.referenceStep = referenceStep;
        this.position = position;
        this.referenceCatch = referenceCatch;
        this.dropZoneComponent = dropZoneComponent;
    }

    public DropTarget(SchrittSequenzView sequence, AbstractSchrittView referenceStep, RelativeStepPosition position) {
        this(sequence, referenceStep, null, referenceStep.getPanel(), position);
    }

    /** Drop target for inserting Before the first step in the given sequence. */
    public DropTarget(SchrittSequenzView sequence) {
        this(sequence, sequence.schritte.get(0), Before);
    }

    public SchrittSequenzView sequence() { return sequence; }
    public AbstractSchrittView referenceStep() { return referenceStep; }
    public RelativeStepPosition position() { return position; }
    public CatchUeberschrift referenceCatch() { return referenceCatch; }
    public Component dropZoneComponent() { return dropZoneComponent; }
}
