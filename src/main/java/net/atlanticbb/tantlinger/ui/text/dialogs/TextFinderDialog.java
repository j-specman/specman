package net.atlanticbb.tantlinger.ui.text.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Vector;
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
import specman.undo.manager.UndoRecording;
import static specman.Specman.editor;

/** This is an adaption of the original TextFinerDialog, extended to work
 * on a list of text components rather than a single one. Unfortunately the
 * original dialog class was not flexible enough to implement the extended
 * functionality as a derivation. */
public class TextFinderDialog extends JDialog {

  private static final long serialVersionUID = 1L;
  private static final I18n i18n = I18n.getInstance("net.atlanticbb.tantlinger.ui.text.dialogs");
  public static final char[] WORD_SEPARATORS = new char[]{' ', '\t', '\n', '\r', '\f', '.', ',', ':', '-', '(', ')', '[', ']', '{', '}', '<', '>', '/', '|', '\\', '\'', '"'};
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
    JPanel p1 = new JPanel(new BorderLayout());
    JPanel pc1 = new JPanel(new BorderLayout());
    JPanel pf = new JPanel();
    pf.setLayout(new DialogLayout(20, 5));
    pf.setBorder(new EmptyBorder(8, 5, 8, 0));
    pf.add(new JLabel(i18n.str("find_what")));
    this.txtFind1 = new JTextField();
    this.docFind = this.txtFind1.getDocument();
    pf.add(this.txtFind1);
    pc1.add(pf, "Center");
    this.popupManager.registerJTextComponent(this.txtFind1);
    JPanel po = new JPanel(new GridLayout(2, 2, 8, 2));
    po.setBorder(new TitledBorder(new EtchedBorder(), i18n.str("options")));
    JCheckBox chkWord = new JCheckBox(i18n.str("whole_words_only"));
    chkWord.setMnemonic('w');
    this.modelWord = chkWord.getModel();
    po.add(chkWord);
    ButtonGroup bg = new ButtonGroup();
    JRadioButton rdUp = new JRadioButton(i18n.str("search_up"));
    rdUp.setMnemonic('u');
    this.modelUp = rdUp.getModel();
    bg.add(rdUp);
    po.add(rdUp);
    JCheckBox chkCase = new JCheckBox(i18n.str("match_case"));
    chkCase.setMnemonic('c');
    this.modelCase = chkCase.getModel();
    po.add(chkCase);
    JRadioButton rdDown = new JRadioButton(i18n.str("search_down"), true);
    rdDown.setMnemonic('d');
    this.modelDown = rdDown.getModel();
    bg.add(rdDown);
    po.add(rdDown);
    pc1.add(po, "South");
    p1.add(pc1, "Center");
    JPanel p01 = new JPanel(new FlowLayout());
    JPanel p = new JPanel(new GridLayout(2, 1, 2, 8));
    ActionListener findAction = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TextFinderDialog.this.findNext(false, false);
      }
    };
    JButton btFind = new JButton(i18n.str("find_next"));
    btFind.addActionListener(findAction);
    btFind.setMnemonic('f');
    p.add(btFind);
    ActionListener closeAction = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TextFinderDialog.this.setVisible(false);
      }
    };
    JButton btClose = new JButton(i18n.str("close"));
    btClose.addActionListener(closeAction);
    btClose.setDefaultCapable(true);
    p.add(btClose);
    p01.add(p);
    p1.add(p01, "East");
    this.tb.addTab(i18n.str("find"), p1);
    JPanel p2 = new JPanel(new BorderLayout());
    JPanel pc2 = new JPanel(new BorderLayout());
    JPanel pc = new JPanel();
    pc.setLayout(new DialogLayout(20, 5));
    pc.setBorder(new EmptyBorder(8, 5, 8, 0));
    pc.add(new JLabel(i18n.str("find_what")));
    this.txtFind2 = new JTextField();
    this.txtFind2.setDocument(this.docFind);
    pc.add(this.txtFind2);
    this.popupManager.registerJTextComponent(this.txtFind2);
    pc.add(new JLabel(i18n.str("replace")));
    JTextField txtReplace = new JTextField();
    this.docReplace = txtReplace.getDocument();
    pc.add(txtReplace);
    pc2.add(pc, "Center");
    this.popupManager.registerJTextComponent(txtReplace);
    po = new JPanel(new GridLayout(2, 2, 8, 2));
    po.setBorder(new TitledBorder(new EtchedBorder(), i18n.str("options")));
    chkWord = new JCheckBox(i18n.str("whole_words_only"));
    chkWord.setMnemonic('w');
    chkWord.setModel(this.modelWord);
    po.add(chkWord);
    bg = new ButtonGroup();
    rdUp = new JRadioButton(i18n.str("search_up"));
    rdUp.setMnemonic('u');
    rdUp.setModel(this.modelUp);
    bg.add(rdUp);
    po.add(rdUp);
    chkCase = new JCheckBox(i18n.str("match_case"));
    chkCase.setMnemonic('c');
    chkCase.setModel(this.modelCase);
    po.add(chkCase);
    rdDown = new JRadioButton(i18n.str("search_down"), true);
    rdDown.setMnemonic('d');
    rdDown.setModel(this.modelDown);
    bg.add(rdDown);
    po.add(rdDown);
    pc2.add(po, "South");
    p2.add(pc2, "Center");
    JPanel p02 = new JPanel(new FlowLayout());
    p = new JPanel(new GridLayout(3, 1, 2, 8));
    ActionListener replaceAction = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TextFinderDialog.this.findNext(true, false);
      }
    };
    JButton btReplace = new JButton(i18n.str("replace"));
    btReplace.addActionListener(replaceAction);
    btReplace.setMnemonic('r');
    p.add(btReplace);
    ActionListener replaceAllAction = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int result = TextFinderDialog.this.findNext(true, true);
        if (result < 0) {
          return;
        }
        JOptionPane.showMessageDialog(TextFinderDialog.this.owner, result + " " + TextFinderDialog.i18n.str("replacements_prompt"), "Info", 1);
      }
    };
    JButton btReplaceAll = new JButton(i18n.str("replace_all"));
    btReplaceAll.addActionListener(replaceAllAction);
    btReplaceAll.setMnemonic('a');
    p.add(btReplaceAll);
    btClose = new JButton(i18n.str("close"));
    btClose.addActionListener(closeAction);
    btClose.setDefaultCapable(true);
    p.add(btClose);
    p02.add(p);
    p2.add(p02, "East");
    p01.setPreferredSize(p02.getPreferredSize());
    this.tb.addTab(i18n.str("replace"), p2);
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
    int startMonitorIndex = this.monitorIndex;
    int startCaretPosition = this.monitor.getCaretPosition();
    int sumResult = 0;
    do {
      int findResult = findNextInCurrentTextComponent(doReplace);
      if (findResult == -1) {
        return findResult;
      }
      if (findResult == 1 && !replaceAll) {
        return findResult;
      }
      sumResult += findResult;
    } while(moveToNextComponent(startMonitorIndex, startCaretPosition));
    if (!replaceAll) {
      this.warning(i18n.str("text_not_found"));
    }
    return sumResult;
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
    String key = "";

    try {
      key = this.docFind.getText(0, this.docFind.getLength());
    } catch (BadLocationException var14) {
    }

    if (key.length() == 0) {
      this.warning(i18n.str("no_target_prompt"));
      return -1;
    } else {
      if (this.modelWord.isSelected()) {
        for(int k = 0; k < WORD_SEPARATORS.length; ++k) {
          if (key.indexOf(WORD_SEPARATORS[k]) >= 0) {
            this.warning(i18n.str("illegal_character_prompt") + " '" + WORD_SEPARATORS[k] + "'");
            return -1;
          }
        }
      }

      String replacement = "";
      if (doReplace) {
        try {
          replacement = this.docReplace.getText(0, this.docReplace.getLength());
        } catch (BadLocationException var13) {
        }
      }

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

      if (!this.modelCase.isSelected()) {
        searchData = searchData.toLowerCase();
        key = key.toLowerCase();
      }

      while(true) {
        int index;
        if (!this.searchUp) {
          index = searchData.indexOf(key, pos);
        } else {
          index = searchData.lastIndexOf(key, pos - 1);
        }

        if (index >= 0 && index < searchData.length()) {
          if (this.modelWord.isSelected()) {
            boolean s1 = index > 0;
            boolean b1 = s1 && !this.isSeparator(searchData.charAt(index - 1));
            boolean s2 = index + key.length() < searchData.length();
            boolean b2 = s2 && !this.isSeparator(searchData.charAt(index + key.length()));
            if (b1 || b2) {
              if (!this.searchUp && s2) {
                pos = index + key.length();
                continue;
              }

              if (this.searchUp && s1) {
                pos = index;
                continue;
              }

              return 0;
            }
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

        return 0;
      }
    }
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

  protected boolean isSeparator(char ch) {
    for(int k = 0; k < WORD_SEPARATORS.length; ++k) {
      if (ch == WORD_SEPARATORS[k]) {
        return true;
      }
    }

    return false;
  }

  protected void warning(String message) {
    JOptionPane.showMessageDialog(this.owner, message, TITLE, 1);
  }

  static {
    TITLE = i18n.str("find_and_replace");
  }

  private class DialogLayout implements LayoutManager {
    protected static final int COMP_TWO_COL = 0;
    protected static final int COMP_BIG = 1;
    protected static final int COMP_BUTTON = 2;
    protected int m_divider = -1;
    protected int m_hGap = 10;
    protected int m_vGap = 5;
    protected Vector m_v = new Vector();

    public DialogLayout() {
    }

    public DialogLayout(int hGap, int vGap) {
      this.m_hGap = hGap;
      this.m_vGap = vGap;
    }

    public void addLayoutComponent(String name, Component comp) {
    }

    public void removeLayoutComponent(Component comp) {
    }

    public Dimension preferredLayoutSize(Container parent) {
      this.m_v.removeAllElements();
      int w = 0;
      int h = 0;
      int type = -1;

      for(int k = 0; k < parent.getComponentCount(); ++k) {
        Component comp = parent.getComponent(k);
        int newType = this.getLayoutType(comp);
        if (k == 0) {
          type = newType;
        }

        if (type != newType) {
          Dimension d = this.preferredLayoutSize(this.m_v, type);
          w = Math.max(w, d.width);
          h += d.height + this.m_vGap;
          this.m_v.removeAllElements();
          type = newType;
        }

        this.m_v.addElement(comp);
      }

      Dimension dx = this.preferredLayoutSize(this.m_v, type);
      w = Math.max(w, dx.width);
      h += dx.height + this.m_vGap;
      h -= this.m_vGap;
      Insets insets = parent.getInsets();
      return new Dimension(w + insets.left + insets.right, h + insets.top + insets.bottom);
    }

    protected Dimension preferredLayoutSize(Vector v, int type) {
      int w = 0;
      int h = 0;
      Component comp;
      Dimension dx;
      int k;
      switch (type) {
        case 0:
          int divider = this.getDivider(v);

          for(k = 1; k < v.size(); k += 2) {
            comp = (Component)v.elementAt(k);
            dx = comp.getPreferredSize();
            w = Math.max(w, dx.width);
            h += dx.height + this.m_vGap;
          }

          h -= this.m_vGap;
          return new Dimension(divider + w, h);
        case 1:
          for(k = 0; k < v.size(); ++k) {
            comp = (Component)v.elementAt(k);
            dx = comp.getPreferredSize();
            w = Math.max(w, dx.width);
            h += dx.height + this.m_vGap;
          }

          h -= this.m_vGap;
          return new Dimension(w, h);
        case 2:
          Dimension d = this.getMaxDimension(v);
          w = d.width + this.m_hGap;
          h = d.height;
          return new Dimension(w * v.size() - this.m_hGap, h);
        default:
          throw new IllegalArgumentException("Illegal type " + type);
      }
    }

    public Dimension minimumLayoutSize(Container parent) {
      return this.preferredLayoutSize(parent);
    }

    public void layoutContainer(Container parent) {
      this.m_v.removeAllElements();
      int type = -1;
      Insets insets = parent.getInsets();
      int w = parent.getWidth() - insets.left - insets.right;
      int x = insets.left;
      int y = insets.top;

      for(int k = 0; k < parent.getComponentCount(); ++k) {
        Component comp = parent.getComponent(k);
        int newType = this.getLayoutType(comp);
        if (k == 0) {
          type = newType;
        }

        if (type != newType) {
          y = this.layoutComponents(this.m_v, type, x, y, w);
          this.m_v.removeAllElements();
          type = newType;
        }

        this.m_v.addElement(comp);
      }

      this.layoutComponents(this.m_v, type, x, y, w);
      this.m_v.removeAllElements();
    }

    protected int layoutComponents(Vector v, int type, int x, int y, int w) {
      int k;
      Component comp1;
      switch (type) {
        case 0:
          int divider = this.getDivider(v);

          for(k = 1; k < v.size(); k += 2) {
            comp1 = (Component)v.elementAt(k - 1);
            Component comp2 = (Component)v.elementAt(k);
            Dimension dx = comp2.getPreferredSize();
            comp1.setBounds(x, y, divider, dx.height);
            comp2.setBounds(x + divider, y, w - divider, dx.height);
            y += dx.height + this.m_vGap;
          }

          return y;
        case 1:
          for(k = 0; k < v.size(); ++k) {
            comp1 = (Component)v.elementAt(k);
            Dimension d = comp1.getPreferredSize();
            comp1.setBounds(x, y, w, d.height);
            y += d.height + this.m_vGap;
          }

          return y;
        case 2:
          Dimension dxx = this.getMaxDimension(v);
          int ww = dxx.width * v.size() + this.m_hGap * (v.size() - 1);
          int xx = x + Math.max(0, (w - ww) / 2);

          for(int kx = 0; kx < v.size(); ++kx) {
            Component comp = (Component)v.elementAt(kx);
            comp.setBounds(xx, y, dxx.width, dxx.height);
            xx += dxx.width + this.m_hGap;
          }

          return y + dxx.height;
        default:
          throw new IllegalArgumentException("Illegal type " + type);
      }
    }

    public int getHGap() {
      return this.m_hGap;
    }

    public int getVGap() {
      return this.m_vGap;
    }

    public void setDivider(int divider) {
      if (divider > 0) {
        this.m_divider = divider;
      }

    }

    public int getDivider() {
      return this.m_divider;
    }

    protected int getDivider(Vector v) {
      if (this.m_divider > 0) {
        return this.m_divider;
      } else {
        int divider = 0;

        for(int k = 0; k < v.size(); k += 2) {
          Component comp = (Component)v.elementAt(k);
          Dimension d = comp.getPreferredSize();
          divider = Math.max(divider, d.width);
        }

        divider += this.m_hGap;
        return divider;
      }
    }

    protected Dimension getMaxDimension(Vector v) {
      int w = 0;
      int h = 0;

      for(int k = 0; k < v.size(); ++k) {
        Component comp = (Component)v.elementAt(k);
        Dimension d = comp.getPreferredSize();
        w = Math.max(w, d.width);
        h = Math.max(h, d.height);
      }

      return new Dimension(w, h);
    }

    protected int getLayoutType(Component comp) {
      if (comp instanceof AbstractButton) {
        return 2;
      } else {
        return !(comp instanceof JPanel) && !(comp instanceof JScrollPane) && !(comp instanceof JTabbedPane) ? 0 : 1;
      }
    }
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
