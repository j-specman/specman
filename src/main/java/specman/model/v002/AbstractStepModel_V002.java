package specman.model.v002;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import specman.ChangeInfo;
import specman.view.RoundedBorderDecorationStyle;

import java.util.List;
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
    public final UUID id;
    public final EditorContentModel_V002 content;
    public final int color;
    public final ChangeInfoModel_V002 changeInfo;
    public final UUID sourceStepId;
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
            UUID id,
            EditorContentModel_V002 content,
            int color,
            ChangeInfo changeInfo,
            UUID sourceStepId,
            RoundedBorderDecorationStyle decorationStyle) {
        this.id = id;
        this.content = content;
        this.color = color;
        this.changeInfo = ChangeInfoModel_V002.from(changeInfo);
        this.sourceStepId = sourceStepId;
        this.decorationStyle = decorationStyle;
    }

    public void addStepRecursively(List<AbstractStepModel_V002> allSteps) {
        allSteps.add(this);
    }
}
