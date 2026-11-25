package specman.editarea.image;

import javax.swing.Icon;

/** What an image edit area shows: a vector picture with a natural size, which can be drawn at any scale and which
 * the PDF export takes over as SVG. */
public interface Picture {
  /** @return the width at 100%, in pixels */
  int width();

  /** @return the height at 100%, in pixels */
  int height();

  /** @return the picture drawn at the given fraction of its natural size */
  Icon scaledIcon(float scale);

  /** @return the picture as SVG which has the natural size of the picture */
  byte[] svg();
}
