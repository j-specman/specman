package headless;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.awt.GraphicsEnvironment;

/**
 * Switches AWT to headless mode for tests. The property is read once and cached when the first AWT
 * class is initialized, so it must be set before that. The static block runs when JUnit registers
 * the extension, which happens before the test class is instantiated.
 */
public class HeadlessExtension implements BeforeAllCallback {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!GraphicsEnvironment.isHeadless()) {
      throw new IllegalStateException(
        "AWT was initialized before HeadlessExtension could enable headless mode. " +
        "Run the test with -Djava.awt.headless=true.");
    }
  }
}
