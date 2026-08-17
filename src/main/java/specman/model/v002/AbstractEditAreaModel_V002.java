package specman.model.v002;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes({
    @Type(value = TextEditAreaModel_V002.class,     name = "text"),
    @Type(value = ListItemEditAreaModel_V002.class, name = "listItem"),
    @Type(value = ImageEditAreaModel_V002.class,    name = "image"),
    @Type(value = TableEditAreaModel_V002.class,    name = "table"),
})
public abstract class AbstractEditAreaModel_V002 {
}
