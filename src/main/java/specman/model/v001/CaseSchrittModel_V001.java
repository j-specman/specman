package specman.model.v001;

import specman.ChangeInfo;
import specman.SchrittID;
import specman.view.RoundedBorderDecorationStyle;

import java.util.ArrayList;
import java.util.List;

public class CaseSchrittModel_V001 extends StrukturierterSchrittModel_V001 {
	public final ZweigSchrittSequenzModel_V001 sonstSequenz;
	public final List<ZweigSchrittSequenzModel_V001> caseSequenzen;
	public final ArrayList<Float> spaltenbreitenAnteile;

	public CaseSchrittModel_V001(
			SchrittID id,
			EditorContentModel_V001 inhalt,
			int farbe,
			ChangeInfo changeInfo,
			boolean zugeklappt,
			ZweigSchrittSequenzModel_V001 sonstSequenz,
			ArrayList<Float> spaltenbreitenAnteile,
			SchrittID quellschrittID,
			RoundedBorderDecorationStyle decorationStyle) {
		super(id, inhalt, farbe, changeInfo, zugeklappt, quellschrittID, decorationStyle);
		this.sonstSequenz = sonstSequenz;
		this.caseSequenzen = new ArrayList<ZweigSchrittSequenzModel_V001>();
		this.spaltenbreitenAnteile = spaltenbreitenAnteile;
	}

	@Deprecated public CaseSchrittModel_V001() { // For Jackson only
		sonstSequenz = null;
		caseSequenzen = null;
		spaltenbreitenAnteile = null;
	}

	public void caseHinzufuegen(ZweigSchrittSequenzModel_V001 sequenz) {
		caseSequenzen.add(sequenz);
	}

	@Override public void addStepRecursively(List<AbstractSchrittModel_V001> allSteps) {
		super.addStepRecursively(allSteps);
		sonstSequenz.addStepsRecursively(allSteps);
		for (ZweigSchrittSequenzModel_V001 caseSequenz: caseSequenzen) {
			caseSequenz.addStepsRecursively(allSteps);
		}
	}

}
