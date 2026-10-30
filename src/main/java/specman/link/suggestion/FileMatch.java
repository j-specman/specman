package specman.link.suggestion;

import specman.link.LinkAliases;

import java.io.File;

/** A diagram which has the characters of an alias at the positions the alias of an example assignment found its characters.
 * The matches of all examples tell whether there is one file the alias stands for or several the user has to choose from. */
class FileMatch {
  private final LinkAliases.Entry source;
  private final File file;

  FileMatch(LinkAliases.Entry source, File file) {
    this.source = source;
    this.file = file;
  }

  LinkAliases.Entry source() {
    return source;
  }

  File file() {
    return file;
  }

  /** @return whether both matches found the same file, however they got there */
  boolean isSameFileAs(FileMatch other) {
    return file.equals(other.file);
  }
}
