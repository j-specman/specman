package specman.metatag;

import java.awt.Color;

/**
 * Quick experiment: a static per-step meta badge (e.g. "assigned user") shown to the left of the
 * step number - light red background, dark red user icon. Not a real feature yet, just a proof of
 * concept for what a second meta-strip widget alongside KlappButton could look like; none of the
 * zoom/positioning refinements are generalized out of MetaTagPanel for this, it's wired in ad
 * hoc for now.
 */
public class UserMetaTag extends GenericMetaTag {
  private static final Color BACKGROUND = new Color(255, 205, 205);
  private static final Color BORDER_COLOR = new Color(0x8B0000);
  private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, "
    + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
    + "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.";

  public UserMetaTag() {
    super("user", "User ", LOREM_IPSUM, BACKGROUND, BORDER_COLOR);
  }
}
