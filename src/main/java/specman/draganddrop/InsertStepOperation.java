package specman.draganddrop;

import specman.EditException;
import specman.EditorI;
import specman.Specman;
import specman.view.*;

public class InsertStepOperation implements DragOperation {

    private final Class<? extends AbstractSchrittView> stepClass;
    private final Specman specman;

    public InsertStepOperation(Class<? extends AbstractSchrittView> stepClass, Specman specman) {
        this.stepClass = stepClass;
        this.specman = specman;
    }

    @Override
    public boolean canDrop(DropTarget target) {
        return target.sequence() != null && target.referenceStep() != null;
    }

    @Override
    public void execute(DropTarget target, EditorI editor) throws EditException {
        SchrittSequenzView sequence = target.referenceStep().getParent();
        AbstractSchrittView inserted = null;
        if (stepClass == EinfacherSchrittView.class)    { inserted = sequence.einfachenSchrittZwischenschieben(target.position(), target.referenceStep()); }
        else if (stepClass == WhileSchrittView.class)   { inserted = sequence.whileSchrittZwischenschieben(target.position(), target.referenceStep()); }
        else if (stepClass == WhileWhileSchrittView.class) { inserted = sequence.whileWhileSchrittZwischenschieben(target.position(), target.referenceStep()); }
        else if (stepClass == IfElseSchrittView.class)  { inserted = sequence.ifElseSchrittZwischenschieben(target.position(), target.referenceStep()); }
        else if (stepClass == IfSchrittView.class)      { inserted = sequence.ifSchrittZwischenschieben(target.position(), target.referenceStep()); }
        else if (stepClass == CaseSchrittView.class)    { inserted = sequence.caseSchrittZwischenschieben(target.position(), target.referenceStep()); }
        else if (stepClass == SubsequenzSchrittView.class) { inserted = sequence.subsequenzSchrittZwischenschieben(target.position(), target.referenceStep()); }
        else if (stepClass == BreakSchrittView.class)   { inserted = sequence.breakSchrittZwischenschieben(target.position(), target.referenceStep()); }
        if (inserted != null) {
            specman.newStepPostInit(inserted);
            specman.getHauptSequenz().resyncStepnumberStyleADBL();
        }
    }

    /** Used when dropping onto an empty sequence (append instead of insert). */
    public void executeAppend(SchrittSequenzView sequence) {
        if (stepClass == EinfacherSchrittView.class)       { sequence.einfachenSchrittAnhaengen(); }
        else if (stepClass == WhileSchrittView.class)      { sequence.whileSchrittAnhaengen(); }
        else if (stepClass == WhileWhileSchrittView.class) { sequence.whileWhileSchrittAnhaengen(); }
        else if (stepClass == IfElseSchrittView.class)     { sequence.ifElseSchrittAnhaengen(); }
        else if (stepClass == IfSchrittView.class)         { sequence.ifSchrittAnhaengen(); }
        else if (stepClass == CaseSchrittView.class)       { sequence.caseSchrittAnhaengen(); }
        else if (stepClass == SubsequenzSchrittView.class) { sequence.subsequenzSchrittAnhaengen(); }
        else if (stepClass == BreakSchrittView.class)      { sequence.breakSchrittAnhaengen(); }
    }
}
