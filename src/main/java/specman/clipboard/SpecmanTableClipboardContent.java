package specman.clipboard;

import java.io.Serializable;

/** Payload of the dedicated Specman table clipboard flavor. Carries the instance ID of the editor
 * that placed the content, so paste can decide how to handle change-tracking markings.
 * Must be Serializable: its DataFlavor's MIME type is application/x-java-serialized-object,
 * which the OS clipboard has to actually serialize/deserialize when pasting across two separate
 * Specman processes (same-instance paste works regardless, since the object is handed over
 * directly within one JVM without needing serialization). */
public class SpecmanTableClipboardContent implements Serializable {
  public final String instanceId;
  public final String serializedTable;

  public SpecmanTableClipboardContent(String instanceId, String serializedTable) {
    this.instanceId = instanceId;
    this.serializedTable = serializedTable;
  }
}
