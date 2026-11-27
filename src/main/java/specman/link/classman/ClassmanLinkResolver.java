package specman.link.classman;

import classman.model.v001.ClassDiagramModel_V001;
import classman.model.v001.io.ClassDiagramParseException;
import classman.model.v001.io.ClassDiagramParser_V001;
import specman.link.LinkLocation;
import specman.util.TextFiles;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/** Follows a Classman link to the diagram file and reads it, without any user interface: for the area which shows the
 * diagram as well as for the sanitizer. Only a file with the extension of diagram files is read, and it has to be a
 * class diagram. */
public final class ClassmanLinkResolver {
  private final File baseDirectory;

  /** For links in no document in particular: A relative path starts in the working directory. */
  public ClassmanLinkResolver() {
    this(null);
  }

  /** @param documentFile the document which contains the links, null if it has no file. A relative path starts in the
   * directory of the document, see {@link LinkLocation#baseDirectoryOf(File)}. */
  public ClassmanLinkResolver(File documentFile) {
    this.baseDirectory = LinkLocation.baseDirectoryOf(documentFile);
  }

  /** @return what the link leads to. A link whose alias nobody knows leads nowhere. */
  public ClassmanLinkResolution resolve(String linkText) {
    ClassmanLinkReference reference;
    try {
      reference = ClassmanLink.parse(linkText);
    }
    catch (IllegalArgumentException notALink) {
      return ClassmanLinkResolution.broken(null, ClassmanLinkProblem.NOT_A_LINK, linkText);
    }
    File file = reference.location().resolve(baseDirectory);
    if (file == null) {
      return ClassmanLinkResolution.broken(null, ClassmanLinkProblem.ALIAS_UNKNOWN, reference.alias());
    }
    return resolve(file);
  }

  /** @return what the file has, taken as the file which a link leads to */
  public ClassmanLinkResolution resolve(File file) {
    if (!file.getName().toLowerCase(Locale.ROOT).endsWith("." + ClassmanLink.FILE_EXTENSION)) {
      return ClassmanLinkResolution.broken(file, ClassmanLinkProblem.WRONG_EXTENSION, file.getPath());
    }
    if (!file.isFile()) {
      return ClassmanLinkResolution.broken(file, ClassmanLinkProblem.FILE_MISSING, file.getPath());
    }
    String source;
    try {
      source = TextFiles.read(file);
    }
    catch (IOException e) {
      return ClassmanLinkResolution.broken(file, ClassmanLinkProblem.UNREADABLE, e.getMessage());
    }
    return resolveSource(file, source);
  }

  private ClassmanLinkResolution resolveSource(File file, String source) {
    try {
      ClassDiagramModel_V001 diagram = new ClassDiagramParser_V001().parse(source);
      return ClassmanLinkResolution.ok(file, source, diagram.id, diagram.name);
    }
    catch (ClassDiagramParseException | IllegalArgumentException problem) {
      return ClassmanLinkResolution.invalid(file, source, problem.getMessage());
    }
  }
}
