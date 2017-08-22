package fr.gouv.vitam.access.internal.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.gouv.culture.archivesdefrance.seda.v2.AgentType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.KeywordsType;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;
import fr.gouv.vitam.access.internal.core.serializer.AgentTypeDeserializer;
import fr.gouv.vitam.access.internal.core.serializer.IdentifierTypeDeserializer;
import fr.gouv.vitam.access.internal.core.serializer.KeywordTypeDeserializer;
import fr.gouv.vitam.access.internal.core.serializer.LevelTypeDeserializer;
import fr.gouv.vitam.access.internal.core.serializer.TextByLangDeserializer;
import fr.gouv.vitam.access.internal.core.serializer.TextTypeDeSerializer;
import fr.gouv.vitam.common.model.unit.TextByLang;

public interface UnitMapper {

    static ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        SimpleModule module = new SimpleModule();

        module.addDeserializer(TextByLang.class, new TextByLangDeserializer());
        module.addDeserializer(LevelType.class, new LevelTypeDeserializer());
        module.addDeserializer(IdentifierType.class, new IdentifierTypeDeserializer());
        module.addDeserializer(TextType.class, new TextTypeDeSerializer());
        module.addDeserializer(KeywordsType.KeywordType.class, new KeywordTypeDeserializer());
        module.addDeserializer(AgentType.class, new AgentTypeDeserializer());

        objectMapper.registerModule(module);

        return objectMapper;
    }

}
