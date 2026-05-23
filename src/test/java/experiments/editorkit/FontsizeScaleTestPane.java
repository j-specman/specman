package experiments.editorkit;

import specman.graphics.Styles;

import javax.swing.*;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

class FontsizeScaleTestPane extends JEditorPane {
  private StyledDocument document;
  private Element element;

  FontsizeScaleTestPane() {
    setContentType("text/html");
    setText("<html><body>" +
      "<font size=\"1\">1</font>" +
      "<font size=\"2\">2</font>" +
      "<font size=\"3\">3</font>" +
      "<font size=\"4\">4</font>" +
      "<font size=\"5\">5</font>" +
      "<font size=\"6\">6</font>" +
      "<font size=\"7\">7</font>" +
      "</body></html>");
    setFont(Styles.DEFAULTFONT.deriveFont(Fontsize.SWING_FONTSIZE));
    putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    document = (StyledDocument)getDocument();
  }

  int write2readFontsize(int num) {
    return StyleConstants.getFontSize(document.getCharacterElement(num).getAttributes());
  }

}
