package classman.editor;

import java.util.Locale;
import java.util.ResourceBundle;

/** The texts of the editor, in the language the host asks for. */
public class EditorTexts {
  private static final String BUNDLE = "classman.editor.messages";

  private final ResourceBundle bundle;

  public EditorTexts(Locale locale) {
    this.bundle = ResourceBundle.getBundle(BUNDLE, locale, ResourceBundle.Control.getNoFallbackControl(
      ResourceBundle.Control.FORMAT_PROPERTIES));
  }

  public String get(String key) {
    return bundle.getString(key);
  }
}
