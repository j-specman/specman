package specman.link;

import org.junit.jupiter.api.Test;
import specman.link.SpecmanLink.Reference;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Specman link names a file, by alias or path, and optionally a step in it by ID and number. */
class SpecmanLinkTest {

  @Test
  void stepLinkWithAlias() {
    String link = SpecmanLink.of(new File("UC-720.nsd"), "uc720", "2d34f85e", "1.3");

    assertEquals("specman://uc720#2d34f85e,1.3", link);
    Reference reference = SpecmanLink.parse(link);
    assertEquals("uc720", reference.alias());
    assertEquals("2d34f85e", reference.stepId());
    assertEquals("1.3", reference.stepNumber());
    assertTrue(reference.hasAlias());
    assertTrue(reference.hasStep());
  }

  @Test
  void stepLinkWithPath() {
    File file = new File("UC 720.nsd").getAbsoluteFile();

    Reference reference = SpecmanLink.parse(SpecmanLink.of(file, "", "2d34f85e", "1.3"));

    assertEquals(file, reference.location().resolve(new File(".")));
    assertFalse(reference.hasAlias());
    assertEquals("2d34f85e", reference.stepId());
  }

  @Test
  void numberIsOptional() {
    String link = SpecmanLink.of(new File("UC-720.nsd"), "uc720", "2d34f85e", null);

    assertEquals("specman://uc720#2d34f85e", link);
    assertNull(SpecmanLink.parse(link).stepNumber());
  }

  @Test
  void linkWithoutStepNamesTheFileOnly() {
    String link = SpecmanLink.of(new File("UC-720.nsd"), "uc720", null, null);

    assertEquals("specman://uc720", link);
    assertFalse(SpecmanLink.parse(link).hasStep());
  }

  /** Links of the form which was used before the fragment was introduced are still read. */
  @Test
  void linkOfOldFormIsRead() {
    assertEquals(SpecmanLink.parse("specman://uc720#2d34f85e,1.3"), SpecmanLink.parse("specman://uc720,2d34f85e,1.3"));
    assertEquals(SpecmanLink.parse("specman://uc720#2d34f85e"), SpecmanLink.parse("specman://uc720,2d34f85e"));
  }

  @Test
  void linkOfOldFormWithPathIsRead() {
    Reference reference = SpecmanLink.parse("specman:///C:/project/UC-720.nsd,2d34f85e,1.3");

    assertEquals(new File("C:/project/UC-720.nsd"), reference.location().resolve(new File(".")));
    assertEquals("2d34f85e", reference.stepId());
    assertEquals("1.3", reference.stepNumber());
  }

  /** The comma in a path is encoded, so a link to a file with a comma in its name, which has no fragment, isn't taken for a
   * link of the old form. */
  @Test
  void linkToFileWithCommaInPathNamesTheFileOnly() {
    File file = new File("Kunden, Liste.nsd").getAbsoluteFile();

    Reference reference = SpecmanLink.parse(SpecmanLink.of(file, "", null, null));

    assertEquals(file, reference.location().resolve(new File(".")));
    assertFalse(reference.hasStep());
  }

  /** A relative path names a file in or near the directory of the document the link is in. */
  @Test
  void stepLinkWithRelativePath() {
    Reference reference = SpecmanLink.parse("specman://../specs/UC-720.nsd#2d34f85e,1.3");

    assertTrue(reference.location().isRelative());
    assertFalse(reference.isLocal());
    assertEquals("2d34f85e", reference.stepId());
    assertEquals("specman://../specs/UC-720.nsd#2d34f85e,1.3", SpecmanLink.of(reference.location(), "2d34f85e", "1.3"));
  }

  /** A link without file points to a step of the document which contains it. */
  @Test
  void localLink() {
    Reference reference = SpecmanLink.parse("specman://#2d34f85e,1.3");

    assertTrue(reference.isLocal());
    assertEquals("2d34f85e", reference.stepId());
    assertEquals("1.3", reference.stepNumber());
    assertEquals("specman://#2d34f85e,1.3", SpecmanLink.of(LinkLocation.local(), "2d34f85e", "1.3"));
  }

  @Test
  void localLinkWithSlashAddedByToolIsTheSame() {
    assertEquals(SpecmanLink.parse("specman://#2d34f85e,1.3"), SpecmanLink.parse("specman:///#2d34f85e,1.3"));
  }

  @Test
  void slashesAddedByToolsAreIgnored() {
    assertEquals(SpecmanLink.parse("specman://uc720#2d34f85e,1.3"), SpecmanLink.parse("specman://uc720/#2d34f85e,1.3/"));
    assertEquals(SpecmanLink.parse("specman://uc720"), SpecmanLink.parse("specman://uc720/"));
  }

  @Test
  void tooManyPartsAreRefused() {
    assertThrows(IllegalArgumentException.class, () -> SpecmanLink.parse("specman://uc720#a,b,c"));
    assertThrows(IllegalArgumentException.class, () -> SpecmanLink.parse("specman://uc720,a,b,c"));
  }

  /** A local link names a step, as a link to the document alone is of no use. */
  @Test
  void linkWithoutFileAndWithoutStepIsRefused() {
    assertNull(SpecmanLink.parseOrNull("specman://#"));
    assertNull(SpecmanLink.parseOrNull("specman://"));
  }

  /** A link of the old form always had a file, so it is no local link. */
  @Test
  void linkOfOldFormWithoutFileIsRefused() {
    assertNull(SpecmanLink.parseOrNull("specman://,2d34f85e,1.3"));
  }

  @Test
  void textWhichIsNoLink() {
    assertNull(SpecmanLink.parseOrNull("uc720"));
    assertNull(SpecmanLink.parseOrNull("classman://uc720"));
    assertNull(SpecmanLink.parseOrNull(null));
  }
}
