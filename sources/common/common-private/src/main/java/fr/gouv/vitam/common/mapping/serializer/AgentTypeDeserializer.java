package fr.gouv.vitam.common.mapping.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.culture.archivesdefrance.seda.v2.AgentType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;

public class AgentTypeDeserializer extends JsonDeserializer<AgentType> {
    /**
     * @param jp   representation (json, xml, string)
     * @param ctxt
     * @return
     * @throws IOException
     */
    @Override
    public AgentType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        IdentifierType identifierType = new IdentifierType();
        identifierType.setValue(node.asText());
        AgentType agentType = new AgentType();
        agentType.getContent().clear();

        node.fieldNames().forEachRemaining(s -> {
            agentType.getContent().add(new JAXBElement<>(new QName("fr:gouv:culture:archivesdefrance:seda:v2.0", s)
                , String.class, node.get(s).asText()));
        });

        return agentType;
    }

}
