package net.atlanticbb.tantlinger.ui.text.actions;

import org.bushe.swing.action.ActionList;

/** Creates SpecmanAlignActions rather than plain HTMLAlignActions from the Shef library. */
public class SpecmanAlignActionFactory {

  private SpecmanAlignActionFactory() {
  }

  /** @param afterAlign is called after any of the actions has aligned text */
  public static ActionList createAlignActionList(Runnable afterAlign) {
    ActionList list = new ActionList("align");
    for (int alignment = HTMLAlignAction.LEFT; alignment <= HTMLAlignAction.JUSTIFY; alignment++) {
      list.add(new SpecmanAlignAction(alignment, afterAlign));
    }
    return list;
  }

}
