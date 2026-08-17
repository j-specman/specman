package specman.view;

import specman.ChangeInfo;
import specman.EditorI;
import specman.SchrittID;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.WhileSchrittModel_V001;
import specman.model.v002.WhileStepModel_V002;

public class WhileSchrittView extends SchleifenSchrittView {
	
	protected WhileSchrittView(SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, ChangeInfo changeInfo, boolean withDefaultContent) {
		super(parent, initialerText, id, changeInfo, false);
		if (withDefaultContent) {
			initWiederholsequenz(einschrittigeInitialsequenz(id.naechsteEbene(), changeInfo));
		}
	}

	public WhileSchrittView(SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, ChangeInfo changeInfo) {
		this(parent, initialerText, id, changeInfo, true);
	}

	public WhileSchrittView(SchrittSequenzView parent, WhileSchrittModel_V001 model) {
		super(parent, model, false);
	}

	public WhileSchrittView(SchrittSequenzView parent, EditorContentModel_V001 initialerText) {
		this(parent, initialerText, (SchrittID) null, null);
	}
	
	@Override
	public WhileStepModel_V002 generiereModel(boolean formatierterText) {
		return new WhileStepModel_V002(
			stepId,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			klappen.isSelected(),
			wiederholSequenz.generiereSchrittSequenzModel(formatierterText),
			linkerBalken.getWidth(),
			null,
			getDecorated());
	}

}
