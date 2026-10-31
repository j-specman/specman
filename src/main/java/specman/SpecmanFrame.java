package specman;

import specman.graphics.IconReader;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The application window around a {@link Specman} instance. Everything which needs a real window
 * (title bar, icon, size, location, closing) lives here. {@link Specman} is a {@link javax.swing.JRootPane}
 * and becomes the frame's root pane, so that it can be instantiated headless without a frame.
 */
public class SpecmanFrame extends JFrame {
  private static final int INITIAL_WIDTH = 1100;
  private static final int INITIAL_HEIGHT = 700;

  public SpecmanFrame() {
    setIconImage(IconReader.readImageIcon("specman").getImage());
    setTitle(Specman.SPECMAN_TITLE);
  }

  public void open(Specman specman) {
    setRootPane(specman);
    specman.attachFrame(this);
    setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    setLocation((screen.width - INITIAL_WIDTH) / 2, (screen.height - INITIAL_HEIGHT) / 2);
    setVisible(true);
    bringToFront();
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    specman.installGlassPane();
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (specman.confirmClose()) {
          dispose();
          System.exit(0);
        }
      }
    });
  }

  /** Windows doesn't let a process which another one started take the foreground, so a Specman instance started for
   * a link would appear behind the window the user clicked in. Being always on top for a moment gets around that. */
  private void bringToFront() {
    setAlwaysOnTop(true);
    toFront();
    requestFocus();
    setAlwaysOnTop(false);
  }
}
