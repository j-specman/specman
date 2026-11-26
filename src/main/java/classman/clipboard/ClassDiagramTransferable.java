package classman.clipboard;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;

/** Clipboard content for a class diagram which offers two flavors: the source of the diagram for pasting it as a
 * diagram, and the picture for everything else, e.g. Paint or Word. */
public class ClassDiagramTransferable implements Transferable {

  public static final DataFlavor CLASS_DIAGRAM_FLAVOR =
    new DataFlavor(ClassDiagramClipboardContent.class, "Class Diagram");

  private static final DataFlavor[] SUPPORTED_FLAVORS = {CLASS_DIAGRAM_FLAVOR, DataFlavor.imageFlavor};

  private final ClassDiagramClipboardContent content;
  private final BufferedImage picture;

  public ClassDiagramTransferable(ClassDiagramClipboardContent content, BufferedImage picture) {
    this.content = content;
    this.picture = picture;
  }

  @Override
  public DataFlavor[] getTransferDataFlavors() {
    return SUPPORTED_FLAVORS.clone();
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return CLASS_DIAGRAM_FLAVOR.equals(flavor) || DataFlavor.imageFlavor.equals(flavor);
  }

  @Override
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
    if (CLASS_DIAGRAM_FLAVOR.equals(flavor)) {
      return content;
    }
    if (DataFlavor.imageFlavor.equals(flavor)) {
      return picture;
    }
    throw new UnsupportedFlavorException(flavor);
  }
}
