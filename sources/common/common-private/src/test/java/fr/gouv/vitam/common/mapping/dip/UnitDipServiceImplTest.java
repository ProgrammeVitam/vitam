/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.common.mapping.dip;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static fr.gouv.vitam.common.mapping.dip.UnitMapper.buildObjectMapper;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.xmlunit.builder.Input.fromString;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

public class UnitDipServiceImplTest {

    @Test
    public void should_map_json_unit_to_xml() throws InvalidParseOperationException {
        // Given
        ArchiveUnitMapper archiveUnitMapper = new ArchiveUnitMapper();
        UnitDipServiceImpl unitDipService = new UnitDipServiceImpl(archiveUnitMapper, buildObjectMapper());

        InputStream inputStream = getClass().getResourceAsStream("/unit_with_rules.json");
        JsonNode jsonNode = JsonHandler.getFromInputStream(inputStream);

        Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put("vitam", "fr:gouv:culture:archivesdefrance:seda:v2.1");

        // When
        Response response = unitDipService.jsonToXml(jsonNode, "");

        // Then
        String entity = (String) response.getEntity();
        assertThat(fromString(entity), hasXPath("//vitam:Management/vitam:AccessRule/vitam:Rule",
            equalTo("ACC-00002"))
            .withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity), hasXPath("//vitam:Management/vitam:DisseminationRule/vitam:Rule",
            equalTo("DIS-00001"))
            .withNamespaceContext(prefix2Uri));
    }

    @Test
    public void should_map_json_unit_to_xml_with_keyword() throws InvalidParseOperationException {
        // Given
        ArchiveUnitMapper archiveUnitMapper = new ArchiveUnitMapper();
        UnitDipServiceImpl unitDipService = new UnitDipServiceImpl(archiveUnitMapper, buildObjectMapper());

        InputStream inputStream = getClass().getResourceAsStream("/unit_with_keyword.json");
        JsonNode jsonNode = JsonHandler.getFromInputStream(inputStream);

        Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put("vitam", "fr:gouv:culture:archivesdefrance:seda:v2.1");

        // When
        Response response = unitDipService.jsonToXml(jsonNode, "");

        // Then
        String entity = (String) response.getEntity();
        assertThat(fromString(entity), hasXPath("//vitam:Keyword/vitam:KeywordType",
            equalTo("subject"))
            .withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity), hasXPath("//vitam:Keyword/vitam:KeywordContent",
            equalTo("blabla"))
            .withNamespaceContext(prefix2Uri));

    }

    @Test
    public void should_map_json_unit_to_xml_with_agent_type() throws InvalidParseOperationException {
        // Given
        ArchiveUnitMapper archiveUnitMapper = new ArchiveUnitMapper();
        UnitDipServiceImpl unitDipService = new UnitDipServiceImpl(archiveUnitMapper, buildObjectMapper());

        InputStream inputStream = getClass().getResourceAsStream("/unit_with_agent_type.json");
        JsonNode jsonNode = JsonHandler.getFromInputStream(inputStream);

        Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put("vitam", "fr:gouv:culture:archivesdefrance:seda:v2.1");

        // When
        Response response = unitDipService.jsonToXml(jsonNode, "");

        // Then
        String entity = (String) response.getEntity();
        assertThat(fromString(entity), hasXPath("//vitam:Addressee/vitam:FirstName",
            equalTo("FirstName2"))
            .withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity), hasXPath("//vitam:Addressee/vitam:BirthName",
            equalTo("BirthName2"))
            .withNamespaceContext(prefix2Uri));
    }

    @Test
    public void should_map_json_unit_to_xml_with_originating_agency_organization_description_metadata()
        throws InvalidParseOperationException {
        // Given
        ArchiveUnitMapper archiveUnitMapper = new ArchiveUnitMapper();
        UnitDipServiceImpl unitDipService = new UnitDipServiceImpl(archiveUnitMapper, buildObjectMapper());

        InputStream inputStream = getClass().getResourceAsStream(
            "/unit_originating_agency_with_organization_descriptive_metadata.json");
        JsonNode jsonNode = JsonHandler.getFromInputStream(inputStream);

        Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put("vitam", "fr:gouv:culture:archivesdefrance:seda:v2.1");

        // When
        Response response = unitDipService.jsonToXml(jsonNode, "");

        // Then
        String entity = (String) response.getEntity();
        assertThat(fromString(entity),
            hasXPath("//vitam:OriginatingAgency/vitam:Identifier",
                equalTo("RATP")).withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity),
            hasXPath("//vitam:OriginatingAgency/vitam:OrganizationDescriptiveMetadata/vitam:XXXX/vitam:YYYY",
                equalTo("description libre YYYY")).withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity),
            hasXPath("//vitam:OriginatingAgency/vitam:OrganizationDescriptiveMetadata/vitam:DescriptionOA",
                equalTo("La RATP est un etablissement public")).withNamespaceContext(prefix2Uri));

    }

    @Test
    public void should_map_json_unit_to_xml_with_submission_agency_organization_description_metadata()
        throws InvalidParseOperationException {
        // Given
        ArchiveUnitMapper archiveUnitMapper = new ArchiveUnitMapper();
        UnitDipServiceImpl unitDipService = new UnitDipServiceImpl(archiveUnitMapper, buildObjectMapper());

        InputStream inputStream = getClass().getResourceAsStream(
            "/unit_submission_agency_with_organization_descriptive_metadata.json");
        JsonNode jsonNode = JsonHandler.getFromInputStream(inputStream);

        Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put("vitam", "fr:gouv:culture:archivesdefrance:seda:v2.1");

        // When
        Response response = unitDipService.jsonToXml(jsonNode, "");

        // Then
        String entity = (String) response.getEntity();
        assertThat(fromString(entity),
            hasXPath("//vitam:SubmissionAgency/vitam:Identifier",
                equalTo("RATP")).withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity),
            hasXPath("//vitam:SubmissionAgency/vitam:OrganizationDescriptiveMetadata/vitam:XXXX/vitam:YYYY",
                equalTo("description libre YYYY")).withNamespaceContext(prefix2Uri));
        assertThat(fromString(entity),
            hasXPath("//vitam:SubmissionAgency/vitam:OrganizationDescriptiveMetadata/vitam:DescriptionOA",
                equalTo("La RATP est un etablissement public")).withNamespaceContext(prefix2Uri));

    }

    @Test
    public void should_map_json_unit_to_xml_with_related_object_reference_metadata()
        throws InvalidParseOperationException {
        // Given
        ArchiveUnitMapper archiveUnitMapper = new ArchiveUnitMapper();
        UnitDipServiceImpl unitDipService = new UnitDipServiceImpl(archiveUnitMapper, buildObjectMapper());

        InputStream inputStream = getClass().getResourceAsStream(
            "/unit_with_related_object_reference_metadata.json");
        JsonNode jsonNode = JsonHandler.getFromInputStream(inputStream);

        Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put("vitam", "fr:gouv:culture:archivesdefrance:seda:v2.1");

        // When
        Response response = unitDipService.jsonToXml(jsonNode, "");

        // Then
        String entity = (String) response.getEntity();
        assertThat(fromString(entity),
            hasXPath("//vitam:RelatedObjectReference/vitam:Requires/vitam:RepositoryArchiveUnitPID",
                equalTo("aeaqaaaaaab2szrxabosoaloqmivdliaaaaq")).withNamespaceContext(prefix2Uri));

    }
}
