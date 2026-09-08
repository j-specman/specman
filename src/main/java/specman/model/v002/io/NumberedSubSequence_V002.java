package specman.model.v002.io;

import specman.StepNumber;
import specman.model.v002.StepSequenceModel_V002;

public class NumberedSubSequence_V002 {
    public final StepSequenceModel_V002 sequence;
    public final StepNumber base;

    public NumberedSubSequence_V002(StepSequenceModel_V002 sequence, StepNumber base) {
        this.sequence = sequence;
        this.base = base;
    }
}
