package specman.editarea.keylistener;

import specman.Specman;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedPosition;
import specman.editarea.markups.MarkedChar;
import specman.editarea.markups.MarkedCharSequence;
import specman.editarea.markups.MarkupBackgroundStyleInitializer;
import specman.editarea.markups.MarkupRecovery;
import specman.model.v001.Markup_V001;
import specman.undo.manager.UndoRecording;
import static specman.Specman.editor;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.List;

abstract class AbstractKeyEventHandler extends AbstractKeyHandler {
  protected final KeyEvent event;

  protected AbstractKeyEventHandler(TextEditArea textArea, KeyEvent event) {
    super(textArea);
    this.event = event;
  }

  abstract void handle();

  protected MarkedCharSequence findMarkups() {
    MarkedCharSequence seq = new MarkedCharSequence();
    WrappedDocument doc = getWrappedDocument();
    for (WrappedPosition p = doc.fromModel(0); p.exists(); p = p.inc()) {
      MarkedChar c = new MarkedChar(doc, p);
      seq.add(c);
    }
    return seq;
  }

  protected void backupMarkupsAndRecoverAfterDefaultKeyOperation() {
    MarkedCharSequence marksBackup = findMarkups();
    UndoRecording ur = editor().composeUndo();
    SwingUtilities.invokeLater(() -> {
      List<Markup_V001> recoveredChangemarks = new MarkupRecovery(getWrappedDocument(), marksBackup).recover();
      new MarkupBackgroundStyleInitializer(textArea, recoveredChangemarks).styleChangedTextSections();
      ur.close();
    });
  }
}
