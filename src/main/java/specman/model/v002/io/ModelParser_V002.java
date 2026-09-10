package specman.model.v002.io;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.TerminalNode;
import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.editarea.markups.MarkupType;
import specman.model.v002.AbstractEditAreaModel_V002;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.BranchSequenceModel_V002;
import specman.model.v002.BreakStepModel_V002;
import specman.model.v002.CaseStepModel_V002;
import specman.model.v002.CatchAreaModel_V002;
import specman.model.v002.CatchSequenceModel_V002;
import specman.model.v002.CoCatchModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.DoWhileStepModel_V002;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.IfElseStepModel_V002;
import specman.model.v002.IfStepModel_V002;
import specman.model.v002.ImageEditAreaModel_V002;
import specman.model.v002.ListItemEditAreaModel_V002;
import specman.model.v002.Markup_V002;
import specman.model.v002.PdfExportOptionsModel_V002;
import specman.model.v002.SimpleStepModel_V002;
import specman.model.v002.SpecmanModel_V002Lexer;
import specman.model.v002.SpecmanModel_V002Parser;
import specman.model.v002.StepSequenceModel_V002;
import specman.model.v002.SubsequenceStepModel_V002;
import specman.model.v002.TableEditAreaModel_V002;
import specman.model.v002.TextEditAreaModel_V002;
import specman.model.v002.WhileStepModel_V002;
import specman.view.RoundedBorderDecorationStyle;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a SpecmanModel_V002 text file into a {@link DiagramModel_V002}. */
public class ModelParser_V002 {

