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

package fr.gouv.vitam.collect.internal.core.csv;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.collect.internal.core.exceptions.CollectInvalidCsvFormatException;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

public class CsvMetadataValidatorTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    private SedaSchemaInfoResolver sedaSchemaInfoResolver;

    private CsvMetadataValidator csvMetadataValidator;

    @Before
    public void setUp() throws Exception {
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(loadUnitSchema()).when(adminManagementClient).getUnitSchema();
        sedaSchemaInfoResolver = new SedaSchemaInfoResolver(adminManagementClientFactory);
        csvMetadataValidator = new CsvMetadataValidator();
    }

    @Test
    public void validateComplexSedaHeaders() throws Exception {
        CSVParser parser = CsvHelper.createParser(PropertiesUtils.getResourceAsStream("csv/metadata_full_seda.csv"));
        List<String> headerNames = parser.getHeaderNames();
        assertThatHeaderNamesAreValid(headerNames);
    }

    @Test
    public void testHeaderValidation_DuplicateHeaders() {
        // Given
        List<String> headerLines = List.of(
            "File;File;Content.Title",
            "File;Content.Title;Content.Title",
            "File;Management.NeedAuthorization;Management.NeedAuthorization",
            "File;Management.AppraisalRule.Rule.0;Management.AppraisalRule.Rule.0",
            "File;ArchiveUnitProfile;ArchiveUnitProfile"
        );

        // When / Then
        for (String headerLine : headerLines) {
            assertThatHeaderNamesAreInvalid(headerLine, "Duplicate header name '");
        }
    }

    @Test
    public void testHeaderValidation_MissingFileHeader() {
        assertThatHeaderNamesAreInvalid(
            "Content.Title;Content.Description;Management.AppraisalRule.PreventInheritance;ArchiveUnitProfile",
            "Missing required 'File' header name"
        );
    }

    @Test
    public void testHeaderValidation_NoHeaderToSet() {
        assertThatHeaderNamesAreInvalid("File", "No header to set");
    }

    @Test
    public void testHeaderValidation_HeaderSanityChecks_TooLong() {
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content" + StringUtils.repeat(".A", 150)),
            "Invalid header name 'Content.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A.A....': Header name is too long"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content." + StringUtils.repeat('A', 120)),
            "Content.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA': Field name too long"
        );
    }

    @Test
    public void testHeaderValidation_InvalidCategory() {
        // Given
        List<String> headerLines = List.of(
            "File;Content.",
            "File;Management.",
            "File;Content.Title;Management.AppraisalRule.Rule;BadField"
        );

        // When / Then
        for (String headerLine : headerLines) {
            assertThatHeaderNamesAreInvalid(
                headerLine,
                "Only accepted names are 'File', 'Content.*', 'Management.*' or 'ArchiveUnitProfile'"
            );
        }
    }

    @Test
    public void testHeaderValidation_TooManyHeaders() {
        // Given
        List<String> headerNames = ListUtils.union(
            List.of("File"),
            IntStream.range(0, 10000).mapToObj(i -> "Content.Field" + i).toList()
        );

        // When / Then
        assertThatHeaderNamesAreInvalid(headerNames, "Too many header names 10001 (max = 10000)");
    }

    @Test
    public void testHeaderValidation_MixImplicitAndExplicitArrayIndex() {
        // Given
        List<String> headerLines = List.of(
            "File;Content.SystemId;Content.SystemId.1",
            "File;Content.Description;Content.Description.attr;Content.Description.1;Content.Description.1.attr",
            "File;Content.Title;Content.Title.attr;Content.Title.1;Content.Title.1.attr",
            "File;Management.AppraisalRule.Rule;Management.AppraisalRule.Rule.1",
            "File;Management.AppraisalRule.StartDate;Management.AppraisalRule.StartDate.1"
        );

        // When / Then
        for (String headerLine : headerLines) {
            assertThatHeaderNamesAreInvalid(headerLine, "Cannot mix implicit array and array index syntaxes for field");
        }
    }

    @Test
    public void testHeaderValidation_MissingHeaderIndex() {
        assertThatHeaderNamesAreInvalid(
            "File;Content.SystemId.0;Content.SystemId.2",
            "Expected header name 'Content.SystemId.1' since header 'Content.SystemId.2' is declared"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Description.0;Content.Description.2",
            "Expected header name 'Content.Description.1' since header 'Content.Description.2' is declared"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Title.0;Content.Title.0.attr;Content.Title.2;Content.Title.2.attr",
            "Expected header name 'Content.Title.1' since header 'Content.Title.2' is declared"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.EventIdentifier;Content.Event.5.EventIdentifier",
            "Expected header name 'Content.Event.1' since header 'Content.Event.5' is declared"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.AppraisalRule.Rule.0;Management.AppraisalRule.Rule.2",
            "Expected header name 'Management.AppraisalRule.Rule.1' since header 'Management.AppraisalRule.Rule.2' is declared"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.AppraisalRule.RefNonRuleId.0;Management.AppraisalRule.RefNonRuleId.3",
            "Expected header name 'Management.AppraisalRule.RefNonRuleId.1' since header 'Management.AppraisalRule.RefNonRuleId.3' is declared"
        );
    }

    @Test
    public void testHeaderValidation_InvalidRulePropertiesArrayIndexWithoutMatchingRule() {
        assertThatHeaderNamesAreInvalid(
            "File;Management.StorageRule.StartDate.1",
            "Rule property field 'Management.StorageRule.StartDate.1' does not have a corresponding 'Management.StorageRule.Rule.1'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.AppraisalRule.StartDate.0",
            "Invalid header name 'Management.AppraisalRule.StartDate.0': Rule property field 'Management.AppraisalRule.StartDate.0' does not have a corresponding 'Management.AppraisalRule.Rule.0'."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.AccessRule.StartDate.1",
            "Invalid header name 'Management.AccessRule.StartDate.1': Rule property field 'Management.AccessRule.StartDate.1' does not have a corresponding 'Management.AccessRule.Rule.1'."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.DisseminationRule.Rule.0;Management.DisseminationRule.StartDate.1",
            "Invalid header name 'Management.DisseminationRule.StartDate.1': Rule property field 'Management.DisseminationRule.StartDate.1' does not have a corresponding 'Management.DisseminationRule.Rule.1'."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.ReuseRule.StartDate.1",
            "Invalid header name 'Management.ReuseRule.StartDate.1': Rule property field 'Management.ReuseRule.StartDate.1' does not have a corresponding 'Management.ReuseRule.Rule.1'."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.ClassificationRule.StartDate.1",
            "Invalid header name 'Management.ClassificationRule.StartDate.1': Rule property field 'Management.ClassificationRule.StartDate.1' does not have a corresponding 'Management.ClassificationRule.Rule.1'."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.HoldRule.StartDate.1",
            "Invalid header name 'Management.HoldRule.StartDate.1': Rule property field 'Management.HoldRule.StartDate.1' does not have a corresponding 'Management.HoldRule.Rule.1'."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.HoldRule.HoldEndDate.1",
            "Invalid header name 'Management.HoldRule.HoldEndDate.1': Rule property field 'Management.HoldRule.HoldEndDate.1' does not have a corresponding 'Management.HoldRule.Rule.1'."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.HoldRule.HoldOwner.1",
            "Invalid header name 'Management.HoldRule.HoldOwner.1': Rule property field 'Management.HoldRule.HoldOwner.1' does not have a corresponding 'Management.HoldRule.Rule.1'."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.HoldRule.HoldReassessingDate.1",
            "Invalid header name 'Management.HoldRule.HoldReassessingDate.1': Rule property field 'Management.HoldRule.HoldReassessingDate.1' does not have a corresponding 'Management.HoldRule.Rule.1'."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.HoldRule.HoldReason.1",
            "Invalid header name 'Management.HoldRule.HoldReason.1': Rule property field 'Management.HoldRule.HoldReason.1' does not have a corresponding 'Management.HoldRule.Rule.1'."
        );
    }

    @Test
    public void testHeaderValidation_ValidRulePropertiesArrayIndexWithMatchingRule() {
        // Given
        List<String> headerLines = List.of(
            "File;Management.StorageRule.Rule.0;Management.StorageRule.Rule.1;Management.StorageRule.StartDate.0;Management.StorageRule.StartDate.1",
            "File;Management.AppraisalRule.Rule.0;Management.AppraisalRule.StartDate",
            "File;Management.AccessRule.Rule;Management.AccessRule.StartDate.0",
            "File;Management.DisseminationRule.Rule.0;Management.DisseminationRule.Rule.1;Management.DisseminationRule.StartDate.0",
            "File;Management.ReuseRule.Rule.0;Management.ReuseRule.Rule.1;Management.ReuseRule.StartDate",
            "File;Management.ClassificationRule.Rule.0;Management.ClassificationRule.Rule.1;Management.ClassificationRule.StartDate.1",
            "File;Management.HoldRule.Rule.0;Management.HoldRule.StartDate;Management.HoldRule.HoldEndDate.0;Management.HoldRule.Rule.1;Management.HoldRule.HoldEndDate.1;Management.HoldRule.HoldOwner.1;Management.HoldRule.HoldReassessingDate.1;Management.HoldRule.HoldReason.1"
        );

        // When / Then
        for (String headerLine : headerLines) {
            assertThatHeaderNamesAreValid(headerLine);
        }
    }

    @Test
    public void testHeaderValidation_ValidMissingHeaderIndexForRuleProperties() {
        // Given
        List<String> headerLines = List.of(
            "File;Management.StorageRule.Rule.0;Management.StorageRule.Rule.1;Management.StorageRule.StartDate.1",
            "File;Management.AppraisalRule.Rule.0;Management.AppraisalRule.Rule.1;Management.AppraisalRule.StartDate.1",
            "File;Management.AccessRule.Rule.0;Management.AccessRule.Rule.1;Management.AccessRule.StartDate.1",
            "File;Management.DisseminationRule.Rule.0;Management.DisseminationRule.Rule.1;Management.DisseminationRule.StartDate.1",
            "File;Management.ReuseRule.Rule.0;Management.ReuseRule.Rule.1;Management.ReuseRule.StartDate.1",
            "File;Management.ClassificationRule.Rule.0;Management.ClassificationRule.Rule.1;Management.ClassificationRule.StartDate.1",
            "File;Management.HoldRule.Rule.0;Management.HoldRule.Rule.1;Management.HoldRule.StartDate.1;Management.HoldRule.HoldEndDate.1;Management.HoldRule.HoldOwner.1;Management.HoldRule.HoldReassessingDate.1;Management.HoldRule.HoldReason.1"
        );

        // When / Then
        for (String headerLine : headerLines) {
            assertThatHeaderNamesAreValid(headerLine);
        }
    }

    @Test
    public void testHeaderValidation_ForbiddenFields() {
        assertThatHeaderNamesAreInvalid(
            "File;Content.ArchiveUnitProfile",
            "Seda Field 'Content.ArchiveUnitProfile' is reserved / forbidden."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.LogBook.Event.0.EventIdentifier",
            "Seda Field 'Management.LogBook' is reserved / forbidden."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Management.UpdateOperation.SystemId",
            "Seda Field 'Management.UpdateOperation' is reserved / forbidden."
        );
    }

    @Test
    public void testHeaderValidation_InvalidArrayFields() {
        assertThatHeaderNamesAreInvalid("File;Content.Title.1000000000000", "Array index '1000000000000' too large");
        assertThatHeaderNamesAreInvalid(
            "File;Content.Description.0.0",
            "Invalid array declaration at 'Content.Description.0'"
        );
        assertThatHeaderNamesAreInvalid("File;Content.Tag.0.0", "Invalid array declaration at 'Content.Tag.0'");
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Management.AppraisalRule.Rule.0.0"),
            "Invalid array declaration at 'Management.AppraisalRule.Rule.0'"
        );
        assertThatHeaderNamesAreInvalid("File;Content.0.Title", "Field 'Content' is not an array");
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Management.0.AppraisalRule.Rule.0"),
            "Field 'Management' is not an array"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Management.StorageRule.0.Rule"),
            "Field 'Management.StorageRule' is not an array"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Management.AppraisalRule.PreventInheritance.0"),
            "Field 'Management.AppraisalRule.PreventInheritance' is not an array"
        );
    }

    @Test
    public void testHeaderValidation_TooLargeArrayIndex() {
        // Given
        List<String> headerLines = List.of(
            "File;Content.SystemId.10000",
            "File;Content.Description.10000",
            "File;Content.Title.10000",
            "File;Management.HoldRule.Rule.10000"
        );

        // When / Then
        for (String headerLine : headerLines) {
            assertThatHeaderNamesAreInvalid(headerLine, "Array index '10000' too large");
        }
    }

    @Test
    public void testHeaderValidation_EmptyFieldName() {
        // Given
        List<String> headerLines = List.of(
            "File;Content..",
            "File;Content.A.",
            "File;Content..A",
            "File;Management.AppraisalRule.   .Rule"
        );

        // When / Then
        for (String headerLine : headerLines) {
            assertThatHeaderNamesAreInvalid(headerLine, "Empty or blank field name");
        }
    }

    @Test
    public void testHeaderValidation_ValidFieldNameContainingDigit() {
        assertThatHeaderNamesAreValid("File;Content.Field1");
    }

    @Test
    public void testHeaderValidation_InvalidFieldNameStartingWithDigit() {
        assertThatHeaderNamesAreInvalid("File;Content.1Field", "Field name cannot start with a digit");
    }

    @Test
    public void testHeaderValidation_IllegalFieldNamesPrefix() {
        // Given
        List<String> headerLines = List.of("File;Content._AZ", "File;Content.-AZ");

        // When / Then
        for (String headerLine : headerLines) {
            assertThatHeaderNamesAreInvalid(headerLine, "Field name cannot start with '_' or '-'");
        }
    }

    @Test
    public void testHeaderValidation_IllegalFieldNamesReservedCharacters() {
        // Given
        List<String> headerLines = List.of(
            "File;Content.A\u0000Z",
            "File;Content.A\tZ",
            "File;Content.A\u001FZ",
            "File;Content.A\u007FZ",
            "File;Content.A\u009FZ",
            "File;Content.A A",
            "File;Content.A\tA",
            "File;Content.A\nA",
            "File;Content.A\rA",
            "File;Content.A'A",
            "File;Content.A\"A",
            "File;Content.A`A",
            "File;Content.A,A",
            "File;Content.A:A",
            "File;Content.A°A",
            "File;Content.A$A",
            "File;Content.A§A",
            "File;Content.A&A",
            "File;Content.A#A",
            "File;Content.A*A",
            "File;Content.A+A",
            "File;Content.A=A",
            "File;Content.A/A",
            "File;Content.A|A",
            "File;Content.A\\A",
            "File;Content.A(A",
            "File;Content.A)A",
            "File;Content.A{A",
            "File;Content.A}A",
            "File;Content.A[A",
            "File;Content.A]A",
            "File;Content.A@A",
            "File;Content.A~A",
            "File;Content.A^A",
            "File;Content.A!A",
            "File;Content.A?A",
            "File;Content.A<A",
            "File;Content.A>A",
            "File;Content.A%A"
        );

        // When / Then
        for (String headerLine : headerLines) {
            assertThatHeaderNamesAreInvalid(headerLine, "Reserved / illegal characters");
        }
    }

    @Test
    public void testHeaderValidation_InvalidTitleHeaderNames() {
        assertThatHeaderNamesAreInvalid(
            "File;Content.Title.subfield",
            "Valid Content.Title[.*] or Content.Title[.*].attr expected"
        );
        assertThatHeaderNamesAreInvalid("File;Content.Title.0.attr", "Missing base header name 'Content.Title.0'");
        assertThatHeaderNamesAreInvalid(
            "File;Content.Title.0;Content.Title.1.attr",
            "Missing base header name 'Content.Title.1'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Title;Content.Title.attr.0",
            "Reserved 'attr' keyword can only be used as a suffix"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Title;Content.Title.attr.subfield",
            "Reserved 'attr' keyword can only be used as a suffix"
        );
    }

    @Test
    public void testHeaderValidation_InvalidDescriptionHeaderNames() {
        assertThatHeaderNamesAreInvalid(
            "File;Content.Description.subfield",
            "Valid Content.Description[.*] or Content.Description[.*].attr expected"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Description.0.attr",
            "Missing base header name 'Content.Description.0'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Description.0;Content.Description.1.attr",
            "Missing base header name 'Content.Description.1'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Description;Content.Description.attr.0",
            "Reserved 'attr' keyword can only be used as a suffix"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Description;Content.Description.attr.subfield",
            "Reserved 'attr' keyword can only be used as a suffix"
        );
    }

    @Test
    public void testHeaderValidation_InvalidApiFieldNameAsSedaPath() {
        assertThatHeaderNamesAreInvalid(
            "File;Content.Title_",
            "Header must be Seda path 'Content.Title' instead of Vitam field name 'Content.Title_'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Title_.0",
            "Header must be Seda path 'Content.Title' instead of Vitam field name 'Content.Title_'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Description_",
            "Header must be Seda path 'Content.Description' instead of Vitam field name 'Content.Description_'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Description_.0",
            "Header must be Seda path 'Content.Description' instead of Vitam field name 'Content.Description_'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Signature.ReferencedObject.SignedObjectDigest.Algorithm",
            "Header must be Seda path 'Content.Signature.ReferencedObject.SignedObjectDigest.attr' instead of Vitam field name 'Content.Signature.ReferencedObject.SignedObjectDigest.Algorithm'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Signature.0.ReferencedObject.SignedObjectDigest.Algorithm",
            "Header must be Seda path 'Content.Signature.ReferencedObject.SignedObjectDigest.attr' instead of Vitam field name 'Content.Signature.ReferencedObject.SignedObjectDigest.Algorithm'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Signature.ReferencedObject.SignedObjectDigest.MessageDigest",
            "Header must be Seda path 'Content.Signature.ReferencedObject.SignedObjectDigest' instead of Vitam field name 'Content.Signature.ReferencedObject.SignedObjectDigest.MessageDigest'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Signature.0.ReferencedObject.SignedObjectDigest.MessageDigest",
            "Header must be Seda path 'Content.Signature.ReferencedObject.SignedObjectDigest' instead of Vitam field name 'Content.Signature.ReferencedObject.SignedObjectDigest.MessageDigest'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.evType",
            "Header must be Seda path 'Content.Event.EventType' instead of Vitam field name 'Content.Event.evType'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.evType",
            "Header must be Seda path 'Content.Event.EventType' instead of Vitam field name 'Content.Event.evType'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.evDetData",
            "Header must be Seda path 'Content.Event.EventDetailData' instead of Vitam field name 'Content.Event.evDetData'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.evDetData",
            "Header must be Seda path 'Content.Event.EventDetailData' instead of Vitam field name 'Content.Event.evDetData'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.evId",
            "Header must be Seda path 'Content.Event.EventIdentifier' instead of Vitam field name 'Content.Event.evId'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.evId",
            "Header must be Seda path 'Content.Event.EventIdentifier' instead of Vitam field name 'Content.Event.evId'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.evTypeDetail",
            "Header must be Seda path 'Content.Event.EventDetail' instead of Vitam field name 'Content.Event.evTypeDetail'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.evTypeDetail",
            "Header must be Seda path 'Content.Event.EventDetail' instead of Vitam field name 'Content.Event.evTypeDetail'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.evTypeProc",
            "Header must be Seda path 'Content.Event.EventTypeCode' instead of Vitam field name 'Content.Event.evTypeProc'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.evTypeProc",
            "Header must be Seda path 'Content.Event.EventTypeCode' instead of Vitam field name 'Content.Event.evTypeProc'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.linkingAgentIdentifier.LinkingAgentIdentifierValue",
            "Header must be Seda path 'Content.Event.LinkingAgentIdentifier' instead of Vitam field name 'Content.Event.linkingAgentIdentifier'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.linkingAgentIdentifier.LinkingAgentIdentifierValue",
            "Header must be Seda path 'Content.Event.LinkingAgentIdentifier' instead of Vitam field name 'Content.Event.linkingAgentIdentifier'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.linkingAgentIdentifier.LinkingAgentIdentifierType",
            "Header must be Seda path 'Content.Event.LinkingAgentIdentifier' instead of Vitam field name 'Content.Event.linkingAgentIdentifier'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.linkingAgentIdentifier.LinkingAgentIdentifierType",
            "Header must be Seda path 'Content.Event.LinkingAgentIdentifier' instead of Vitam field name 'Content.Event.linkingAgentIdentifier'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.linkingAgentIdentifier.LinkingAgentRole",
            "Header must be Seda path 'Content.Event.LinkingAgentIdentifier' instead of Vitam field name 'Content.Event.linkingAgentIdentifier'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.linkingAgentIdentifier.LinkingAgentRole",
            "Header must be Seda path 'Content.Event.LinkingAgentIdentifier' instead of Vitam field name 'Content.Event.linkingAgentIdentifier'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.evDateTime",
            "Header must be Seda path 'Content.Event.EventDateTime' instead of Vitam field name 'Content.Event.evDateTime'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.evDateTime",
            "Header must be Seda path 'Content.Event.EventDateTime' instead of Vitam field name 'Content.Event.evDateTime'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.outcome",
            "Header must be Seda path 'Content.Event.Outcome' instead of Vitam field name 'Content.Event.outcome'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.outcome",
            "Header must be Seda path 'Content.Event.Outcome' instead of Vitam field name 'Content.Event.outcome'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.outDetail",
            "Header must be Seda path 'Content.Event.OutcomeDetail' instead of Vitam field name 'Content.Event.outDetail'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.outDetail",
            "Header must be Seda path 'Content.Event.OutcomeDetail' instead of Vitam field name 'Content.Event.outDetail'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.outMessg",
            "Header must be Seda path 'Content.Event.OutcomeDetailMessage' instead of Vitam field name 'Content.Event.outMessg'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.outMessg",
            "Header must be Seda path 'Content.Event.OutcomeDetailMessage' instead of Vitam field name 'Content.Event.outMessg'"
        );
    }

    @Test
    public void testHeaderValidation_InvalidSedaExtensionPoints() {
        assertThatHeaderNamesAreInvalid(
            "File;Content.Title.MyExtension",
            "Valid Content.Title[.*] or Content.Title[.*].attr expected"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Description.MyExtension",
            "Valid Content.Description[.*] or Content.Description[.*].attr expected"
        );

        assertThatHeaderNamesAreInvalid("File;Content.Tag.MyExtension", "Field 'Content.Tag' is not an object.");

        assertThatHeaderNamesAreInvalid(
            "File;Content.Signature.MyExtension",
            "Invalid seda extension point 'Content.Signature'"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Signature.ReferencedObject.SignedObjectDigest.MyExtension",
            "Invalid seda extension point 'Content.Signature.ReferencedObject.SignedObjectDigest'"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.evId",
            "Header must be Seda path 'Content.Event.EventIdentifier' instead of Vitam field name 'Content.Event.evId'"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.MyExtension",
            "Invalid seda extension point 'Content.Event'. Invalid field 'MyExtension'. Available fields: [EventDateTime, EventDetail, EventDetailData, EventIdentifier, EventType, EventTypeCode, LinkingAgentIdentifier, Outcome, OutcomeDetail, OutcomeDetailMessage]"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Invoice.Provider.MyKeyword.MyExtension",
            "Field 'Content.Invoice.Provider.MyKeyword' is not an object."
        );

        assertThatHeaderNamesAreInvalid(
            "File;Management.Unknown",
            "Invalid seda extension point 'Management'. Invalid field 'Unknown'. Available fields: [AccessRule, AppraisalRule, ClassificationRule, DisseminationRule, HoldRule, NeedAuthorization, ReuseRule, StorageRule]"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Management.HoldRule.Unknown",
            "Invalid seda extension point 'Management.HoldRule'. Invalid field 'Unknown'. Available fields: [HoldEndDate, HoldOwner, HoldReason, HoldReassessingDate, PreventInheritance, PreventRearrangement, RefNonRuleId, Rule, StartDate]"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Management.HoldRule.EndDate",
            "Invalid seda extension point 'Management.HoldRule'. Invalid field 'EndDate'. Available fields: [HoldEndDate, HoldOwner, HoldReason, HoldReassessingDate, PreventInheritance, PreventRearrangement, RefNonRuleId, Rule, StartDate]"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.SigningInformation.DetachedRole",
            "Invalid seda extension point 'Content.SigningInformation'. Invalid field 'DetachedRole'. Available fields: [AdditionalProof, DetachedSigningRole, Extended, SignatureDescription, SigningRole, TimestampingInformation]"
        );

        assertThatHeaderNamesAreInvalid(
            "File;ArchiveUnitProfile.Extra",
            "Field 'ArchiveUnitProfile' is not an object."
        );
    }

    @Test
    public void testHeaderValidation_ValidSedaExtensionPoints() {
        assertThatHeaderNamesAreValid("File;Content.MyExtension");
        assertThatHeaderNamesAreValid("File;Content.MyExtension.SubField1");
        assertThatHeaderNamesAreValid("File;Content.MyExtension.SubField1;Content.MyExtension.SubField2");

        assertThatHeaderNamesAreValid("File;Content.Invoice.MyExtension");
        assertThatHeaderNamesAreValid("File;Content.Invoice.MyExtension.SubField1");
        assertThatHeaderNamesAreValid(
            "File;Content.Invoice.MyExtension.SubField1;Content.Invoice.MyExtension.SubField2"
        );

        assertThatHeaderNamesAreValid("File;Content.SigningInformation.Extended.MyExtension");
        assertThatHeaderNamesAreValid("File;Content.SigningInformation.Extended.MyExtension.SubField1");
        assertThatHeaderNamesAreValid(
            "File;Content.SigningInformation.Extended.MyExtension.SubField1;Content.SigningInformation.Extended.MyExtension.SubField2"
        );

        assertThatHeaderNamesAreValid("File;Content.OriginatingAgency.OrganizationDescriptiveMetadata.MyExtension");
        assertThatHeaderNamesAreValid(
            "File;Content.OriginatingAgency.OrganizationDescriptiveMetadata.MyExtension.SubField1"
        );
        assertThatHeaderNamesAreValid(
            "File;Content.OriginatingAgency.OrganizationDescriptiveMetadata.MyExtension.SubField1;Content.OriginatingAgency.OrganizationDescriptiveMetadata.MyExtension.SubField2"
        );

        assertThatHeaderNamesAreValid("File;Content.SubmissionAgency.OrganizationDescriptiveMetadata.MyExtension");
        assertThatHeaderNamesAreValid(
            "File;Content.SubmissionAgency.OrganizationDescriptiveMetadata.MyExtension.SubField1"
        );
        assertThatHeaderNamesAreValid(
            "File;Content.SubmissionAgency.OrganizationDescriptiveMetadata.MyExtension.SubField1;Content.SubmissionAgency.OrganizationDescriptiveMetadata.MyExtension.SubField2"
        );
    }

    @Test
    public void testHeaderValidation_InvalidAttrHeaderName() {
        assertThatHeaderNamesAreInvalid(
            "File;Content.attr.Title",
            "Reserved 'attr' keyword can only be used as a suffix"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Title.0;Content.Title.0.attr.0",
            "Reserved 'attr' keyword can only be used as a suffix"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Title.0;Content.Title.0.attr.0",
            "Reserved 'attr' keyword can only be used as a suffix"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Title.attr.attr",
            "Reserved 'attr' keyword can only be used as a suffix"
        );

        assertThatHeaderNamesAreInvalid("File;Content.Title.0.attr", "Missing base header name 'Content.Title.0'");

        assertThatHeaderNamesAreInvalid(
            "File;Content.Title.0;Content.Title.1.attr",
            "Missing base header name 'Content.Title.1'"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Title;Content.Title.attr.0",
            "Reserved 'attr' keyword can only be used as a suffix"
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.Title;Content.Title.attr.subfield",
            "Reserved 'attr' keyword can only be used as a suffix"
        );

        assertThatHeaderNamesAreInvalid("File;Content.MyExtension;Content.MyExtension.attr", "Reserved 'attr' suffix");

        assertThatHeaderNamesAreInvalid(
            "File;Content.MyExtension.attr",
            "Missing base header name 'Content.MyExtension'"
        );

        assertThatHeaderNamesAreInvalid("File;Content.Tag;Content.Tag.attr", "Reserved 'attr' suffix");
    }

    @Test
    public void testHeaderValidation_ObjectFieldsCannotBeHeaderNames() {
        assertThatHeaderNamesAreInvalid("File;Content.Writer", "Field 'Content.Writer' is an object.");
        assertThatHeaderNamesAreInvalid("File;Content.Invoice", "Field 'Content.Invoice' is an object.");
        assertThatHeaderNamesAreInvalid(
            "File;Management.AppraisalRule",
            "Field 'Management.AppraisalRule' is an object."
        );
    }

    @Test
    public void testHeaderValidation_InvalidArrayIndexForNonArrayFields() {
        assertThatHeaderNamesAreInvalid(
            "File;Content.Sender.Gender.0",
            "Field 'Content.Sender.Gender' is not an array"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Sender.0.Gender.0",
            "Field 'Content.Sender.Gender' is not an array"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.EventIdentifier.0",
            "Field 'Content.Event.EventIdentifier' is not an array"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Content.Event.0.EventIdentifier.0",
            "Field 'Content.Event.EventIdentifier' is not an array"
        );

        assertThatHeaderNamesAreInvalid(
            "File;Management.NeedAuthorization.0",
            "Field 'Management.NeedAuthorization' is not an array"
        );

        assertThatHeaderNamesAreInvalid("File;ArchiveUnitProfile.0", "Field 'ArchiveUnitProfile' is not an array");
    }

    @Test
    public void testHeaderValidation_ExternalFieldCannotBeAnObjectAndAValueField() {
        assertThatHeaderNamesAreInvalid(
            "File;Content.MyExtension;Content.MyExtension.SubField",
            "Field 'Content.MyExtension' is not an object."
        );
        assertThatHeaderNamesAreInvalid(
            "File;Content.MyExtension.SubField;Content.MyExtension",
            "Field 'Content.MyExtension' is an object."
        );
    }

    @Test
    public void testMultipleValidationErrors() {
        assertThatHeaderNamesAreInvalid(
            "File;Unknown;Content.Title_;Content.Description.0;Content.Description.2;Content.MyExtension;Content.MyExtension.SubField;Management.AppraisalRule;Management.StorageRule.Rule",
            """
            CSV validation failed. 5 errors:
            - Invalid header name 'Unknown': Invalid header name 'Unknown'. Only accepted names are 'File', 'Content.*', 'Management.*' or 'ArchiveUnitProfile'
            - Invalid header name 'Content.Title_': Header must be Seda path 'Content.Title' instead of Vitam field name 'Content.Title_'
            - Invalid header name 'Content.Description.2': Expected header name 'Content.Description.1' since header 'Content.Description.2' is declared
            - Invalid header name 'Content.MyExtension.SubField': Field 'Content.MyExtension' is not an object.
            - Invalid header name 'Management.AppraisalRule': Field 'Management.AppraisalRule' is an object."""
        );
    }

    private void assertThatHeaderNamesAreInvalid(String headersLine, String... errorMessages) {
        assertThatHeaderNamesAreInvalid(parseHeaders(headersLine), errorMessages);
    }

    private void assertThatHeaderNamesAreInvalid(List<String> headerNames, String... errorMessages) {
        assertThatThrownBy(() -> csvMetadataValidator.validateHeaderNames(sedaSchemaInfoResolver, headerNames))
            .isInstanceOf(CollectInvalidCsvFormatException.class)
            .hasMessageContaining("Invalid header name")
            .hasMessageContainingAll(errorMessages);
    }

    private void assertThatHeaderNamesAreValid(String headersLine) {
        assertThatHeaderNamesAreValid(parseHeaders(headersLine));
    }

    private void assertThatHeaderNamesAreValid(List<String> headerNames) {
        assertThatCode(
            () -> csvMetadataValidator.validateHeaderNames(sedaSchemaInfoResolver, headerNames)
        ).doesNotThrowAnyException();
    }

    private static List<String> parseHeaders(String headerLine) {
        return List.of(headerLine.split(";"));
    }

    private RequestResponse<SchemaResponse> loadUnitSchema() throws InvalidParseOperationException, IOException {
        List<SchemaResponse> unitSchemaModels = JsonHandler.getFromInputStreamAsTypeReference(
            PropertiesUtils.getResourceAsStream("unit-schema-with-custom-fields.json"),
            new TypeReference<>() {}
        );
        return new RequestResponseOK<SchemaResponse>().addAllResults(unitSchemaModels);
    }
}
