package specman.draganddrop;

import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.view.AbstractSchrittView;

public sealed interface DragSource {

    final class NewStep implements DragSource {
        private final Class<? extends AbstractSchrittView> stepClass;

        public NewStep(Class<? extends AbstractSchrittView> stepClass) { this.stepClass = stepClass; }
        public Class<? extends AbstractSchrittView> stepClass() { return stepClass; }
    }

    final class NewCaseBranch implements DragSource {}

    final class ExistingStep implements DragSource {
        private final StepnumberLabel label;
        private final AbstractSchrittView step;

        public ExistingStep(StepnumberLabel label, AbstractSchrittView step) {
            this.label = label;
            this.step = step;
        }
        public StepnumberLabel label() { return label; }
        public AbstractSchrittView step() { return step; }
    }
}
