package specman.model.v001;

import specman.ChangeInfo;
import specman.SchrittID;
import specman.Specman;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;

public class IfSchrittModel_V001 extends StrukturierterSchrittModel_V001 {
	public final ZweigSchrittSequenzModel_V001 ifSequenz;
	public final int leerBreite;

	@Deprecated public IfSchrittModel_V001() { // For Jackson only
		this.ifSequenz = new ZweigSchrittSequenzModel_V001();
//		this.leerBreite = 0;
		this.leerBreite = 20*Specman.instance().getZoomFactor()/100; /**@author PVN */
	}

	public IfSchrittModel_V001(
		SchrittID id,
		EditorContentModel_V001 inhalt,
		int farbe,
		RoundedBorderDecorationStyle decorationStyle,
		boolean zugeklappt,
		ChangeInfo changeInfo,
		ZweigSchrittSequenzModel_V001 ifSequenz,
		int leerBreite, SchrittID quellschrittID) {
		super(id, inhalt, farbe, changeInfo, zugeklappt, quellschrittID, decorationStyle);
		this.ifSequenz = ifSequenz;
//		this.leerBreite = leerBreite;
		this.leerBreite = 20*Specman.instance().getZoomFactor()/100; /**@author PVN */
	}

	@Override public void addStepRecursively(List<AbstractSchrittModel_V001> allSteps) {
		super.addStepRecursively(allSteps);
		ifSequenz.addStepsRecursively(allSteps);
	}
}
