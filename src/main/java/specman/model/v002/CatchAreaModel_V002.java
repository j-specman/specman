package specman.model.v002;

import java.util.ArrayList;
import java.util.List;

public class CatchAreaModel_V002 {
    public final List<CatchSequenceModel_V002> catchSequences;
    public final List<Integer> sequencesWidthPercent;
    public final boolean collapsed;

    @Deprecated public CatchAreaModel_V002() { // For Jackson only
        catchSequences = null;
        sequencesWidthPercent = null;
        collapsed = false;
    }

    public CatchAreaModel_V002(List<Integer> sequencesWidthPercent, boolean collapsed) {
        this.sequencesWidthPercent = sequencesWidthPercent;
        this.collapsed = collapsed;
        this.catchSequences = new ArrayList<>();
    }
}
