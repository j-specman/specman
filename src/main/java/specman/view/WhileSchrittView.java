package specman.view;

import specman.ChangeInfo;
import specman.StepNumber;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.WhileStepModel_V002;

public class WhileSchrittView extends SchleifenSchrittView {
	
	protected WhileSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText, StepNumber id, ChangeInfo changeInfo, boolean withDefaultContent) {
		super(parent, initialerText, id, changeInfo, false);
		if (withDefaultContent) {
			initWiederholsequenz(einschrittigeInitialsequenz(id.naechsteEbene(), changeInfo));
		}
	}

	public WhileSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText, StepNumber id, ChangeInfo changeInfo) {
		this(parent, initialerText, id, changeInfo, true);
	}

	public WhileSchrittView(SchrittSequenzView parent, WhileStepModel_V002 model) {
		super(parent, model, false);
	}

	public WhileSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText) {
		this(parent, initialerText, (StepNumber) null, null);
	}
	
	@Override
	public WhileStepModel_V002 generiereModel(boolean formatierterText) {
		return new WhileStepModel_V002(
			id,
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