    /** Parses SpecmanModel_V002 text content and returns the diagram model.
     *  @throws ModelParseException if the content contains syntax errors. */
    public DiagramModel_V002 parse(String content) throws ModelParseException {
        String name = extractName(content);

        SpecmanModel_V002Lexer lexer = new SpecmanModel_V002Lexer(CharStreams.fromString(content));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SpecmanModel_V002Parser parser = new SpecmanModel_V002Parser(tokens);

        List<String> errors = new ArrayList<>();
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                errors.add("line " + line + ":" + charPositionInLine + " " + msg);
            }
        });

        SpecmanModel_V002Parser.DiagramContext ctx = parser.diagram();
        if (!errors.isEmpty()) {
            throw new ModelParseException(errors.get(0));
        }
        return buildDiagram(ctx, name);
    }

    // -----------------------------------------------------------------------
    // Name extraction (from comment line, before ANTLR strips comments)
    // -----------------------------------------------------------------------

    private String extractName(String content) {
        Matcher m = Pattern.compile("//\\s*Specman:\\s*(.+)").matcher(content);
        return m.find() ? m.group(1).trim() : "frame0";
    }

    // -----------------------------------------------------------------------
    // Diagram
    // -----------------------------------------------------------------------

    private DiagramModel_V002 buildDiagram(SpecmanModel_V002Parser.DiagramContext ctx, String name) {
        Settings s = buildSettings(ctx.settings());
        EditorContentModel_V002 intro = ctx.intro() != null
            ? buildEditContainer(ctx.intro().editContainerHead(), ctx.intro().editContainerTail())
            : null;
        EditorContentModel_V002 outro = ctx.outro() != null
            ? buildEditContainer(ctx.outro().editContainerHead(), ctx.outro().editContainerTail())
            : null;
        StepSequenceModel_V002 mainSeq = buildMainSequence(ctx.mainSequence());
        return new DiagramModel_V002(name, s.width, s.zoomFactor, s.changeModeEnabled, mainSeq, intro, outro, s.pdfOptions, s.changeSetName);
    }

    private static class Settings {
        int width = 700;
        int zoomFactor = 100;
        boolean changeModeEnabled = false;
        String changeSetName = "yellow";
        PdfExportOptionsModel_V002 pdfOptions = null;
    }

    private Settings buildSettings(SpecmanModel_V002Parser.SettingsContext ctx) {
        Settings s = new Settings();
        if (ctx == null) {
            return s;
        }
        for (SpecmanModel_V002Parser.SettingEntryContext entry : ctx.settingEntry()) {
            String keyword = entry.start.getText();
            if ("width".equals(keyword)) {
                s.width = Integer.parseInt(entry.STEP_NUM().getText());
            } else if ("zoom".equals(keyword)) {
                s.zoomFactor = Integer.parseInt(entry.STEP_NUM().getText());
            } else if ("changeModeEnabled".equals(keyword)) {
                s.changeModeEnabled = Boolean.parseBoolean(entry.boolVal().getText());
            } else if ("changeSetName".equals(keyword)) {
                s.changeSetName = entry.ID().getText();
            } else if ("pdfOptions".equals(keyword)) {
                s.pdfOptions = buildPdfOptions(entry.pdfOptionEntry());
            }
        }
        return s;
    }

    private PdfExportOptionsModel_V002 buildPdfOptions(List<SpecmanModel_V002Parser.PdfOptionEntryContext> entries) {
        String filename = null;
        String modelFilename = null;
        String pageSize = null;
        boolean portrait = true;
        boolean paging = false;
        for (SpecmanModel_V002Parser.PdfOptionEntryContext entry : entries) {
            String keyword = entry.start.getText();
            if ("filename".equals(keyword)) {
                filename = stripBackticks(entry.BACKTICK_STRING().getText());
            } else if ("modelFilename".equals(keyword)) {
                modelFilename = stripBackticks(entry.BACKTICK_STRING().getText());
            } else if ("pageSize".equals(keyword)) {
                pageSize = entry.ID().getText();
            } else if ("portrait".equals(keyword)) {
                portrait = Boolean.parseBoolean(entry.boolVal().getText());
            } else if ("paging".equals(keyword)) {
                paging = Boolean.parseBoolean(entry.boolVal().getText());
            }
        }
        return new PdfExportOptionsModel_V002(filename, modelFilename, pageSize, portrait, paging);
    }

    // -----------------------------------------------------------------------
    // Sequences
    // -----------------------------------------------------------------------

    private StepSequenceModel_V002 buildMainSequence(SpecmanModel_V002Parser.MainSequenceContext ctx) {
        // Use deep collection so top-level catches can reference break steps nested anywhere
        Map<String, String> breakStepIds = collectBreakStepIdsDeep(ctx.step());
        StepSequenceModel_V002 seq = new StepSequenceModel_V002(
            AbstractStepModel_V002.generateId(),
            ChangeInfo.untracked(),
            buildCatchArea(ctx.catchBlock(), breakStepIds));
        for (SpecmanModel_V002Parser.StepContext s : ctx.step()) {
            seq.steps.add(buildStep(s, breakStepIds));
        }
        return seq;
    }

    /** Collects stepNum → stepId for all break steps, searching recursively through nested steps. */
    private Map<String, String> collectBreakStepIdsDeep(List<SpecmanModel_V002Parser.StepContext> steps) {
        Map<String, String> map = new LinkedHashMap<>();
        collectBreakStepIdsDeepInto(steps, map);
        return map;
    }

    private void collectBreakStepIdsDeepInto(List<SpecmanModel_V002Parser.StepContext> steps,
                                               Map<String, String> map) {
        for (SpecmanModel_V002Parser.StepContext s : steps) {
            if (s.breakStep() != null) {
                map.put(s.breakStep().stepNum().getText(), s.breakStep().stepId().getText());
            }
            // Recurse into structured steps
            if (s.whileStep() != null) {
                collectBreakStepIdsDeepInto(s.whileStep().step(), map);
            }
            if (s.doWhileStep() != null) {
                collectBreakStepIdsDeepInto(s.doWhileStep().step(), map);
            }
            if (s.ifElseStep() != null) {
                collectBreakStepIdsDeepInto(s.ifElseStep().ifBranch().step(), map);
                collectBreakStepIdsDeepInto(s.ifElseStep().elseBranch().step(), map);
            }
            if (s.ifStep() != null) {
                collectBreakStepIdsDeepInto(s.ifStep().ifBranch().step(), map);
            }
            if (s.caseStep() != null) {
                collectBreakStepIdsDeepInto(s.caseStep().defaultBranch().step(), map);
                for (SpecmanModel_V002Parser.CaseBranchContext cb : s.caseStep().caseBranch()) {
                    collectBreakStepIdsDeepInto(cb.step(), map);
                }
            }
            if (s.subsequenceStep() != null) {
                collectBreakStepIdsDeepInto(s.subsequenceStep().step(), map);
            }
        }
    }

    /** Collects stepNum → stepId (from text) for all break steps in the list. */
    private Map<String, String> collectBreakStepIds(List<SpecmanModel_V002Parser.StepContext> steps) {
        Map<String, String> map = new LinkedHashMap<>();
        for (SpecmanModel_V002Parser.StepContext s : steps) {
            if (s.breakStep() != null) {
                map.put(s.breakStep().stepNum().getText(), s.breakStep().stepId().getText());
            }
        }
        return map;
    }

    private CatchAreaModel_V002 buildCatchArea(List<SpecmanModel_V002Parser.CatchBlockContext> catches,
                                               Map<String, String> breakStepIds) {
        CatchAreaModel_V002 area = new CatchAreaModel_V002(null, false);
        for (SpecmanModel_V002Parser.CatchBlockContext c : catches) {
            area.catchSequences.add(buildCatchSequence(c, breakStepIds));
        }
        return area;
    }

    private CatchSequenceModel_V002 buildCatchSequence(SpecmanModel_V002Parser.CatchBlockContext ctx,
                                                       Map<String, String> breakStepIds) {
        String breakNum = ctx.stepNum().getText();
        String breakId = breakStepIds.getOrDefault(breakNum, AbstractStepModel_V002.generateId());
        EditorContentModel_V002 heading = buildEditContainer(ctx.editContainerHead(), ctx.editContainerTail());
        List<CoCatchModel_V002> coCatches = new ArrayList<>();
        for (SpecmanModel_V002Parser.CoCatchContext cc : ctx.coCatch()) {
            String ccNum = cc.stepNum().getText();
            String ccId = breakStepIds.getOrDefault(ccNum, AbstractStepModel_V002.generateId());
            coCatches.add(new CoCatchModel_V002(ccId,
                buildEditContainer(cc.editContainerHead(), null),
                ChangeInfo.untracked()));
        }
        CatchSequenceModel_V002 catchSeq = new CatchSequenceModel_V002(
            breakId, ChangeInfo.untracked(), heading, coCatches, 18);
        for (SpecmanModel_V002Parser.StepContext s : ctx.step()) {
            catchSeq.steps.add(buildStep(s, new LinkedHashMap<>()));
        }
        return catchSeq;
    }

    private BranchSequenceModel_V002 buildBranch(SpecmanModel_V002Parser.EditContainerHeadContext headCtx,
                                                  SpecmanModel_V002Parser.EditContainerTailContext tailCtx,
                                                  List<SpecmanModel_V002Parser.StepContext> steps,
                                                  List<SpecmanModel_V002Parser.CatchBlockContext> catches) {
        EditorContentModel_V002 heading = buildEditContainer(headCtx, tailCtx);
        Map<String, String> breakStepIds = collectBreakStepIdsDeep(steps);
        BranchSequenceModel_V002 seq = new BranchSequenceModel_V002(
            AbstractStepModel_V002.generateId(),
            ChangeInfo.untracked(),
            buildCatchArea(catches, breakStepIds),
            heading);
        for (SpecmanModel_V002Parser.StepContext s : steps) {
            seq.steps.add(buildStep(s, breakStepIds));
        }
        return seq;
    }

    private StepSequenceModel_V002 buildLoopSequence(List<SpecmanModel_V002Parser.StepContext> steps,
                                                      List<SpecmanModel_V002Parser.CatchBlockContext> catches) {
        Map<String, String> breakStepIds = collectBreakStepIdsDeep(steps);
        StepSequenceModel_V002 seq = new StepSequenceModel_V002(
            AbstractStepModel_V002.generateId(),
            ChangeInfo.untracked(),
            buildCatchArea(catches, breakStepIds));
        for (SpecmanModel_V002Parser.StepContext s : steps) {
            seq.steps.add(buildStep(s, breakStepIds));
        }
        return seq;
    }

    // -----------------------------------------------------------------------
    // Steps
    // -----------------------------------------------------------------------

    private AbstractStepModel_V002 buildStep(SpecmanModel_V002Parser.StepContext ctx,
                                              Map<String, String> breakStepIds) {
        if (ctx.simpleStep() != null)     { return buildSimpleStep(ctx.simpleStep()); }
        if (ctx.breakStep() != null)      { return buildBreakStep(ctx.breakStep(), breakStepIds); }
        if (ctx.whileStep() != null)      { return buildWhileStep(ctx.whileStep()); }
        if (ctx.doWhileStep() != null)    { return buildDoWhileStep(ctx.doWhileStep()); }
        if (ctx.ifElseStep() != null)     { return buildIfElseStep(ctx.ifElseStep()); }
        if (ctx.ifStep() != null)         { return buildIfStep(ctx.ifStep()); }
        if (ctx.caseStep() != null)       { return buildCaseStep(ctx.caseStep()); }
        if (ctx.subsequenceStep() != null){ return buildSubsequenceStep(ctx.subsequenceStep()); }
        throw new IllegalStateException("Unknown step type in context: " + ctx.getText());
    }

    private SimpleStepModel_V002 buildSimpleStep(SpecmanModel_V002Parser.SimpleStepContext ctx) {
        return new SimpleStepModel_V002(
            ctx.stepId().getText(),
            ctx.stepNum().getText(),
            buildStepContent(ctx.editContainerHead(), ctx.editContainerTail()),
            -1,
            buildChangeInfo(ctx.changeParam()),
            null,
            RoundedBorderDecorationStyle.None);
    }

    private BreakStepModel_V002 buildBreakStep(SpecmanModel_V002Parser.BreakStepContext ctx,
                                               Map<String, String> breakStepIds) {
        String stepNum = ctx.stepNum().getText();
        // Use the ID recorded during collectBreakStepIds so catches can reference it.
        String id = breakStepIds.getOrDefault(stepNum, ctx.stepId().getText());
        return new BreakStepModel_V002(
            id,
            stepNum,
            buildStepContent(ctx.editContainerHead(), ctx.editContainerTail()),
            -1,
            buildChangeInfo(ctx.changeParam()),
            null,
            RoundedBorderDecorationStyle.None);
    }

    private WhileStepModel_V002 buildWhileStep(SpecmanModel_V002Parser.WhileStepContext ctx) {
        org.antlr.v4.runtime.tree.TerminalNode barWidthNode = ctx.STEP_NUM();
        int barWidth = barWidthNode == null ? 18 : Integer.parseInt(barWidthNode.getText());
        return new WhileStepModel_V002(
            ctx.stepId().getText(),
            ctx.stepNum().getText(),
            buildStepContent(ctx.editContainerHead(), ctx.editContainerTail()),
            -1,
            ChangeInfo.untracked(),
            false,
            buildLoopSequence(ctx.step(), ctx.catchBlock()),
            barWidth,
            null,
            RoundedBorderDecorationStyle.None);
    }

    private DoWhileStepModel_V002 buildDoWhileStep(SpecmanModel_V002Parser.DoWhileStepContext ctx) {
        org.antlr.v4.runtime.tree.TerminalNode barWidthNode = ctx.STEP_NUM();
        int barWidth = barWidthNode == null ? 18 : Integer.parseInt(barWidthNode.getText());
        return new DoWhileStepModel_V002(
            ctx.stepId().getText(),
            ctx.stepNum().getText(),
            buildStepContent(ctx.editContainerHead(), ctx.editContainerTail()),
            -1,
            ChangeInfo.untracked(),
            false,
            buildLoopSequence(ctx.step(), List.of()),
            barWidth,
            null,
            RoundedBorderDecorationStyle.None);
    }

    private IfElseStepModel_V002 buildIfElseStep(SpecmanModel_V002Parser.IfElseStepContext ctx) {
        SpecmanModel_V002Parser.IfBranchContext ifCtx = ctx.ifBranch();
        SpecmanModel_V002Parser.ElseBranchContext elseCtx = ctx.elseBranch();
        BranchSequenceModel_V002 ifSeq = buildBranch(
            ifCtx.editContainerHead(), ifCtx.editContainerTail(), ifCtx.step(), ifCtx.catchBlock());
        BranchSequenceModel_V002 elseSeq = buildBranch(
            elseCtx.editContainerHead(), elseCtx.editContainerTail(), elseCtx.step(), elseCtx.catchBlock());
        org.antlr.v4.runtime.tree.TerminalNode percentNode = ctx.PERCENT();
        float ifWidthRatio = percentNode == null ? 0.5f
            : Float.parseFloat(percentNode.getText().replace("%", "")) / 100.0f;        return new IfElseStepModel_V002(
            ctx.stepId().getText(),
            ctx.stepNum().getText(),
            buildStepContent(ctx.editContainerHead(), ctx.editContainerTail()),
            -1,
            RoundedBorderDecorationStyle.None,
            false,
            ChangeInfo.untracked(),
            ifSeq,
            elseSeq,
            ifWidthRatio,
            null);
    }

    private IfStepModel_V002 buildIfStep(SpecmanModel_V002Parser.IfStepContext ctx) {
        SpecmanModel_V002Parser.IfBranchContext ifCtx = ctx.ifBranch();
        BranchSequenceModel_V002 ifSeq = buildBranch(
            ifCtx.editContainerHead(), ifCtx.editContainerTail(), ifCtx.step(), ifCtx.catchBlock());
        org.antlr.v4.runtime.tree.TerminalNode emptyWidthNode = ctx.STEP_NUM();
        int emptyWidth = emptyWidthNode == null ? 20 : Integer.parseInt(emptyWidthNode.getText());
        return new IfStepModel_V002(
            ctx.stepId().getText(),
            ctx.stepNum().getText(),
            buildStepContent(ctx.editContainerHead(), ctx.editContainerTail()),
            -1,
            RoundedBorderDecorationStyle.None,
            false,
            ChangeInfo.untracked(),
            ifSeq,
            emptyWidth,
            null);
    }

    private CaseStepModel_V002 buildCaseStep(SpecmanModel_V002Parser.CaseStepContext ctx) {
        SpecmanModel_V002Parser.DefaultBranchContext defCtx = ctx.defaultBranch();
        BranchSequenceModel_V002 defaultSeq = buildBranch(
            defCtx.editContainerHead(), defCtx.editContainerTail(), defCtx.step(), List.of());
        List<Float> columnWidthRatios;
        List<org.antlr.v4.runtime.tree.TerminalNode> percents = ctx.PERCENT();
        if (!percents.isEmpty()) {
            columnWidthRatios = new ArrayList<>();
            for (org.antlr.v4.runtime.tree.TerminalNode p : percents) {
                float val = Float.parseFloat(p.getText().replace("%", "")) / 100.0f;
                columnWidthRatios.add(val);
            }
        } else {
            int numBranches = 1 + ctx.caseBranch().size();
            float equalShare = 1.0f / numBranches;
            columnWidthRatios = new ArrayList<>();
            for (int i = 0; i < numBranches; i++) {
                columnWidthRatios.add(equalShare);
            }
        }
        CaseStepModel_V002 step = new CaseStepModel_V002(
            ctx.stepId().getText(),
            ctx.stepNum().getText(),
            buildStepContent(ctx.editContainerHead(), ctx.editContainerTail()),
            -1,
            ChangeInfo.untracked(),
            false,
            defaultSeq,
            columnWidthRatios,
            null,
            RoundedBorderDecorationStyle.None);
        for (SpecmanModel_V002Parser.CaseBranchContext cb : ctx.caseBranch()) {
            step.addCase(buildBranch(cb.editContainerHead(), cb.editContainerTail(), cb.step(), List.of()));
        }
        return step;
    }

    private SubsequenceStepModel_V002 buildSubsequenceStep(SpecmanModel_V002Parser.SubsequenceStepContext ctx) {
        boolean flat = ctx.KW_FLAT() != null;
        return new SubsequenceStepModel_V002(
            ctx.stepId().getText(),
            ctx.stepNum().getText(),
            buildStepContent(ctx.editContainerHead(), ctx.editContainerTail()),
            -1,
            ChangeInfo.untracked(),
            false,
            buildLoopSequence(ctx.step(), ctx.catchBlock()),
            null,
            RoundedBorderDecorationStyle.None,
            flat);
    }

    // -----------------------------------------------------------------------
    // EditContainer
    // -----------------------------------------------------------------------

    private EditorContentModel_V002 buildStepContent(SpecmanModel_V002Parser.EditContainerHeadContext head,
                                                      SpecmanModel_V002Parser.EditContainerTailContext tail) {
        return buildEditContainer(head, tail);
    }

    private EditorContentModel_V002 buildEditContainer(SpecmanModel_V002Parser.EditContainerHeadContext head,
                                                        SpecmanModel_V002Parser.EditContainerTailContext tail) {
        EditorContentModel_V002 content = new EditorContentModel_V002();
        content.areas.add(buildText(head.htmlContent()));
        if (tail != null) {
            for (SpecmanModel_V002Parser.AreaContext a : tail.area()) {
                content.areas.addAll(buildAreas(a));
            }
        }
        return content;
    }

    // -----------------------------------------------------------------------
    // Areas
    // -----------------------------------------------------------------------

    private List<AbstractEditAreaModel_V002> buildAreas(SpecmanModel_V002Parser.AreaContext ctx) {
        List<AbstractEditAreaModel_V002> result = new ArrayList<>();
        if (ctx.listBlock() != null) {
            boolean ordered = Boolean.parseBoolean(ctx.listBlock().boolVal().getText());
            for (SpecmanModel_V002Parser.ListItemContext item : ctx.listBlock().listItem()) {
                EditorContentModel_V002 itemContent = new EditorContentModel_V002();
                itemContent.areas.add(buildText(item.editContainerHead().htmlContent()));
                if (item.listItemTail() != null) {
                    for (SpecmanModel_V002Parser.TextAreaContext ta : item.listItemTail().textArea()) {
                        itemContent.areas.add(buildText(ta.htmlContent()));
                    }
                    for (SpecmanModel_V002Parser.TableBlockContext tb : item.listItemTail().tableBlock()) {
                        itemContent.areas.add(buildTable(tb));
                    }
                    for (SpecmanModel_V002Parser.ImageBlockContext ib : item.listItemTail().imageBlock()) {
                        itemContent.areas.add(buildImage(ib));
                    }
                }
                result.add(new ListItemEditAreaModel_V002(itemContent, ordered, buildChangeInfo(item.changeParam())));
            }
        } else if (ctx.textArea() != null) {
            result.add(buildText(ctx.textArea().htmlContent()));
        } else if (ctx.tableBlock() != null) {
            result.add(buildTable(ctx.tableBlock()));
        } else if (ctx.imageBlock() != null) {
            result.add(buildImage(ctx.imageBlock()));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Text / Markups
    // -----------------------------------------------------------------------

    private TextEditAreaModel_V002 buildText(SpecmanModel_V002Parser.HtmlContentContext ctx) {
        String html = stripBackticks(ctx.BACKTICK_STRING().getText());
        String plain = ctx.plainAttr() != null
            ? stripBackticks(ctx.plainAttr().BACKTICK_STRING().getText())
            : "";
        List<Markup_V002> markups = ctx.markupsAttr() != null
            ? buildMarkups(ctx.markupsAttr())
            : new ArrayList<>();
        return new TextEditAreaModel_V002(html, plain, markups, (ChangeInfo) null);
    }

    private List<Markup_V002> buildMarkups(SpecmanModel_V002Parser.MarkupsAttrContext ctx) {
        List<Markup_V002> result = new ArrayList<>();
        for (SpecmanModel_V002Parser.MarkupContext m : ctx.markup()) {
            int from = Integer.parseInt(m.STEP_NUM(0).getText());
            int to = Integer.parseInt(m.STEP_NUM(1).getText());
            MarkupType type = MarkupType.valueOf(m.markupType().getText());
            String changeset = m.ID() != null ? m.ID().getText() : null;
            result.add(new Markup_V002(from, to, type, changeset));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Table
    // -----------------------------------------------------------------------

    private TableEditAreaModel_V002 buildTable(SpecmanModel_V002Parser.TableBlockContext ctx) {
        List<TerminalNode> percents = ctx.PERCENT();
        int width = parsePercent(percents.get(0).getText());
        List<Integer> colWidths = null;
        if (percents.size() > 1) {
            colWidths = new ArrayList<>();
            for (int i = 1; i < percents.size(); i++) {
                colWidths.add(parsePercent(percents.get(i).getText()));
            }
        }
        List<List<EditorContentModel_V002>> cells = new ArrayList<>();
        for (SpecmanModel_V002Parser.TableRowContext row : ctx.tableRow()) {
            List<EditorContentModel_V002> rowCells = new ArrayList<>();
            for (SpecmanModel_V002Parser.TableCellContext cell : row.tableCell()) {
                EditorContentModel_V002 cellContent;
                if (cell.editContainerTail() != null) {
                    cellContent = buildEditContainer(cell.editContainerHead(), cell.editContainerTail());
                } else {
                    cellContent = buildEditContainer(cell.editContainerHead(), null);
                }
                rowCells.add(cellContent);
            }
            cells.add(rowCells);
        }
        return new TableEditAreaModel_V002(cells, width, colWidths, (ChangeInfo) null);
    }

    // -----------------------------------------------------------------------
    // Image
    // -----------------------------------------------------------------------

    private ImageEditAreaModel_V002 buildImage(SpecmanModel_V002Parser.ImageBlockContext ctx) {
        float scale = parseScalePercent(ctx.PERCENT().getText());
        String type = ctx.ID().getText();
        String base64Block = ctx.BASE64_BLOCK().getText(); // "[base64:...]"
        String base64Data = base64Block
            .replaceFirst("^\\[base64:", "")
            .replaceFirst("\\]$", "");
        byte[] imageData = base64Data.isEmpty()
            ? new byte[0]
            : Base64.getDecoder().decode(base64Data);
        return new ImageEditAreaModel_V002(imageData, type, scale, (ChangeInfo) null);
    }

    // -----------------------------------------------------------------------
    // ChangeInfo
    // -----------------------------------------------------------------------

    private ChangeInfo buildChangeInfo(SpecmanModel_V002Parser.ChangeParamContext ctx) {
        if (ctx == null) {
            return ChangeInfo.untracked();
        }
        String type = ctx.changeType().getText();
        String changesetName = ctx.ID().getText();
        Aenderungsart art = switch (type) {
            case "added"   -> Aenderungsart.Hinzugefuegt;
            case "removed" -> Aenderungsart.Geloescht;
            default        -> Aenderungsart.Untracked;
        };
        if (art == Aenderungsart.Untracked) {
            return ChangeInfo.untracked();
        }
        ChangeSet cs = ChangeSet.fromName(changesetName);
        if (cs == null) {
            cs = ChangeSet.changeset();
        }
        return new ChangeInfo(art, cs);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String stripBackticks(String s) {
        if (s != null && s.length() >= 2 && s.charAt(0) == '`' && s.charAt(s.length() - 1) == '`') {
            return s.substring(1, s.length() - 1);
        }
        return s != null ? s : "";
    }

    private int parsePercent(String s) {
        return (int) Math.round(Double.parseDouble(s.replace("%", "").replace(",", ".")));
    }

    private float parseScalePercent(String s) {
        return (float) (Double.parseDouble(s.replace("%", "").replace(",", ".")) / 100.0);
    }
}
