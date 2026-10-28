package specman;

import specman.draganddrop.DragMouseAdapter;
import specman.draganddrop.DragSource;
import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;
import specman.ops.buttons.CreateBreakStepOpButton;
import specman.ops.buttons.CreateCaseBranchADBLOpButton;
import specman.ops.buttons.CreateCaseStepOpButton;
import specman.ops.buttons.CreateCatchSequenceADBLOpButton;
import specman.ops.buttons.CreateIfElseStepOpButton;
import specman.ops.buttons.CreateIfStepOpButton;
import specman.ops.buttons.CreateSimpleStepOpButton;
import specman.ops.buttons.CreateSubsequenceStepOpButton;
import specman.ops.buttons.CreateWhileStepOpButton;
import specman.ops.buttons.CreateWhileWhileStepOpButton;
import specman.ops.buttons.DragSourceProvider;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static specman.i18n.Messages.i18n;

public class StepButtonBar extends JToolBar {

  private static final float TEXT_SCALE = 1.2f;
  private static final String PREF_SHOW_TEXT = "stepButtonBarShowText";
  private static final String LABEL_PROPERTY = "label";
  private static final String TOOLTIP_PROPERTY = "tooltip";

  private final List<JButton> stepButtons = new ArrayList<>();
  private final JButton toggleButton = new JButton();
  private boolean showText;

  StepButtonBar(Specman specman) {
    super(JToolBar.VERTICAL);
    showText = Preferences.userNodeForPackage(Specman.class).getBoolean(PREF_SHOW_TEXT, true);

    toggleButton.setMargin(new Insets(0, 0, 0, 0));
    toggleButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    toggleButton.addActionListener(e -> toggleShowText());
    add(toggleButton);
    addSeparator();

    addDragButton(new CreateSimpleStepOpButton(specman), SpecmanIcons.STEP_SIMPLE, "simple", specman);
    addDragButton(new CreateWhileStepOpButton(specman), SpecmanIcons.STEP_WHILE, "while", specman);
    addDragButton(new CreateWhileWhileStepOpButton(specman), SpecmanIcons.STEP_WHILEWHILE, "while.while", specman);
    addDragButton(new CreateIfElseStepOpButton(specman), SpecmanIcons.STEP_IFELSE, "if.else", specman);
    addDragButton(new CreateIfStepOpButton(specman), SpecmanIcons.STEP_IF, "if", specman);
    addDragButton(new CreateCaseStepOpButton(specman), SpecmanIcons.STEP_CASE, "case", specman);
    addDragButton(new CreateSubsequenceStepOpButton(specman), SpecmanIcons.STEP_SUBSEQUENCE, "subsequence", specman);
    addDragButton(new CreateBreakStepOpButton(specman), SpecmanIcons.STEP_BREAK, "break", specman);
    addDragButton(new CreateCatchSequenceADBLOpButton(specman), SpecmanIcons.STEP_CATCH, "catch", specman);
    addDragButton(new CreateCaseBranchADBLOpButton(specman), SpecmanIcons.STEP_CASE_BRANCH, "case.branch", specman);

    applyShowText();
  }

  /** @param stepKey suffix of the message keys {@code palette.label.*} (text shown next to the icon)
   * and {@code palette.create.*} (tooltip in icon-only mode) */
  private void addDragButton(JButton button, String iconPath, String stepKey, Specman specman) {
    button.setIcon(IconReader.readSvgIcon(iconPath, SpecmanIcons.SIZE_TOOLBAR));
    button.putClientProperty(LABEL_PROPERTY, i18n("palette.label." + stepKey));
    button.putClientProperty(TOOLTIP_PROPERTY, i18n("palette.create." + stepKey));
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setFont(button.getFont().deriveFont(button.getFont().getSize2D() * TEXT_SCALE));
    button.setIconTextGap(8);
    button.setHorizontalAlignment(SwingConstants.LEADING);
    button.setHorizontalTextPosition(SwingConstants.TRAILING);
    button.setAlignmentX(Component.LEFT_ALIGNMENT);
    stepButtons.add(button);
    add(button);
    DragMouseAdapter dragAdapter = new DragMouseAdapter(specman, this);
    button.addMouseListener(dragAdapter);
    button.addMouseMotionListener(dragAdapter);
  }

  private void toggleShowText() {
    showText = !showText;
    Preferences.userNodeForPackage(Specman.class).putBoolean(PREF_SHOW_TEXT, showText);
    applyShowText();
    Container topLevel = getTopLevelAncestor();
    if (topLevel != null) {
      topLevel.revalidate();
      topLevel.repaint();
    }
  }

  private void applyShowText() {
    updateToggleButton();
    updateStepButtonTexts();
    if (showText) {
      equalizeButtonWidths();
    }
    else {
      releaseButtonWidths();
    }
  }

  private void updateToggleButton() {
    toggleButton.setIcon(IconReader.readSvgIcon(showText ? SpecmanIcons.PANEL_HIDE_TEXT : SpecmanIcons.PANEL_SHOW_TEXT,
        SpecmanIcons.SIZE_TOOLBAR));
    toggleButton.setToolTipText(i18n(showText ? "palette.toggle.icons.only" : "palette.toggle.icons.text"));
  }

  /** Shows the label next to the icon, or only a tooltip in icon-only mode. Resets the preferred size for the width calculation. */
  private void updateStepButtonTexts() {
    for (JButton button : stepButtons) {
      button.setText(showText ? (String) button.getClientProperty(LABEL_PROPERTY) : null);
      button.setToolTipText(showText ? null : (String) button.getClientProperty(TOOLTIP_PROPERTY));
      button.setPreferredSize(null);
    }
  }

  /** Gives all buttons, including the toggle button, the width of the widest one. */
  private void equalizeButtonWidths() {
    int maxWidth = stepButtons.stream().mapToInt(b -> b.getPreferredSize().width).max().orElse(0);
    for (JButton button : stepButtons) {
      button.setPreferredSize(new Dimension(maxWidth, button.getPreferredSize().height));
      button.setMaximumSize(button.getPreferredSize());
    }
    toggleButton.setMaximumSize(new Dimension(maxWidth, toggleButton.getPreferredSize().height));
  }

  private void releaseButtonWidths() {
    for (JButton button : stepButtons) {
      button.setMaximumSize(null);
    }
    toggleButton.setMaximumSize(null);
  }

  public DragSource dragSourceFor(JButton button) {
    return ((DragSourceProvider) button).dragSource();
  }

}
