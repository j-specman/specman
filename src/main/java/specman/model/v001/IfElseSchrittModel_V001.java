package specman.model.v001;

import specman.ChangeInfo;
import specman.SchrittID;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;

public class IfElseSchrittModel_V001 extends StrukturierterSchrittModel_V001 {
	public final ZweigSchrittSequenzModel_V001 ifSequenz;
	public final ZweigSchrittSequenzModel_V001 elseSequenz;
	public final float ifBreitenanteil;

	@Deprecated public IfElseSchrittModel_V001() {  // For Jackson only
		this.ifSequenz = new ZweigSchrittSequenzModel_V001();
		this.elseSequenz = new ZweigSchrittSequenzModel_V001();
		this.ifBreitenanteil = 0.0f;
	}

	public IfElseSchrittModel_V001(
		SchrittID id,
		EditorContentModel_V001 inhalt,
		int farbe,
		RoundedBorderDecorationStyle decorationStyle,
		boolean zugeklappt,
		ChangeInfo changeInfo,
		ZweigSchrittSequenzModel_V001 ifSequenz,
		ZweigSchrittSequenzModel_V001 elseSequenz,
		float ifBreitenanteil, SchrittID quellschrittID) {
		super(id, inhalt, farbe, changeInfo, zugeklappt, quellschrittID, decorationStyle);
		this.ifSequenz = ifSequenz;
		this.elseSequenz = elseSequenz;
		this.ifBreitenanteil = ifBreitenanteil;
	}

	@Override public void addStepRecursively(List<AbstractSchrittModel_V001> allSteps) {
		super.addStepRecursively(allSteps);
		ifSequenz.addStepsRecursively(allSteps);
		elseSequenz.addStepsRecursively(allSteps);
	}
}
