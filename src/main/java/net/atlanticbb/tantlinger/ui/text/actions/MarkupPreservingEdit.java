package net.atlanticbb.tantlinger.ui.text.actions;

import specman.editarea.TextEditArea;
import specman.editarea.markups.MarkedCharSequence;
import specman.editarea.markups.MarkupBackgroundStyleInitializer;
import specman.editarea.markups.MarkupRecovery;
import specman.model.v002.Markup_V002;
import specman.undo.manager.UndoRecording;

import javax.swing.JEditorPane;
import java.util.List;

import static specman.Specman.editor;

/**
 * Runs an edit which rebuilds parts of the HTML document of a text area, like changing the paragraph type or the
 * alignment, so that the change marks survive it. The whole edit is recorded as one undo step.
 */
final class MarkupPreservingEdit {

  private MarkupPreservingEdit() {
  }

  static void run(JEditorPane jEditorPane, Runnable edit) {
    try (UndoRecording ignored = editor().composeUndo()) {
      TextEditArea editArea = (TextEditArea) jEditorPane;
      MarkedCharSequence marksBackup = editArea.findMarkups();
      edit.run();
      recoverMarkups(editArea, marksBackup);
    }
  }

  private static void recoverMarkups(TextEditArea editArea, MarkedCharSequence marksBackup) {
    if (marksBackup != null) {
      List<Markup_V002> recoveredChangemarks = new MarkupRecovery(editArea.getWrappedDocument(), marksBackup).recover();
      new MarkupBackgroundStyleInitializer(editArea, recoveredChangemarks).styleChangedTextSections();
    }
  }
}
