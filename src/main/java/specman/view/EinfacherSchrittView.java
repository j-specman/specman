package specman.view;

import specman.ChangeInfo;
import specman.StepNumber;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.SimpleStepModel_V002;

import javax.swing.*;

public class EinfacherSchrittView extends AbstractSchrittView {

	public EinfacherSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText, StepNumber id, ChangeInfo changeInfo) {
		super(parent, initialerText, id, changeInfo);
	}

	public EinfacherSchrittView(SchrittSequenzView parent, SimpleStepModel_V002 model) {
		super(parent, model.content, model.id, model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
		this.id = model.id;
	}

	@Override
	public JComponent getDecoratedComponent() { return decorated(editContainer); }

	@Override
	public SimpleStepModel_V002 generiereModel(boolean formatierterText) {
		return new SimpleStepModel_V002(
			id,
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
