package specman.model.v002;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import specman.ChangeInfo;
import specman.StepNumber;
import specman.view.RoundedBorderDecorationStyle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes({
    @Type(value = SimpleStepModel_V002.class,       name = "simple"),
    @Type(value = BreakStepModel_V002.class,        name = "break"),
    @Type(value = SourceStepModel_V002.class,       name = "source"),
    @Type(value = IfStepModel_V002.class,           name = "if"),
    @Type(value = IfElseStepModel_V002.class,       name = "ifElse"),
    @Type(value = CaseStepModel_V002.class,         name = "case"),
    @Type(value = WhileStepModel_V002.class,        name = "while"),
    @Type(value = DoWhileStepModel_V002.class,      name = "doWhile"),
    @Type(value = SubsequenceStepModel_V002.class,  name = "subsequence"),
})
public abstract class AbstractStepModel_V002 {
    public static final int ID_LENGTH = 8;

    public final String id;
    public final EditorContentModel_V002 content;
    public final int color;
    public final ChangeInfoModel_V002 changeInfo;
    public final String sourceStepId;
    public final RoundedBorderDecorationStyle decorationStyle;

    @Deprecated AbstractStepModel_V002() { // For Jackson only
        id = null;
        content = null;
        color = 0;
        changeInfo = null;
        sourceStepId = null;
        decorationStyle = null;
    }

    AbstractStepModel_V002(
            String id,
            EditorContentModel_V002 content,
            int color,
            ChangeInfo changeInfo,
            String sourceStepId,
            RoundedBorderDecorationStyle decorationStyle) {
        this.id = id;
        this.content = content;
        this.color = color;
        this.changeInfo = ChangeInfoModel_V002.from(changeInfo);
        this.sourceStepId = sourceStepId;
        this.decorationStyle = decorationStyle;
    }

    /** Returns a random 8-character hex ID derived from the first 32 bits of a random UUID.
     *  Modelled after Git short hashes, which use 7-8 hex characters as a human-readable
     *  yet practically unique identifier within a bounded corpus.
     *  Collision probability for typical documents (&lt;1000 steps) is negligible (~0.01%). */
    public static String generateId() {
        return normalizeId(UUID.randomUUID().toString());
    }

    /** Normalizes a step ID to 8-character hex. Accepts both the current short form
     *  (e.g. {@code "09a3df2d"}) and full UUIDs from older .nsd files
     *  (e.g. {@code "09a3df2d-c187-45ad-9ed7-eaf1b6aeed8e"}). */
    public static String normalizeId(String id) {
        if (id == null) {
            return null;
        }
        return id.contains("-") ? id.replace("-", "").substring(0, ID_LENGTH) : id;
    }

    public void addStepRecursively(List<AbstractStepModel_V002> allSteps) {
        allSteps.add(this);
    }

    public List<NumberedSubSequence_V002> subSequencesFor(StepNumber myNumber) {
        return Collections.emptyList();
    }

    public StepNumber nextSlotInOuterSequence(StepNumber myNumber, Map<String, StepNumber> stepNumbers) {
        return myNumber;
    }
}
