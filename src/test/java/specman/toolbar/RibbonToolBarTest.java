package specman.toolbar;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.JButton;
import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.i18n.Messages.i18n;

@ExtendWith(HeadlessExtension.class)
class RibbonToolBarTest {

  private static ToolbarGroup group(String titleKey, int buttons) {
    ToolbarGroup group = new ToolbarGroup(titleKey);
    for (int i = 0; i < buttons; i++) {
      JButton button = new JButton();
      button.setPreferredSize(new Dimension(24, 24));
      group.addControl(button);
    }
    return group;
  }

  private static RibbonToolBar threeGroups() {
    RibbonToolBar bar = new RibbonToolBar();
    bar.addGroup(group("toolbar.group.file", 3));
    bar.addGroup(group("toolbar.group.step", 3));
    bar.addGroup(group("toolbar.group.view", 3));
    return bar;
  }

  @Test
  void groupTitleIsLocalized() {
    // file is "File" in English and "Datei" in German, so the text proves the key was resolved in either locale
    ToolbarGroup group = group("toolbar.group.file", 1);
    assertTrue(group.getComponent(1) instanceof javax.swing.JLabel);
    String title = ((javax.swing.JLabel) group.getComponent(1)).getText();
    assertEquals(i18n("toolbar.group.file"), title);
    assertFalse(title.startsWith("?"), "missing i18n key");
    assertNotEquals("toolbar.group.file", title);
  }

  @Test
  void allGroupsShareOneRowWithTheSameHeight() {
    RibbonToolBar bar = threeGroups();
    bar.setSize(bar.getPreferredSize());
    bar.doLayout();

    int groupHeight = bar.getComponent(0).getHeight();
    int x = 0;
    for (java.awt.Component group : bar.getComponents()) {
      assertEquals(0, group.getY());
      assertEquals(groupHeight, group.getHeight());
      assertEquals(x, group.getX());
      x += group.getWidth();
    }
  }

  @Test
  void widerBarKeepsEachGroupAtItsPreferredWidth() {
    RibbonToolBar bar = threeGroups();
    bar.setSize(bar.getPreferredSize().width * 2, bar.getPreferredSize().height);
    bar.doLayout();

    for (java.awt.Component group : bar.getComponents()) {
      assertEquals(group.getPreferredSize().width, group.getWidth());
    }
  }

  @Test
  void narrowWidthScrollsInsteadOfWrapping() {
    RibbonToolBar bar = threeGroups();
    int groupHeight = bar.getPreferredSize().height;
    javax.swing.JScrollPane scrollPane = bar.inScrollPane(null);

    scrollPane.setSize(bar.getPreferredSize().width / 2, 200);
    scrollPane.doLayout();
    // the parent layout gives the pane its preferred height, which includes the scroll bar
    scrollPane.invalidate();
    scrollPane.setSize(scrollPane.getWidth(), scrollPane.getPreferredSize().height);
    scrollPane.doLayout();
    scrollPane.getViewport().doLayout();

    assertTrue(scrollPane.getHorizontalScrollBar().isVisible());
    assertEquals(groupHeight + scrollPane.getHorizontalScrollBar().getHeight(), scrollPane.getHeight());
    assertEquals(groupHeight, bar.getHeight());
    assertEquals(bar.getPreferredSize().width, bar.getWidth());
  }

  @Test
  void sufficientWidthNeedsNoScrollBar() {
    RibbonToolBar bar = threeGroups();
    javax.swing.JScrollPane scrollPane = bar.inScrollPane(null);

    scrollPane.setSize(bar.getPreferredSize().width + 10, 200);
    scrollPane.doLayout();
    scrollPane.getViewport().doLayout();

    assertFalse(scrollPane.getHorizontalScrollBar().isVisible());
  }
}
