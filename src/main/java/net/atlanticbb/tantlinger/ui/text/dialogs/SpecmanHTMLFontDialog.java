package net.atlanticbb.tantlinger.ui.text.dialogs;

import net.atlanticbb.tantlinger.i18n.I18n;
import net.atlanticbb.tantlinger.ui.UIUtils;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLFontSizeAction;
import net.atlanticbb.tantlinger.ui.text.actions.SpecmanFontSizeAction;
import specman.Specman;
import static specman.Specman.editor;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Vector;

public class SpecmanHTMLFontDialog extends HTMLOptionDialog {

  private static final long serialVersionUID = 1L;
  private static final I18n i18n = I18n.getInstance("net.atlanticbb.tantlinger.ui.text.dialogs");
  private static Icon icon = UIUtils.getIcon("resources/images/x32/", "fontsize.png");
  private static String title;
  private static String desc;
  private JPanel jContentPane = null;
  private JLabel fontLabel = null;
  private JComboBox<FontFamily> fontCombo = null;
  private JComboBox<String> sizeCombo = null;
  private JPanel stylePanel = null;
  private JCheckBox boldCB = null;
  private JCheckBox italicCB = null;
  private JCheckBox ulCB = null;
  private JPanel previewPanel = null;
  private JLabel previewLabel = null;
  private JPanel spacerPanel = null;
  private String text = "";

  public SpecmanHTMLFontDialog(Frame parent, String text) {
    super(parent, title, desc, icon);
    this.initialize(text);
  }

  public SpecmanHTMLFontDialog(Dialog parent, String text) {
    super(parent, title, desc, icon);
    this.initialize(text);
  }

  public boolean isBold() {
    return this.boldCB.isSelected();
  }

  public boolean isItalic() {
    return this.italicCB.isSelected();
  }

  public boolean isUnderline() {
    return this.ulCB.isSelected();
  }

  public void setBold(boolean b) {
    this.boldCB.setSelected(b);
    this.updatePreview();
  }

  public void setItalic(boolean b) {
    this.italicCB.setSelected(b);
    this.updatePreview();
  }

  public void setUnderline(boolean b) {
    this.ulCB.setSelected(b);
    this.updatePreview();
  }

  public void setFontName(String fn) {
    this.fontCombo.setSelectedItem(fn);
    this.updatePreview();
  }

  public String getFontName() {
    return ((FontFamily)this.fontCombo.getSelectedItem()).toFamiliyName();
  }

  public int getFontSize() {
    return HTMLFontSizeAction.FONT_SIZES[this.sizeCombo.getSelectedIndex()];
  }

  public void setFontSize(int size) {
    this.sizeCombo.setSelectedItem(swingFontsize2HTMLFontsize(size));
    this.updatePreview();
  }

  public String getHTML() {
    String html = "<font ";
    html = html + "name=\"" + this.fontCombo.getSelectedItem() + "\" ";
    html = html + "size=\"" + (this.sizeCombo.getSelectedIndex() + 1) + "\">";
    if (this.boldCB.isSelected()) {
      html = html + "<b>";
    }

    if (this.italicCB.isSelected()) {
      html = html + "<i>";
    }

    if (this.ulCB.isSelected()) {
      html = html + "<u>";
    }

    html = html + this.text;
    if (this.boldCB.isSelected()) {
      html = html + "</b>";
    }

    if (this.italicCB.isSelected()) {
      html = html + "</i>";
    }

    if (this.ulCB.isSelected()) {
      html = html + "</u>";
    }

    html = html + "</font>";
    return html;
  }

  private void initialize(String text) {
    this.setContentPane(this.getJContentPane());
    this.pack();
    this.setSize(285, this.getHeight());
    this.setResizable(false);
    this.text = text;
  }

