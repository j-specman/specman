package specman.undo;

import specman.EditException;
import specman.view.SchrittSequenzView;
import specman.view.AbstractSchrittView;

import javax.swing.undo.CannotUndoException;

import static specman.view.StepRemovalPurpose.Discard;
import static specman.i18n.Messages.i18n;

public class UndoableSchrittEntfernt extends AbstractUndoableInteraction {

	private final AbstractSchrittView schritt;
	private final SchrittSequenzView sequenz;
	private int schrittIndex;
	
	public UndoableSchrittEntfernt(AbstractSchrittView schritt, SchrittSequenzView sequenz, int schrittIndex) {
		this.schritt = schritt;
		this.sequenz = sequenz;
		this.schrittIndex = schrittIndex;
	}
	
	@Override
	public void undoEdit() throws CannotUndoException {
		sequenz.schrittHinzufuegen(schritt, schrittIndex);
	}

	@Override
	public void redoEdit() throws EditException {
		schrittIndex = sequenz.schrittEntfernen(schritt, Discard);
	}

	@Override
	public String getPresentationName() {
		return i18n("undo.step.removed");
	}

	@Override
	public String getUndoPresentationName() {
		return i18n("undo.step.removed.undo");
	}

	@Override
	public String getRedoPresentationName() {
		return i18n("undo.step.removed.redo");
	}

}
