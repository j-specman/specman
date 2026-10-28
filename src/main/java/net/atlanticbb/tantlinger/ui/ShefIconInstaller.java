package net.atlanticbb.tantlinger.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;

import javax.swing.AbstractButton;
import javax.swing.Action;

/**
 * Applies the icons chosen by {@link ShefActionIconMapper} to the SHEF editor UI. Buttons, menu items and
 * popup entries created from an action follow the {@link Action#SMALL_ICON} property, so setting it on the
 * action updates all of them; the given toolbar buttons then get the larger {@link SpecmanIcons#SIZE_TOOLBAR}
 * variant. The two steps must run in this order, because changing an action's SMALL_ICON resets the icon
 * of every button bound to it.
 */
public final class ShefIconInstaller {

  private ShefIconInstaller() {
  }

  /** Must be called after all actions and the toolbar buttons have been created. */
  public static void install(Iterable<?> actions, Iterable<? extends AbstractButton> toolbarButtons) {
    setActionIcons(actions);
    enlargeToolbarIcons(toolbarButtons);
  }

  private static void setActionIcons(Iterable<?> actions) {
    for (Object candidate : actions) {
      if (candidate instanceof Action action) {
        String iconPath = ShefActionIconMapper.pathFor(action);
        if (iconPath != null) {
          action.putValue(Action.SMALL_ICON, IconReader.readSvgIcon(iconPath, SpecmanIcons.SIZE_SMALL));
        }
      }
    }
  }

  private static void enlargeToolbarIcons(Iterable<? extends AbstractButton> toolbarButtons) {
    for (AbstractButton button : toolbarButtons) {
      if (button.getAction() != null
          && button.getAction().getValue(Action.SMALL_ICON) instanceof FlatSVGIcon smallIcon) {
        button.setIcon(smallIcon.derive(SpecmanIcons.SIZE_TOOLBAR, SpecmanIcons.SIZE_TOOLBAR));
      }
    }
  }

}
