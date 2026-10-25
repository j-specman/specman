package specman.model.v002;

import java.util.ArrayList;
import java.util.List;

public class DiagramModel_V002 {
    public String name;
    public int width;
    public int zoomFactor;
    public boolean changeModeEnabled;
    public String changeSetName;
    public StepSequenceModel_V002 mainSequence;
    public EditorContentModel_V002 intro;
    public EditorContentModel_V002 outro;
    public PdfExportOptionsModel_V002 pdfExportOptions;
    public List<MetaTagConfigModel_V002> metaTagConfigs = new ArrayList<>();

    @Deprecated public DiagramModel_V002() { // For Jackson only
        name = null;
        width = 0;
        zoomFactor = 0;
        changeModeEnabled = false;
        changeSetName = null;
        mainSequence = null;
        intro = null;
        outro = null;
        pdfExportOptions = null;
    }

    public DiagramModel_V002(String name, int width, int zoomFactor, boolean changeModeEnabled, StepSequenceModel_V002 mainSequence, EditorContentModel_V002 intro, EditorContentModel_V002 outro, PdfExportOptionsModel_V002 pdfExportOptions, String changeSetName) {
        this.name = name;
        this.width = width;
        this.zoomFactor = zoomFactor;
        this.changeModeEnabled = changeModeEnabled;
        this.changeSetName = changeSetName;
        this.mainSequence = mainSequence;
        this.intro = intro;
        this.outro = outro;
        this.pdfExportOptions = pdfExportOptions;
    }

    public List<AbstractStepModel_V002> queryAllSteps() {
        List<AbstractStepModel_V002> allSteps = new ArrayList<>();
        mainSequence.addStepsRecursively(allSteps);
        return allSteps;
    }
}
