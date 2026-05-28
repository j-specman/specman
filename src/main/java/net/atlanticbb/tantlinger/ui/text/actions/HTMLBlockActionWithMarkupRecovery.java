package net.atlanticbb.tantlinger.ui.text.actions;

import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import org.bushe.swing.action.ActionList;
import specman.Specman;
import specman.editarea.TextEditArea;
import specman.editarea.markups.MarkedCharSequence;
import specman.editarea.markups.MarkupBackgroundStyleInitializer;
import specman.editarea.markups.MarkupRecovery;
import specman.model.v001.Markup_V001;
import specman.undo.manager.UndoRecording;
import static specman.Specman.editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.List;

public class HTMLBlockActionWithMarkupRecovery extends HTMLBlockAction {
  private final HTMLBlockAction core;

  private HTMLBlockActionWithMarkupRecovery(HTMLBlockAction core) throws IllegalArgumentException {
    super(readPrivateType(core));
    this.core = core;
  }

  private static int readPrivateType(HTMLBlockAction core) {
    try {
      Class clazz = core.getClass();
      Field typeField = clazz.getDeclaredField("type");
      typeField.setAccessible(true);
      return (int)typeField.get(core);
    }
    catch(Exception x) {
      throw new RuntimeException(x);
    }
  }

  @Override
  protected void sourceEditPerformed(ActionEvent actionEvent, JEditorPane jEditorPane) {
    core.sourceEditPerformed(actionEvent, jEditorPane);
  }

  @Override
  protected void wysiwygEditPerformed(ActionEvent actionEvent, JEditorPane jEditorPane) {
    try (UndoRecording ur = editor().composeUndo()) {
      MarkedCharSequence marksBackup = backupMarkups(jEditorPane);
      core.wysiwygEditPerformed(actionEvent, jEditorPane);
      recoverMarkups(marksBackup, jEditorPane);
    }
  }

  private void recoverMarkups(MarkedCharSequence marksBackup, JEditorPane jEditorPane) {
    if (marksBackup != null) {
      TextEditArea editArea = (TextEditArea) jEditorPane;
      List<Markup_V001> recoveredChangemarks = new MarkupRecovery(editArea.getWrappedDocument(), marksBackup).recover();
      new MarkupBackgroundStyleInitializer(editArea, recoveredChangemarks).styleChangedTextSections();
    }
  }

  private MarkedCharSequence backupMarkups(JEditorPane jEditorPane) {
    return ((TextEditArea)jEditorPane).findMarkups();
  }

  /** This turned out as the best compromise to inject the required markup recovery functionality
   * into actions for changing a paragraph type, repesented by {@link HTMLBlockAction} objects in Shef.
   * Assuming that {@link HTMLEditorPane} is the only class from Shef which is re-written within Specman,
   * we have to <i>wrap</i> HTMLBlockActions. The only realy bad thing here is that we have to use
   * reflection to read the private <code>type</code> field of HTMLBlockAction. */
  public static ActionList wrap(ActionList lst) {
    ActionList wrapped = new ActionList(lst.getId());
    for (int i = 0; i < lst.size(); i++) {
      Object o = lst.get(i);
      if (o == null) {
        wrapped.add(null);
      }
      else {
        HTMLBlockAction core = (HTMLBlockAction) o;
        HTMLBlockActionWithMarkupRecovery action = new HTMLBlockActionWithMarkupRecovery(core);
        wrapped.add(action);
      }
    }
    return wrapped;
  }

}
