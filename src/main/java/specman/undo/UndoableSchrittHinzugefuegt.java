package specman.undo;

import specman.EditException;
import specman.view.SchrittSequenzView;
import specman.view.AbstractSchrittView;

import javax.swing.undo.CannotRedoException;

import static specman.view.StepRemovalPurpose.Discard;
import static specman.i18n.Messages.i18n;

public class UndoableSchrittHinzugefuegt extends AbstractUndoableInteraction {

	private final AbstractSchrittView schritt;
	private final SchrittSequenzView sequenz;
	private int schrittIndex;
	
	public UndoableSchrittHinzugefuegt(AbstractSchrittView schritt, SchrittSequenzView sequenz) {
		this.schritt = schritt;
		this.sequenz = sequenz;
	}
	
	@Override
	public void undoEdit() throws EditException {
		schrittIndex = sequenz.schrittEntfernen(schritt, Discard);
	}

	@Override public boolean canUndo() {
		try {
			return sequenz.checkSchrittEntfernen(schritt, Discard);
		}
		catch(EditException ex) {
			return false;
		}
	}

	@Override
	public void redoEdit() throws CannotRedoException {
		sequenz.schrittHinzufuegen(schritt, schrittIndex);
	}

	@Override
	public String getPresentationName() {
		return i18n("undo.step.added");
	}

	@Override
	public String getUndoPresentationName() {
		return i18n("undo.step.added.undo");
	}

	@Override
	public String getRedoPresentationName() {
		return i18n("undo.step.added.redo");
	}

}
