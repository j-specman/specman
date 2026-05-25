package specman.model.v001;

import specman.ChangeInfo;
import specman.SchrittID;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;

public class WhileSchrittModel_V001 extends StrukturierterSchrittModel_V001 {
	public final SchrittSequenzModel_V001 wiederholSequenz;
	public final int balkenbreite;

	@Deprecated public WhileSchrittModel_V001() { // For Jackson only
		wiederholSequenz = null;
		balkenbreite = 0;
	}

	public WhileSchrittModel_V001(
		SchrittID id,
		EditorContentModel_V001 inhalt,
		int farbe,
		ChangeInfo changeInfo,
		boolean zugeklappt,
		SchrittSequenzModel_V001 wiederholSequenz,
		int balkenbreite,
		SchrittID quellschrittID,
		RoundedBorderDecorationStyle decorationStyle) {
		super(id, inhalt, farbe, changeInfo, zugeklappt, quellschrittID, decorationStyle);
		this.wiederholSequenz = wiederholSequenz;
		this.balkenbreite = balkenbreite;
	}

	@Override public void addStepRecursively(List<AbstractSchrittModel_V001> allSteps) {
		super.addStepRecursively(allSteps);
		wiederholSequenz.addStepsRecursively(allSteps);
	}
}
