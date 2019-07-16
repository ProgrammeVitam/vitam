package fr.gouv.vitam.functional.administration.common.server;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.DocumentValidator;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

public class ReferentialDocumentValidatorsTest {

    private static final String ACCESS_CONTRACT_OK_JSON_FILE = "access-contract_OK.json";
    private static final String ACCESSION_REGISTER_DETAIL_OK_JSON_FILE = "accession_register_detail_OK.json";
    private static final String ACCESSION_REGISTER_SUMMARY_OK_JSON_FILE = "accession_register_summary_OK.json";
    private static final String AGENCIES_OK_JSON_FILE = "agencies_OK.json";
    private static final String CONTEXT_OK_JSON_FILE = "context_OK.json";
    private static final String FILE_FORMAT_OK_JSON_FILE = "file_format_OK.json";
    private static final String FILE_RULES_OK_JSON_FILE = "file_rules_OK.json";
    private static final String INGEST_CONTRACT_OK_JSON_FILE = "ingest_contract_OK.json";
    private static final String PROFILE_OK_JSON_FILE = "profile_OK.json";
    private static final String SECURITY_PROFILE_OK_JSON_FILE = "security_profile_OK.json";

    @Test
    public void valid_AccessContract() throws Exception {

        // Given
        final DocumentValidator schemaValidator =
            ReferentialDocumentValidators.getValidator(FunctionalAdminCollections.ACCESS_CONTRACT);

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESS_CONTRACT_OK_JSON_FILE)));
    }

    @Test
    public void valid_AccessionRegisterDetail() throws Exception {

        // Given
        final DocumentValidator schemaValidator =
            ReferentialDocumentValidators.getValidator(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler
                .getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_DETAIL_OK_JSON_FILE)));
    }

    @Test
    public void valid_AccessionRegisterSummary() throws Exception {

        // Given
        final DocumentValidator schemaValidator =
            ReferentialDocumentValidators.getValidator(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler
                .getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_SUMMARY_OK_JSON_FILE)));
    }

    @Test
    public void valid_Agencies() throws Exception {
        // Given
        final DocumentValidator schemaValidator =
            ReferentialDocumentValidators.getValidator(FunctionalAdminCollections.AGENCIES);

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AGENCIES_OK_JSON_FILE)));
    }

    @Test
    public void valid_Context() throws Exception {
        // Given
        final DocumentValidator schemaValidator =
            ReferentialDocumentValidators.getValidator(FunctionalAdminCollections.CONTEXT);

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(CONTEXT_OK_JSON_FILE)));
    }

    @Test
    public void valid_FileFormat() throws Exception {
        // Given
        final DocumentValidator schemaValidator =
            ReferentialDocumentValidators.getValidator(FunctionalAdminCollections.FORMATS);

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FILE_FORMAT_OK_JSON_FILE)));
    }

    @Test
    public void valid_FileRules() throws Exception {
        // Given
        final DocumentValidator schemaValidator =
            ReferentialDocumentValidators.getValidator(FunctionalAdminCollections.RULES);

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FILE_RULES_OK_JSON_FILE)));
    }

    @Test
    public void valid_IngestContract() throws Exception {
        // Given
        final DocumentValidator schemaValidator =
            ReferentialDocumentValidators.getValidator(FunctionalAdminCollections.INGEST_CONTRACT);

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(INGEST_CONTRACT_OK_JSON_FILE)));
    }



    @Test
    public void valid_Profile() throws Exception {
        // Given
        final DocumentValidator schemaValidator =
            ReferentialDocumentValidators.getValidator(FunctionalAdminCollections.PROFILE);

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(PROFILE_OK_JSON_FILE)));
    }


    @Test
    public void valid_SecurityProfile() throws Exception {
        // Given
        final DocumentValidator schemaValidator =
            ReferentialDocumentValidators.getValidator(FunctionalAdminCollections.SECURITY_PROFILE);

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(SECURITY_PROFILE_OK_JSON_FILE)));
    }
}
