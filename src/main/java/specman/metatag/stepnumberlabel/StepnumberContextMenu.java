package specman.metatag.stepnumberlabel;

import specman.graphics.IconReader;
import specman.graphics.SvgIcon;
import specman.i18n.ICheckBoxMenuItem;
import specman.i18n.IMenu;
import specman.i18n.IMenuItem;
import specman.metatag.generic.GenericMetaTag;
import specman.metatag.generic.config.MetaTagConfig;
import specman.ops.CopyStepLinkOp;
import specman.undo.UndoableFlatNumberingToggled;
import specman.undo.UndoableMetaTagAssigned;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;
import static specman.Specman.editor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

public class StepnumberContextMenu implements MouseListener {
  public static final String DELETE_ITEM = "menu.context.delete";
  public static final String COPY_ITEM = "menu.context.copy";
  public static final String CUT_ITEM = "menu.context.cut";
  public static final String PASTE_ITEM = "menu.context.paste";
  public static final String COPY_LINK_ITEM = "menu.context.copy.link";
  public static final String MOVE_LEFT_ITEM = "menu.context.move.left";
  public static final String MOVE_RIGHT_ITEM = "menu.context.move.right";
  public static final String MOVE_UP_ITEM = "menu.context.move.up";
  public static final String MOVE_DOWN_ITEM = "menu.context.move.down";
  public static final String FLAT_NUMBERING_ITEM = "menu.context.flat.numbering";
  public static final String TAGS_MENU = "menu.context.tags";
  public static final String CONFIGURE_TAGS_ITEM = "menu.context.configure";

  private static final int META_TAG_ICON_SIZE = 16;
  private static final int MENU_ICON_SIZE = 16;

  private final JPopupMenu popup;
  private final JMenuItem delete;
  private final JMenuItem copy;
  private final JMenuItem cut;
  private final JMenuItem paste;
  private final JMenuItem copyLink;
  private final JMenuItem left;
  private final JMenuItem right;
  private final JMenuItem up;
  private final JMenuItem down;
  private final JCheckBoxMenuItem toggleFlatNumbering;
  private final JPopupMenu.Separator metaTagSeparator = new JPopupMenu.Separator();
  private final JMenu metaTagMenu = new IMenu(TAGS_MENU);
  private AbstractSchrittView currentStep;
  private StepnumberLabel initiatingLabel;

  static StepnumberContextMenu instance = new StepnumberContextMenu();

  private StepnumberContextMenu() {
    popup = new JPopupMenu();
    delete = createDeleteItem();
    copy = createCopyItem();
    cut = createCutItem();
    paste = createPasteItem();
    copyLink = createCopyLinkItem();
    left = createLeftItem();
    right = createRightItem();
    up = createUpItem();
    down = createDownItem();
    toggleFlatNumbering = createSubnumberingItem();
    popup.add(metaTagSeparator);
    metaTagMenu.setIcon(IconReader.readImageIcon("tag"));
    popup.add(metaTagMenu);
  }

  private JMenuItem createUpItem() {
    return createItem(MOVE_UP_ITEM,"arrow-up", e -> {
      try(UndoRecording ur = editor().composeUndo()) {
        currentStep.moveCoCatchUpUDBL(initiatingLabel);
      }
    });
  }

  private JMenuItem createDownItem() {
    return createItem(MOVE_DOWN_ITEM,"arrow-down", e -> {
      try(UndoRecording ur = editor().composeUndo()) {
        currentStep.moveCoCatchDownUDBL(initiatingLabel);
      }
    });
  }

