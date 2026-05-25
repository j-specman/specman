package specman.model.v001;

import specman.ChangeInfo;
import specman.SchrittID;
import specman.view.RoundedBorderDecorationStyle;

public class QuellSchrittModel_V001 extends AbstractSchrittModel_V001{

    @Deprecated public QuellSchrittModel_V001() {} // For Jackson only

    public QuellSchrittModel_V001(
        SchrittID id,
        EditorContentModel_V001 inhalt,
        int farbe,
        ChangeInfo changeInfo,
        SchrittID zielschrittID,
        RoundedBorderDecorationStyle decorationStyle) {
        super(id, inhalt, farbe, changeInfo, zielschrittID, decorationStyle);
    }
}
