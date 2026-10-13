package net.atlanticbb.tantlinger.ui.text.dialogs;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import net.atlanticbb.tantlinger.i18n.I18n;
import net.atlanticbb.tantlinger.ui.text.TextEditPopupManager;
import specman.Specman;
import specman.editarea.InteractiveStepFragment;
import specman.search.MetaTagFreetextSearch;
import specman.search.SearchScope;
import specman.search.TextMatcher;
import specman.undo.manager.UndoRecording;
import static specman.Specman.editor;

/** This is an adaption of the original TextFinerDialog, extended to work
 * on a list of text components rather than a single one. Unfortunately the
 * original dialog class was not flexible enough to implement the extended
 * functionality as a derivation. */
public class TextFinderDialog extends JDialog {

  private static final long serialVersionUID = 1L;
  private static final I18n i18n = I18n.getInstance("net.atlanticbb.tantlinger.ui.text.dialogs");
  // The labels of the "Search in" box are no i18n entries as these live in the SHEF library
  private static final String SEARCH_IN_LABEL = "Search in:";
  // Label, gap, field, gap, buttons. The buttons have the same width on both tabs.
  private static final String FORM_COLUMN_SPECS = "left:pref, 4dlu, fill:120dlu:grow, 8dlu, fill:max(pref;60dlu)";
  // Takes the extra height of the tab with less rows, so the options stick to the bottom on both tabs
  private static final String FILLER_ROW = "fill:0px:grow";

  private final MetaTagFreetextSearch tagSearch = new MetaTagFreetextSearch();
  private final DefaultComboBoxModel<SearchScope> modelScope = new DefaultComboBoxModel<>(SearchScope.values());
  public static final int FIND = 0;
  public static final int REPLACE = 1;
  protected Frame owner;
  protected List<JTextComponent> allTextComponents;
  protected JTextComponent monitor;
  protected int monitorIndex;
  protected JTabbedPane tb;
  protected JTextField txtFind1;
  protected JTextField txtFind2;
  protected Document docFind;
  protected Document docReplace;
  protected ButtonModel modelWord;
  protected ButtonModel modelCase;
  protected ButtonModel modelUp;
  protected ButtonModel modelDown;
  private TextEditPopupManager popupManager = TextEditPopupManager.getInstance();
  protected boolean searchUp = false;
  protected String searchData;
  private static final String TITLE;

  public TextFinderDialog(Frame owner, int index) {
    super(owner, TITLE, false);
    this.init(index);
  }

  public TextFinderDialog(Dialog owner, int index) {
    super(owner, TITLE, false);
    this.init(index);
  }

  public void initSearchCycle(JTextComponent tc) {
    this.allTextComponents = editor().queryAllTextComponents(tc);
    String selectedText = tc.getSelectedText();
    if (selectedText != null) {
      txtFind1.setText(selectedText);
      txtFind2.setText(selectedText);
    }
    int monitorIndex = allTextComponents.indexOf(tc);
    this.setJTextComponent(monitorIndex);
  }

  private void init(int index) {
    this.tb = new JTabbedPane();
    this.tb.addTab(i18n.str("find"), createFindPanel());
    this.tb.addTab(i18n.str("replace"), createReplacePanel());
    this.tb.setSelectedIndex(index);
    this.getContentPane().add(this.tb, "Center");
    WindowListener flst = new WindowAdapter() {
      public void windowActivated(WindowEvent e) {
        if (TextFinderDialog.this.tb.getSelectedIndex() == 0) {
          if (!TextFinderDialog.this.txtFind1.hasFocus()) {
            TextFinderDialog.this.txtFind1.requestFocusInWindow();
          } else if (!TextFinderDialog.this.txtFind2.hasFocus()) {
            TextFinderDialog.this.txtFind2.requestFocusInWindow();
          }
        }

      }

      public void windowDeactivated(WindowEvent e) {
        TextFinderDialog.this.searchData = null;
      }
    };
    this.addWindowListener(flst);
    this.pack();
    this.setResizable(false);
  }

