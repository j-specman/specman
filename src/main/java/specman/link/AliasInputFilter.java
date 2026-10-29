package specman.link;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/** Keeps characters out of an alias field which an alias must not contain, and lets it grow to
 * {@link LinkAliases#MAX_ALIAS_LENGTH} characters at most. Pasted text is shortened accordingly. */
class AliasInputFilter extends DocumentFilter {

  @Override
  public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs) throws BadLocationException {
    replace(fb, offset, 0, text, attrs);
  }

  @Override
  public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
    if (text == null || text.isEmpty()) {
      super.replace(fb, offset, length, text, attrs);
      return;
    }
    String allowed = LinkAliases.keepAllowedCharacters(text);
    int room = Math.max(0, LinkAliases.MAX_ALIAS_LENGTH - (fb.getDocument().getLength() - length));
    if (allowed.length() > room) {
      allowed = allowed.substring(0, room);
    }
    // Nothing of what the user typed is usable: Leave everything as it is, a selection included
    if (!allowed.isEmpty()) {
      super.replace(fb, offset, length, allowed, attrs);
    }
  }
}
