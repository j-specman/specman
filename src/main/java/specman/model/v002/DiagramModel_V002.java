package specman.model.v002;

import java.util.ArrayList;
import java.util.List;

public class DiagramModel_V002 {
    public final String name;
    public final int width;
    public final int zoomFactor;
    public final boolean changeModeEnabled;
    public final StepSequenceModel_V002 mainSequence;
    public final EditorContentModel_V002 intro;
    public final EditorContentModel_V002 outro;
    public final PdfExportOptionsModel_V002 pdfExportOptions;
    public final String changeSetName;

    @Deprecated public DiagramModel_V002() { // For Jackson only
        name = null;
        width = 0;
        zoomFactor = 0;
        changeModeEnabled = false;
        mainSequence = null;
        intro = null;
        outro = null;
        pdfExportOptions = null;
        changeSetName = null;
    }

    public DiagramModel_V002(String name, int width, int zoomFactor, boolean changeModeEnabled, StepSequenceModel_V002 mainSequence, EditorContentModel_V002 intro, EditorContentModel_V002 outro, PdfExportOptionsModel_V002 pdfExportOptions, String changeSetName) {
        this.name = name;
        this.width = width;
        this.zoomFactor = zoomFactor;
        this.changeModeEnabled = changeModeEnabled;
        this.mainSequence = mainSequence;
        this.intro = intro;
        this.outro = outro;
        this.pdfExportOptions = pdfExportOptions;
        this.changeSetName = changeSetName;
    }

    public List<AbstractStepModel_V002> queryAllSteps() {
        List<AbstractStepModel_V002> allSteps = new ArrayList<>();
        mainSequence.addStepsRecursively(allSteps);
        return allSteps;
    }
}
