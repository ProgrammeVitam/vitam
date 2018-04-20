/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.common.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.SchemaValidationStatus.SchemaValidationStatusEnum;
import org.junit.Test;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

public class SchemaValidationUtilsTest {

    private static final String AU_JSON_FILE = "archive-unit_OK.json";
    private static final String AU_INVALID_JSON_FILE = "archive-unit_Invalid.json";
    private static final String AU_INVALID_DATE_JSON_FILE = "archive-unit_date_Invalid.json";
    private static final String COMPLEX_JSON_FILE = "complex_archive_unit.json";
    private static final String OBJECT_BIRTH_PLACE_JSON_FILE = "object_birth_place_archive_unit.json";
    private static final String STRING_BIRTH_PLACE_JSON_FILE = "string_birth_place_archive_unit.json";


    private static final String ACCESS_CONTRACT_OK_JSON_FILE = "access-contract_OK.json";
    public static final String ACCESSION_REGISTER_DETAIL_OK_JSON_FILE = "accession_register_detail_OK.json";
    public static final String ACCESSION_REGISTER_SUMMARY_OK_JSON_FILE = "accession_register_summary_OK.json";
    public static final String AGENCIES_OK_JSON_FILE = "agencies_OK.json";
    public static final String ARCHIVE_UNIT_PROFILE_OK_JSON_FILE = "archive_unit_profile_OK.json";
    public static final String CONTEXT_OK_JSON_FILE = "context_OK.json";
    public static final String FILE_FORMAT_OK_JSON_FILE = "file_format_OK.json";
    public static final String FILE_RULES_OK_JSON_FILE = "file_rules_OK.json";
    public static final String INGEST_CONTRACT_OK_JSON_FILE = "ingest_contract_OK.json";
    public static final String PROFILE_OK_JSON_FILE = "profile_OK.json";
    public static final String SECURITY_PROFILE_OK_JSON_FILE = "security_profile_OK.json";
    public static final String ONTOLOGY_OK_JSON_FILE = "ontology_OK.json";
    public static final String ONTOLOGY_KO_JSON_FILE = "ontology_KO.json";

    public static final String TAG_ARCHIVE_UNIT = "ArchiveUnit";

