package specman.i18n;

import specman.Specman;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Access to the localized UI texts in {@code i18n/messages*.properties}. Placeholders are written
 * as {0}, {1}, ... and substituted literally (no MessageFormat quoting rules, apostrophes are
 * plain characters). The language is fixed at startup, see {@link #applyLanguagePreference()}.
 */
public final class Messages {

  public static final String LANGUAGE_PREF = "language";
  public static final String LANGUAGE_SYSTEM = "system";

  private static final String BUNDLE_NAME = "i18n.messages";
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

  private static ResourceBundle bundle;

  private Messages() {}

  /** Must be called before the first UI component is created. Without a stored language
   * preference, or with {@link #LANGUAGE_SYSTEM}, the operating system locale stays in effect. */
  public static synchronized void applyLanguagePreference() {
    String language = savedLanguage();
    if (!LANGUAGE_SYSTEM.equals(language)) {
      Locale.setDefault(Locale.forLanguageTag(language));
    }
    bundle = null;
  }

  public static String savedLanguage() {
    return Preferences.userNodeForPackage(Specman.class).get(LANGUAGE_PREF, LANGUAGE_SYSTEM);
  }

  public static synchronized String i18n(String key) {
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE_NAME);
    }
    return bundle.containsKey(key) ? bundle.getString(key) : "?" + key;
  }

  public static String i18n(String key, Object... args) {
    Matcher matcher = PLACEHOLDER.matcher(i18n(key));
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      int index = Integer.parseInt(matcher.group(1));
      String replacement = index < args.length ? String.valueOf(args[index]) : matcher.group();
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }

}
