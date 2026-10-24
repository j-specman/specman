package specman;

import net.atlanticbb.tantlinger.shef.HTMLEditorPane;
import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;
import specman.ops.buttons.AcceptChangesADBLOpButton;
import specman.ops.buttons.BirdsViewSpecmanOpButton;
import specman.ops.buttons.CollapseAllOpButton;
import specman.ops.buttons.DeleteStepOpButton;
import specman.ops.buttons.ExpandAllOpButton;
import specman.ops.buttons.ExportPDFOpButton;
import specman.ops.buttons.MergeChangeSetsADBLOpButton;
import specman.ops.buttons.RejectChangesADBLOpButton;
import specman.ops.buttons.ReviewOpButton;

import specman.ops.buttons.ToggleBorderTypeOpButton;
import specman.ops.buttons.ToneOpButton;
import specman.settings.SettingAutoLoad;
import specman.settings.SettingAutoSave;
import specman.toolbar.RibbonToolBar;
import specman.toolbar.ToolbarGroup;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static specman.i18n.Messages.i18n;

/**
 * The single icon toolbar below the menu bar: the file, step, change, text, insert, view and export
 * function groups, each with its controls in two rows. The text and insert controls come from the SHEF editor pane.
 */
public class DiagramToolBar extends RibbonToolBar {

  public static final String SHADE_BUTTON = "toolbar.shade";
  public static final String DELETE_STEP_BUTTON = "toolbar.delete.step";
  public static final String TOGGLE_BORDER_BUTTON = "toolbar.toggle.border";
  public static final String TRACK_CHANGES_BUTTON = "toolbar.track.changes";
  public static final String ACCEPT_CHANGES_BUTTON = "toolbar.accept.changes";
  public static final String REJECT_CHANGES_BUTTON = "toolbar.reject.changes";
  public static final String MERGE_CHANGESETS_BUTTON = "toolbar.merge.changesets";
  public static final String REVIEW_BUTTON = "toolbar.review";
  public static final String BIRDSVIEW_BUTTON = "toolbar.birdsview";
  public static final String EXPORT_PDF_BUTTON = "toolbar.export.pdf";

  private final ToolbarToggleButton aenderungenVerfolgen;
  private final AcceptChangesADBLOpButton aenderungenUebernehmen;
  private final RejectChangesADBLOpButton aenderungenVerwerfen;
  private final MergeChangeSetsADBLOpButton mergeChangeSets;
  private final ZoomComboBox zoom;
  private final List<AbstractButton> stepButtons;
  private final HTMLEditorPane shefEditorPane;
  private final SettingAutoSave autoSave = new SettingAutoSave();
  private final ToolbarToggleButton autoLoadToggle;
  private static final int TEXT_FIRST_ROW_SIZE = 2;
  private static final int INSERT_FIRST_ROW_SIZE = 3;