    @Test
    public void givenDefaultConstructorThenValidateJsonOK() throws Exception {
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        SchemaValidationStatus status = schemaValidation
            .validateUnit(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AU_JSON_FILE))
                .get(TAG_ARCHIVE_UNIT));
        assertTrue(status.getValidationStatus().equals(SchemaValidationStatusEnum.VALID));
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenExternalTxtConstructorThenKO() throws Exception {
        String schemaString = "I am not a correct json schema";
        new SchemaValidationUtils(schemaString, true);
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenExternalXmlConstructorThenKO() throws Exception {
        String schemaString = "<xml>XML</xml>";
        new SchemaValidationUtils(schemaString, true);
    }


    @Test(expected = ProcessingException.class)
    public void givenExternalIncorrectJsonConstructorThenKo() throws Exception {
        String schemaString = "\"id\":\"myId\"}";
        new SchemaValidationUtils(schemaString, true);
    }

    @Test
    public void givenExternalCorrectJsonConstructorThenKo() throws Exception {
        String schemaString = "{\"id\":\"myId\"}";
        new SchemaValidationUtils(schemaString, true);
    }

    @Test
    public void givenExternalCorrectJsonConstructorThenValidateArchiveUnitThenOk() throws Exception {
        // empty schema
        String schemaString = "{}";
        SchemaValidationUtils schemaValidation = new SchemaValidationUtils(schemaString, true);
        SchemaValidationStatus status = schemaValidation
            .validateUnit(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AU_JSON_FILE))
                .get(TAG_ARCHIVE_UNIT));
        assertTrue(status.getValidationStatus().equals(SchemaValidationStatusEnum.VALID));
    }

    @Test
    public void givenConstructorWithCorrectSchemaThenValidateJsonOK() throws Exception {
        final SchemaValidationUtils schemaValidation =
            new SchemaValidationUtils("json-schema/archive-unit-schema.json");
        SchemaValidationStatus status = schemaValidation
            .validateUnit(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AU_JSON_FILE))
                .get(TAG_ARCHIVE_UNIT));
        assertTrue(status.getValidationStatus().equals(SchemaValidationStatusEnum.VALID));
    }

    @Test
    public void givenComplexArchiveUnitJsonThenValidateJsonObjectBirthPlaceOK() throws Exception {
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        SchemaValidationStatus status = schemaValidation
            .validateUnit(
                JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(OBJECT_BIRTH_PLACE_JSON_FILE))
                    .get(TAG_ARCHIVE_UNIT));
        assertTrue(status.getValidationStatus().equals(SchemaValidationStatusEnum.VALID));
    }

    @Test
    public void givenComplexArchiveUnitJsonThenValidateJsonObjectBirthPlaceKO() throws Exception {
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        SchemaValidationStatus status = schemaValidation
            .validateUnit(
                JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(STRING_BIRTH_PLACE_JSON_FILE))
                    .get(TAG_ARCHIVE_UNIT));
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
    }

    @Test
    public void givenComplexArchiveUnitJsonThenValidateJsonOK() throws Exception {
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        SchemaValidationStatus status = schemaValidation
            .validateUnit(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(COMPLEX_JSON_FILE))
                .get(TAG_ARCHIVE_UNIT));
        assertTrue(status.getValidationStatus().equals(SchemaValidationStatusEnum.VALID));
    }

    @Test(expected = FileNotFoundException.class)
    public void givenConstructorWithInexistingSchemaThenException() throws Exception {
        new SchemaValidationUtils("json-schema/archive-unit-schema-inexisting.json");
    }

    @Test(expected = InvalidParseOperationException.class)
    public void givenConstructorWithIncorrectSchemaThenException() throws Exception {
        new SchemaValidationUtils("manifestOK.xml");
    }


    @Test
    public void givenInvalidJsonFileThenValidateKO() throws Exception {
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        SchemaValidationStatus status = schemaValidation
            .validateUnit(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AU_INVALID_JSON_FILE)));
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
    }

    @Test
    public void givenInvalidDateKO() throws Exception {
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        SchemaValidationStatus status = schemaValidation
            .validateUnit(
                JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AU_INVALID_DATE_JSON_FILE)));
        assertTrue(status.getValidationStatus().equals(SchemaValidationStatusEnum.RULE_BAD_START_END_DATE));
        assertTrue(status.getValidationMessage().contains("EndDate is before StartDate"));
    }

    @Test
    public void should_is_ok_when_au_has_title_() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();

        // When
        SchemaValidationStatus status = schemaValidation
            .validateUnit(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream("simple_unit.json")));

        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
        assertThat(status.getValidationMessage()).contains("Title_");
    }



    @Test
    public void valid_AccessContract() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESS_CONTRACT_OK_JSON_FILE)),
            "AccessContract");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);

    }

    @Test
    public void valid_AccessionRegisterDetail() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_DETAIL_OK_JSON_FILE)),
            "AccessionRegisterDetail");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);

    }

    @Test
    public void valid_AccessionRegisterSummary() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler
                .getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_SUMMARY_OK_JSON_FILE)),
            "AccessionRegisterSummary");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);

    }

    @Test
    public void valid_Agencies() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AGENCIES_OK_JSON_FILE)), "Agencies");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);
    }

    @Test
    public void valid_ArchiveUnitProfiles() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_PROFILE_OK_JSON_FILE)),
            "ArchiveUnitProfile");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);
    }

    @Test
    public void valid_Ontologies() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ONTOLOGY_OK_JSON_FILE)),
            "Ontology");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);
    }

    @Test
    public void unvalid_Ontologies() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ONTOLOGY_KO_JSON_FILE)),
            "Ontology");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.NOT_AU_JSON_VALID);
    }

    @Test
    public void valid_Context() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(CONTEXT_OK_JSON_FILE)), "Context");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);
    }

    @Test
    public void valid_FileFormat() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FILE_FORMAT_OK_JSON_FILE)),
            "FileFormat");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);
    }

    @Test
    public void valid_FileRules() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FILE_RULES_OK_JSON_FILE)), "FileRules");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);
    }

    @Test
    public void valid_IngestContract() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(INGEST_CONTRACT_OK_JSON_FILE)),
            "IngestContract");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);
    }



    @Test
    public void valid_Profile() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(PROFILE_OK_JSON_FILE)), "Profile");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);
    }


    @Test
    public void valid_SecurityProfile() throws Exception {
        // Given
        final SchemaValidationUtils schemaValidation = new SchemaValidationUtils();
        // When
        SchemaValidationStatus status = schemaValidation.validateJson(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(SECURITY_PROFILE_OK_JSON_FILE)),
            "SecurityProfile");
        // Then
        assertThat(status.getValidationStatus()).isEqualTo(SchemaValidationStatusEnum.VALID);
    }



}
