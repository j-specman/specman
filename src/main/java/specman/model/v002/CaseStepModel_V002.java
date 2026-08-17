package specman.model.v002;

import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseStepModel_V002 extends StructuredStepModel_V002 {
    public final BranchSequenceModel_V002 defaultSequence;
    public final List<BranchSequenceModel_V002> caseSequences;
    public final List<Float> columnWidthRatios;

    @Deprecated public CaseStepModel_V002() { // For Jackson only
        defaultSequence = null;
        caseSequences = null;
        columnWidthRatios = null;
    }

    public CaseStepModel_V002(UUID id, EditorContentModel_V002 content, int color, ChangeInfo changeInfo, boolean collapsed, BranchSequenceModel_V002 defaultSequence, List<Float> columnWidthRatios, UUID sourceStepId, RoundedBorderDecorationStyle decorationStyle) {
        super(id, content, color, changeInfo, collapsed, sourceStepId, decorationStyle);
        this.defaultSequence = defaultSequence;
        this.caseSequences = new ArrayList<>();
        this.columnWidthRatios = columnWidthRatios;
    }

    public void addCase(BranchSequenceModel_V002 sequence) {
        caseSequences.add(sequence);
    }

    @Override public void addStepRecursively(List<AbstractStepModel_V002> allSteps) {
        super.addStepRecursively(allSteps);
        defaultSequence.addStepsRecursively(allSteps);
        for (BranchSequenceModel_V002 caseSequence : caseSequences) {
            caseSequence.addStepsRecursively(allSteps);
        }
    }
}
