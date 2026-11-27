package specman.ops;

import specman.link.LinkLocation;
import specman.link.SpecmanLink;
import specman.link.SpecmanLink.Reference;

import java.io.File;
import java.util.List;

import static specman.i18n.Messages.i18n;

/** Follows the target of a hyperlink in a step text. A link to the open file is followed right here. A link to another
 * file starts another Specman instance, because one instance shows one model only. Web and mail links
 * are handed to the system. Other kinds of links are refused, as following them could start arbitrary programs. */
public class FollowHyperlinkOp extends AbstractSpecmanOp {
  private static final List<String> SYSTEM_SCHEMES = List.of("http:", "https:", "mailto:");

  public FollowHyperlinkOp(SpecmanOpContext context) {
    super(context);
  }

  public void run(String target) {
    String link = target.strip();
    if (link.startsWith(SpecmanLink.SCHEME)) {
      followSpecmanLink(link);
    }
    else if (isSystemScheme(link)) {
      context.openInBrowser(link);
    }
    else {
      context.showToast(i18n("msg.hyperlink.unsupported", link));
    }
  }

  private void followSpecmanLink(String link) {
    Reference reference = SpecmanLink.parseOrNull(link);
    if (reference == null || reference.isLocal() || refersToCurrentlyOpenFile(reference)) {
      new NavigateToLinkOp(context).run(link);
    }
    else {
      context.openInNewInstance(linkForNewInstance(reference, link));
    }
  }

  /** Whether the link's file is known and open. An alias nobody knows is not, the instance following the link asks for the file. */
  private boolean refersToCurrentlyOpenFile(Reference reference) {
    File file = reference.location().resolve(baseDirectory());
    return file != null && isOpen(file);
  }

  /** A relative path starts in the directory of the document with the link, which the new instance doesn't have, so the
   * new instance gets the path of the file. Any other link tells it all itself. */
  private String linkForNewInstance(Reference reference, String link) {
    if (!reference.location().isRelative()) {
      return link;
    }
    File file = reference.location().resolve(baseDirectory());
    return SpecmanLink.of(LinkLocation.absolute(file), reference.stepId(), reference.stepNumber());
  }

  private File baseDirectory() {
    return LinkLocation.baseDirectoryOf(getDiagrammDatei());
  }

  private boolean isSystemScheme(String link) {
    String lowerCaseLink = link.toLowerCase();
    for (String scheme : SYSTEM_SCHEMES) {
      if (lowerCaseLink.startsWith(scheme)) {
        return true;
      }
    }
    return false;
  }
}
