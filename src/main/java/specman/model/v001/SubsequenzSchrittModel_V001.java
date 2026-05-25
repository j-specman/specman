package specman.model.v001;

import specman.ChangeInfo;
import specman.SchrittID;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;

public class SubsequenzSchrittModel_V001 extends StrukturierterSchrittModel_V001 {
	public final SchrittSequenzModel_V001 subsequenz;
  public final boolean flatNumbering;

	@Deprecated public SubsequenzSchrittModel_V001() { // For Jackson only
		subsequenz = null;
    flatNumbering = false;
	}

	public SubsequenzSchrittModel_V001(
		SchrittID id,
		EditorContentModel_V001 inhalt,
		int farbe,
		ChangeInfo changeInfo,
		boolean zugeklappt,
		SchrittSequenzModel_V001 subsequenz,
		SchrittID quellschrittID,
		RoundedBorderDecorationStyle decorationStyle,
    boolean flatNumbering) {
		super(id, inhalt, farbe, changeInfo, zugeklappt, quellschrittID, decorationStyle);
		this.subsequenz = subsequenz;
    this.flatNumbering = flatNumbering;
	}

	@Override public void addStepRecursively(List<AbstractSchrittModel_V001> allSteps) {
		super.addStepRecursively(allSteps);
		subsequenz.addStepsRecursively(allSteps);
	}
}
