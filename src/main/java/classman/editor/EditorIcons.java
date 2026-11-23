package classman.editor;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.UIManager;
import java.awt.Color;

/** The icons of the editor. Pure black in an icon is drawn in the text color of the look-and-feel. */
final class EditorIcons {
  static final int SIZE = 28;
  static final int TOGGLE_SIZE = 24;

  static final String CLASS = "classman/editor/icons/class.svg";
  static final String INTERFACE = "classman/editor/icons/interface.svg";
  static final String ENUM = "classman/editor/icons/enum.svg";
  static final String NOTE = "classman/editor/icons/note.svg";
  static final String ASSOCIATION = "classman/editor/icons/association.svg";
  static final String AGGREGATION = "classman/editor/icons/aggregation.svg";
  static final String COMPOSITION = "classman/editor/icons/composition.svg";
  static final String EXTENDS = "classman/editor/icons/extends.svg";
  static final String IMPLEMENTS = "classman/editor/icons/implements.svg";
  static final String ANCHOR = "classman/editor/icons/anchor.svg";
  static final String ASSOC_CLASS = "classman/editor/icons/assocclass.svg";
  static final String FORMAT_PAINTER = "classman/editor/icons/formatpainter.svg";
  static final String HIDE_TEXT = "images/svg/material/left_panel_close.svg";
  static final String SHOW_TEXT = "images/svg/material/left_panel_open.svg";

  private EditorIcons() {}

  static FlatSVGIcon read(String path, int size) {
    if (EditorIcons.class.getClassLoader().getResource(path) == null) {
      throw new IllegalArgumentException("Can't load svg icon " + path);
    }
    FlatSVGIcon icon = new FlatSVGIcon(path, size, size, EditorIcons.class.getClassLoader());
    icon.setColorFilter(new FlatSVGIcon.ColorFilter(
      color -> Color.BLACK.equals(color) ? UIManager.getColor("Label.foreground") : color));
    return icon;
  }
}
