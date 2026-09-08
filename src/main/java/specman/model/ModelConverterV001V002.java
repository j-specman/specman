package specman.model;

import specman.model.v001.*;
import specman.model.v002.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelConverterV001V002 {

    public static DiagramModel_V002 convert(StruktogrammModel_V001 v1) {
        Map<String, String> stepIdMap = new HashMap<>();
        buildStepIdMap(v1.hauptSequenz, stepIdMap);
        return new DiagramModel_V002(
            v1.name,
            v1.breite,
            v1.zoomFaktor,
            v1.changeModeenabled,
            convertSequence(v1.hauptSequenz, stepIdMap),
            convertContent(v1.intro),
            convertContent(v1.outro),
            convertPdfOptions(v1.pdfExportOptions),
            v1.changeSetName
        );
    }

    // First pass: walk the entire step tree and assign an 8-char hex ID to every step,
    // keyed by SchrittID.toString(). Catch sequences share the ID of their
    // linked break step (same SchrittID), so they get the same ID here too.
    private static void buildStepIdMap(SchrittSequenzModel_V001 seq, Map<String, String> map) {
        if (seq == null || seq.schritte == null) {
            return;
        }
        for (AbstractSchrittModel_V001 step : seq.schritte) {
            if (step.id != null && !step.id.numbers.isEmpty()) {
                map.put(step.id.toString(), AbstractStepModel_V002.generateId());
            }
            buildSubSequenceIds(step, map);
        }
        buildCatchSequenceIds(seq, map);
    }

    private static void buildSubSequenceIds(AbstractSchrittModel_V001 step, Map<String, String> map) {
        if (step instanceof IfElseSchrittModel_V001) {
            IfElseSchrittModel_V001 s = (IfElseSchrittModel_V001) step;
            buildStepIdMap(s.ifSequenz, map);
            buildStepIdMap(s.elseSequenz, map);
        } else if (step instanceof IfSchrittModel_V001) {
            IfSchrittModel_V001 s = (IfSchrittModel_V001) step;
            buildStepIdMap(s.ifSequenz, map);
        } else if (step instanceof CaseSchrittModel_V001) {
            CaseSchrittModel_V001 s = (CaseSchrittModel_V001) step;
            buildStepIdMap(s.sonstSequenz, map);
            if (s.caseSequenzen != null) {
                s.caseSequenzen.forEach(cs -> buildStepIdMap(cs, map));
            }
        } else if (step instanceof WhileWhileSchrittModel_V001) {
            buildStepIdMap(((WhileSchrittModel_V001) step).wiederholSequenz, map);
        } else if (step instanceof WhileSchrittModel_V001) {
            buildStepIdMap(((WhileSchrittModel_V001) step).wiederholSequenz, map);
        } else if (step instanceof SubsequenzSchrittModel_V001) {
            buildStepIdMap(((SubsequenzSchrittModel_V001) step).subsequenz, map);
        }
    }

    private static void buildCatchSequenceIds(SchrittSequenzModel_V001 seq, Map<String, String> map) {
        if (seq.catchBereich == null || seq.catchBereich.catchSequences == null) {
            return;
        }
        for (CatchSchrittSequenzModel_V001 catchSeq : seq.catchBereich.catchSequences) {
            // Catch sequence id equals its linked break step's id — reuse that ID
            if (catchSeq.id != null && !catchSeq.id.numbers.isEmpty()) {
                map.computeIfAbsent(catchSeq.id.toString(), k -> AbstractStepModel_V002.generateId());
            }
            buildStepIdMap(catchSeq, map);
        }
    }

    // Second pass: convert using the pre-built ID map

    private static StepSequenceModel_V002 convertSequence(SchrittSequenzModel_V001 v1, Map<String, String> map) {
        if (v1 == null) {
            return null;
        }
        StepSequenceModel_V002 v2 = new StepSequenceModel_V002(
            AbstractStepModel_V002.generateId(),
            v1.changeInfo != null ? v1.changeInfo.toChangeInfo() : null,
            convertCatchArea(v1.catchBereich, map)
        );
        if (v1.schritte != null) {
            v1.schritte.forEach(s -> v2.steps.add(convertStep(s, map)));
        }
        return v2;
    }

    private static BranchSequenceModel_V002 convertBranchSequence(ZweigSchrittSequenzModel_V001 v1, Map<String, String> map) {
        if (v1 == null) {
            return null;
        }
        BranchSequenceModel_V002 v2 = new BranchSequenceModel_V002(
            AbstractStepModel_V002.generateId(),
            v1.changeInfo != null ? v1.changeInfo.toChangeInfo() : null,
            convertCatchArea(v1.catchBereich, map),
            convertContent(v1.ueberschrift)
        );
        if (v1.schritte != null) {
            v1.schritte.forEach(s -> v2.steps.add(convertStep(s, map)));
        }
        return v2;
    }

    private static CatchSequenceModel_V002 convertCatchSequence(CatchSchrittSequenzModel_V001 v1, Map<String, String> map) {
        if (v1 == null) {
            return null;
        }
        // Use the same ID as the linked break step so the view can find the break step by ID
        String id = v1.id != null
            ? map.getOrDefault(v1.id.toString(), AbstractStepModel_V002.generateId())
            : AbstractStepModel_V002.generateId();
        List<CoCatchModel_V002> coCatches = new ArrayList<>();
        if (v1.coCatches != null) {
            v1.coCatches.forEach(cc -> coCatches.add(convertCoCatch(cc, map)));
        }
        CatchSequenceModel_V002 v2 = new CatchSequenceModel_V002(
            id,
            v1.changeInfo != null ? v1.changeInfo.toChangeInfo() : null,
            convertContent(v1.ueberschrift),
            coCatches,
            v1.headingRightBarWidth
        );
        if (v1.schritte != null) {
            v1.schritte.forEach(s -> v2.steps.add(convertStep(s, map)));
        }
        return v2;
    }

    private static CatchAreaModel_V002 convertCatchArea(CatchBereichModel_V001 v1, Map<String, String> map) {
        if (v1 == null) {
            return null;
        }
        CatchAreaModel_V002 v2 = new CatchAreaModel_V002(v1.sequencesWidthPercent, v1.zugeklappt);
        if (v1.catchSequences != null) {
            v1.catchSequences.forEach(cs -> v2.catchSequences.add(convertCatchSequence(cs, map)));
        }
        return v2;
    }

    private static CoCatchModel_V002 convertCoCatch(CoCatchModel_V001 v1, Map<String, String> map) {
        String breakStepId = v1.breakStepId != null
            ? map.getOrDefault(v1.breakStepId.toString(), AbstractStepModel_V002.generateId())
            : null;
        return new CoCatchModel_V002(
            breakStepId,
            convertContent(v1.heading),
            v1.changeInfo != null ? v1.changeInfo.toChangeInfo() : null
        );
    }

    public static AbstractStepModel_V002 convertStep(AbstractSchrittModel_V001 v1, Map<String, String> map) {
        String id = v1.id != null
            ? map.getOrDefault(v1.id.toString(), AbstractStepModel_V002.generateId())
            : AbstractStepModel_V002.generateId();
        if (v1 instanceof WhileWhileSchrittModel_V001) {
            WhileWhileSchrittModel_V001 s = (WhileWhileSchrittModel_V001) v1;
            return new DoWhileStepModel_V002(id, null, convertContent(s.inhalt), s.farbe,
                changeInfo(s), s.zugeklappt,
                convertSequence(s.wiederholSequenz, map), s.balkenbreite, null, s.decorationStyle);
        }
        if (v1 instanceof WhileSchrittModel_V001) {
            WhileSchrittModel_V001 s = (WhileSchrittModel_V001) v1;
            return new WhileStepModel_V002(id, null, convertContent(s.inhalt), s.farbe,
                changeInfo(s), s.zugeklappt,
                convertSequence(s.wiederholSequenz, map), s.balkenbreite, null, s.decorationStyle);
        }
        if (v1 instanceof IfElseSchrittModel_V001) {
            IfElseSchrittModel_V001 s = (IfElseSchrittModel_V001) v1;
            return new IfElseStepModel_V002(id, null, convertContent(s.inhalt), s.farbe, s.decorationStyle,
                s.zugeklappt, changeInfo(s),
                convertBranchSequence(s.ifSequenz, map),
                convertBranchSequence(s.elseSequenz, map),
                s.ifBreitenanteil, null);
        }
        if (v1 instanceof IfSchrittModel_V001) {
            IfSchrittModel_V001 s = (IfSchrittModel_V001) v1;
            return new IfStepModel_V002(id, null, convertContent(s.inhalt), s.farbe, s.decorationStyle,
                s.zugeklappt, changeInfo(s),
                convertBranchSequence(s.ifSequenz, map),
                s.leerBreite, null);
        }
        if (v1 instanceof CaseSchrittModel_V001) {
            CaseSchrittModel_V001 s = (CaseSchrittModel_V001) v1;
            CaseStepModel_V002 caseStep = new CaseStepModel_V002(id, null, convertContent(s.inhalt), s.farbe,
                changeInfo(s), s.zugeklappt,
                convertBranchSequence(s.sonstSequenz, map),
                s.spaltenbreitenAnteile != null ? new ArrayList<>(s.spaltenbreitenAnteile) : null,
                null, s.decorationStyle);
            if (s.caseSequenzen != null) {
                s.caseSequenzen.forEach(cs -> caseStep.addCase(convertBranchSequence(cs, map)));
            }
            return caseStep;
        }
        if (v1 instanceof SubsequenzSchrittModel_V001) {
            SubsequenzSchrittModel_V001 s = (SubsequenzSchrittModel_V001) v1;
            return new SubsequenceStepModel_V002(id, null, convertContent(s.inhalt), s.farbe,
                changeInfo(s), s.zugeklappt,
                convertSequence(s.subsequenz, map),
                null, s.decorationStyle, s.flatNumbering);
        }
        if (v1 instanceof QuellSchrittModel_V001) {
            return new SourceStepModel_V002(id, null, convertContent(v1.inhalt), v1.farbe,
                changeInfo(v1), null, v1.decorationStyle);
        }
        if (v1 instanceof BreakSchrittModel_V001) {
            return new BreakStepModel_V002(id, null, convertContent(v1.inhalt), v1.farbe,
                changeInfo(v1), null, v1.decorationStyle);
        }
        // EinfacherSchrittModel_V001 and StrukturierterSchrittModel_V001 fallback
        return new SimpleStepModel_V002(id, null, convertContent(v1.inhalt), v1.farbe,
            changeInfo(v1), null, v1.decorationStyle);
    }

    private static specman.ChangeInfo changeInfo(AbstractSchrittModel_V001 step) {
        return step.changeInfo != null ? step.changeInfo.toChangeInfo() : null;
    }

    public static EditorContentModel_V002 convertContent(EditorContentModel_V001 v1) {
        if (v1 == null) {
            return null;
        }
        if (v1.areas == null) {
            return new EditorContentModel_V002();
        }
        List<AbstractEditAreaModel_V002> areas = v1.areas.stream()
            .map(ModelConverterV001V002::convertEditArea)
            .collect(Collectors.toList());
        return new EditorContentModel_V002(areas);
    }

    private static AbstractEditAreaModel_V002 convertEditArea(AbstractEditAreaModel_V001 v1) {
        if (v1 instanceof TextEditAreaModel_V001) {
            return convertTextArea((TextEditAreaModel_V001) v1);
        }
        if (v1 instanceof ImageEditAreaModel_V001) {
            return convertImageArea((ImageEditAreaModel_V001) v1);
        }
        if (v1 instanceof TableEditAreaModel_V001) {
            return convertTableArea((TableEditAreaModel_V001) v1);
        }
        if (v1 instanceof ListItemEditAreaModel_V001) {
            return convertListItem((ListItemEditAreaModel_V001) v1);
        }
        throw new RuntimeException("Unknown edit area type: " + v1.getClass());
    }

    private static TextEditAreaModel_V002 convertTextArea(TextEditAreaModel_V001 v1) {
        List<Markup_V002> markups = v1.markups == null ? new ArrayList<>() :
            v1.markups.stream()
                .map(m -> new Markup_V002(m.getFrom(), m.getTo(), m.getType(), m.getChangeset()))
                .collect(Collectors.toList());
        return new TextEditAreaModel_V002(
            v1.text, v1.plainText, markups,
            v1.changeInfo != null ? v1.changeInfo.toChangeInfo() : null
        );
    }

    private static ImageEditAreaModel_V002 convertImageArea(ImageEditAreaModel_V001 v1) {
        return new ImageEditAreaModel_V002(
            v1.imageData, v1.imageType, v1.individualScalePercent,
            v1.changeInfo != null ? v1.changeInfo.toChangeInfo() : null
        );
    }

    private static TableEditAreaModel_V002 convertTableArea(TableEditAreaModel_V001 v1) {
        List<List<EditorContentModel_V002>> cells = new ArrayList<>();
        if (v1.cells != null) {
            for (List<EditorContentModel_V001> row : v1.cells) {
                cells.add(row.stream().map(ModelConverterV001V002::convertContent).collect(Collectors.toList()));
            }
        }
        return new TableEditAreaModel_V002(
            cells, v1.tableWidthPercent, v1.columnsWidthPercent,
            v1.changeInfo != null ? v1.changeInfo.toChangeInfo() : null
        );
    }

    private static ListItemEditAreaModel_V002 convertListItem(ListItemEditAreaModel_V001 v1) {
        return new ListItemEditAreaModel_V002(
            convertContent(v1.content), v1.ordered,
            v1.changeInfo != null ? v1.changeInfo.toChangeInfo() : null
        );
    }

    private static PdfExportOptionsModel_V002 convertPdfOptions(PDFExportOptionsModel_V001 v1) {
        if (v1 == null) {
            return null;
        }
        return new PdfExportOptionsModel_V002(v1.filename, v1.modelFilename, v1.pageSize, v1.portrait, v1.paging);
    }
}
