/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.access.internal.core;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mapping.dip.ObjectGroupMapper;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static fr.gouv.vitam.common.mapping.dip.UnitMapper.buildObjectMapper;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.xmlunit.builder.Input.fromString;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

public class ObjectGroupDipServiceImplTest {

    @Test
    public void should_map_json_object_group_to_xml_with_fileInfo() throws InvalidParseOperationException {
        // Given
        ObjectGroupMapper objectGroupMapper = new ObjectGroupMapper();
        ObjectGroupDipServiceImpl objectGrouDipService = new ObjectGroupDipServiceImpl(objectGroupMapper, buildObjectMapper());

        InputStream inputStream = getClass().getResourceAsStream("/simple_objectGroup_with_phisical_and_objectGroup.json");
        JsonNode jsonNode = JsonHandler.getFromInputStream(inputStream);

        Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put("vitam", "fr:gouv:culture:archivesdefrance:seda:v2.1");

        // When
        Response response = objectGrouDipService.jsonToXml(jsonNode, "");

        // Then
        String entity = (String) response.getEntity();
        assertThat(fromString(entity), hasXPath("//vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:FileInfo/vitam:Filename",
            equalTo("Filename0"))
            .withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity), hasXPath("//vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:FileInfo/vitam:CreatingApplicationName",
            equalTo("CreatingApplicationName0"))
            .withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity), hasXPath("//vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:FileInfo/vitam:CreatingApplicationVersion",
            equalTo("CreatingApplicationVersion0"))
            .withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity), hasXPath("//vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:FileInfo/vitam:DateCreatedByApplication",
            equalTo("2006-05-04T18:13:51.0"))
            .withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity), hasXPath("//vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:FileInfo/vitam:CreatingOs",
            equalTo("CreatingOs0"))
            .withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity), hasXPath("//vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:FileInfo/vitam:CreatingOsVersion",
            equalTo("CreatingOsVersion0"))
            .withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity), hasXPath("//vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:FileInfo/vitam:LastModified",
            equalTo("2006-05-04T18:13:51.0"))
            .withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity), hasXPath("//vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:OtherMetadata/vitam:testMD",
                equalTo("test value"))
                .withNamespaceContext(prefix2Uri));
    }

}
