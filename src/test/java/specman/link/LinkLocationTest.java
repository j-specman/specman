package specman.link;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** How a link names a file: by alias, by absolute or relative path, or not at all. */
class LinkLocationTest {

  private static File inside(Path directory, String path) {
    return directory.resolve(path).toFile();
  }

  @Test
  void aliasIsWrittenAsItIs() {
    LinkLocation location = LinkLocation.parse("uc720");

    assertTrue(location.isAlias());
    assertEquals("uc720", location.alias());
    assertEquals("uc720", location.text());
  }

  @Test
  void absolutePathRoundTrips() {
    File file = new File("Kunden, Liste.nsd").getAbsoluteFile();

    LinkLocation location = LinkLocation.parse(LinkLocation.absolute(file).text());

    assertFalse(location.isAlias() || location.isRelative() || location.isLocal());
    assertEquals(file, location.resolve(new File("somewhere")));
  }

  @Test
  void fileWithAliasIsNamedByAliasAndWithoutByPath() {
    File file = new File("x.nsd").getAbsoluteFile();

    assertEquals(LinkLocation.alias("uc720"), LinkLocation.of(file, "uc720"));
    assertEquals(LinkLocation.absolute(file), LinkLocation.of(file, ""));
  }

  @Test
  void localLocationHasNoFile() {
    LinkLocation location = LinkLocation.parse("");

    assertTrue(location.isLocal());
    assertEquals("", location.text());
    assertNull(location.resolve(new File(".")));
  }

  @Test
  void fileInTheSameDirectory(@TempDir Path directory) {
    LinkLocation location = LinkLocation.relative(inside(directory, "x.cld"), directory.toFile());

    assertEquals("./x.cld", location.text());
    assertTrue(location.isRelative());
  }

  @Test
  void fileInASubdirectory(@TempDir Path directory) {
    assertEquals("./diagrams/x.cld", LinkLocation.relative(inside(directory, "diagrams/x.cld"), directory.toFile()).text());
  }

  @Test
  void fileInTheParentDirectory(@TempDir Path directory) {
    File base = inside(directory, "specs");

    assertEquals("../x.cld", LinkLocation.relative(inside(directory, "x.cld"), base).text());
  }

  @Test
  void fileInASiblingDirectory(@TempDir Path directory) {
    File base = inside(directory, "specs");

    assertEquals("../diagrams/x.cld", LinkLocation.relative(inside(directory, "diagrams/x.cld"), base).text());
  }

  /** A directory whose name begins with two dots is not the parent directory. */
  @Test
  void directoryWithDotsInItsName(@TempDir Path directory) {
    assertEquals("./..x/y.cld", LinkLocation.relative(inside(directory, "..x/y.cld"), directory.toFile()).text());
  }

  @Test
  void blanksAndCommasAreEncoded(@TempDir Path directory) {
    File file = inside(directory, "Dokumente mit, Komma/x y.cld");

    LinkLocation location = LinkLocation.relative(file, directory.toFile());

    assertEquals("./Dokumente%20mit%2C%20Komma/x%20y.cld", location.text());
    assertEquals(file, location.resolve(directory.toFile()));
  }

  @Test
  void relativePathIsResolvedFromTheBaseDirectory(@TempDir Path directory) {
    File base = inside(directory, "specs");
    File file = inside(directory, "diagrams/x.cld");

    LinkLocation parsed = LinkLocation.parse("../diagrams/x.cld");

    assertEquals(file, parsed.resolve(base));
    assertEquals(inside(directory, "other/diagrams/x.cld"), parsed.resolve(inside(directory, "other/specs")));
  }

  @Test
  void parsedLocationEqualsTheOneItWasWrittenFrom(@TempDir Path directory) {
    LinkLocation relative = LinkLocation.relative(inside(directory, "x y.cld"), directory.toFile());

    assertEquals(relative, LinkLocation.parse(relative.text()));
  }

  /** On Windows, a file on another drive can't be reached by a relative path. */
  @Test
  void fileOnAnotherDriveHasNoRelativePath() {
    assumeTrue(File.separatorChar == '\\');

    assertNull(LinkLocation.relative(new File("C:\\a\\x.cld"), new File("D:\\b")));
  }

  @Test
  void baseDirectoryIsTheOneOfTheDocument(@TempDir Path directory) {
    assertEquals(directory.toFile(), LinkLocation.baseDirectoryOf(inside(directory, "UC-720.nsd")));
  }

  @Test
  void baseDirectoryWithoutDocumentIsTheWorkingDirectory() {
    assertEquals(new File(".").getAbsoluteFile(), LinkLocation.baseDirectoryOf(null));
  }

  @Test
  void invalidPathIsRefused() {
    assertThrows(IllegalArgumentException.class, () -> LinkLocation.parse("./a b"));
  }

  /** What is neither an alias, nor a path, nor empty is no location, e.g. what is left of a link of the old form which had no
   * file. */
  @Test
  void textWhichIsNoAliasIsRefused() {
    assertThrows(IllegalArgumentException.class, () -> LinkLocation.parse("a b"));
    assertThrows(IllegalArgumentException.class, () -> LinkLocation.parse(",1,2"));
    assertThrows(IllegalArgumentException.class, () -> LinkLocation.parse("uc720.nsd"));
  }
}
