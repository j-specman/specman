package experiments.editorkit;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.graphics.Styles;

import javax.swing.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class TestEditor extends JFrame {
  private JEditorPane field;
  private Container contentPane;
  private HTMLEditComponentsProvider editComponents;

  TestEditor() throws Exception {
    setSize(300, 500);
    contentPane = this.getContentPane();
    contentPane.setLayout(new FormLayout("80px:grow", "30px,fill:pref:grow,30px,30px"));

    editComponents = new HTMLEditComponentsProvider();
    contentPane.add(editComponents.getFormatToolbar(), CC.xy(1, 1));

    field = new JEditorPane();
    field.setContentType("text/html");
    field.setFont(Styles.DEFAULTFONT.deriveFont(Fontsize.SWING_FONTSIZE));
    field.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

    field.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
          newline2BR(e);
      }
    });
    contentPane.add(field, CC.xy(1, 2));
    editComponents.instrumentWysEditor(field, "<html>Überschrift<br>blabla<br>Nummerierte Liste:<ul><li>eins<li>zwei, zwwei<li>Unterliste<ul><li>drei eins<li>drei zwei</ul></ul>drei<br><br><br>vier");

    this.setDefaultCloseOperation(EXIT_ON_CLOSE);

    setVisible(true);
  }

  /** Pressing ENTER in an HTML-styled {@link JEditorPane} should produce a <br>, but it does not
   * by default. So we insert one and consume the key event to avoid whatever the default behaviour is.
   * General idea was found here:
   * https://stackoverflow.com/questions/61933403/jeditorpane-content-type-text-html-line-breaks-with-no-paragraph-creation
   */
  private void newline2BR(KeyEvent e) {
    try {
      if (e.getKeyCode() == KeyEvent.VK_ENTER) {
        HTMLEditorKit kit = (HTMLEditorKit) field.getEditorKit();
        kit.insertHTML((HTMLDocument) field.getDocument(), field.getCaretPosition(),
          "<br>", 0, 0, HTML.Tag.BR);
        field.setCaretPosition(field.getCaretPosition()); // This moves caret to next line
        e.consume();
      }
    }
    catch (Exception x) {
      throw new RuntimeException(x);
    }
  }

  public static void main(String[] args) throws Exception {
    new TestEditor();
  }

}
