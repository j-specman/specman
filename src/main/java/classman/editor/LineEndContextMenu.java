package classman.editor;

import classman.model.v001.ElementStyleModel_V001;
import classman.model.v001.EndType_V001;
import classman.model.v001.RelationEnd_V001;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.util.Locale;
import java.util.function.UnaryOperator;

/** The menu of the handle at an end of a selected relation or anchor line. For an association, aggregation or
 * composition it has the submenu *Type*, which says what there is at that end (an arrowhead, nothing, or a diamond) and
 * changes it. For a relation it also opens the dialog for the style of the relation, so that there is one menu less to
 * know: it is the only menu on a relation. For both it links the end to the center of its box. An entry which doesn't
 * fit the end right now is there but can't be chosen, and its tooltip says why. */
class LineEndContextMenu extends JPopupMenu {
  private static final int TYPE_ICON_SIZE = 20;
  private static final EndType_V001[] TYPES_IN_MENU = {EndType_V001.NAVIGABLE, EndType_V001.ASSOCIATION,
    EndType_V001.COMPOSITION, EndType_V001.AGGREGATION};

  /** @param stylePicker asks the user for the style of a relation, given a copy of the one to start from, and gives
   * null if the user doesn't choose one
   * @param showDiagram shows the diagram as the editing has it now */
  LineEndContextMenu(DiagramEditing editing, RelationEnd_V001 end, EditorTexts texts,
                     UnaryOperator<ElementStyleModel_V001> stylePicker, Runnable showDiagram) {
    if (editing.isRelationSelected() && editing.offersEndTypes()) {
      add(typeMenu(editing, end, texts, showDiagram));
    }
    JMenuItem center = new JMenuItem(texts.get("menu.line.end.center"));
    editing.canCenterEnd(end).applyTo(center);
    center.addActionListener(e -> {
      editing.centerEnd(end);
      showDiagram.run();
    });
    add(center);
    if (editing.isRelationSelected()) {
      addSeparator();
      add(new StyleMenuEntry(editing, editing.selectedRelation(), texts, stylePicker, showDiagram));
    }
  }

  /** The types as they look at the end: the icons of the tool bar have the diamond at the left and the arrowhead at the
   * right, so they are turned around where the end is on the other side: the diamond for the end at the right, the
   * arrowhead for the end at the left. */
  private static JMenu typeMenu(DiagramEditing editing, RelationEnd_V001 end, EditorTexts texts, Runnable showDiagram) {
    JMenu menu = new JMenu(texts.get("menu.line.end.type"));
    for (EndType_V001 type : TYPES_IN_MENU) {
      JMenuItem item = new JMenuItem(texts.get("menu.line.end.type." + type.name().toLowerCase(Locale.ROOT)),
        iconOf(type, end));
      editing.canSetEndType(end, type).applyTo(item);
      item.addActionListener(e -> {
        editing.setEndType(end, type);
        showDiagram.run();
      });
      menu.add(item);
    }
    return menu;
  }

  private static Icon iconOf(EndType_V001 type, RelationEnd_V001 end) {
    String path = switch (type) {
      case NAVIGABLE -> EditorIcons.NAVIGABLE;
      case ASSOCIATION -> EditorIcons.ASSOCIATION;
      case COMPOSITION -> EditorIcons.COMPOSITION;
      case AGGREGATION -> EditorIcons.AGGREGATION;
    };
    Icon icon = EditorIcons.read(path, TYPE_ICON_SIZE);
    boolean isOnOtherSide = type.isDiamond() ? end == RelationEnd_V001.TO
      : type == EndType_V001.NAVIGABLE && end == RelationEnd_V001.FROM;
    return isOnOtherSide ? new MirroredIcon(icon) : icon;
  }
}
