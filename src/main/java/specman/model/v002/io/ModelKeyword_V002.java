package specman.model.v002.io;

import static specman.model.v002.SpecmanModel_V002Lexer.*;

/** Keyword strings for the SpecmanModel_V002 text format.
 *  Each constant wraps the string value from the ANTLR4 lexer vocabulary
 *  and returns it via {@link #toString()} — the grammar is the single source of truth.
 *  Adding a keyword here requires a matching KW_xxx rule in SpecmanModel_V002.g4. */
public enum ModelKeyword_V002 {

    SIMPLE(KW_SIMPLE),
    BREAK(KW_BREAK),
    WHILE(KW_WHILE),
    DO_WHILE(KW_DO_WHILE),
    IF_ELSE(KW_IF_ELSE),
    IF(KW_IF),
    CASE(KW_CASE),
    SUBSEQUENCE(KW_SUBSEQUENCE),
    MAIN_SEQUENCE(KW_MAIN_SEQUENCE),
    INTRO(KW_INTRO),
    OUTRO(KW_OUTRO),
    SETTINGS(KW_SETTINGS),
    IF_BRANCH(KW_IF_BRANCH),
    ELSE_BRANCH(KW_ELSE_BRANCH),
    DEFAULT_BRANCH(KW_DEFAULT_BRANCH),
    CASE_BRANCH(KW_CASE_BRANCH),
    CATCH(KW_CATCH),
    CO_CATCH(KW_CO_CATCH),
    LIST(KW_LIST),
    ITEM(KW_ITEM),
    TABLE(KW_TABLE),
    ROW(KW_ROW),
    CELL(KW_CELL),
    IMAGE(KW_IMAGE),
    TEXT(KW_TEXT),
    ORDERED(KW_ORDERED),
    FLAT(KW_FLAT),
    COLS(KW_COLS),
    WIDTH(KW_WIDTH),
    ZOOM(KW_ZOOM),
    BAR_WIDTH(KW_BAR_WIDTH),
    IF_RATIO(KW_IF_RATIO),
    EMPTY_WIDTH(KW_EMPTY_WIDTH),
    PLAIN(KW_PLAIN),
    MARKUPS(KW_MARKUPS),
    SCALE(KW_SCALE),
    TYPE(KW_TYPE),
    CHANGE(KW_CHANGE),
    CHANGE_MODE(KW_CHANGE_MODE),
    CHANGESET_NAME(KW_CHANGESET_NAME),
    PDF_OPTIONS(KW_PDF_OPTIONS),
    FILENAME(KW_FILENAME),
    MODEL_FILENAME(KW_MODEL_FILENAME),
    PAGE_SIZE(KW_PAGE_SIZE),
    PORTRAIT(KW_PORTRAIT),
    PAGING(KW_PAGING);

    private final String keyword;

    ModelKeyword_V002(int tokenType) {
        this.keyword = VOCABULARY.getLiteralName(tokenType).replace("'", "");
    }

    @Override
    public String toString() {
        return keyword;
    }
}
