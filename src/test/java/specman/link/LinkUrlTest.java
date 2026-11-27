package specman.link;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The form which all link URLs share: scheme, then the location of the file, then optionally a hash sign and the
 * fragment which names a part of the file. */
class LinkUrlTest {
  private static final String SCHEME = "test://";

  @Test
  void aliasWithFragment() {
    String url = LinkUrl.of(SCHEME, LinkLocation.alias("hilfe"), "0a1b,Name");

    assertEquals("test://hilfe#0a1b,Name", url);
    LinkUrl parsed = LinkUrl.parse(url, SCHEME);
    assertEquals(LinkLocation.alias("hilfe"), parsed.location());
    assertEquals("0a1b,Name", parsed.fragment());
  }

  @Test
  void withoutFragment() {
    LinkUrl parsed = LinkUrl.parse(LinkUrl.of(SCHEME, LinkLocation.alias("hilfe"), null), SCHEME);

    assertEquals(LinkLocation.alias("hilfe"), parsed.location());
    assertNull(parsed.fragment());
  }

  @Test
  void emptyFragmentIsNotNoFragment() {
    assertEquals("", LinkUrl.parse("test://hilfe#", SCHEME).fragment());
  }

  /** The location is encoded so that neither a blank, nor a comma, nor a hash sign in the path can be mistaken for what
   * separates the parts of the URL. */
  @Test
  void pathWithBlankCommaAndHash() {
    File file = new File("Dokumente, mit #Hash/x.cld").getAbsoluteFile();

    String url = LinkUrl.of(SCHEME, LinkLocation.absolute(file), "fragment");

    String location = url.substring(SCHEME.length(), url.indexOf("#fragment"));
    assertFalse(location.contains(" ") || location.contains(",") || location.contains("#"), location);
    LinkUrl parsed = LinkUrl.parse(url, SCHEME);
    assertEquals(file, parsed.location().resolve(new File(".")));
    assertEquals("fragment", parsed.fragment());
  }

  @Test
  void relativePath() {
    LinkUrl parsed = LinkUrl.parse("test://../diagrams/hilfe.cld#a", SCHEME);

    assertTrue(parsed.location().isRelative());
    assertEquals("a", parsed.fragment());
  }

  @Test
  void surroundingBlanksAreIgnored() {
    assertEquals(LinkLocation.alias("hilfe"), LinkUrl.parse("  test://hilfe#a \n", SCHEME).location());
  }

  /** Tools which treat the link like a web address write an empty path as a slash, and add one at the end. */
  @Test
  void slashesBeforeTheHashSignAndAtTheEndAreIgnored() {
    LinkUrl parsed = LinkUrl.parse("test://hilfe/#a,b/", SCHEME);

    assertEquals(LinkLocation.alias("hilfe"), parsed.location());
    assertEquals("a,b", parsed.fragment());
  }

  @Test
  void slashAfterPathIsIgnored() {
    File file = new File("x.cld").getAbsoluteFile();
    String url = LinkUrl.of(SCHEME, LinkLocation.absolute(file), null);

    assertEquals(file, LinkUrl.parse(url + "/", SCHEME).location().resolve(new File(".")));
  }

  /** A URL without location is a local link, which the kind of link may or may not allow. */
  @Test
  void urlWithoutLocationHasLocalLocation() {
    assertTrue(LinkUrl.parse("test://#a,b", SCHEME).location().isLocal());
    assertTrue(LinkUrl.parse("test:///#a,b", SCHEME).location().isLocal());
    assertEquals("a,b", LinkUrl.parse("test://#a,b", SCHEME).fragment());
  }

  @Test
  void urlOfOtherSchemeIsRefused() {
    assertTrue(assertThrows(IllegalArgumentException.class, () -> LinkUrl.parse("other://hilfe", SCHEME))
      .getMessage().contains(SCHEME));
  }
}