  private void updatePreview() {
    int style = 0;
    if (this.boldCB.isSelected()) {
      ++style;
    }

    if (this.italicCB.isSelected()) {
      style += 2;
    }

    if (this.ulCB.isSelected()) {
      this.previewLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, this.previewLabel.getForeground()));
    } else {
      this.previewLabel.setBorder((Border)null);
    }

    String font = ((FontFamily)this.fontCombo.getSelectedItem()).toFamiliyName();
    Integer size = HTMLFontSizeAction.FONT_SIZES[this.sizeCombo.getSelectedIndex()];
    Font f = new Font(font, style, size);
    this.previewLabel.setFont(f);
  }

  private JPanel getJContentPane() {
    if (this.jContentPane == null) {
      GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
      gridBagConstraints21.gridx = 0;
      gridBagConstraints21.gridwidth = 3;
      gridBagConstraints21.anchor = 17;
      gridBagConstraints21.fill = 2;
      gridBagConstraints21.insets = new Insets(5, 0, 0, 0);
      gridBagConstraints21.gridy = 1;
      GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
      gridBagConstraints2.fill = 0;
      gridBagConstraints2.gridy = 0;
      gridBagConstraints2.weightx = (double)1.0F;
      gridBagConstraints2.anchor = 17;
      gridBagConstraints2.gridx = 2;
      GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
      gridBagConstraints1.fill = 2;
      gridBagConstraints1.gridy = 0;
      gridBagConstraints1.weightx = (double)1.0F;
      gridBagConstraints1.anchor = 17;
      gridBagConstraints1.insets = new Insets(0, 0, 0, 5);
      gridBagConstraints1.gridx = 1;
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.insets = new Insets(0, 0, 0, 5);
      gridBagConstraints.gridy = 0;
      this.fontLabel = new JLabel();
      this.fontLabel.setText(i18n.str("font"));
      this.jContentPane = new JPanel();
      this.jContentPane.setLayout(new GridBagLayout());
      this.jContentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      this.jContentPane.add(this.fontLabel, gridBagConstraints);
      this.jContentPane.add(this.getFontCombo(), gridBagConstraints1);
      this.jContentPane.add(this.getSizeCombo(), gridBagConstraints2);
      this.jContentPane.add(this.getStylePanel(), gridBagConstraints21);
    }

    return this.jContentPane;
  }

  private String swingFontsize2HTMLFontsize(int zoomedSwingFontsize) {
    int swingFontsize = zoomedSwingFontsize * 100 / editor().getZoomFactor();
    for (int i = 0; i < HTMLFontSizeAction.FONT_SIZES.length; ++i) {
      if (SpecmanFontSizeAction.FONT_SIZES[i] == swingFontsize) {
        return HTMLFontSizeAction.SIZES[i];
      }
    }
    return HTMLFontSizeAction.SIZES[SpecmanFontSizeAction.DEFAULT_FONTSIZE_INDEX];
  }

  private JComboBox getFontCombo() {
    if (this.fontCombo == null) {

// No system fonts yet
//      GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
//      String[] envfonts = gEnv.getAvailableFontFamilyNames();
//      for(int i = 0; i < envfonts.length; ++i) {
//        fonts.add(envfonts[i]);
//      }

      this.fontCombo = new JComboBox(FontFamily.ALL_FONTS);
      this.fontCombo.addItemListener(e -> SpecmanHTMLFontDialog.this.updatePreview());
    }

    return this.fontCombo;
  }

  private JComboBox getSizeCombo() {
    if (this.sizeCombo == null) {
      this.sizeCombo = new JComboBox(HTMLFontSizeAction.SIZES);
      this.sizeCombo.setSelectedIndex(SpecmanFontSizeAction.DEFAULT_FONTSIZE_INDEX);
      this.sizeCombo.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          SpecmanHTMLFontDialog.this.updatePreview();
        }
      });
    }

    return this.sizeCombo;
  }

  private JPanel getStylePanel() {
    if (this.stylePanel == null) {
      GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
      gridBagConstraints7.gridx = 0;
      gridBagConstraints7.anchor = 18;
      gridBagConstraints7.weighty = (double)1.0F;
      gridBagConstraints7.fill = 1;
      gridBagConstraints7.gridy = 3;
      GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
      gridBagConstraints6.gridx = 1;
      gridBagConstraints6.gridwidth = 1;
      gridBagConstraints6.gridheight = 4;
      gridBagConstraints6.fill = 1;
      gridBagConstraints6.weightx = (double)1.0F;
      gridBagConstraints6.weighty = (double)1.0F;
      gridBagConstraints6.anchor = 18;
      gridBagConstraints6.gridy = 0;
      GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
      gridBagConstraints5.gridx = 0;
      gridBagConstraints5.anchor = 17;
      gridBagConstraints5.insets = new Insets(0, 0, 0, 5);
      gridBagConstraints5.weighty = (double)0.0F;
      gridBagConstraints5.gridy = 2;
      GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
      gridBagConstraints4.gridx = 0;
      gridBagConstraints4.anchor = 17;
      gridBagConstraints4.insets = new Insets(0, 0, 0, 5);
      gridBagConstraints4.gridy = 1;
      GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
      gridBagConstraints3.gridx = 0;
      gridBagConstraints3.anchor = 17;
      gridBagConstraints3.insets = new Insets(5, 0, 0, 5);
      gridBagConstraints3.gridy = 0;
      this.stylePanel = new JPanel();
      this.stylePanel.setLayout(new GridBagLayout());
      this.stylePanel.add(this.getBoldCB(), gridBagConstraints3);
      this.stylePanel.add(this.getItalicCB(), gridBagConstraints4);
      this.stylePanel.add(this.getUlCB(), gridBagConstraints5);
      this.stylePanel.add(this.getPreviewPanel(), gridBagConstraints6);
      this.stylePanel.add(this.getSpacerPanel(), gridBagConstraints7);
    }

    return this.stylePanel;
  }

  private JCheckBox getBoldCB() {
    if (this.boldCB == null) {
      this.boldCB = new JCheckBox();
      this.boldCB.setText(i18n.str("bold"));
      this.boldCB.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          SpecmanHTMLFontDialog.this.updatePreview();
        }
      });
    }

    return this.boldCB;
  }

  private JCheckBox getItalicCB() {
    if (this.italicCB == null) {
      this.italicCB = new JCheckBox();
      this.italicCB.setText(i18n.str("italic"));
      this.italicCB.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          SpecmanHTMLFontDialog.this.updatePreview();
        }
      });
    }

    return this.italicCB;
  }

  private JCheckBox getUlCB() {
    if (this.ulCB == null) {
      this.ulCB = new JCheckBox();
      this.ulCB.setText(i18n.str("underline"));
      this.ulCB.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          SpecmanHTMLFontDialog.this.updatePreview();
        }
      });
    }

    return this.ulCB;
  }

  private JPanel getPreviewPanel() {
    if (this.previewPanel == null) {
      this.previewLabel = new JLabel();
      this.previewLabel.setText("AaBbYyZz");
      JPanel spacer = new JPanel(new FlowLayout(0));
      spacer.setBackground(Color.WHITE);
      spacer.add(this.previewLabel);
      this.previewPanel = new JPanel();
      this.previewPanel.setLayout(new BorderLayout());
      this.previewPanel.setBorder(BorderFactory.createCompoundBorder((Border)null, BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder((Border)null, i18n.str("preview"), 0, 0, (Font)null, (Color)null), BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), BorderFactory.createBevelBorder(1)))));
      this.previewPanel.setPreferredSize(new Dimension(90, 100));
      this.previewPanel.setMaximumSize(this.previewPanel.getPreferredSize());
      this.previewPanel.setMinimumSize(this.previewPanel.getPreferredSize());
      this.previewPanel.add(spacer, (Object)null);
    }

    return this.previewPanel;
  }

  private JPanel getSpacerPanel() {
    if (this.spacerPanel == null) {
      this.spacerPanel = new JPanel();
    }

    return this.spacerPanel;
  }

  static {
    title = i18n.str("font");
    desc = i18n.str("font_desc");
  }

  public static final class FontFamily {
    public static final FontFamily SITKA = new FontFamily("Sitka - Serif", "SitkaDisplay");
    public static final FontFamily ROBOTO = new FontFamily("Roboto - Sans Serif", "Roboto");
    public static final FontFamily COURIER_PRIME = new FontFamily("Courier - Monospace", "CourierPrime");

    public static final Vector ALL_FONTS = new Vector(java.util.List.of(
      SITKA, ROBOTO, COURIER_PRIME
    ));

    private final String displayName, familiyName;

    FontFamily(String displayName, String familiyName) {
      this.displayName = displayName;
      this.familiyName = familiyName;
    }

    @Override
    public String toString() { return displayName; }

    public String toFamiliyName() { return this.familiyName; }
  }
}