  private JPanel createFindPanel() {
    this.txtFind1 = new JTextField();
    this.docFind = this.txtFind1.getDocument();
    this.popupManager.registerJTextComponent(this.txtFind1);

    JPanel panel = createFormPanel("fill:pref, 4dlu, fill:pref, " + FILLER_ROW + ", 8dlu, pref");
    panel.add(new JLabel(i18n.str("find_what")), CC.xy(1, 1));
    panel.add(this.txtFind1, CC.xy(3, 1));
    panel.add(createButton("find_next", 'f', e -> findNext(false, false)), CC.xy(5, 1));
    panel.add(createCloseButton(), CC.xy(5, 3));
    panel.add(createOptionsPanel(), CC.xyw(1, 6, 5));
    return panel;
  }

  private JPanel createReplacePanel() {
    this.txtFind2 = new JTextField();
    this.txtFind2.setDocument(this.docFind);
    this.popupManager.registerJTextComponent(this.txtFind2);
    JTextField txtReplace = new JTextField();
    this.docReplace = txtReplace.getDocument();
    this.popupManager.registerJTextComponent(txtReplace);

    JPanel panel = createFormPanel("fill:pref, 4dlu, fill:pref, 4dlu, fill:pref, " + FILLER_ROW + ", 8dlu, pref");
    panel.add(new JLabel(i18n.str("find_what")), CC.xy(1, 1));
    panel.add(this.txtFind2, CC.xy(3, 1));
    panel.add(createButton("replace", 'r', e -> findNext(true, false)), CC.xy(5, 1));
    panel.add(new JLabel(i18n.str("replace")), CC.xy(1, 3));
    panel.add(txtReplace, CC.xy(3, 3));
    panel.add(createButton("replace_all", 'a', e -> replaceAll()), CC.xy(5, 3));
    panel.add(createCloseButton(), CC.xy(5, 5));
    panel.add(createOptionsPanel(), CC.xyw(1, 8, 5));
    return panel;
  }

  private void replaceAll() {
    int result = findNext(true, true);
    if (result >= 0) {
      JOptionPane.showMessageDialog(owner, result + " " + i18n.str("replacements_prompt"), "Info", 1);
    }
  }

  private static JPanel createFormPanel(String rowSpecs) {
    JPanel panel = new JPanel(new FormLayout(FORM_COLUMN_SPECS, rowSpecs));
    panel.setBorder(new EmptyBorder(8, 5, 8, 5));
    return panel;
  }

  private JButton createButton(String i18nKey, char mnemonic, ActionListener action) {
    JButton button = new JButton(i18n.str(i18nKey));
    button.addActionListener(action);
    button.setMnemonic(mnemonic);
    return button;
  }

  private JButton createCloseButton() {
    JButton button = new JButton(i18n.str("close"));
    button.addActionListener(e -> setVisible(false));
    button.setDefaultCapable(true);
    return button;
  }

  /** Below the buttons, so it spans the whole width of the dialog. The options are the same on
   * both tabs, the first panel created creates their models. */
  private JPanel createOptionsPanel() {
    JCheckBox chkWord = new JCheckBox(i18n.str("whole_words_only"));
    chkWord.setMnemonic('w');
    JRadioButton rdUp = new JRadioButton(i18n.str("search_up"));
    rdUp.setMnemonic('u');
    JCheckBox chkCase = new JCheckBox(i18n.str("match_case"));
    chkCase.setMnemonic('c');
    JRadioButton rdDown = new JRadioButton(i18n.str("search_down"), true);
    rdDown.setMnemonic('d');
    if (this.modelWord == null) {
      this.modelWord = chkWord.getModel();
      this.modelUp = rdUp.getModel();
      this.modelCase = chkCase.getModel();
      this.modelDown = rdDown.getModel();
    } else {
      chkWord.setModel(this.modelWord);
      rdUp.setModel(this.modelUp);
      chkCase.setModel(this.modelCase);
      rdDown.setModel(this.modelDown);
    }
    ButtonGroup bg = new ButtonGroup();
    bg.add(rdUp);
    bg.add(rdDown);

    JPanel optionsGrid = new JPanel(new GridLayout(2, 2, 8, 2));
    optionsGrid.add(chkWord);
    optionsGrid.add(rdUp);
    optionsGrid.add(chkCase);
    optionsGrid.add(rdDown);

    JPanel searchIn = new JPanel(new BorderLayout(8, 0));
    searchIn.add(new JLabel(SEARCH_IN_LABEL), BorderLayout.WEST);
    searchIn.add(new JComboBox<>(modelScope), BorderLayout.CENTER);

    JPanel panel = new JPanel(new BorderLayout(0, 4));
    panel.setBorder(new TitledBorder(new EtchedBorder(), i18n.str("options")));
    panel.add(searchIn, BorderLayout.NORTH);
    panel.add(optionsGrid, BorderLayout.CENTER);
    return panel;
  }

