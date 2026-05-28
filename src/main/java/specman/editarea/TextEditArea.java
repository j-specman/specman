package specman.editarea;

import net.atlanticbb.tantlinger.ui.text.CompoundUndoManager;
import org.apache.commons.lang.StringUtils;
import specman.ChangeInfo;

import static specman.ChangeInfo.fromModel;
import specman.EditorI;
import specman.Specman;
import static specman.ChangeSet.changeset;
import specman.editarea.keylistener.TextEditAreaKeyListener;
import specman.editarea.markups.*;
import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedElement;
import specman.editarea.document.WrappedPosition;
import specman.editarea.stepnumberlabel.BreakCatchScrollMouseAdapter;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.model.v001.Markup_V001;
import specman.model.v001.GeloeschtMarkierung_V001;
import specman.model.v001.TextEditAreaModel_V001;
import specman.pdf.FormattedShapeText;
import specman.pdf.Shape;
import specman.undo.UndoableStepnumberLinkAdded;
import specman.undo.UndoableStepnumberLinkRemoved;
import specman.undo.manager.UndoRecording;
import specman.undo.props.UDBL;
import specman.view.AbstractSchrittView;
import specman.view.CatchUeberschrift;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import javax.swing.text.html.CSS;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static specman.editarea.HTMLTags.BODY_INTRO;
import static specman.editarea.HTMLTags.BODY_OUTRO;
import static specman.editarea.HTMLTags.HEAD_INTRO;
import static specman.editarea.HTMLTags.HEAD_OUTRO;
import static specman.editarea.HTMLTags.HTML_INTRO;
import static specman.editarea.HTMLTags.HTML_OUTRO;
import specman.ChangeSet;

import static specman.graphics.Styles.FONTSIZE;
import static specman.graphics.Styles.TEXT_BACKGROUND_COLOR_STANDARD;
import static specman.graphics.Styles.INDIKATOR_GELOESCHT_MARKIERT;
import static specman.graphics.Styles.DEFAULTFONT;
import static specman.graphics.Styles.ganzerSchrittGeloeschtStil;
import static specman.ChangeSet.STEPNUMBER_LINK_COLOR;
import static specman.editarea.markups.MarkupSearchPurpose.All;
import static specman.editarea.markups.MarkupSearchPurpose.FirstChangeOnly;
import static specman.Specman.editor;

public class TextEditArea extends JEditorPane implements EditArea<TextEditAreaModel_V001> {
    private static final String INITIAL_EMPTY_CONTENT_INDICATOR = "x";

    private WrappedElement hoveredElement;
    private ChangeInfo changeInfo;
    private TextEditAreaModel_V001 deletionBackup;

    public TextEditArea(TextEditAreaModel_V001 model, Font font) {
        this.changeInfo = fromModel(model.changeInfo, model.aenderungsart);
        String initialText = model.isEmpty() ? INITIAL_EMPTY_CONTENT_INDICATOR : model.text;
        editor().instrumentWysEditor(this, initialText, 0);
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        setFont(font);
        addKeyListener(new TextEditAreaKeyListener(this));
        addMouseListener();
        addMouseMotionListener();
        setBackground(changeInfo.panelColor());
        registerToolTipManager();
        styleChangedTextSections(model);
        if (model.isEmpty()) {
          clear();
        }
    }

    private void clear() {
      WrappedDocument wd = getWrappedDocument();
      wd.removeFrom(wd.start());
    }

    /** Produces an empty TextEditArea which by first creating it from a single letter model
     * object and removing that letter immediately afterwards. This crazy-looking procedure
     * compensates a strange default behaviour of JEditorPane: when creating it immediately
     * with empty content, the content, being interactively added later on, sometimes gets
     * wrapped into <p style=\"margin-top: 0\">...</p>. The reason is unclear and it usually
     * doesn't matter. However, it causes unintuitive behaviour when merging two text areas,
     * e.g. when dissolving an area by pressing BACKSPACE at the very beginning of a text.
     * Instead of a single flowtext, the paragraph boundary is still around as is represented
     * as a line break in the merged text. That's different from the user's expectation.
     * <p>
     * the problem can still arise by <div>s, but the compensation here, already helps a lot. */
    public TextEditArea(Font font) {
      this(new TextEditAreaModel_V001(""), font);
    }

