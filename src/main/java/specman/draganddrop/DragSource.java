package specman.draganddrop;

import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.view.AbstractSchrittView;

public sealed interface DragSource {

    boolean isMove();

    final class StepCreation implements DragSource {
        private final Class<? extends AbstractSchrittView> stepClass;

        public StepCreation(Class<? extends AbstractSchrittView> stepClass) { this.stepClass = stepClass; }
        public Class<? extends AbstractSchrittView> stepClass() { return stepClass; }
        @Override public boolean isMove() { return false; }
    }

    final class CaseBranchCreation implements DragSource {
        @Override public boolean isMove() { return false; }
    }

    final class StepMove implements DragSource {
        private final StepnumberLabel label;
        private final AbstractSchrittView step;

        public StepMove(StepnumberLabel label, AbstractSchrittView step) {
            this.label = label;
            this.step = step;
        }
        public StepnumberLabel label() { return label; }
        public AbstractSchrittView step() { return step; }
        @Override public boolean isMove() { return true; }
    }
}
