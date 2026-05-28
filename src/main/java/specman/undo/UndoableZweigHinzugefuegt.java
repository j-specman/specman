package specman.undo;

import specman.view.CaseSchrittView;
import specman.view.ZweigSchrittSequenzView;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class UndoableZweigHinzugefuegt extends AbstractUndoableInteraction {
	final ZweigSchrittSequenzView zweig;
	final CaseSchrittView caseSchritt;
	int zweigIndex;

	public UndoableZweigHinzugefuegt(ZweigSchrittSequenzView zweig, CaseSchrittView caseSchritt) {
		this.caseSchritt = caseSchritt;
		this.zweig = zweig;
	}

	@Override
	public void undoEdit() throws CannotUndoException {
		zweigIndex = caseSchritt.zweigEntfernen(zweig);
	}

	@Override
	public void redoEdit() throws CannotRedoException {
		caseSchritt.zweigHinzufuegen(zweig, zweigIndex);
	}

}
