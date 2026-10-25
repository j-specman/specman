package specman.model.v002;

import specman.ChangeInfo;

import java.util.ArrayList;
import java.util.List;

public class StepSequenceModel_V002 {
    public String id;
    public ChangeInfoModel_V002 changeInfo;
    public List<AbstractStepModel_V002> steps;
    public CatchAreaModel_V002 catchArea;

    @Deprecated public StepSequenceModel_V002() { // For Jackson only
        id = null;
        changeInfo = null;
        steps = null;
        catchArea = null;
    }

    public StepSequenceModel_V002(String id, ChangeInfo changeInfo, CatchAreaModel_V002 catchArea) {
        this.id = id;
        this.changeInfo = ChangeInfoModel_V002.from(changeInfo);
        this.steps = new ArrayList<>();
        this.catchArea = catchArea;
    }

    public void addStepsRecursively(List<AbstractStepModel_V002> allSteps) {
        for (AbstractStepModel_V002 step : steps) {
            step.addStepRecursively(allSteps);
        }
    }
}
