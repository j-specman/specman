package net.atlanticbb.tantlinger.ui.text.actions;

import net.atlanticbb.tantlinger.ui.UIUtils;
import specman.Specman;
import static specman.Specman.editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.Serial;

public class UnorderedListItemAction extends BasicEditAction {
  @Serial
  private static final long serialVersionUID = 1L;

  public UnorderedListItemAction() {
    super("List item");
    this.putValue("SmallIcon", UIUtils.getIcon("resources/images/x16/", "listunordered.png"));
    this.putValue("ShortDescription", this.getValue("Name"));
  }

  @Override
  protected void doEdit(ActionEvent actionEvent, JEditorPane jEditorPane) {
    editor().toggleListItem(false);
  }

}
