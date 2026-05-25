package specman.model.v001;

import specman.ChangeInfo;
import specman.SchrittID;
import specman.view.RoundedBorderDecorationStyle;

public class BreakSchrittModel_V001 extends AbstractSchrittModel_V001 {

    @Deprecated public BreakSchrittModel_V001() {} // For Jackson only

    public BreakSchrittModel_V001(
        SchrittID id,
        EditorContentModel_V001 inhalt,
        int farbe,
        ChangeInfo changeInfo,
        SchrittID quellschrittID,
        RoundedBorderDecorationStyle decorationStyle) {
        super(id, inhalt, farbe, changeInfo, quellschrittID, decorationStyle);
    }
}
