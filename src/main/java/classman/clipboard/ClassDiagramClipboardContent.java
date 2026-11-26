package classman.clipboard;

import java.io.Serializable;

/** Payload of the class diagram clipboard flavor: the source of the diagram.
 * Must be Serializable: the MIME type of the flavor is application/x-java-serialized-object, which the OS clipboard has
 * to actually serialize when another process pastes. Within one JVM the object is handed over directly. */
public class ClassDiagramClipboardContent implements Serializable {
  public final String source;

  public ClassDiagramClipboardContent(String source) {
    this.source = source;
  }
}
