package headless;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.awt.GraphicsEnvironment;

/**
 * Switches AWT to headless mode for tests and provides each test with a fresh {@link SpecmanForTests}.
 * The headless property is read once and cached when the first AWT class is initialized, so it must
 * be set before that. The static block runs when JUnit registers the extension, which happens before
 * the test class is instantiated.
 * <p>
 * Tests access the instance by statically importing {@link #specman}. It is replaced before each
 * test, so no state leaks from one test into the next.
 */
public class HeadlessExtension implements BeforeAllCallback, BeforeEachCallback {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  public static SpecmanForTests specman;

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!GraphicsEnvironment.isHeadless()) {
      throw new IllegalStateException(
        "AWT was initialized before HeadlessExtension could enable headless mode. " +
        "Run the test with -Djava.awt.headless=true.");
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    specman = new SpecmanForTests();
  }
}
