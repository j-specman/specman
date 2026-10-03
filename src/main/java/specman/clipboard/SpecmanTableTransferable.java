package specman.clipboard;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/** Multi-flavor clipboard transferable for a single Specman table. Offers the Specman-native
 * flavor (carrying instance ID + serialized content as a structured object) and plain text as
 * fallback for interoperability with external tools. */
public class SpecmanTableTransferable implements Transferable {

  public static final DataFlavor SPECMAN_TABLE_FLAVOR =
      new DataFlavor(SpecmanTableClipboardContent.class, "Specman Table");

  private static final DataFlavor[] SUPPORTED_FLAVORS = {SPECMAN_TABLE_FLAVOR, DataFlavor.stringFlavor};

  private final SpecmanTableClipboardContent content;

  public SpecmanTableTransferable(SpecmanTableClipboardContent content) {
    this.content = content;
  }

  @Override
  public DataFlavor[] getTransferDataFlavors() {
    return SUPPORTED_FLAVORS.clone();
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return SPECMAN_TABLE_FLAVOR.equals(flavor) || DataFlavor.stringFlavor.equals(flavor);
  }

  @Override
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
    if (SPECMAN_TABLE_FLAVOR.equals(flavor)) {
      return content;
    }
    if (DataFlavor.stringFlavor.equals(flavor)) {
      return content.serializedTable;
    }
    throw new UnsupportedFlavorException(flavor);
  }
}