  private JCheckBoxMenuItem createSubnumberingItem() {
    JCheckBoxMenuItem item = new ICheckBoxMenuItem(FLAT_NUMBERING_ITEM);
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try (UndoRecording ur = editor().composeUndo()) {
          currentStep.toggleFlatNumbering(toggleFlatNumbering.getState());
          editor().addEdit(new UndoableFlatNumberingToggled(currentStep, toggleFlatNumbering.getState()));
        }
      }
    });
    popup.add(item);
    return item;
  }

  private JMenuItem createItem(String messageKey, String iconBasename, ActionListener actionListener) {
    ImageIcon icon = IconReader.readImageIcon(iconBasename);
    JMenuItem item = new IMenuItem(messageKey, icon);
    item.addActionListener(actionListener);
    popup.add(item);
    return item;
  }

  private JMenuItem createDeleteItem() {
    return createItem(DELETE_ITEM,"loeschen",
      e -> editor().deleteStepADBL(currentStep, initiatingLabel));
  }

  private JMenuItem createCopyItem() {
    return createItem(COPY_ITEM,"copy", e -> editor().copyStepToClipboard(currentStep));
  }

  private JMenuItem createCutItem() {
    return createItem(CUT_ITEM,"cut", e -> editor().cutStepToClipboard(currentStep, initiatingLabel));
  }

  private JMenuItem createPasteItem() {
    return createItem(PASTE_ITEM,"paste", e -> editor().pasteStepsAfter(currentStep));
  }

  private JMenuItem createCopyLinkItem() {
    JMenuItem item = new IMenuItem(COPY_LINK_ITEM, new SvgIcon("copy-link", MENU_ICON_SIZE));
    item.addActionListener(e -> new CopyStepLinkOp(currentStep).run());
    popup.add(item);
    return item;
  }

  private JMenuItem createLeftItem() {
    return createItem(MOVE_LEFT_ITEM,"arrow-left",
      e -> editor().moveBranchSequenceLeftADBL(currentStep, initiatingLabel));
  }

  private JMenuItem createRightItem() {
    return createItem(MOVE_RIGHT_ITEM,"arrow-right",
      e -> editor().moveBranchSequenceRightADBL(currentStep, initiatingLabel));
  }

  private StepnumberLabel label(MouseEvent e) {
    return (StepnumberLabel) e.getComponent();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (SwingUtilities.isRightMouseButton(e)) {
      AbstractSchrittView step = editor().findStep(label(e));
      initMenuForStep(step, label(e));
      // Not showing means headless in unit tests: The initialized menu stays operable via popupMenu()
      if (e.getComponent().isShowing()) {
        popup.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }

  /** The menu as initialized for the step last right-clicked. Allows tests to operate the menu without showing it. */
  public static JPopupMenu popupMenu() {
    return instance.popup;
  }

  private void initMenuForStep(AbstractSchrittView currentStep, StepnumberLabel initiatingLabel) {
    this.currentStep = currentStep;
    this.initiatingLabel = initiatingLabel;
    this.delete.setEnabled(currentStep.allowsDeletion(initiatingLabel));
    boolean clipboardAllowed = currentStep.allowsClipboardOperations();
    this.copy.setEnabled(clipboardAllowed);
    this.cut.setEnabled(clipboardAllowed);
    this.paste.setEnabled(clipboardAllowed);
    // A link needs a file name, which an unsaved diagram doesn't have yet
    this.copyLink.setEnabled(editor().getDiagrammDatei() != null);
    this.toggleFlatNumbering.setVisible(currentStep.getFlatNumbering() != null);
    if (toggleFlatNumbering.isVisible()) {
      toggleFlatNumbering.setState(currentStep.getFlatNumbering());
    }
    initLeftRightMenuItems();
    initUpDownMenuItems();
    initMetaTagMenu();
  }

  private void initMetaTagMenu() {
    boolean tagsAllowed = currentStep.allowsTags(initiatingLabel);
    metaTagSeparator.setVisible(tagsAllowed);
    metaTagMenu.setVisible(tagsAllowed);
    if (!tagsAllowed) {
      return;
    }
    metaTagMenu.removeAll();
    List<MetaTagConfig> configs = editor().metaTagConfigs();
    for (MetaTagConfig config : configs) {
      Icon icon = config.hasIcon() ? new SvgIcon(META_TAG_ICON_SIZE, config.getIconSvg()) : null;
      String text = config.getLabel().isEmpty() ? config.getName() : config.getLabel();
      JMenuItem item = new JMenuItem(text, icon);
      item.setEnabled(!currentStep.hasTag(config));
      item.addActionListener(e -> assignTag(config));
      metaTagMenu.add(item);
    }
    if (!configs.isEmpty()) {
      metaTagMenu.addSeparator();
    }
    JMenuItem configure = new IMenuItem(CONFIGURE_TAGS_ITEM,IconReader.readImageIcon("tags"));
    configure.addActionListener(e -> editor().openMetaTagConfigs());
    metaTagMenu.add(configure);
  }

  private void assignTag(MetaTagConfig config) {
    GenericMetaTag tag = new GenericMetaTag(config, null);
    int index = currentStep.addTag(tag);
    editor().addEdit(new UndoableMetaTagAssigned(currentStep, tag, index));
  }

  private void initLeftRightMenuItems() {
    boolean leftAllowed = currentStep.allowsBranchSequenceMoveLeft(initiatingLabel);
    boolean rightAllowed = currentStep.allowsBranchSequenceMoveRight(initiatingLabel);
    boolean leftOrRight = leftAllowed || rightAllowed;
    this.left.setVisible(leftOrRight);
    this.right.setVisible(leftOrRight);
    if (leftOrRight) {
      this.left.setEnabled(leftAllowed);
      this.right.setEnabled(rightAllowed);
    }
  }

  private void initUpDownMenuItems() {
    boolean upAllowed = currentStep.allowsCoCatchMoveUp(initiatingLabel);
    boolean downAllowed = currentStep.allowsCoCatchMoveDown(initiatingLabel);
    boolean upOrDown = upAllowed || downAllowed;
    this.up.setVisible(upOrDown);
    this.down.setVisible(upOrDown);
    if (upOrDown) {
      this.up.setEnabled(upAllowed);
      this.down.setEnabled(downAllowed);
    }
  }

  @Override public void mousePressed(MouseEvent e) {}
  @Override public void mouseReleased(MouseEvent e) {}
  @Override public void mouseEntered(MouseEvent e) {}
  @Override public void mouseExited(MouseEvent e) {}
}
