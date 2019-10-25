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
}