  public void setJTextComponent(int monitorIndex) {
    JTextComponent nextMonitor = allTextComponents.get(monitorIndex);
    if (nextMonitor != this.monitor) {
      this.monitorIndex = monitorIndex;
      this.monitor = nextMonitor;
    }
  }

  public static String toString(JTextComponent c) {
    try {
      return (c == null) ? "null" : "'" + c.getText(0, c.getDocument().getLength()) + "'";
    }
    catch(BadLocationException blx) {
      throw new RuntimeException(blx);
    }
  }

  public JTextComponent getJTextComponent() {
    return this.monitor;
  }

  public void setSelectedIndex(int index) {
    this.tb.setSelectedIndex(index);
  }

  public int getSelectedIndex() {
    return this.tb.getSelectedIndex();
  }

  public void show(int index) {
    this.setSelectedIndex(index);
    this.setLocationRelativeTo(this.owner);
    this.setVisible(true);
    if (!this.monitor.hasFocus()) {
      this.monitor.requestFocusInWindow();
    }

  }

  public int findNext(boolean doReplace, boolean replaceAll) {
    if (modelScope.getSelectedItem() == SearchScope.TAGS) {
      return findNextInTags(doReplace, replaceAll);
    }
    int startMonitorIndex = this.monitorIndex;
    int startCaretPosition = this.monitor.getCaretPosition();
    int sumResult = 0;
    do {
      int findResult = findNextInCurrentTextComponent(doReplace);
      if (findResult == -1) {
        return findResult;
      }
      if (findResult == 1 && !replaceAll) {
        unfoldToShowHit();
        return findResult;
      }
      sumResult += findResult;
    } while(moveToNextComponent(startMonitorIndex, startCaretPosition));
    if (!replaceAll) {
      this.warning(i18n.str("text_not_found"));
    }
    return sumResult;
  }

  /** The search also visits text areas inside folded steps. Only the one finally showing a hit
   * unfolds what hides it, otherwise a search would unfold everything it passes. Layout and focus
   * of the text area (which could not take the focus while it was hidden) must wait for the
   * layout pass the unfolding has triggered. */
  private void unfoldToShowHit() {
    if (monitor instanceof InteractiveStepFragment fragment && editor().unfoldToShow(fragment)) {
      SwingUtilities.invokeLater(() -> {
        monitor.requestFocusInWindow();
        monitor.scrollRectToVisible(monitor.getBounds());
      });
    }
  }

  /** @return the number of replacements for "replace all", otherwise 1 if there was a hit, 0 if not,
   * -1 if the search can't be run */
  private int findNextInTags(boolean doReplace, boolean replaceAll) {
    String key = searchKey();
    if (key == null) {
      return -1;
    }
    TextMatcher matcher = createMatcher();
    String replacement = replacementText();
    if (replaceAll) {
      return tagSearch.replaceAll(key, replacement, matcher);
    }
    boolean hit = doReplace
      ? tagSearch.replace(key, replacement, matcher)
      : tagSearch.findNext(key, matcher);
    if (!hit) {
      warning(i18n.str("text_not_found"));
    }
    return hit ? 1 : 0;
  }

  private TextMatcher createMatcher() {
    return new TextMatcher(modelCase.isSelected(), modelWord.isSelected(), modelUp.isSelected());
  }

