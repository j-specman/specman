package specman.editarea.image;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** The SVG loader doesn't throw when it can't load a graphic, it logs the reason and returns nothing. This takes
 * the reason from the log while it is attached, so we can tell the user. Use it in try-with-resources around
 * loading the graphic. */
class SvgLoadProblemCatcher extends Handler implements AutoCloseable {
  /** The loader belongs to jsvg, which FlatLaf brings along but which isn't on our compile classpath. */
  private static final Logger LOADER_LOG = Logger.getLogger("com.github.weisj.jsvg.parser.SVGLoader");

  private String problem;

  SvgLoadProblemCatcher() {
    LOADER_LOG.addHandler(this);
  }

  /** @return what the loader gave as reason for not loading, null if it didn't say anything */
  String problem() { return problem; }

  @Override
  public void publish(LogRecord record) {
    if (problem == null && record.getLevel().intValue() >= Level.WARNING.intValue()) {
      problem = describe(record);
    }
  }

  private static String describe(LogRecord record) {
    Throwable cause = record.getThrown();
    if (cause == null) {
      return record.getMessage();
    }
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
  }

  @Override public void flush() {}

  @Override
  public void close() {
    LOADER_LOG.removeHandler(this);
  }
}
