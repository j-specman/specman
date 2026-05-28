package specman.view;

import specman.ChangeInfo;
import specman.EditorI;
import specman.SchrittID;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.EinfacherSchrittModel_V001;

import javax.swing.*;

public class EinfacherSchrittView extends AbstractSchrittView {

	public EinfacherSchrittView(SchrittSequenzView parent, EditorContentModel_V001 initialerText, SchrittID id, ChangeInfo changeInfo) {
		super(parent, initialerText, id, changeInfo);
	}

	public EinfacherSchrittView(SchrittSequenzView parent, EinfacherSchrittModel_V001 model) {
		super(parent, model.inhalt, model.id, ChangeInfo.fromModel(model.changeInfo, model.aenderungsart));
	}

	@Override
	public JComponent getDecoratedComponent() { return decorated(editContainer); }

	@Override
	public EinfacherSchrittModel_V001 generiereModel(boolean formatierterText) {
		EinfacherSchrittModel_V001 model = new EinfacherSchrittModel_V001(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			getQuellschrittID(),
			getDecorated()
		);
		return model;
	}

	@Override
	public JComponent getPanel() { return editContainer; }

	public specman.pdf.Shape getShape() {
		return decoratedShape(editContainer.getShape());
	}

}
