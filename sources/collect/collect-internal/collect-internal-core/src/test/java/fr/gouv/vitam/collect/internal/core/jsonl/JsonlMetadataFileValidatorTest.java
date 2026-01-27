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
package fr.gouv.vitam.collect.internal.core.jsonl;

import fr.gouv.vitam.collect.common.exception.CollectInternalErrorsDetailsException;
import fr.gouv.vitam.common.PropertiesUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

public class JsonlMetadataFileValidatorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testParseValidJsonlMetadataFile() throws Exception {
        assertValid("update/metadata.jsonl", true);
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_InvalidFieldNames() throws Exception {
        assertInvalid(
            "update/metadata_invalid_field_name_dot.jsonl",
            true,
            "Invalid unit metadata at index: 0. Field name must be root-level field: 'Description.Level'"
        );

        assertInvalid(
            "update/metadata_invalid_field_name_dash.jsonl",
            true,
            "Invalid unit metadata at index: 0. Forbidden field name '#id'"
        );

        assertInvalid(
            "update/metadata_invalid_field_name_spacing.jsonl",
            true,
            "Invalid unit metadata at index: 0. Illegal field name ' a'"
        );

        assertInvalid(
            "update/metadata_invalid_field_name_underscore.jsonl",
            true,
            "Invalid unit metadata at index: 0. Illegal field name '_id'"
        );

        assertInvalid(
            "update/metadata_invalid_field_name_dollar.jsonl",
            true,
            "Invalid unit metadata at index: 0. Illegal field name '$myField'"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataSelector_InvalidKeys() throws Exception {
        assertInvalid(
            "update/metadata_invalid_selector_key_name_dollar.jsonl",
            false,
            "Invalid field name: 'a.$bad' at index 0"
        );

        assertInvalid(
            "update/metadata_invalid_selector_key_name_underscore.jsonl",
            false,
            "Invalid field name: '_bad' at index 0"
        );

        assertInvalid(
            "update/metadata_invalid_selector_key_name_spacing.jsonl",
            false,
            "Invalid field name: 'a a' at index 0"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_EmptyRequest() throws Exception {
        assertInvalid("update/metadata_invalid_empty.jsonl", true, "Empty jsonl file");

        assertInvalid(
            "update/metadata_invalid_empty_line.jsonl",
            true,
            "Cannot validate json-lines request: Json is not valid from Sanitize check"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_EmptyUnitContent() throws Exception {
        assertInvalid(
            "update/metadata_null_unit_content.jsonl",
            false,
            "Invalid unit metadata at index: 1. Empty metadata content"
        );

        assertInvalid(
            "update/metadata_empty_unit_content.jsonl",
            false,
            "Invalid unit metadata at index: 1. Empty metadata content"
        );

        assertInvalid(
            "update/metadata_missing_unit_content.jsonl",
            false,
            "Invalid unit metadata at index: 1. Empty metadata content"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_SanityCheckKo() throws Exception {
        assertInvalid(
            "update/metadata_invalid_sanity_check_ko.jsonl",
            false,
            "Cannot validate json-lines request: Json is not valid from Sanitize check"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_CumulativeErrorReporting() throws Exception {
        assertInvalid(
            "update/metadata_cumulative_error_reporting.jsonl",
            true,
            """
            JSONL validation failed. 2 errors:
            - Invalid unit metadata at index: 0. Missing metadata identification information
            - Invalid unit metadata at index: 2. Illegal unit file path 'toto/../tata'"""
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_UnitFormatKo() throws Exception {
        assertInvalid(
            "update/metadata_invalid_unit_format.jsonl",
            false,
            "Cannot deserialize value of type `fr.gouv.vitam.common.model.unit.LevelType`"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_UnitWithManagementRuleEndDate() throws Exception {
        assertInvalid(
            "update/metadata_invalid_unit_management_rule_end_date.jsonl",
            false,
            "Invalid unit metadata at index: 0. Unit Appraisal Rules cannot contain 'EndDate' field"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_MissingUnitIdentificationInformation() throws Exception {
        assertInvalid(
            "update/metadata_missing_unit_identification_information.jsonl",
            false,
            "Invalid unit metadata at index: 1. Missing metadata identification information"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_InvalidFile() throws Exception {
        assertInvalid(
            "update/metadata_empty_file.jsonl",
            false,
            "Invalid unit metadata at index: 0. Empty unit file path '    '"
        );

        assertInvalid(
            "update/metadata_illegal_file.jsonl",
            false,
            "Invalid unit metadata at index: 0. Illegal unit file path 'toto/../tata'"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_ObjectFilesForbiddenForUpdateOperations() throws Exception {
        assertInvalid(
            "update/metadata.jsonl",
            false,
            "Invalid unit metadata at index: 0. ObjectFiles field not allowed for update operations"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_InvalidObjectFiles() throws Exception {
        assertInvalid(
            "update/metadata_illegal_object_files.jsonl",
            true,
            "Invalid unit metadata at index: 0. Illegal ObjectFiles path 'path/../to/object_files.pdf'"
        );
        assertInvalid(
            "update/metadata_empty_object_files.jsonl",
            true,
            "Invalid unit metadata at index: 0. Empty ObjectFiles path"
        );
        assertInvalid(
            "update/metadata_blank_object_files.jsonl",
            true,
            "Invalid unit metadata at index: 0. Empty ObjectFiles path"
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_InvalidUnitSelectorForFirstUpload() throws Exception {
        assertInvalid(
            "update/metadata_unit_selector_for_update_only.jsonl",
            true,
            "Invalid selector key '#id' for upload operation at index: 0."
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_ValidUnitSelectorForFirstUpload() throws Exception {
        assertValid("update/metadata_unit_selector_for_update_only.jsonl", false);
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_ValidUploadPathUnitSelectorForFirstUpload() throws Exception {
        assertValid("update/metadata_unit_selector_for_insert_or_update.jsonl", false);
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_ValidUploadPathUnitSelectorForUpdate() throws Exception {
        assertValid("update/metadata_unit_selector_for_insert_or_update.jsonl", true);
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_ValidUnitWithUpdateOperationForFirstUploadMode() throws Exception {
        assertValid("update/metadata_update_operation_ok.jsonl", true);
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_InvalidUnitWithUpdateOperationForUpdateMode() throws Exception {
        assertInvalid(
            "update/metadata_update_operation_ok.jsonl",
            false,
            """
            JSONL validation failed. 3 errors:
            - Invalid unit metadata at index: 0: 'Management.UpdateOperation.*' fields not supported in update APIs
            - Invalid unit metadata at index: 1: 'Management.UpdateOperation.*' fields not supported in update APIs
            - Invalid unit metadata at index: 2: 'Management.UpdateOperation.*' fields not supported in update APIs"""
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_InvalidUnitWithUpdateOperation() throws Exception {
        assertInvalid(
            "update/metadata_update_operation_ko.jsonl",
            true,
            """
            JSONL validation failed. 9 errors:
            - Invalid unit metadata at index: 0. Missing or empty '#management.UpdateOperation.ArchiveUnitIdentifierKey.MetadataValue' field
            - Invalid unit metadata at index: 1. Missing or empty '#management.UpdateOperation.ArchiveUnitIdentifierKey.MetadataName' field
            - Invalid unit metadata at index: 2. Both '#management.UpdateOperation.SystemId' and '#management.UpdateOperation.ArchiveUnitIdentifierKey.MetadataName' headers are set
            - Invalid unit metadata at index: 3. Missing or empty '#management.UpdateOperation.ArchiveUnitIdentifierKey.MetadataValue' field
            - Invalid unit metadata at index: 4. Missing or empty '#management.UpdateOperation.ArchiveUnitIdentifierKey.MetadataName' field
            - Invalid unit metadata at index: 5. Cannot set other metadata field 'Tag' when '#management.UpdateOperation' header is defined
            - Invalid unit metadata at index: 6. Cannot set other metadata field 'Tag' when '#management.UpdateOperation' header is defined
            - Invalid unit metadata at index: 7. Only top-level (root) units can have '#management.UpdateOperation.*' fields
            - Invalid unit metadata at index: 8. Only top-level (root) units can have '#management.UpdateOperation.*' fields"""
        );
    }

    @Test
    public void testParseInvalidJsonlMetadataFile_InvalidUnitWithUpdateOperationWhenXAttachementIdHeaderIsSet()
        throws Exception {
        assertInvalid(
            "update/metadata_update_operation_ok.jsonl",
            true,
            true,
            """
            JSONL validation failed. 3 errors:
            - Cannot set '#management.UpdateOperation.*' fields when explicit HTTP header 'X-Attachement-Id' is set
            - Cannot set '#management.UpdateOperation.*' fields when explicit HTTP header 'X-Attachement-Id' is set
            - Cannot set '#management.UpdateOperation.*' fields when explicit HTTP header 'X-Attachement-Id' is set"""
        );
    }

    private static void assertValid(String resourcesFile, boolean isFirstUpload) throws Exception {
        JsonlMetadataFileValidator validator = new JsonlMetadataFileValidator();
        File jsonlMetadataFile = PropertiesUtils.getResourceFile(resourcesFile);
        assertThatCode(() -> validator.validate(jsonlMetadataFile, isFirstUpload, false)).doesNotThrowAnyException();
    }

    private void assertInvalid(String resourcesFile, boolean isFirstUpload, String expectedMessage) throws Exception {
        assertInvalid(resourcesFile, isFirstUpload, false, expectedMessage);
    }

    private void assertInvalid(
        String resourcesFile,
        boolean isFirstUpload,
        boolean explicitAttachementMode,
        String expectedMessage
    ) throws Exception {
        JsonlMetadataFileValidator validator = new JsonlMetadataFileValidator();
        File jsonlMetadataFile = PropertiesUtils.getResourceFile(resourcesFile);
        assertThatThrownBy(() -> validator.validate(jsonlMetadataFile, isFirstUpload, explicitAttachementMode))
            .isInstanceOf(CollectInternalErrorsDetailsException.class)
            .hasMessageContaining(expectedMessage);
    }
}
