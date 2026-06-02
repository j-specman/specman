package specman.draganddrop;

import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.view.AbstractSchrittView;

public sealed interface DragSource {
  enum Type {
        None,
        StepCreation,
        CaseBranchCreation,
        CatchSequenceCreation,
        StepMove
    }

    Type type();

    default Type supported(Type... supportedTypes) {
        for (Type t : supportedTypes) {
            if (type() == t) {
                return t;
            }
        }
        return Type.None;
    }

    default void rejectAllBut(Type... supportedTypes) throws UnsupportedDragSourceException {
        if (supported(supportedTypes) == null) {
            throw new UnsupportedDragSourceException();
        }
    }

    default boolean isCaseBranchCreation() { return type() == Type.CaseBranchCreation; }
    default boolean isStepMove() { return type() == Type.StepMove; }
    default boolean isCatchSequenceCreation() { return type() == Type.CatchSequenceCreation; }


    final class StepCreation implements DragSource {
        private final Class<? extends AbstractSchrittView> stepClass;
        public StepCreation(Class<? extends AbstractSchrittView> stepClass) { this.stepClass = stepClass; }
        public Class<? extends AbstractSchrittView> stepClass() { return stepClass; }
        @Override public Type type() { return Type.StepCreation; }
    }

    final class CaseBranchCreation implements DragSource {
        @Override public Type type() { return Type.CaseBranchCreation; }
    }

    final class CatchSequenceCreation implements DragSource {
        @Override public Type type() { return Type.CatchSequenceCreation; }
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
        @Override public Type type() { return Type.StepMove; }
    }
}
