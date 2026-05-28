package specman.undo;

import specman.view.CaseSchrittView;
import specman.view.ZweigSchrittSequenzView;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class UndoableZweigEntfernt extends AbstractUndoableInteraction {
	final ZweigSchrittSequenzView zweig;
	final CaseSchrittView caseSchritt;
	final int zweigIndex;

	public UndoableZweigEntfernt(ZweigSchrittSequenzView zweig, CaseSchrittView caseSchritt, int zweigIndex) {
		this.zweig = zweig;
		this.caseSchritt = caseSchritt;
		this.zweigIndex = zweigIndex;
	}

	@Override
	public void undoEdit() throws CannotUndoException {
		caseSchritt.zweigHinzufuegen(zweig, zweigIndex);
	}

	@Override
	public void redoEdit() throws CannotRedoException {
		caseSchritt.zweigEntfernen(zweig);
	}


}
