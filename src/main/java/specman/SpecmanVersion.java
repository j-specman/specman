package specman;

import java.util.Properties;

public class SpecmanVersion {
  public static final String UNKNOWN_VERSION = "unknown";
  static Properties versionProperties;

  public static String getVersion() {
    if (versionProperties == null) {
      versionProperties = new Properties();
      try {
        versionProperties.load(SpecmanVersion.class.getResourceAsStream("/specman-version.properties"));
      }
      catch (Exception e) {
        e.printStackTrace();
        return UNKNOWN_VERSION;
      }
    }
    return versionProperties.getProperty("version", UNKNOWN_VERSION);
  }

  /** Gibt den vorderen Teil der Versionsnummer zurück - alles vor dem zweiten Punkt, also
   * z.B. "1.2" bei Version "1.2.34". Dieser Teil gilt beim Laden von Dateien als notwendig
   * einzuhalten, um keine Warnung über mögliche Inkompatibilitäten auszulösen. */
  public static String getCompatibilityVersionPrefix() {
    String version = getVersion();
    int secondDotIndex = version.indexOf('.', version.indexOf('.') + 1);
    if (secondDotIndex != -1) {
      return version.substring(0, secondDotIndex);
    }
    return version;
  }
}
