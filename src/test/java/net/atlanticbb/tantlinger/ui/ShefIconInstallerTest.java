package net.atlanticbb.tantlinger.ui;

import net.atlanticbb.tantlinger.ui.text.actions.CopyAction;
import org.junit.jupiter.api.Test;
import specman.graphics.SpecmanIcons;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ShefIconInstallerTest {

  @Test
  void installReplacesSmallIconWithSvgIcon() {
    Action copy = new CopyAction();
    Icon before = (Icon) copy.getValue(Action.SMALL_ICON);
    ShefIconInstaller.install(List.of(copy), List.of());
    Icon after = (Icon) copy.getValue(Action.SMALL_ICON);
    assertNotNull(after);
    assertEquals(SpecmanIcons.SIZE_SMALL, after.getIconWidth());
    assertNotSame(before, after);
  }

  @Test
  void installGivesToolbarButtonsTheLargeIconAfterTheActionIconWasReplaced() {
    Action copy = new CopyAction();
    JButton button = new JButton(copy);
    ShefIconInstaller.install(List.of(copy), List.of(button));
    assertEquals(SpecmanIcons.SIZE_TOOLBAR, button.getIcon().getIconWidth());
    assertEquals(SpecmanIcons.SIZE_SMALL, ((Icon) copy.getValue(Action.SMALL_ICON)).getIconWidth());
  }

}
