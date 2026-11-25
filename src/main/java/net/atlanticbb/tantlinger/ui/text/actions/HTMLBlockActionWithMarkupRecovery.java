package net.atlanticbb.tantlinger.ui.text.actions;

import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import org.bushe.swing.action.ActionList;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;

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
    MarkupPreservingEdit.run(jEditorPane, () -> core.wysiwygEditPerformed(actionEvent, jEditorPane));
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