  DiagramToolBar(Specman specman, HTMLEditorPane shefEditorPane) {
    this.shefEditorPane = shefEditorPane;
    ToneOpButton einfaerben = new ToneOpButton(specman);
    DeleteStepOpButton loeschen = new DeleteStepOpButton(specman);
    ToggleBorderTypeOpButton toggleBorderType = new ToggleBorderTypeOpButton(specman);

    aenderungenVerfolgen = new ToolbarToggleButton(SpecmanIcons.TRACK_CHANGES, i18n(TRACK_CHANGES_BUTTON));
    aenderungenVerfolgen.setName(TRACK_CHANGES_BUTTON);
    aenderungenUebernehmen = new AcceptChangesADBLOpButton(specman);
    aenderungenVerwerfen = new RejectChangesADBLOpButton(specman);
    mergeChangeSets = new MergeChangeSetsADBLOpButton(specman);

    ReviewOpButton review = new ReviewOpButton(specman);
    zoom = new ZoomComboBox(specman);
    CollapseAllOpButton collapseAll = new CollapseAllOpButton(specman);
    ExpandAllOpButton expandAll = new ExpandAllOpButton(specman);
    BirdsViewSpecmanOpButton birdsview =new BirdsViewSpecmanOpButton(specman);
    ExportPDFOpButton exportPDF = new ExportPDFOpButton(specman);

    stepButtons = List.of(einfaerben, loeschen, toggleBorderType);

    ToolbarGroup file = new ToolbarGroup("toolbar.group.file");
    JButton save = new JButton();
    save.addActionListener(e -> specman.diagrammSpeichern(false));
    addButton(file, save, SpecmanIcons.FILE_SAVE, "toolbar.save");
    autoLoadToggle = new ToolbarToggleButton(SpecmanIcons.AUTO_LOAD, i18n("toolbar.autoload"));
    autoLoadToggle.setSelected(SettingAutoLoad.isEnabled());
    autoLoadToggle.addActionListener(e -> {
      SettingAutoLoad.setEnabled(autoLoadToggle.isSelected());
      if (autoLoadToggle.isSelected()) {
        specman.checkWorkingCopyNow();
      }
    });
    file.addControl(autoLoadToggle);
    file.nextRow();
    file.addControl(new JLabel(i18n("toolbar.autosave")));
    file.addControl(autoSave.toolbarComponent());
    addGroup(file);

    ToolbarGroup step = new ToolbarGroup("toolbar.group.step");
    addButton(step, einfaerben, SpecmanIcons.TONE, SHADE_BUTTON);
    addButton(step, loeschen, SpecmanIcons.DELETE, DELETE_STEP_BUTTON);
    step.nextRow();
    addButton(step, toggleBorderType, SpecmanIcons.TOGGLE_BORDER, TOGGLE_BORDER_BUTTON);
    addGroup(step);

    ToolbarGroup changes = new ToolbarGroup("toolbar.group.changes");
    changes.addControl(aenderungenVerfolgen);
    addButton(changes, aenderungenUebernehmen, SpecmanIcons.ACCEPT_CHANGES, ACCEPT_CHANGES_BUTTON);
    addButton(changes, aenderungenVerwerfen, SpecmanIcons.REJECT_CHANGES, REJECT_CHANGES_BUTTON);
    changes.nextRow();
    addButton(changes, mergeChangeSets, SpecmanIcons.MERGE_CHANGE_SETS, MERGE_CHANGESETS_BUTTON);
    addButton(changes, review, SpecmanIcons.REVIEW, REVIEW_BUTTON);
    addGroup(changes);

    ToolbarGroup text = new ToolbarGroup("toolbar.group.text");
    // paragraph and font on top, size, color, inline styles and alignment below
    text.addControlsInTwoRows(shefEditorPane.getTextGroupComponents(), TEXT_FIRST_ROW_SIZE);
    addGroup(text);

    ToolbarGroup insert = new ToolbarGroup("toolbar.group.insert");
    // lists and link on top, image, table and step number link below
    insert.addControlsInTwoRows(shefEditorPane.getInsertGroupComponents(), INSERT_FIRST_ROW_SIZE);
    addGroup(insert);

    ToolbarGroup view = new ToolbarGroup("toolbar.group.view");
    view.addControl(zoom);
    view.nextRow();
    addButton(view, collapseAll, SpecmanIcons.COLLAPSE_ALL, "toolbar.collapse.all");
    addButton(view, expandAll, SpecmanIcons.EXPAND_ALL, "toolbar.expand.all");
    addButton(view, birdsview, SpecmanIcons.BIRDS_VIEW, BIRDSVIEW_BUTTON);
    addGroup(view);

    ToolbarGroup export = new ToolbarGroup("toolbar.group.export");
    addButton(export, exportPDF, SpecmanIcons.PDF, EXPORT_PDF_BUTTON);
    addGroup(export);
  }

  /** The button is named like its tooltip's message key, which is how tests find it. */
  private static void addButton(ToolbarGroup group, AbstractButton button, String iconPath, String tooltipKey) {
    button.setIcon(IconReader.readSvgIcon(iconPath, SpecmanIcons.SIZE_TOOLBAR));
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setToolTipText(i18n(tooltipKey));
    button.setName(tooltipKey);
    group.addControl(button);
  }

  public void setChangeModeEnabled(boolean enabled) {
    aenderungenVerfolgen.setSelected(enabled);
  }

  public boolean isChangeModeEnabled() {
    return aenderungenVerfolgen.isSelected();
  }

  /** Re-reads the auto-save and auto-load preferences, e.g. after the settings dialog was closed. */
  public void reloadFileSettings() {
    autoSave.reloadFromPreferences();
    autoLoadToggle.setSelected(SettingAutoLoad.isEnabled());
  }

  /** Enables the step buttons only for a resolvable step and the insert buttons only for a focused text area. */
  void updateFocusDependentButtons(boolean stepResolvable, boolean textAreaFocused) {
    stepButtons.forEach(button -> button.setEnabled(stepResolvable));
    shefEditorPane.setTextAreaActionsEnabled(textAreaFocused);
  }

  public void updateZoomDisplay(int prozent) {
    zoom.updateDisplay(prozent);
  }

}
