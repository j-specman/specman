package net.atlanticbb.tantlinger.ui.text.actions;

import net.atlanticbb.tantlinger.ui.text.HTMLUtils;
import specman.Specman;
import static specman.Specman.editor;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;

public class SpecmanFontSizeAction extends HTMLFontSizeAction {
  public static final int DEFAULT_FONTSIZE = 15;
  public static final int DEFAULT_FONTSIZE_INDEX = 3;

  /** The font sizes from {@link HTMLFontSizeAction} are not accurate. The sizes
   * here turned out to be correct at least on Windows. In fact the sizes should
   * be initialized dynamically by rendering HTML strings with font-size 1, 2, 3,
   * etc. in an editor pane and then check which physical font sizes came out
   * of that. This might by machine-dependent. */
  public static int[] FONT_SIZES = new int[] { 8, 10, 12, DEFAULT_FONTSIZE, 19, 25, 38 };

  /** Must be duplicated here because the superclass field is private */
  protected int size;

  public SpecmanFontSizeAction(int size) throws IllegalArgumentException {
    super(size);
    this.size = size;
  }

  protected void updateWysiwygContextState(JEditorPane ed) {
    AttributeSet at = HTMLUtils.getCharacterAttributes(ed);
    if (at.isDefined(StyleConstants.FontSize)) {
      int zoomedFontSize = (Integer)at.getAttribute(StyleConstants.FontSize);
      int unzoomedFontSize = zoomedFontSize * 100 / editor().getZoomFactor();
      this.setSelected(unzoomedFontSize == FONT_SIZES[this.size]);
    }
    else {
      this.setSelected(this.size == DEFAULT_FONTSIZE_INDEX);
    }
  }


}
