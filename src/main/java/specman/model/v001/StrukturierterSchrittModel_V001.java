package specman.model.v001;

import specman.ChangeInfo;
import specman.SchrittID;
import specman.view.RoundedBorderDecorationStyle;

public class StrukturierterSchrittModel_V001 extends AbstractSchrittModel_V001 {
	public final boolean zugeklappt;

	@Deprecated public StrukturierterSchrittModel_V001() { // For Jackson only
		zugeklappt = false;
	}

	public StrukturierterSchrittModel_V001(
			SchrittID id,
			EditorContentModel_V001 inhalt,
			int farbe,
			ChangeInfo changeInfo,
			boolean zugeklappt,
			SchrittID quellschrittID,
			RoundedBorderDecorationStyle decorationStyle) {
		super(id, inhalt, farbe, changeInfo, quellschrittID, decorationStyle);
		this.zugeklappt = zugeklappt;
	}
}
