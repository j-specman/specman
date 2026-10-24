package specman.graphics;

/**
 * Single place mapping each icon function to its SVG resource path. Callers and IconResourcesTest use these constants.
 */
public final class SpecmanIcons {

  /** Menus, popups, context menus and the SHEF format toolbar. */
  public static final int SIZE_SMALL = 16;
  /** Large toolbar buttons (DiagramToolBar, undo/redo large icon). */
  public static final int SIZE_TOOLBAR = 24;
  /** Header icon of dialogs. */
  public static final int SIZE_DIALOG = 32;

  private static final String MATERIAL = "images/svg/material/";
  private static final String SPECMAN = "images/svg/specman/";

  public static final String DELETE = MATERIAL + "delete.svg";

  // DiagramToolBar
  public static final String TONE = MATERIAL + "contrast.svg";
  public static final String TOGGLE_BORDER = MATERIAL + "rounded_corner.svg";
  public static final String TRACK_CHANGES = MATERIAL + "track_changes.svg";
  public static final String ACCEPT_CHANGES = MATERIAL + "done_all.svg";
  public static final String REJECT_CHANGES = MATERIAL + "delete_history.svg";
  public static final String MERGE_CHANGE_SETS = MATERIAL + "merge.svg";
  public static final String REVIEW = MATERIAL + "text_compare.svg";
  public static final String BIRDS_VIEW = MATERIAL + "picture_in_picture.svg";
  public static final String PDF = MATERIAL + "picture_as_pdf.svg";

  // StepnumberContextMenu
  public static final String COPY = MATERIAL + "content_copy.svg";
  public static final String CUT = MATERIAL + "content_cut.svg";
  public static final String PASTE = MATERIAL + "content_paste.svg";
  // Material Symbols has no arrow_drop_left/right: arrow_left/right are the same filled triangle as arrow_drop_up/down, rotated
  public static final String MOVE_UP = MATERIAL + "arrow_drop_up.svg";
  public static final String MOVE_DOWN = MATERIAL + "arrow_drop_down.svg";
  public static final String MOVE_LEFT = MATERIAL + "arrow_left.svg";
  public static final String MOVE_RIGHT = MATERIAL + "arrow_right.svg";

  // File menu
  public static final String FILE_NEW = MATERIAL + "note_add.svg";
  public static final String FILE_OPEN = MATERIAL + "folder_open.svg";
  public static final String FILE_RECENT = MATERIAL + "history.svg";
  public static final String FILE_SAVE = MATERIAL + "save.svg";
  public static final String AUTO_LOAD = MATERIAL + "autorenew.svg";
  public static final String FILE_SAVE_AS = MATERIAL + "save_as.svg";
  public static final String EXPORT_GRAPHVIZ = MATERIAL + "account_tree.svg";
  public static final String SETTINGS = MATERIAL + "settings.svg";
  public static final String EXIT = MATERIAL + "logout.svg";

  // Collapse/expand all steps (toolbar + View menu)
  public static final String COLLAPSE_ALL = MATERIAL + "unfold_less.svg";
  public static final String EXPAND_ALL = MATERIAL + "unfold_more.svg";
  // KlappButton of a single step
  public static final String COLLAPSE_STEP = MATERIAL + "remove.svg";
  public static final String EXPAND_STEP = MATERIAL + "add.svg";

  // StepButtonBar text/icon-only toggle
  public static final String PANEL_HIDE_TEXT = MATERIAL + "left_panel_close.svg";
  public static final String PANEL_SHOW_TEXT = MATERIAL + "left_panel_open.svg";

  // SHEF editor: format toolbar / menus / popups / dialogs
  public static final String FORMAT_BOLD = MATERIAL + "format_bold.svg";
  public static final String FORMAT_ITALIC = MATERIAL + "format_italic.svg";
  public static final String FORMAT_UNDERLINE = MATERIAL + "format_underlined.svg";
  public static final String LIST_UNORDERED = MATERIAL + "format_list_bulleted.svg";
  public static final String LIST_ORDERED = MATERIAL + "format_list_numbered.svg";
  public static final String ALIGN_LEFT = MATERIAL + "format_align_left.svg";
  public static final String ALIGN_CENTER = MATERIAL + "format_align_center.svg";
  public static final String ALIGN_RIGHT = MATERIAL + "format_align_right.svg";
  public static final String ALIGN_JUSTIFY = MATERIAL + "format_align_justify.svg";
  public static final String LINK = MATERIAL + "link.svg";
  public static final String IMAGE = MATERIAL + "image.svg";
  public static final String TABLE = MATERIAL + "table.svg";
  public static final String FONT_SIZE = MATERIAL + "format_size.svg";
  public static final String FONT_COLOR = MATERIAL + "format_color_text.svg";
  public static final String STEPNUMBER_LINK = MATERIAL + "jump_to_element.svg";
  public static final String UNDO = MATERIAL + "undo.svg";
  public static final String REDO = MATERIAL + "redo.svg";
  public static final String SELECT_ALL = MATERIAL + "select_all.svg";
  public static final String FIND_REPLACE = MATERIAL + "find_replace.svg";
  public static final String LINE_BREAK = MATERIAL + "keyboard_return.svg";
  public static final String HORIZONTAL_RULE = MATERIAL + "horizontal_rule.svg";
  public static final String CLEAR_STYLES = MATERIAL + "format_clear.svg";
  public static final String ELEMENT_PROPERTIES = MATERIAL + "tune.svg";
  public static final String FONT_DIALOG = MATERIAL + "font_download.svg";
  public static final String SPECIAL_CHARACTER = MATERIAL + "special_character.svg";
  public static final String META_TAG = MATERIAL + "sell.svg";
  public static final String META_TAGS = MATERIAL + "style.svg";

  // Own step-type icons (see images/svg/specman/)
  public static final String STEP_SIMPLE = SPECMAN + "step-simple.svg";
  public static final String STEP_IF = SPECMAN + "step-if.svg";
  public static final String STEP_IFELSE = SPECMAN + "step-ifelse.svg";
  public static final String STEP_CASE = SPECMAN + "step-case.svg";
  public static final String STEP_CASE_BRANCH = SPECMAN + "step-case-branch.svg";
  public static final String STEP_WHILE = SPECMAN + "step-while.svg";
  public static final String STEP_WHILEWHILE = SPECMAN + "step-whilewhile.svg";
  public static final String STEP_SUBSEQUENCE = SPECMAN + "step-subsequence.svg";
  public static final String STEP_BREAK = SPECMAN + "step-break.svg";
  public static final String STEP_CATCH = SPECMAN + "step-catch.svg";

  private SpecmanIcons() {
  }

}
