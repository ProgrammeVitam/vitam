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
package fr.gouv.vitam.functional.administration.common.server;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.DocumentValidator;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

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
    private static final String MANAGEMENT_CONTRACT_OK_JSON_FILE = "management-contract_OK.json";

    @Test
    public void valid_AccessContract() throws Exception {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.ACCESS_CONTRACT
        );

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESS_CONTRACT_OK_JSON_FILE))
        );
    }

    @Test
    public void valid_AccessionRegisterDetail() throws Exception {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL
        );

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_DETAIL_OK_JSON_FILE))
        );
    }

    @Test
    public void valid_AccessionRegisterSummary() throws Exception {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY
        );

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(ACCESSION_REGISTER_SUMMARY_OK_JSON_FILE))
        );
    }

    @Test
    public void valid_Agencies() throws Exception {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.AGENCIES
        );

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(AGENCIES_OK_JSON_FILE))
        );
    }

    @Test
    public void valid_Context() throws Exception {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.CONTEXT
        );

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(CONTEXT_OK_JSON_FILE))
        );
    }

    @Test
    public void valid_FileFormat() throws Exception {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.FORMATS
        );

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FILE_FORMAT_OK_JSON_FILE))
        );
    }

    @Test
    public void valid_FileRules() throws Exception {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.RULES
        );

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(FILE_RULES_OK_JSON_FILE))
        );
    }

    @Test
    public void valid_IngestContract() throws Exception {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.INGEST_CONTRACT
        );

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(INGEST_CONTRACT_OK_JSON_FILE))
        );
    }

    @Test
    public void valid_Profile() throws Exception {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.PROFILE
        );

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(PROFILE_OK_JSON_FILE))
        );
    }

    @Test
    public void valid_SecurityProfile() throws Exception {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.SECURITY_PROFILE
        );

        // When / Then
        schemaValidator.validateDocument(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(SECURITY_PROFILE_OK_JSON_FILE))
        );
    }

    @Test
    public void valid_ManagementContract() {
        // Given
        final DocumentValidator schemaValidator = ReferentialDocumentValidators.getValidator(
            FunctionalAdminCollections.MANAGEMENT_CONTRACT
        );

        // When / Then
        assertThatCode(() -> {
            schemaValidator.validateDocument(
                JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(MANAGEMENT_CONTRACT_OK_JSON_FILE))
            );
        }).doesNotThrowAnyException();
    }
}
