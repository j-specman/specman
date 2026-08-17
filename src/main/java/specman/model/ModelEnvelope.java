package specman.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import specman.model.v001.StruktogrammModel_V001;
import specman.model.v002.DiagramModel_V002;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelEnvelope {
    public final static Set<Class<?>> SUPPORTED_MODEL_CLASSES;

    public String specmanVersion;
    public String modelType;

    static {
        try {
            Field payloadField = ModelEnvelope.class.getDeclaredField("model");
            JsonSubTypes jsonSubTypes = payloadField.getAnnotation(JsonSubTypes.class);
            SUPPORTED_MODEL_CLASSES = Arrays.stream(jsonSubTypes.value()).map(Type::value).collect(Collectors.toSet());
        }
        catch(NoSuchFieldException nsfx) {
            throw new IllegalArgumentException(nsfx.getMessage(), nsfx);
        }
    }

    /** Um die Serialisierung bzw. De-Serialisierung des eingebetteten Modells zu gewärleisten,
     * müssen die unterstützten Modellversions-Klassen explizit für Jackson deklariert werden. Dies
     * geschieht über die {@link @JsonSubType.Type} Annotation. Der jeweilige Typ des Modells
     * wird beim Anlegen des Envelopes in {@link #modelType} zur Verfügung gestellt und dient
     * Jackson bei der Deserialisierung als Indikator, wie das Modell zu interpretieren ist. */
    @JsonTypeInfo(use = Id.CLASS, include = As.EXTERNAL_PROPERTY, property = "modelType")
    @JsonSubTypes(value = {
            @Type(value = StruktogrammModel_V001.class),
            @Type(value = DiagramModel_V002.class),
    })
    public Object model;

}
