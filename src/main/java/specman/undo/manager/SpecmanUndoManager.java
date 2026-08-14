package specman.undo.manager;

import org.apache.commons.lang.StringUtils;
import specman.Specman;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.util.Stack;

import static specman.undo.manager.UndoRecordingMode.Composing;
import static specman.undo.manager.UndoRecordingMode.ComposingWhilePaused;

/** Derivation of a Swing undo manager for the following tasks:
 * <ul>
 *   <li>It displays an asterisk in the current Specman title bar as an indicator for unsaved changes
 *   if there are any undoable edits recorded.</li>
 *   <li>It allows pausing undo recording. Especially during undo and redo operations, the recording is
 *   paused (see {@link specman.undo.AbstractUndoableInteraction}).</li>
 *   <li>It allows to summarize multiple edits into composite undos for complex operations.</i>
 * </ul>
 * The manager's recording mode is organized as a stack in case it is changed multiple times within a
 * call chain. It behaves a bit like a simple transaction manager. */
public class SpecmanUndoManager extends UndoManager {
    private static final String UNSAVED_CHANGES_INDICATOR = "*";

    private final Specman specman;
    private final Stack<UndoRecordingMode> recordingMode = new Stack<>();
    private UndoableComposition recordingComposition;
    /** Set when a model was restored from an auto-save working copy. In that case the file
     * on disk still holds the older pre-crash state, so the model must be treated as modified
     * even though the undo stack is empty (all edits were discarded after loading the working
     * copy). Will be cleared by {@link #discardAllEdits()} when the user saves the model. */
    private boolean unsavedWorkingCopy = false;

    public SpecmanUndoManager(Specman specman) {
        this.specman = specman;
        recordingMode.push(UndoRecordingMode.Normal);
    }

    @Override
    public synchronized boolean addEdit(UndoableEdit anEdit) {
        switch(recordingMode.peek()) {
            case Normal:
                boolean success = super.addEdit(anEdit);
                if (success) {
                    updateUnsavedChangesIndicatorInTitleBar();
                    //System.out.println("Wert false");
                }
                return success;
            case Composing:
                recordingComposition.add(anEdit);
                break;
        }
        return true;
    }


    @Override
    public boolean replaceEdit(UndoableEdit anEdit) {
        return super.replaceEdit(anEdit);
    }

    @Override
    public synchronized void discardAllEdits() {
        super.discardAllEdits();
        unsavedWorkingCopy = false;
        updateUnsavedChangesIndicatorInTitleBar();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        updateUnsavedChangesIndicatorInTitleBar();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        updateUnsavedChangesIndicatorInTitleBar();
    }

    private void updateUnsavedChangesIndicatorInTitleBar() {
        String currentTitle = specman.getTitle();
        if (canUndo() || unsavedWorkingCopy) {
            if (!hasUnsavedChanges()) {
                specman.setTitle(UNSAVED_CHANGES_INDICATOR + currentTitle);
            }
        } else {
            if (hasUnsavedChanges()) {
                specman.setTitle(StringUtils.removeStart(currentTitle, UNSAVED_CHANGES_INDICATOR));
            }
        }
    }

    public void pushRecordingMode(UndoRecordingMode mode) {
        UndoRecordingMode currentMode = recordingMode.peek();
        if (currentMode == UndoRecordingMode.Paused && mode == Composing) {
            mode = ComposingWhilePaused;
        }
        if (mode == Composing && recordingComposition == null) {
            recordingComposition = new UndoableComposition();
        }
        recordingMode.push(mode);
    }

    public void popRecordingMode() {
        UndoRecordingMode lastMode = recordingMode.pop();
        if (lastMode == Composing && !recordingMode.contains(Composing)) {
            addEdit(recordingComposition);
            recordingComposition = null;
        }
    }

    public boolean hasUnsavedChanges() {
        return specman.getTitle().startsWith(UNSAVED_CHANGES_INDICATOR);
    }

    public void markAsUnsavedWorkingCopy() {
        unsavedWorkingCopy = true;
        updateUnsavedChangesIndicatorInTitleBar();
    }
}