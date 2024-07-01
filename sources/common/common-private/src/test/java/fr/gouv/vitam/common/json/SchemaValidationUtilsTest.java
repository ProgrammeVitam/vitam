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
package fr.gouv.vitam.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaValidationUtilsTest {

    private static final String ARCHIVE_UNIT_PROFILE_OK_JSON_FILE = "aup-validator/archive_unit_profile_OK.json";
    private static final String ARCHIVE_UNIT_PROFILE_WITH_PATTERN_PROPERTIES =
        "aup-validator/archive_unit_profile_description_with_pattern_properties.json";
    private static final String ARCHIVE_UNIT_PROFILE_WITHOUT_PATTERN_PROPERTIES =
        "aup-validator/archive_unit_profile_description_without_pattern_properties.json";
    private static final String ARCHIVE_UNIT_PROFILE_WITH_PROPERTIES_FIELD =
        "aup-validator/archive_unit_profile_with_properties_field.json";
    private static final String ARCHIVE_UNIT_PROFILE_WITH_TYPE_ITEMS_NESTED_TYPE_PATTERN_PROPERTIES =
        "aup-validator/archive_unit_profile_with_type_items_nested_type_pattern_properties.json";

    @Test
    public void testExtractFieldsFromSchema() throws Exception {
        // Given
        JsonNode jsonArcUnit = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_PROFILE_OK_JSON_FILE)
        );
        ArchiveUnitProfileModel archiveUnitProfile = JsonHandler.getFromJsonNode(
            jsonArcUnit,
            ArchiveUnitProfileModel.class
        );
        // When
        Collection<String> extractFields = SchemaValidationUtils.extractFieldsFromSchema(
            archiveUnitProfile.getControlSchema()
        );

        // Then
        assertThat(extractFields).hasSize(14);
        // assert child fields are present
        assertThat(extractFields).contains("Rule");
        assertThat(extractFields).contains("StartDate");
        assertThat(extractFields).contains("PreventRulesId");
        // assert parent fields are not present
        assertThat(extractFields).doesNotContain("Rules");
        assertThat(extractFields).doesNotContain("#management");
    }

    @Test
    public void testExtractFieldsFromSchema_with_field_Description_and_Title_with_pattern_properties()
        throws FileNotFoundException, InvalidParseOperationException {
        // GIVEN
        JsonNode jsonArcUnit = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_PROFILE_WITH_PATTERN_PROPERTIES)
        );
        ArchiveUnitProfileModel archiveUnitProfile = JsonHandler.getFromJsonNode(
            jsonArcUnit,
            ArchiveUnitProfileModel.class
        );

        // WHEN
        Collection<String> extractFields = SchemaValidationUtils.extractFieldsFromSchema(
            archiveUnitProfile.getControlSchema()
        );

        // THEN
        assertThat(extractFields).doesNotContain("patternProperties", "Description_", "Title_");
        assertThat(extractFields).containsExactlyInAnyOrder(
            "ArchiveUnitProfile",
            "Rule",
            "StartDate",
            "EndDate",
            "FinalAction",
            "DescriptionLevel",
            "Title",
            "OriginatingSystemId",
            "Description",
            "CustodialHistoryItem",
            "Tag",
            "KeywordContent",
            "KeywordType",
            "FirstName",
            "BirthName",
            "Identifier",
            "SentDate",
            "ReceivedDate"
        );
    }

    @Test
    public void testExtractFieldsFromSchema_with_field_Description_and_Title_without_pattern_properties()
        throws FileNotFoundException, InvalidParseOperationException {
        // GIVEN
        JsonNode jsonArcUnit = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_PROFILE_WITHOUT_PATTERN_PROPERTIES)
        );
        ArchiveUnitProfileModel archiveUnitProfile = JsonHandler.getFromJsonNode(
            jsonArcUnit,
            ArchiveUnitProfileModel.class
        );

        // WHEN
        Collection<String> extractFields = SchemaValidationUtils.extractFieldsFromSchema(
            archiveUnitProfile.getControlSchema()
        );

        // THEN
        assertThat(extractFields).doesNotContain("patternProperties", "Description_", "Title_");
        assertThat(extractFields).containsExactlyInAnyOrder("ArchiveUnitProfile", "Title", "Description", "en", "fr");
    }

    @Test
    public void testExtractFieldsFromSchema_with_field_Description_and_Title_wit_properties_field()
        throws FileNotFoundException, InvalidParseOperationException {
        // GIVEN
        JsonNode jsonArcUnit = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_PROFILE_WITH_PROPERTIES_FIELD)
        );
        ArchiveUnitProfileModel archiveUnitProfile = JsonHandler.getFromJsonNode(
            jsonArcUnit,
            ArchiveUnitProfileModel.class
        );

        // WHEN
        Collection<String> extractFields = SchemaValidationUtils.extractFieldsFromSchema(
            archiveUnitProfile.getControlSchema()
        );

        // THEN
        assertThat(extractFields).doesNotContain("patternProperties", "Description_", "Title_");
        assertThat(extractFields).containsExactlyInAnyOrder(
            "ArchiveUnitProfile",
            "properties",
            "Title",
            "Description",
            "en",
            "fr"
        );
    }

    @Test
    public void testExtractFieldsFromSchema_with_field_items()
        throws FileNotFoundException, InvalidParseOperationException {
        // GIVEN
        JsonNode jsonArcUnit = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_PROFILE_WITH_PROPERTIES_FIELD)
        );
        ArchiveUnitProfileModel archiveUnitProfile = JsonHandler.getFromJsonNode(
            jsonArcUnit,
            ArchiveUnitProfileModel.class
        );

        // WHEN
        Collection<String> extractFields = SchemaValidationUtils.extractFieldsFromSchema(
            archiveUnitProfile.getControlSchema()
        );

        // THEN
        assertThat(extractFields).doesNotContain("patternProperties", "Description_", "Title_");
        assertThat(extractFields).containsExactlyInAnyOrder(
            "ArchiveUnitProfile",
            "properties",
            "Title",
            "Description",
            "en",
            "fr"
        );
    }

    @Test
    public void testExtractFieldsFromSchema_with_type_items_nested_type_pattern_properties()
        throws FileNotFoundException, InvalidParseOperationException {
        // GIVEN
        JsonNode jsonArcUnit = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_PROFILE_WITH_TYPE_ITEMS_NESTED_TYPE_PATTERN_PROPERTIES)
        );
        ArchiveUnitProfileModel archiveUnitProfile = JsonHandler.getFromJsonNode(
            jsonArcUnit,
            ArchiveUnitProfileModel.class
        );

        // WHEN
        Collection<String> extractFields = SchemaValidationUtils.extractFieldsFromSchema(
            archiveUnitProfile.getControlSchema()
        );

        // THEN
        assertThat(extractFields).doesNotContain("patternProperties", "Description_", "Title_");
        assertThat(extractFields).containsExactlyInAnyOrder(
            "ArchiveUnitProfile",
            "properties",
            "Title",
            "Description",
            "en",
            "fr",
            "Toto"
        );
    }
}