    private void styleChangedTextSections(TextEditAreaModel_V001 model) {
        new MarkupBackgroundStyleInitializer(this, model.markups).styleChangedTextSections();
    }

    private void addMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
            if (e.isControlDown()) {
                if (stepnumberLinkStyleSet(getWrappedCaretPosition())) {
                    scrollToStepnumber();
                }
            }
            }
        });
    }

    private void addMouseMotionListener() {
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                setHandCursorWhenHoveringStepnumberLink(e);
            }
        });
    }

    /**
     * Changes the cursor when pressing the CONTROL key and hovering over a StepnumberLink.
     * <p>
     * If the text cursor resides in a different TextEditArea than the mouse cursor it's not possible to
     * correctly check both conditions, since they are processed in two different TextEditAreas.
     * So we query if the CONTROL key is currently pressed via a KeyEventDispatcher from {@link Specman}.
     */
    private void setHandCursorWhenHoveringStepnumberLink(MouseEvent e) {
        Point p = new Point(e.getX(), e.getY());
        int textPosition = viewToModel2D(p);

        hoveredElement = getWrappedDocument().getCharacterElement(textPosition);

        EditorI editor = editor();
        Cursor cursorToUse;
        if (editor.isKeyPressed(KeyEvent.VK_CONTROL) && stepnumberLinkStyleSet(hoveredElement)) {
            cursorToUse = BreakCatchScrollMouseAdapter.SCROLL_CURSOR;
        } else {
            cursorToUse = Cursor.getDefaultCursor();
        }
        editor.setCursor(cursorToUse);
    }

    private void registerToolTipManager() {
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        toolTipManager.registerComponent(this);
    }

    @Override
    public String getToolTipText() {
        if (stepnumberLinkStyleSet(hoveredElement)) {
            return BreakCatchScrollMouseAdapter.SCROLL_TOOLTIP;
        }
        return super.getToolTipText();
    }

    @Override
    public void addSchrittnummer(StepnumberLabel schrittNummer) {
        add(schrittNummer);
    }

    private void setStyleUDBL(MutableAttributeSet attr, Color backgroundColor, boolean editable) {
        getWrappedDocument().setCharacterAttributes(0, getPlainText().length(), attr, false);
        setEditableUDBL(editable);
        setBackgroundUDBL(backgroundColor);
    }

    private void setBackgroundUDBL(Color backgroundColor) { UDBL.setBackgroundUDBL(this, backgroundColor); }
    private void setEditableUDBL(boolean editable) { UDBL.setEditable(this, editable); }

    @Override
    public void setQuellStil(ChangeSet changeSet) {
        setStyleUDBL(changeSet.getSourceStyle(), changeSet.panelColor(), false);
    }

    @Override
    public void aenderungsmarkierungenEntfernen() {
        // Nothing to do for text areas - job is completely done in aenderungenVerwerfen/Uebernehmen
    }

    @Override
    public void setGeloeschtMarkiertStilUDBL(ChangeSet triggerSet) {
      aenderungenVerwerfen();
      setChangeInfoUDBL(changeInfo.deleted(triggerSet));
      deletionBackup = getTextWithMarkups(true);
      setStyleUDBL(ganzerSchrittGeloeschtStil, triggerSet.panelColor(), false);
    }

    @Override
    public Component asComponent() { return this; }

    public String getPlainText() {
        try {
            return getText(0, getDocument().getLength());
        } catch (BadLocationException blx) {
            blx.printStackTrace();
            return null;
        }
    }

    public TextEditAreaModel_V001 getTextWithMarkups(boolean formatierterText) {
        String text;
        java.util.List<Markup_V001> markups = null;
        if (formatierterText) {
            cleanupText();
            markups = findMarkups(All);
            text = getText();
        }
        else {
            text = getPlainText().replace("\n", " ").trim();
        }
        return new TextEditAreaModel_V001(text, getPlainText(), markups, changeInfo);
    }

    public void cleanupText() {
        MarkedCharSequence marksBackup = findMarkups();
        setText(getText());
        List<Markup_V001> recoveredChangemarks = new MarkupRecovery(getWrappedDocument(), marksBackup).recover();
        new MarkupBackgroundStyleInitializer(this, recoveredChangemarks).styleChangedTextSections();
    }

    public java.util.List<Markup_V001> findMarkups(MarkupSearchPurpose searchPurpose) {
        java.util.List<Markup_V001> ergebnis = new ArrayList<>();
        WrappedDocument doc = getWrappedDocument();
        for (WrappedElement e : doc.getRootElements()) {
            findMarkups(e, ergebnis, searchPurpose);
            if (!ergebnis.isEmpty() && searchPurpose.stopAfterFirstMatch()) {
                break;
            }
        }
        return ergebnis;
    }

    private void findMarkups(WrappedElement e, java.util.List<Markup_V001> ergebnis, MarkupSearchPurpose searchPurpose) {
        TextMarkup markup = TextMarkup.fromBackground(e);
        if (markup != null && markup.matches(searchPurpose)) {
            ergebnis.add(new Markup_V001(e.getStartOffset().toModel(), e.getEndOffset().toModel()-1, markup));
            if (searchPurpose == FirstChangeOnly) {
                return;
            }
        }
        if (ergebnis.isEmpty() || searchPurpose == All) {
            for (int i = 0; i < e.getElementCount(); i++) {
                findMarkups(e.getElement(i), ergebnis, searchPurpose);
                if (!ergebnis.isEmpty() && searchPurpose.stopAfterFirstMatch()) {
                    break;
                }
            }
        }
    }

    // TODO JL: Muss mit aenderungsmarkierungenVerwerfen zusammengelegt werden
    public int aenderungenUebernehmen() {
        WrappedDocument doc = getWrappedDocument();
        int changesMade = changeInfo.numChanges();

        List<GeloeschtMarkierung_V001> loeschungen = new ArrayList<>();
        for (WrappedElement e : doc.getRootElements()) {
            changesMade += aenderungsmarkierungenUebernehmen(e, loeschungen);
        }
        for (int i = 0; i < loeschungen.size(); i++) {
            GeloeschtMarkierung_V001 loeschung = loeschungen.get((loeschungen.size()) - 1 - i);
            try {
                WrappedPosition loeschungVon = doc.fromModel(loeschung.getVon());
                WrappedPosition loeschungBis = doc.fromModel(loeschung.getBis());
                removeTextAndUnregisterStepnumberLinks(loeschungVon, loeschungBis);
                changesMade++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return changesMade;
    }

    // TODO JL: Muss mit aenderungsmarkierungenUebernehmen zusammengelegt werden
    public int aenderungenVerwerfen() {
        int changesRejected = changeInfo.numChanges();
        if (changeInfo.isAdded()) {
            if (!areaDetachedByMerge()) {
                getParent().removeEditAreaUDBL(this);
            }
        }
        else if (changeInfo.isDeleted()) {
            if (deletionBackup != null) {
                setText(deletionBackup.text);
                setEditable(true);
            }
          deletionBackup = null;
        }
        else {
            WrappedDocument doc = getWrappedDocument();
            List<GeloeschtMarkierung_V001> loeschungen = new ArrayList<>();
            for (WrappedElement e : doc.getRootElements()) {
                changesRejected += aenderungsmarkierungenVerwerfen(e, loeschungen);
            }
            for (int i = 0; i < loeschungen.size(); i++) {
                GeloeschtMarkierung_V001 loeschung = loeschungen.get((loeschungen.size()) - 1 - i);
                try {
                    WrappedPosition loeschungVon = doc.fromModel(loeschung.getVon());
                    WrappedPosition loeschungBis = doc.fromModel(loeschung.getBis());
                    removeTextAndUnregisterStepnumberLinks(loeschungVon, loeschungBis);
                    changesRejected++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        changeInfo = ChangeInfo.untracked();
        return changesRejected;
    }

    /** If an image edit area is removed while discarding changes, this text area
     * may be the directly following one in the same edit container and may be
     * merged with another text area directly above the image area. We can detect
     * this situation by this text area being no longer attached to its parent. */
    private boolean areaDetachedByMerge() { return getParent() == null; }

    // TODO JL: Muss mit aenderungsmarkierungenVerwerfen zusammengelegt werden
    private int aenderungsmarkierungenUebernehmen(WrappedElement e, List<GeloeschtMarkierung_V001> loeschungen) {
        int changesMade = 0;

        WrappedDocument doc = e.getDocument();
        if (elementHatAenderungshintergrund(e)) {
            if (elementHatDurchgestrichenenText(e)) {
                loeschungen.add(new GeloeschtMarkierung_V001(e.getStartOffset().toModel(), e.getEndOffset().toModel()));
            } else {
                AttributeSet attribute = e.getAttributes();
                MutableAttributeSet entfaerbt = new SimpleAttributeSet();
                entfaerbt.addAttributes(attribute);
                StyleConstants.setBackground(entfaerbt, stepnumberLinkChangedStyleSet(e) ? STEPNUMBER_LINK_COLOR.color : TEXT_BACKGROUND_COLOR_STANDARD);
                doc.setCharacterAttributes(e.getStartOffset(), e.getEndOffset().distance(e.getStartOffset()), entfaerbt, true);
                changesMade++;
            }

        }

        for (int i = 0; i < e.getElementCount(); i++) {
            changesMade += aenderungsmarkierungenUebernehmen(e.getElement(i), loeschungen);
        }

        changeInfo = ChangeInfo.untracked();
        return changesMade;
    }

    // TODO JL: Muss mit aenderungsmarkierungenUebernehmen zusammengelegt werden
    private int aenderungsmarkierungenVerwerfen(
            WrappedElement e,
            List<GeloeschtMarkierung_V001> loeschungen) {
        int changesRejected = 0;

        WrappedDocument doc = getWrappedDocument();
        if (elementHatAenderungshintergrund(e)) {
            if (!elementHatDurchgestrichenenText(e)) {
                loeschungen.add(new GeloeschtMarkierung_V001(e.getStartOffset().toModel(), e.getEndOffset().toModel()));
            } else {
                AttributeSet attribute = e.getAttributes();
                MutableAttributeSet entfaerbt = new SimpleAttributeSet();
                entfaerbt.addAttributes(attribute);
                StyleConstants.setBackground(entfaerbt, stepnumberLinkChangedStyleSet(e) ? STEPNUMBER_LINK_COLOR.color : TEXT_BACKGROUND_COLOR_STANDARD);
                StyleConstants.setStrikeThrough(entfaerbt, false);
                doc.setCharacterAttributes(e.getStartOffset(), e.getEndOffset().distance(e.getStartOffset()), entfaerbt, true);
                changesRejected++;
            }

        }
        for (int i = 0; i < e.getElementCount(); i++) {
            changesRejected += aenderungsmarkierungenVerwerfen(e.getElement(i), loeschungen);
        }

        return changesRejected;
    }

    public boolean elementHatDurchgestrichenenText(WrappedElement e) {
        AttributeSet attr = e.getAttributes();
        return StyleConstants.isStrikeThrough(attr);
    }

    public boolean elementHatAenderungshintergrund(WrappedElement e) {
        return elementHatAenderungshintergrund(e, null);
    }

    public boolean elementHatAenderungshintergrund(WrappedElement e, ChangeSet cs) {
        String cssColor = getBackgroundColorFromElement(e);
        if (cs != null) {
            return cs.isAnyBackground(cssColor);
        }
        return ChangeSet.isAnyChangeSetBackground(cssColor);
    }

    public boolean aenderungsStilGesetzt() {
        StyledEditorKit k = (StyledEditorKit) getEditorKit();
        MutableAttributeSet inputAttributes = k.getInputAttributes();
        Object currentTextDecoration = inputAttributes.getAttribute(CSS.Attribute.TEXT_DECORATION);
        if (currentTextDecoration != null && currentTextDecoration.toString().equals(INDIKATOR_GELOESCHT_MARKIERT)) {
            return false;
        }
        Object currentBackgroundColorValue = inputAttributes.getAttribute(CSS.Attribute.BACKGROUND_COLOR);
        return (currentBackgroundColorValue != null && currentBackgroundColorValue.toString().equalsIgnoreCase(changeset().textHtmlColor()));
    }

    public WrappedPosition getWrappedCaretPosition() {
      int caretPosition = getCaretPosition();
      // Not so nice, but hopefully OK: There might occur the situation that a document starts with an
      // unvisible newline but the caret position is 0, so that it points to that invisible character.
      // In that case we keep this 0 rather than compensating anything. There should only bew rare cases
      // where this comes into play. The one we know of is when the user keeps pressing Backspace in a
      // text area whichj is already empty.
      return (caretPosition > 0)
        ? getWrappedDocument().fromUI(getCaretPosition())
        : getWrappedDocument().fromModel(0);
    }

    public MarkedCharSequence findMarkups() {
        MarkedCharSequence seq = new MarkedCharSequence();
        WrappedDocument doc = getWrappedDocument();
        for (WrappedPosition p = doc.fromModel(0); p.exists(); p = p.inc()) {
            MarkedChar c = new MarkedChar(doc, p);
            seq.add(c);
        }
        return seq;
    }

    /** When pressing backspace at the very beginning of a text area, we check if this indicates that the
     * edit area should be dissolved in the sense that it is removed and its content is merged with the
     * one of a preceding and / or succeeding edit area. The method returns null if the structure of edit
     * areas didn't change. */
    public EditArea dissolveEditArea() {
        return getParent().tryDissolveEditAreaUDBL(this);
    }

    private void removeTextAndUnregisterStepnumberLinks(WrappedPosition startOffset, WrappedPosition endOffset) {
        if (startOffset.greater(endOffset)) {
            throw new IllegalArgumentException("StartOffSet is greater than EndOffset - Make sure not to set the length as endOffset");
        }

        WrappedDocument doc = getWrappedDocument();

        for (WrappedPosition currentOffset = startOffset; currentOffset.less(endOffset); ) { // The missing currentOffset++ is intended
            WrappedPosition currentEndOffset = getEndOffsetFromPosition(currentOffset);
            int length = currentEndOffset.distance(currentOffset);
            WrappedElement element = doc.getCharacterElement(currentOffset);

            if (stepnumberLinkStyleSet(element)) {
                String stepnumberLinkID = getStepnumberLinkIDFromElement(currentOffset, currentEndOffset);
                if (!StepnumberLink.isStepnumberLinkDefect(stepnumberLinkID)) {
                    AbstractSchrittView step = editor().findStepByStepID(stepnumberLinkID);
                    step.unregisterStepnumberLink(this);
                    editor().addEdit(new UndoableStepnumberLinkRemoved(step, this));
                }
            }

            currentOffset = currentOffset.inc(length); // Skip already processed positions
        }

        doc.remove(startOffset, endOffset.distance(startOffset));
    }

    public boolean skipToStepnumberLinkStart() {
        WrappedPosition selectionEnd = getWrappedSelectionEnd();
        if (stepnumberLinkStyleSet(selectionEnd.dec())) {
            setCaretPosition(getStartOffsetFromPosition(selectionEnd.dec()).unwrap());
            return true;
        }
        return false;
    }

    public WrappedPosition getWrappedSelectionEnd() {
        return getWrappedDocument().fromUI(getSelectionEnd());
    }

    public WrappedPosition getWrappedSelectionStart() {
        return getWrappedDocument().fromUI(getSelectionStart());
    }

    @Override
    public EditContainer getParent() {
        return (EditContainer) super.getParent();
    }

    public void addImage(BufferedImage image) {
        getParent().addImageUDBL(image, this);
    }

    public EditArea addTable(int columns, int rows) {
        return getParent().addTableUDBL(this, columns, rows);
    }

    public EditArea toggleListItemUDBL(boolean ordered) {
        // TODO JL: Evt. noch schlauer machen. Toggle von Ordered -> Unordered und umgekehrt
        EditContainer editContainer = getParent();
        if (editContainer.getParent() instanceof UnorderedListItemEditArea) {
            UnorderedListItemEditArea unorderedListItemEditArea = (UnorderedListItemEditArea) editContainer.getParent();
            return unorderedListItemEditArea.getParent().dissolveListItemEditAreaUDBL(unorderedListItemEditArea);
        }
        if (editContainer.getParent() instanceof OrderedListItemEditArea) {
            OrderedListItemEditArea orderedListItemEditArea = (OrderedListItemEditArea) editContainer.getParent();
            return orderedListItemEditArea.getParent().dissolveListItemEditAreaUDBL(orderedListItemEditArea);
        }
        else {
            return getParent().addListItemUDBL(this, ordered);
        }
    }

    @Override
    public TextEditAreaModel_V001 toModel(boolean formatierterText) {
      return changeInfo.isDeleted()
        ? deletionBackup
        : getTextWithMarkups(formatierterText);
    }

    @Override
    public void skalieren(int prozentNeu, int prozentAktuell) {
        setFont(DEFAULTFONT.deriveFont((float) FONTSIZE * prozentNeu / 100));
    }

    public TextEditArea split(WrappedPosition textPosition) {
        WrappedDocument document = getWrappedDocument();
        if (!textPosition.isLast()) {
            TextEditArea splittedArea = copyArea();
            document.removeFrom(textPosition);
            splittedArea.remove(textPosition.toModel());
            WrappedDocument splittedAreaDoc = splittedArea.getWrappedDocument();
            splittedArea.setCaretPosition(splittedAreaDoc.start().unwrap());
            return splittedArea;
        }
        return null;
    }

    public TextEditArea copyArea() {
        TextEditAreaModel_V001 modelCopy = new TextEditAreaModel_V001(getText(), getPlainText(), new ArrayList<>(), changeInfo);
        MarkedCharSequence marksBackup = findMarkups();
        TextEditArea areaCopy = new TextEditArea(modelCopy, this.getFont());
        List<Markup_V001> recoveredChangemarks = new MarkupRecovery(getWrappedDocument(), marksBackup).recover();
        new MarkupBackgroundStyleInitializer(areaCopy, recoveredChangemarks).styleChangedTextSections();
        return areaCopy;
    }

    public TextEditArea copySection(WrappedPosition fromPosition, WrappedPosition toPosition) {
        TextEditArea selectionArea = copyArea();
        selectionArea.shrink(fromPosition, toPosition);
        return selectionArea.hasContent() ? selectionArea : null;
    }

    public void shrink(WrappedPosition fromPosition, WrappedPosition toPosition) {
        WrappedDocument doc = getWrappedDocument();
        WrappedPosition end = doc.end();
        WrappedPosition start = doc.start();
        int textLength = getWrappedDocument().getLength();
        if (toPosition.less(fromPosition)) {
            remove(doc.start(), textLength);
        }
        else {
            if (end.greater(toPosition)) {
                doc.removeFrom(toPosition.inc());
            }
            if (fromPosition.greater(start)) {
                remove(start, fromPosition.distance(start));
            }
        }
    }

    private boolean hasContent() { return getWrappedDocument().hasContent(); }

    public void remove(int len) { getWrappedDocument().remove(len); }

    public void remove(WrappedPosition offset, int len) { getWrappedDocument().remove(offset, len); }

    @Override
    public TextEditArea asTextArea() {
        return this;
    }

    @Override
    public boolean isTextArea() { return true; }

    @Override
    public ImageEditArea asImageArea() {
        return null;
    }

    public void appendText(TextEditArea trailingText) {
        MarkedCharSequence marksBackup = findMarkups();
        marksBackup.append(trailingText.findMarkups());
        int endOfOldText = getDocument().getLength();
        String oldText = getText();
        String newText =
          oldText
            .replace(HTML_OUTRO, "")
            .replace(BODY_OUTRO, "")
            .replace(HEAD_OUTRO, "")
            .replace(HEAD_INTRO, "")
            .trim()
          + trailingText.getText()
            .replace(HTML_INTRO, "")
            .replace(BODY_INTRO, "")
            .replace(HEAD_OUTRO, "")
            .replace(HEAD_INTRO, "")
            .trim();
        setText(newText);
        List<Markup_V001> recoveredChangemarks = new MarkupRecovery(getWrappedDocument(), marksBackup).recover();
        new MarkupBackgroundStyleInitializer(this, recoveredChangemarks).styleChangedTextSections();
        setCaretPosition(endOfOldText);
    }

    public void addStepnumberLink(AbstractSchrittView referencedStep) {
        EditorI editor = editor();
        try (UndoRecording ur = editor.composeUndo()) {
            String stepnumberText = referencedStep.getId().toString();

            WrappedDocument doc = getWrappedDocument();
            WrappedPosition caretPos = getWrappedCaretPosition();

            // Add space between two stepnumberlinks to prevent merging them
            if (stepnumberLinkStyleSet(caretPos.dec())) {
                doc.insertString(caretPos, " ", null);
                caretPos = caretPos.inc();
            }

            AttributeSet previousAttribute = doc.getCharacterElement(caretPos).getAttributes();
            MutableAttributeSet stepnumberAttribute = new SimpleAttributeSet(previousAttribute);
            stepnumberAttribute.addAttributes(ChangeSet.STEPNUMBER_LINK_COLOR.background);
            StyleConstants.setBackground(stepnumberAttribute,
                    isTrackingChanges() ? changeset().stepnumberLinkColor() : ChangeSet.STEPNUMBER_LINK_COLOR.color);

            doc.insertString(caretPos, stepnumberText, stepnumberAttribute);

            referencedStep.registerStepnumberLink(this);
            editor.addEdit(new UndoableStepnumberLinkAdded(referencedStep, this));
        }
    }

    private boolean stepnumberLinkNormalStyleSet(WrappedPosition position) {
        return stepnumberLinkNormalStyleSet(getWrappedDocument().getCharacterElement(position));
    }

    private boolean stepnumberLinkNormalStyleSet(WrappedElement element) {
        String color = getBackgroundColorFromElement(element);
        return ChangeSet.STEPNUMBER_LINK_COLOR.isBackground(color);
    }


    public boolean stepnumberLinkChangedStyleSet(WrappedElement element) {
        String color = getBackgroundColorFromElement(element);
        return color != null && color.equalsIgnoreCase(changeset().stepnumberLinkHtmlColor());
    }


    public boolean stepnumberLinkStyleSet(WrappedPosition position) {
        return stepnumberLinkStyleSet(getWrappedDocument().getCharacterElement(position));
    }

    public boolean stepnumberLinkStyleSet(WrappedElement element) {
        return element != null &&
          (stepnumberLinkNormalStyleSet(element) || stepnumberLinkChangedStyleSet(element));
    }


    private String getBackgroundColorFromElement(WrappedElement element) {
        Object backgroundColorValue = element.getAttributes().getAttribute(CSS.Attribute.BACKGROUND_COLOR);
        return backgroundColorValue != null ? backgroundColorValue.toString() : null;
    }

    public WrappedPosition getStartOffsetFromPosition(WrappedPosition position) {
        return getWrappedDocument().getCharacterElement(position).getStartOffset();
    }

    public WrappedPosition getEndOffsetFromPosition(WrappedPosition position) {
        return getWrappedDocument().getCharacterElement(position).getEndOffset();
    }

    public boolean isTrackingChanges() {
        return editor().aenderungenVerfolgen() && isEditable();
    }

    @Override
    public boolean enthaeltAenderungsmarkierungen() {
        return changeInfo.isChange()
          || !findMarkups(FirstChangeOnly).isEmpty();
    }

    private void scrollToStepnumber() {
        EditorI editor = editor();

        WrappedDocument doc = getWrappedDocument();
        WrappedElement element = doc.getCharacterElement(getCaretPosition());

        String stepnumberLinkID = getStepnumberLinkIDFromElement(element.getStartOffset(), element.getEndOffset());

        if (StepnumberLink.isStepnumberLinkDefect(stepnumberLinkID)) {
            JOptionPane.showMessageDialog(this,
                    "Der Schritt, auf den der SchrittnummerLink verwiesen hat, existiert nicht mehr.",
                    "Springen nicht möglich", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // The user might not have focussed anything in the current edit container before
        // he clicked on a step link to scroll to another step. So in case he wants to
        // scroll back by CTRL+ALT+Left, we explicitly add current edit area's container
        // to the edit history here.
        editor.appendToEditHistory(this.getParent());
        AbstractSchrittView step = editor.findStepByStepID(stepnumberLinkID);
        step.scrollTo();
    }

    /**
     * Marks a StepnumberLink as defect.
     */
    public void markStepnumberLinkAsDefect(String id) {
        updateStepnumberLink(id,id + StepnumberLink.STEPNUMBER_DEFECT_MARK);
    }

    public void updateStepnumberLink(String oldID, String newID) {
        for (WrappedElement e : getWrappedDocument().getRootElements()) {
            if (replaceStepnumberLink(e, oldID, newID)) {
                return;
            }
        }
        throw new RuntimeException("Could not find old StepnumberLink " + oldID + " in TextArea '" + getPlainText() + "'."
                + " This indicates a missing unregisterStepnumberLink() call.");
    }

    private boolean replaceStepnumberLink(WrappedElement e, String oldID, String newID) {
        WrappedDocument doc = getWrappedDocument();
        if (stepnumberLinkStyleSet(e)) {
            String stepnumberLinkID = getStepnumberLinkIDFromElement(e);
            if (stepnumberLinkID.equals(oldID)) {
                CompoundUndoManager.beginCompoundEdit(doc.getCore());

                AttributeSet previousAttribute = doc.getCharacterElement(e.getStartOffset()).getAttributes();
                doc.remove(e.getStartOffset(), e.getEndOffset().distance(e.getStartOffset()));
                doc.insertString(e.getStartOffset(), newID, previousAttribute);

                CompoundUndoManager.endCompoundEdit(doc.getCore());
                return true;
            }
        }
        for (int i = 0; i < e.getElementCount(); i++) {
            if (replaceStepnumberLink(e.getElement(i), oldID, newID)) {
                return true;
            }
        }
        return false;
    }

    public List<WrappedElement> findStepnumberLinks() {
        List<WrappedElement> stepnumberLinks = new ArrayList<>();
        for (WrappedElement e : getWrappedDocument().getRootElements()) {
            stepnumberLinks.addAll(findStepnumberLinks(e));
        }
        return stepnumberLinks;
    }

    private List<WrappedElement> findStepnumberLinks(WrappedElement e) {
        List<WrappedElement> stepnumberLinks = new ArrayList<>();

        if (stepnumberLinkStyleSet(e)) {
            stepnumberLinks.add(e);
        }
        for (int i = 0; i < e.getElementCount(); i++) {
            stepnumberLinks.addAll(findStepnumberLinks(e.getElement(i)));
        }
        return stepnumberLinks;
    }

    public void findStepnumberLinkIDs(HashMap<TextEditArea, List<String>> stepnumberLinkMap) {
        List<String> stepnumberLinkIDs = findStepnumberLinks()
            .stream()
            .map(this::getStepnumberLinkIDFromElement)
            .collect(Collectors.toList());
        if (!stepnumberLinkIDs.isEmpty()) {
            stepnumberLinkMap.put(this, stepnumberLinkIDs);
        }
    }

    private String getStepnumberLinkIDFromElement(WrappedElement element) {
        return getStepnumberLinkIDFromElement(element.getStartOffset(), element.getEndOffset());
    }

    public String getStepnumberLinkIDFromElement(WrappedPosition startOffset, WrappedPosition endOffset) {
        return getWrappedDocument().getText(startOffset, endOffset.distance(startOffset));
    }

    @Override public void setEditBackgroundUDBL(Color bg) {
        setBackgroundUDBL(bg);
    }

    @Override
    public void setEditDecorationIndentions(Indentions indentions) {
        Border border = new EmptyBorder(
          indentions.topBorder(),
          indentions.leftBorder(),
          indentions.bottomBorder(),
          indentions.rightBorder());
        setBorder(border);
    }

    @Override
    public boolean enthaelt(InteractiveStepFragment fragment) { return this == fragment; }

    @Override public ChangeInfo getChangeInfo() { return changeInfo; }
    @Override public void setChangeInfo(ChangeInfo changeInfo) {
      this.changeInfo = changeInfo;
      if (!changeInfo.isDeleted()) {
        deletionBackup = null;
      }
    }
    private void setChangeInfoUDBL(ChangeInfo changeInfo) { UDBL.setChangeInfo(this, changeInfo); }

    public Shape getShape() {
        return new Shape(this).withText(new FormattedShapeText(this));
    }

    public int getLength() { return getDocument().getLength(); }

    public Integer getFirstLineHeight() {
        try {
            return (int)modelToView2D(1).getHeight();
        }
        catch(BadLocationException blc) {
            return null;
        }
    }

    public WrappedDocument getWrappedDocument() {
        return new WrappedDocument((StyledDocument) getDocument());
    }

    @Override
    public String toString() {
      return StringUtils.abbreviate(getPlainText().replace("\n", " "), 20);
    }

  @Override
  public List<JTextComponent> getTextAreas() { return List.of(this); }

  public CatchUeberschrift containingCatchHeading() {
    return getParent().containingCatchHeading();
  }

  @Override
  public void viewsNachinitialisieren() {
    if (changeInfo.isDeleted()) {
      setGeloeschtMarkiertStilUDBL(changeInfo.changeSet());
    }
  }

}
