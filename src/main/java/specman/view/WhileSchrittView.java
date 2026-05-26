package specman.view;

import specman.ChangeInfo;
import specman.EditorI;
import specman.SchrittID;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.WhileSchrittModel_V001;

public class WhileSchrittView extends SchleifenSchrittView {
	
	protected WhileSchrittView(EditorI editor, SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, ChangeInfo changeInfo, boolean withDefaultContent) {
		super(editor, parent, initialerText, id, changeInfo, false);
		if (withDefaultContent) {
			initWiederholsequenz(einschrittigeInitialsequenz(editor, id.naechsteEbene(), changeInfo));
		}
	}

	public WhileSchrittView(EditorI editor, SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, ChangeInfo changeInfo) {
		this(editor, parent, initialerText, id, changeInfo, true);
	}

	public WhileSchrittView(EditorI editor, SchrittSequenzView parent, WhileSchrittModel_V001 model) {
		super(editor, parent, model, false);
	}

	public WhileSchrittView(EditorI editor, SchrittSequenzView parent, EditorContentModel_V001 initialerText) {
		this(editor, parent, initialerText, (SchrittID) null, null);
	}
	
	@Override
	public AbstractSchrittModel_V001 generiereModel(boolean formatierterText) {
		WhileSchrittModel_V001 model = new WhileSchrittModel_V001(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			klappen.isSelected(),
			wiederholSequenz.generiereSchrittSequenzModel(formatierterText),
			linkerBalken.getWidth(),
			getQuellschrittID(),
			getDecorated());
		return model;
	}

}
