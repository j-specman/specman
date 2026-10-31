package headless.links;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import specman.editarea.TextEditArea;
import specman.link.AliasPanel;
import specman.link.SpecmanLink;
import specman.view.AbstractSchrittView;
import specman.view.EinfacherSchrittView;

import java.nio.file.Path;
import java.util.List;

import static headless.HeadlessExtension.specman;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static specman.i18n.Messages.i18n;

/** Following hyperlinks in step texts by CTRL+click. */
class FollowHyperlinkTest extends AbstractLinkTest {

  private static TextEditArea textWithHyperlink(EinfacherSchrittView step, String target) {
    TextEditArea text = step.getFirstEditArea().asTextArea();
    text.setText("<a href=\"" + target + "\">link</a>");
    return text;
  }

  private static AbstractSchrittView focusedStep() {
    return specman.findeSchritt((TextEditArea) specman.focusedArea());
  }

  /** Scenario: The text of step 1 links to step 2 of UC-720, which is not the open file. Following the link starts another
   * Specman instance for it and leaves this one alone, as one instance shows one model only. */
  @Test
  void testLinkToOtherFileStartsNewInstance(@TempDir Path directory) throws Exception {
    specman.clickNewSimpleStep();
    EinfacherSchrittView step2 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    String link = copyLink(step2);
    EinfacherSchrittView linkingStep = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-721.nsd");
    TextEditArea text = textWithHyperlink(linkingStep, link);

    specman.ctrlClickOnFirstCharacter(text);

    assertEquals(List.of(link), specman.linksOpenedInNewInstance());
    assertEquals("UC-721.nsd", specman.getDiagrammDatei().getName());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The text of step 1 links to step 3 of the open file. Following the link goes there right away, no new
   * instance is needed. */
  @Test
  void testLinkToOpenFileNavigatesInPlace(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    specman.clickNewSimpleStep();
    EinfacherSchrittView step3 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    TextEditArea text = textWithHyperlink(step1, copyLink(step3));

    specman.ctrlClickOnFirstCharacter(text);

    assertSame(step3, focusedStep());
    assertEquals(List.of(), specman.linksOpenedInNewInstance());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: The link names the open file but no step. Nothing would happen, so the user is told why. */
  @Test
  void testLinkToOpenFileWithoutStepTellsFileIsOpen(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    TextEditArea text = textWithHyperlink(step, fileOnly(copyLink(step)));

    specman.ctrlClickOnFirstCharacter(text);

    assertEquals(List.of(i18n("msg.link.file.already.open")), specman.shownMessages());
    assertEquals(List.of(), specman.linksOpenedInNewInstance());
  }

  /** Scenario: The link has an alias the user has for another file than the open one. */
  @Test
  void testLinkWithKnownAliasOfOtherFileStartsNewInstance(@TempDir Path directory) throws Exception {
    EinfacherSchrittView step1 = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-720.nsd");
    AliasPanel panel = clickCopyLink(step1);
    typeAlias(panel, "uc720");
    click(panel, AliasPanel.OK_BUTTON);
    String link = clipboardText();
    EinfacherSchrittView linkingStep = specman.clickNewSimpleStep();
    saveDiagram(directory, "UC-721.nsd");
    TextEditArea text = textWithHyperlink(linkingStep, link);

    specman.ctrlClickOnFirstCharacter(text);

    assertEquals(List.of(link), specman.linksOpenedInNewInstance());
  }

  /** Scenario: The link has an alias nobody knows. The new instance asks for the file, this one doesn't. */
  @Test
  void testLinkWithUnknownAliasStartsNewInstanceWhichAsks() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    String link = SpecmanLink.SCHEME + "uc999";
    TextEditArea text = textWithHyperlink(step, link);

    specman.ctrlClickOnFirstCharacter(text);

    assertEquals(List.of(link), specman.linksOpenedInNewInstance());
    assertEquals(List.of(), specman.aliasesAskedFor());
  }

  /** Scenario: A broken Specman link is reported like when it comes from the clipboard. */
  @Test
  void testBrokenSpecmanLinkIsReported() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = textWithHyperlink(step, "specman://,1,2");

    specman.ctrlClickOnFirstCharacter(text);

    assertEquals(List.of(i18n("msg.link.invalid")), specman.shownMessages());
    assertEquals(List.of(), specman.linksOpenedInNewInstance());
  }

  /** Scenario: Web and mail addresses are handed to the system. */
  @ParameterizedTest
  @ValueSource(strings = {"https://specman.org/uc-720", "http://localhost:8080/", "mailto:jan@example.org", "HTTPS://specman.org"})
  void testWebAndMailAddressesAreHandedToSystem(String address) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = textWithHyperlink(step, address);

    specman.ctrlClickOnFirstCharacter(text);

    assertEquals(List.of(address), specman.addressesOpenedInBrowser());
    assertEquals(List.of(), specman.linksOpenedInNewInstance());
  }

  /** Scenario: Following a link must not start programs, so anything but Specman, web and mail links is refused. */
  @ParameterizedTest
  @ValueSource(strings = {"file:///C:/Windows/System32/calc.exe", "ftp://example.org/file", "javascript:alert(1)", "uc-720"})
  void testOtherKindsOfLinksAreRefused(String target) {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = textWithHyperlink(step, target);

    specman.ctrlClickOnFirstCharacter(text);

    assertEquals(List.of(i18n("msg.hyperlink.unsupported", target)), specman.shownMessages());
    assertEquals(List.of(), specman.addressesOpenedInBrowser());
    assertEquals(List.of(), specman.linksOpenedInNewInstance());
  }

  /** Scenario: Without CTRL a click on the link just places the cursor, which is what the user wants to edit the link text. */
  @Test
  void testClickWithoutCtrlDoesNotFollowLink() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = textWithHyperlink(step, "https://specman.org");

    specman.clickOnFirstCharacter(text);

    assertEquals(List.of(), specman.addressesOpenedInBrowser());
    assertEquals(List.of(), specman.shownMessages());
  }

  /** Scenario: CTRL+click on text which is no link does nothing. */
  @Test
  void testCtrlClickOnPlainTextDoesNothing() {
    EinfacherSchrittView step = specman.clickNewSimpleStep();
    TextEditArea text = step.getFirstEditArea().asTextArea();
    text.setText("just text");

    specman.ctrlClickOnFirstCharacter(text);

    assertEquals(List.of(), specman.addressesOpenedInBrowser());
    assertEquals(List.of(), specman.linksOpenedInNewInstance());
    assertEquals(List.of(), specman.shownMessages());
  }
}
