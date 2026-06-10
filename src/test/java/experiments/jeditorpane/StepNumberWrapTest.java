package experiments.jeditorpane;

import javax.swing.*;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

public class StepNumberWrapTest extends JFrame {

  static class StepNumberAwareParagraphView extends javax.swing.text.html.ParagraphView {
    private final int reservedWidth;

    StepNumberAwareParagraphView(Element elem, int reservedWidth) {
      super(elem);
      this.reservedWidth = reservedWidth;
    }

    @Override
    public int getFlowSpan(int index) {
      int span = super.getFlowSpan(index);
      return index == 0 ? Math.max(0, span - reservedWidth) : span;
    }
  }

  static class StepNumberAwareHTMLEditorKit extends HTMLEditorKit {
    private final int reservedWidth;

    StepNumberAwareHTMLEditorKit(int reservedWidth) {
      this.reservedWidth = reservedWidth;
    }

    @Override
    public ViewFactory getViewFactory() {
      return new HTMLFactory() {
        @Override
        public View create(Element elem) {
          View v = super.create(elem);
          if (v instanceof javax.swing.text.ParagraphView) {
            return new StepNumberAwareParagraphView(elem, reservedWidth);
          }
          return v;
        }
      };
    }
  }

  static class StepBox extends JPanel {
    private final JEditorPane editor;
    private final JLabel stepLabel;

    StepBox(String title, String stepNumber, String html, boolean withFix) {
      setLayout(null);
      setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder(title),
        BorderFactory.createEmptyBorder(2, 4, 2, 4)
      ));

      Font labelFont = new Font("SansSerif", Font.PLAIN, 10);
      stepLabel = new JLabel(stepNumber);
      stepLabel.setFont(labelFont);
      stepLabel.setForeground(new Color(80, 80, 180));

      FontMetrics fm = getFontMetrics(labelFont);
      int reserved = fm.stringWidth(stepNumber) + 4;

      HTMLEditorKit kit = withFix
        ? new StepNumberAwareHTMLEditorKit(reserved)
        : new HTMLEditorKit();

      editor = new JEditorPane();
      editor.setEditorKit(kit);
      editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
      editor.setFont(new Font("SansSerif", Font.PLAIN, 13));
      editor.setText(html);
      editor.setOpaque(false);

      add(editor);
      add(stepLabel);
    }

    @Override
    public void doLayout() {
      Insets insets = getInsets();
      int x = insets.left;
      int y = insets.top;
      int w = getWidth() - insets.left - insets.right;
      int h = getHeight() - insets.top - insets.bottom;

      editor.setBounds(x, y, w, h);

      Dimension labelPref = stepLabel.getPreferredSize();
      stepLabel.setBounds(x + w - labelPref.width, y, labelPref.width, labelPref.height);
    }
  }

  StepNumberWrapTest() {
    setTitle("Step Number Wrap Test");
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    String stepNumber = "13.3.4.1";
    String html = "<html><body>Das System überprüft ob der Benutzer die Berechtigung hat diese Bilanz zu löschen und führt die Löschung durch.</body></html>";

    JPanel content = new JPanel(new GridLayout(1, 2, 12, 0));
    content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    content.add(new StepBox("Ohne Fix", stepNumber, html, false));
    content.add(new StepBox("Mit Fix", stepNumber, html, true));

    setContentPane(content);
    setSize(700, 160);
    setVisible(true);
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(StepNumberWrapTest::new);
  }
}