  private String replacementText() {
    try {
      return this.docReplace.getText(0, this.docReplace.getLength());
    } catch (BadLocationException e) {
      return "";
    }
  }

  private boolean moveToNextComponent(int startMonitorIndex, int startCaretPosition) {
    int nextMonitorIndex = (monitorIndex == allTextComponents.size() - 1) ? 0 : monitorIndex+1;
    this.setSelection(0, 0, false);
    boolean canMove = nextMonitorIndex != startMonitorIndex;
    if (canMove) {
      JTextComponent nextComponent = allTextComponents.get(nextMonitorIndex);
      nextComponent.requestFocus();
      if (nextComponent.getCaretPosition() != 0) {
        nextComponent.setCaretPosition(0);
      }
    }
    setJTextComponent(nextMonitorIndex);
    return canMove;
  }

  public int findNextInCurrentTextComponent(boolean doReplace) {
    int pos = this.monitor.getCaretPosition();
    String key = searchKey();
    if (key == null) {
      return -1;
    }

    String replacement = doReplace ? replacementText() : "";

    if (this.modelUp.isSelected() != this.searchUp) {
      this.searchUp = this.modelUp.isSelected();
    }

    String searchData = "";

    try {
      searchData = this.monitor.getDocument().getText(0, this.monitor.getDocument().getLength());
    } catch (Exception var12) {
      var12.printStackTrace();
      return -1;
    }

    int index = createMatcher().indexOfHit(searchData, key, pos);
    if (index < 0) {
      return 0;
    }

    if (doReplace) {
      this.setSelection(index, index + key.length(), this.searchUp);
      // Without undo composition, the following replacement is undone in two
      // steps: removing the selected text and adding the replacement text.
      try (UndoRecording ur = editor().composeUndo()) {
        this.monitor.replaceSelection(replacement);
      }
      this.setSelection(index, index + replacement.length(), this.searchUp);
    } else {
      this.setSelection(index, index + key.length(), this.searchUp);
    }

    monitor.scrollRectToVisible(monitor.getBounds());
    return 1;
  }

  /** @return the text to search for, null if there is none or it doesn't fit the options. The
   * user has been told in these cases. */
  private String searchKey() {
    String key = "";

    try {
      key = this.docFind.getText(0, this.docFind.getLength());
    } catch (BadLocationException var14) {
    }

    if (key.length() == 0) {
      this.warning(i18n.str("no_target_prompt"));
      return null;
    }
    Character illegalCharacter = createMatcher().illegalCharacterIn(key);
    if (illegalCharacter != null) {
      this.warning(i18n.str("illegal_character_prompt") + " '" + illegalCharacter + "'");
      return null;
    }
    return key;
  }

  public void setSelection(int xStart, int xFinish, boolean moveUp) {
    if (moveUp) {
      this.monitor.setCaretPosition(xFinish);
      this.monitor.moveCaretPosition(xStart);
    } else {
      this.monitor.setCaretPosition(xStart);
      this.monitor.moveCaretPosition(xFinish);
    }

  }

  protected void warning(String message) {
    JOptionPane.showMessageDialog(this.owner, message, TITLE, 1);
  }

  static {
    TITLE = i18n.str("find_and_replace");
  }

  /** This method is called if the user changes the caret position or the focused edit field while
   * this search/replace dialog is open. It required a re-initialization of the search cycle.
   * Unfortunately the method is also triggered by this dialog itself as it traverses the model's
   * text fields which requires setting focus and caret positions.
   * <p>
   * Temporarily removing the corresponsing event handlers is no solution as there are actually
   * other components which need to be informed. E.g. performing a replace operation requires
   * the save button to be enabled if it not yet is.
   * <p>
   * Switching the dialog in a special mode when changing a carret position and switching it back here
   * turned out to be too fragile.
   */
  public void updateContextState(JEditorPane editor) {
//    System.out.println("updateContextState " + editor.getCaretPosition());
//    editor = (JEditorPane)editor().getLastFocusedTextArea();
//    initSearchCycle(editor);
  }

}
