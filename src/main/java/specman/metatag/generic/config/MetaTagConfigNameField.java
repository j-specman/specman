package specman.metatag.generic.config;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Toolkit;
import java.util.regex.Pattern;

/** Text field for the name of a meta tag configuration. Lets only identifier characters (ASCII
 * letters, digits, underscore) in, whether typed or pasted. */
class MetaTagConfigNameField extends JTextField {

  MetaTagConfigNameField(int columns) {
    super(columns);
    ((AbstractDocument) getDocument()).setDocumentFilter(new IdentifierFilter());
  }

  private static class IdentifierFilter extends DocumentFilter {
    private static final Pattern IDENTIFIER_CHARACTERS = Pattern.compile("[A-Za-z0-9_]*");

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
      if (isAcceptable(string)) {
        super.insertString(fb, offset, string, attr);
      }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
      if (isAcceptable(text)) {
        super.replace(fb, offset, length, text, attrs);
      }
    }

    private boolean isAcceptable(String text) {
      if (text == null || IDENTIFIER_CHARACTERS.matcher(text).matches()) {
        return true;
      }
      Toolkit.getDefaultToolkit().beep();
      return false;
    }
  }
}
