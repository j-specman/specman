package specman.undo;

import specman.EditException;
import specman.Specman;
import specman.undo.manager.UndoRecording;
import static specman.Specman.editor;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

abstract public class AbstractUndoableInteraction implements UndoableEdit {

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public boolean canRedo() {
		return true;
	}

	@Override
	public void die() {}

	@Override
	public boolean addEdit(UndoableEdit anEdit) {
		return false;
	}

	@Override
	public boolean replaceEdit(UndoableEdit anEdit) {
		return false;
	}

	@Override
	public boolean isSignificant() {
		return true;
	}

	@Override
	public String getPresentationName() {
		return null;
	}

	@Override
	public String getUndoPresentationName() {
		return null;
	}

	@Override
	public String getRedoPresentationName() {
		return null;
	}

	@Override public final void undo() throws CannotUndoException {
		try (UndoRecording ur = editor().pauseUndo()) {
			undoEdit();
		}
		catch(EditException ex) {
			editor().showError(ex);
			throw new CannotUndoException();
		}
	}

	@Override public final void redo() throws CannotRedoException {
		try (UndoRecording ur = editor().pauseUndo()) {
			redoEdit();
		}
		catch(EditException ex) {
			editor().showError(ex);
			throw new CannotRedoException();
		}
	}

	protected abstract void undoEdit() throws EditException;

	protected abstract void redoEdit() throws EditException;
}
