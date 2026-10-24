package net.atlanticbb.tantlinger.ui;

import net.atlanticbb.tantlinger.ui.text.actions.ClearStylesAction;
import net.atlanticbb.tantlinger.ui.text.actions.CopyAction;
import net.atlanticbb.tantlinger.ui.text.actions.CutAction;
import net.atlanticbb.tantlinger.ui.text.actions.FindReplaceAction;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLAlignAction;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLBlockAction;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLElementPropertiesAction;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLFontColorAction;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLHorizontalRuleAction;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLInlineAction;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLLineBreakAction;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLLinkAction;
import net.atlanticbb.tantlinger.ui.text.actions.PasteAction;
import net.atlanticbb.tantlinger.ui.text.actions.PasteFormattedAction;
import net.atlanticbb.tantlinger.ui.text.actions.SelectAllAction;
import net.atlanticbb.tantlinger.ui.text.actions.SpecialCharAction;
import net.atlanticbb.tantlinger.ui.text.actions.SpecmanHTMLFontAction;
import specman.graphics.SpecmanIcons;

import javax.swing.Action;
import javax.swing.text.html.HTML;
import java.util.Arrays;

/**
 * Maps those SHEF actions whose classes live only in the bundled SHEF jar (so their constructors cannot
 * be changed) to the Material SVG icon that replaces their bitmap icon. Pure lookup without side effects;
 * {@link ShefIconInstaller} applies the result.
 */
public final class ShefActionIconMapper {

  private ShefActionIconMapper() {
  }

  /** @return the SVG resource path for the action, or {@code null} if the action keeps its own icon */
  public static String pathFor(Action action) {
    if (action instanceof CutAction) {
      return SpecmanIcons.CUT;
    }
    if (action instanceof CopyAction) {
      return SpecmanIcons.COPY;
    }
    if (action instanceof PasteAction || action instanceof PasteFormattedAction) {
      return SpecmanIcons.PASTE;
    }
    if (action instanceof SelectAllAction) {
      return SpecmanIcons.SELECT_ALL;
    }
    if (action instanceof FindReplaceAction) {
      return SpecmanIcons.FIND_REPLACE;
    }
    if (action instanceof HTMLInlineAction inlineAction) {
      return inlineIconPath(inlineAction.getTag());
    }
    if (action instanceof HTMLAlignAction) {
      return alignIconPath(action);
    }
    if (action instanceof HTMLBlockAction blockAction) {
      return blockIconPath(blockAction.getTag());
    }
    if (action instanceof HTMLFontColorAction) {
      return SpecmanIcons.FONT_COLOR;
    }
    if (action instanceof SpecmanHTMLFontAction) {
      return SpecmanIcons.FONT_DIALOG;
    }
    if (action instanceof ClearStylesAction) {
      return SpecmanIcons.CLEAR_STYLES;
    }
    if (action instanceof HTMLElementPropertiesAction) {
      return SpecmanIcons.ELEMENT_PROPERTIES;
    }
    if (action instanceof HTMLLinkAction) {
      return SpecmanIcons.LINK;
    }
    if (action instanceof HTMLLineBreakAction) {
      return SpecmanIcons.LINE_BREAK;
    }
    if (action instanceof HTMLHorizontalRuleAction) {
      return SpecmanIcons.HORIZONTAL_RULE;
    }
    if (action instanceof SpecialCharAction) {
      return SpecmanIcons.SPECIAL_CHARACTER;
    }
    return null;
  }

  private static String inlineIconPath(HTML.Tag tag) {
    if (tag == HTML.Tag.B) {
      return SpecmanIcons.FORMAT_BOLD;
    }
    if (tag == HTML.Tag.I) {
      return SpecmanIcons.FORMAT_ITALIC;
    }
    if (tag == HTML.Tag.U) {
      return SpecmanIcons.FORMAT_UNDERLINE;
    }
    return null;
  }

  private static String blockIconPath(HTML.Tag tag) {
    if (tag == HTML.Tag.OL) {
      return SpecmanIcons.LIST_ORDERED;
    }
    if (tag == HTML.Tag.UL) {
      return SpecmanIcons.LIST_UNORDERED;
    }
    return null;
  }

  /** HTMLAlignAction keeps its alignment private; the action name is the localized ALIGNMENT_NAMES entry. */
  private static String alignIconPath(Action action) {
    int alignment = Arrays.asList(HTMLAlignAction.ALIGNMENT_NAMES).indexOf(action.getValue(Action.NAME));
    return switch (alignment) {
      case HTMLAlignAction.LEFT -> SpecmanIcons.ALIGN_LEFT;
      case HTMLAlignAction.CENTER -> SpecmanIcons.ALIGN_CENTER;
      case HTMLAlignAction.RIGHT -> SpecmanIcons.ALIGN_RIGHT;
      case HTMLAlignAction.JUSTIFY -> SpecmanIcons.ALIGN_JUSTIFY;
      default -> null;
    };
  }

}
