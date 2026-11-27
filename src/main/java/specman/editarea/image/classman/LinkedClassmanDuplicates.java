package specman.editarea.image.classman;

import specman.link.LinkAliases;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The rule that a document links a file at most once. Two diagrams on one file would overwrite each other's changes
 * when the document is saved. */
public class LinkedClassmanDuplicates {

  /** @return true if one of the diagrams links the file */
  public static boolean isLinkedBy(File file, List<LinkedClassmanImageEditArea> areas) {
    return pathsOfLinkedFiles(areas).contains(LinkAliases.absolutePath(file));
  }

  /** Marks the candidates which link a file that an established diagram or an earlier candidate links already. */
  public static void markDuplicates(List<LinkedClassmanImageEditArea> established,
                                    List<LinkedClassmanImageEditArea> candidates) {
    Set<String> linkedPaths = pathsOfLinkedFiles(established);
    for (LinkedClassmanImageEditArea candidate : candidates) {
      File file = candidate.linkedFile();
      if (file != null && !linkedPaths.add(LinkAliases.absolutePath(file))) {
        candidate.markDuplicate();
      }
    }
  }

  private static Set<String> pathsOfLinkedFiles(List<LinkedClassmanImageEditArea> areas) {
    Set<String> paths = new HashSet<>();
    for (LinkedClassmanImageEditArea area : areas) {
      if (area.linkedFile() != null) {
        paths.add(LinkAliases.absolutePath(area.linkedFile()));
      }
    }
    return paths;
  }
}
