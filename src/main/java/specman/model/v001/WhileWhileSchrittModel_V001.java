package specman.model.v001;

import specman.ChangeInfo;
import specman.SchrittID;
import specman.view.RoundedBorderDecorationStyle;

public class WhileWhileSchrittModel_V001 extends WhileSchrittModel_V001 {

    @Deprecated public WhileWhileSchrittModel_V001() {} // For Jackson only

    public WhileWhileSchrittModel_V001(
        SchrittID id,
        EditorContentModel_V001 inhalt,
        int farbe,
        ChangeInfo changeInfo,
        boolean zugeklappt,
        SchrittSequenzModel_V001 wiederholSequenz,
        int balkenbreite,
        SchrittID quellschrittID,
        RoundedBorderDecorationStyle decorationStyle) {
        super(id, inhalt, farbe, changeInfo, zugeklappt, wiederholSequenz, balkenbreite, quellschrittID, decorationStyle);
    }
}
