/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.mapping.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.culture.archivesdefrance.seda.v2.OrganizationDescriptiveMetadataType;
import fr.gouv.vitam.common.mapping.dip.TransformJsonTreeToListOfXmlElement;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OrganizationDescriptiveMetadataTypeDeserializer
    extends JsonDeserializer<OrganizationDescriptiveMetadataType> {

    private static final TypeReference<Map<String, Object>> REFERENCE = new TypeReference<Map<String, Object>>() {};

    private ObjectMapper objectMapper;

    public OrganizationDescriptiveMetadataTypeDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public OrganizationDescriptiveMetadataType deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Map<String, Object> map = objectMapper.convertValue(node, REFERENCE);
        List<Element> elements = TransformJsonTreeToListOfXmlElement.mapJsonToElement(map);
        OrganizationDescriptiveMetadataType organizationDescriptiveMetadataType =
            new OrganizationDescriptiveMetadataType();
        organizationDescriptiveMetadataType.getAny().addAll(elements);
        return organizationDescriptiveMetadataType;
    }
}
