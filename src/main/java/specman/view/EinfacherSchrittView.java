package specman.view;

import specman.ChangeInfo;
import specman.EditorI;
import specman.SchrittID;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.EinfacherSchrittModel_V001;
import specman.model.v002.SimpleStepModel_V002;

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
	public SimpleStepModel_V002 generiereModel(boolean formatierterText) {
		return new SimpleStepModel_V002(
			stepId,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			null,
			getDecorated()
		);
	}

	@Override
	public JComponent getPanel() { return editContainer; }

	public specman.pdf.Shape getShape() {
		return decoratedShape(editContainer.getShape());
	}

}
