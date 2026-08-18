package specman.model.v001;

import specman.ChangeInfo;
import specman.StepNumber;
import specman.view.RoundedBorderDecorationStyle;

public class EinfacherSchrittModel_V001 extends AbstractSchrittModel_V001 {

    @Deprecated public EinfacherSchrittModel_V001() {} // For Jackson only

    public EinfacherSchrittModel_V001(
        StepNumber id,
        EditorContentModel_V001 inhalt,
        int farbe,
        ChangeInfo changeInfo,
        StepNumber quellschrittID,
        RoundedBorderDecorationStyle decorationStyle) {
        super(id, inhalt, farbe, changeInfo, quellschrittID, decorationStyle);
    }
}
