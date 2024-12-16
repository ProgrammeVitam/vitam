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
import fr.gouv.vitam.collect.internal.core.helpers.CsvHelper;
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
        CSVParser parser = CsvHelper.createParser(
            PropertiesUtils.getResourceAsStream("csv/metadata_full_seda_content.csv")
        );
        List<String> headerNames = parser.getHeaderNames();
        assertThatHeaderNamesAreValid(headerNames);
    }

    @Test
    public void testConvertInvalidHeader_DuplicateHeaders() {
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title", "Content.Title"),
            "Duplicate header name 'Content.Title'"
        );
    }

    @Test
    public void testConvertInvalidHeader_MissingFileHeader() {
        assertThatHeaderNamesAreInvalid(
            List.of("Content.Title", "Content.Description"),
            "Missing required 'File' header name"
        );
    }

    @Test
    public void testConvertInvalidHeader_NoHeaderToSet() {
        assertThatHeaderNamesAreInvalid(List.of("File"), "No header to set");
    }

    @Test
    public void testConvertInvalidHeader_HeaderSanityChecks_TooLong() {
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content." + StringUtils.repeat('A', 300)),
            "Invalid header name 'Content.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA...'. Too long"
        );
    }

    @Test
    public void testConvertInvalidHeader_InvalidCategory() {
        // Given
        List<String> headerLines = List.of(
            "File;Content.",
            "File;Management.",
            "File;Content.Title;Management.AppraisalRule.Rule;BadField"
        );

        // When / Then
        for (String headerLine : headerLines) {
            List<String> headerNames = List.of(headerLine.split(";"));
            assertThatHeaderNamesAreInvalid(
                headerNames,
                "Only accepted names are 'File', 'Content.*' or 'Management.*'"
            );
        }
    }

    @Test
    public void testConvertInvalidHeader_TooManyHeaders() {
        // Given
        List<String> headerNames = ListUtils.union(
            List.of("File"),
            IntStream.range(0, 10000).mapToObj(i -> "Content.Field" + i).toList()
        );

        // When / Then
        assertThatHeaderNamesAreInvalid(headerNames, "Too many header names 10001 (max = 10000)");
    }

    @Test
    public void testConvertInvalidHeader_MixImplicitAndExplicitArrayIndex() {
        // Given
        List<String> headerLines = List.of(
            "File;Content.SystemId;Content.SystemId.1",
            "File;Content.Description;Content.Description.attr;Content.Description.1;Content.Description.1.attr",
            "File;Content.Title;Content.Title.attr;Content.Title.1;Content.Title.1.attr"
        );

        // When / Then
        for (String headerLine : headerLines) {
            List<String> headerNames = List.of(headerLine.split(";"));
            assertThatHeaderNamesAreInvalid(
                headerNames,
                "Cannot mix implicit array and array index syntaxes for field"
            );
        }
    }

    @Test
    public void testConvertInvalidHeader_MissingHeaderIndex() {
        // Given
        List<String> headerLines = List.of(
            "File;Content.SystemId.0;Content.SystemId.2",
            "File;Content.Description.0;Content.Description.0.attr;Content.Description.2;Content.Description.2.attr",
            "File;Content.Title.0;Content.Title.0.attr;Content.Title.2;Content.Title.2.attr"
        );

        // When / Then
        for (String headerLine : headerLines) {
            List<String> headerNames = List.of(headerLine.split(";"));
            assertThatHeaderNamesAreInvalid(headerNames, "Missing field '");
        }
    }

    @Test
    public void testConvertInvalidHeader_InvalidArrayFields() {
        assertThatHeaderNamesAreInvalid(List.of("File", "Content.Field.0.0"), "Invalid array index");
        assertThatHeaderNamesAreInvalid(List.of("File", "Content.0.Field.0"), "Invalid array index");
    }

    @Test
    public void testConvertInvalidHeader_TooLargeArrayIndex() {
        // Given
        List<String> headerLines = List.of(
            "File;Content.SystemId.10000",
            "File;Content.Description.10000",
            "File;Content.Title.10000",
            "File;Management.HoldRule.Rule.10000"
        );

        // When / Then
        for (String headerLine : headerLines) {
            List<String> headerNames = List.of(headerLine.split(";"));
            assertThatHeaderNamesAreInvalid(headerNames, "Array index too large");
        }
    }

    @Test
    public void testConvertInvalidHeader_EmptyFieldName() {
        // Given
        List<String> headerLines = List.of(
            "File;Content..",
            "File;Content.A.",
            "File;Content..A",
            "File;Management.AppraisalRule.   .Rule"
        );

        // When / Then
        for (String headerLine : headerLines) {
            List<String> headerNames = List.of(headerLine.split(";"));
            assertThatHeaderNamesAreInvalid(headerNames, "Empty field name");
        }
    }

    @Test
    public void testConvertInvalidHeader_FieldNameContainingDigit() {
        assertThatCode(
            () -> csvMetadataValidator.validateHeaderNames(sedaSchemaInfoResolver, List.of("File", "Content.Field1"))
        ).doesNotThrowAnyException();
    }

    @Test
    public void testConvertInvalidHeader_FieldNameStartingWithDigit() {
        assertThatHeaderNamesAreInvalid(List.of("File", "Content.1Field"), "Field name cannot start with a digit");
    }

    @Test
    public void testConvertInvalidHeader_IllegalFieldNamesPrefix() {
        // Given
        List<String> headerLines = List.of("File;Content._AZ", "File;Content.-AZ");

        // When / Then
        for (String headerLine : headerLines) {
            List<String> headerNames = List.of(headerLine.split(";"));
            assertThatHeaderNamesAreInvalid(headerNames, "'. Field name cannot start with '_' or '-'");
        }
    }

    @Test
    public void testConvertInvalidHeader_IllegalFieldNamesReservedCharacters() {
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
            List<String> headerNames = List.of(headerLine.split(";"));
            assertThatHeaderNamesAreInvalid(headerNames, "'. Reserved characters");
        }
    }

    @Test
    public void testConvertInvalidHeader_InvalidTitleHeaderNames() {
        assertThatHeaderNamesAreInvalid(List.of("File", "Content.Title.subfield"), "Valid Content.Title.* expected");
        assertThatHeaderNamesAreInvalid(List.of("File", "Content.Title.0.0"), "Valid Content.Title.* expected");
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title.0.attr"),
            "Missing base header name 'Content.Title.0'"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title", "Content.Title.0.attr"),
            "Missing base header name 'Content.Title.0'"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title", "Content.Title.attr.0"),
            "Reserved 'attr' keyword can only be used as a prefix"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title", "Content.Title.attr.subfield"),
            "Reserved 'attr' keyword can only be used as a prefix"
        );
    }

    @Test
    public void testConvertInvalidHeader_InvalidDescriptionHeaderNames() {
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Description.subfield"),
            "Valid Content.Description.* expected"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Description.0.0"),
            "Valid Content.Description.* expected"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Description.0.attr"),
            "Missing base header name 'Content.Description.0'"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Description", "Content.Description.0.attr"),
            "Missing base header name 'Content.Description.0'"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Description", "Content.Description.attr.0"),
            "Reserved 'attr' keyword can only be used as a prefix"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Description", "Content.Description.attr.subfield"),
            "Reserved 'attr' keyword can only be used as a prefix"
        );
    }

    @Test
    public void testConvertInvalidHeader_InvalidApiFieldNameAsSedaPath() {
        // Given
        List<String> headerLines = List.of(
            "File;Content.Title_",
            "File;Content.Title_.0",
            "File;Content.Description_",
            "File;Content.Description_.0",
            "File;Content.Signature.ReferencedObject.SignedObjectDigest.Algorithm",
            "File;Content.Signature.0.ReferencedObject.SignedObjectDigest.Algorithm",
            "File;Content.Signature.ReferencedObject.SignedObjectDigest.MessageDigest",
            "File;Content.Signature.0.ReferencedObject.SignedObjectDigest.MessageDigest",
            "File;Content.Event.evType",
            "File;Content.Event.0.evType",
            "File;Content.Event.evDetData",
            "File;Content.Event.0.evDetData",
            "File;Content.Event.evId",
            "File;Content.Event.0.evId",
            "File;Content.Event.evTypeDetail",
            "File;Content.Event.0.evTypeDetail",
            "File;Content.Event.evTypeProc",
            "File;Content.Event.0.evTypeProc",
            "File;Content.Event.linkingAgentIdentifier.LinkingAgentIdentifierValue",
            "File;Content.Event.0.linkingAgentIdentifier.LinkingAgentIdentifierValue",
            "File;Content.Event.linkingAgentIdentifier.LinkingAgentIdentifierType",
            "File;Content.Event.0.linkingAgentIdentifier.LinkingAgentIdentifierType",
            "File;Content.Event.linkingAgentIdentifier.LinkingAgentRole",
            "File;Content.Event.0.linkingAgentIdentifier.LinkingAgentRole",
            "File;Content.Event.evDateTime",
            "File;Content.Event.0.evDateTime",
            "File;Content.Event.outcome",
            "File;Content.Event.0.outcome",
            "File;Content.Event.outDetail",
            "File;Content.Event.0.outDetail",
            "File;Content.Event.outMessg",
            "File;Content.Event.0.outMessg"
        );

        // When / Then
        for (String headerLine : headerLines) {
            List<String> headerNames = List.of(headerLine.split(";"));
            assertThatHeaderNamesAreInvalid(headerNames, " Header must be Seda field name");
        }
    }

    @Test
    public void testConvertInvalidHeader_InvalidSedaExtensionPoints() {
        assertThatHeaderNamesAreInvalid(List.of("File", "Content.Title.MyExtension"), "Valid Content.Title.* expected");

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Description.MyExtension"),
            "Valid Content.Description.* expected"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Tag.MyExtension"),
            "Field 'Content.Tag' is not an object."
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Signature.MyExtension"),
            "Invalid seda extension point 'Content.Signature'"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Signature.ReferencedObject.SignedObjectDigest.MyExtension"),
            "Invalid seda extension point 'Content.Signature.ReferencedObject.SignedObjectDigest'"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Event.MyExtension"),
            "Invalid seda extension point 'Content.Event'"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Invoice.Provider.MyKeyword.MyExtension"),
            "Field 'Content.Invoice.Provider.MyKeyword' is not an object"
        );
    }

    @Test
    public void testConvertInvalidHeader_ValidSedaExtensionPoints() {
        assertThatHeaderNamesAreValid(List.of("File", "Content.MyExtension"));
        assertThatHeaderNamesAreValid(List.of("File", "Content.MyExtension.SubField1"));
        assertThatHeaderNamesAreValid(
            List.of("File", "Content.MyExtension.SubField1", "Content.MyExtension.SubField2")
        );

        assertThatHeaderNamesAreValid(List.of("File", "Content.Invoice.MyExtension"));
        assertThatHeaderNamesAreValid(List.of("File", "Content.Invoice.MyExtension.SubField1"));
        assertThatHeaderNamesAreValid(
            List.of("File", "Content.Invoice.MyExtension.SubField1", "Content.Invoice.MyExtension.SubField2")
        );

        assertThatHeaderNamesAreValid(List.of("File", "Content.SigningInformation.Extended.MyExtension"));
        assertThatHeaderNamesAreValid(List.of("File", "Content.SigningInformation.Extended.MyExtension.SubField1"));
        assertThatHeaderNamesAreValid(
            List.of(
                "File",
                "Content.SigningInformation.Extended.MyExtension.SubField1",
                "Content.SigningInformation.Extended.MyExtension.SubField2"
            )
        );

        assertThatHeaderNamesAreValid(
            List.of("File", "Content.OriginatingAgency.OrganizationDescriptiveMetadata.MyExtension")
        );
        assertThatHeaderNamesAreValid(
            List.of("File", "Content.OriginatingAgency.OrganizationDescriptiveMetadata.MyExtension.SubField1")
        );
        assertThatHeaderNamesAreValid(
            List.of(
                "File",
                "Content.OriginatingAgency.OrganizationDescriptiveMetadata.MyExtension.SubField1",
                "Content.OriginatingAgency.OrganizationDescriptiveMetadata.MyExtension.SubField2"
            )
        );

        assertThatHeaderNamesAreValid(
            List.of("File", "Content.SubmissionAgency.OrganizationDescriptiveMetadata.MyExtension")
        );
        assertThatHeaderNamesAreValid(
            List.of("File", "Content.SubmissionAgency.OrganizationDescriptiveMetadata.MyExtension.SubField1")
        );
        assertThatHeaderNamesAreValid(
            List.of(
                "File",
                "Content.SubmissionAgency.OrganizationDescriptiveMetadata.MyExtension.SubField1",
                "Content.SubmissionAgency.OrganizationDescriptiveMetadata.MyExtension.SubField2"
            )
        );
    }

    @Test
    public void testConvertInvalidHeader_InvalidAttrHeaderName() {
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.attr.Title"),
            "Reserved 'attr' keyword can only be used as a prefix"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title.0", "Content.Title.attr.0"),
            "Reserved 'attr' keyword can only be used as a prefix"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title.0", "Content.Title.0.attr.0"),
            "Reserved 'attr' keyword can only be used as a prefix"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title.attr.attr"),
            "Reserved 'attr' keyword can only be used as a prefix"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title.0.attr"),
            "Missing base header name 'Content.Title.0'"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title", "Content.Title.0.attr"),
            "Missing base header name 'Content.Title.0'"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title", "Content.Title.attr.0"),
            "Reserved 'attr' keyword can only be used as a prefix"
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Title", "Content.Title.attr.subfield"),
            "Reserved 'attr' keyword can only be used as a prefix"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.MyExtension", "Content.MyExtension.attr"),
            "Reserved 'attr' suffix"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.MyExtension.attr"),
            "Missing base header name 'Content.MyExtension'"
        );

        assertThatHeaderNamesAreInvalid(List.of("File", "Content.Tag", "Content.Tag.attr"), "Reserved 'attr' suffix");
    }

    @Test
    public void testConvertInvalidHeader_ObjectFieldsCannotBeHeaderNames() {
        assertThatHeaderNamesAreInvalid(List.of("File", "Content.Writer"), "Field 'Content.Writer' is an object.");

        assertThatHeaderNamesAreInvalid(List.of("File", "Content.Invoice"), "Field 'Content.Invoice' is an object.");
    }

    @Test
    public void testConvertInvalidHeader_InvalidArrayIndexForNonArrayFields() {
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Sender.Gender.0"),
            "Field 'Content.Sender.Gender' is not an array"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Sender.0.Gender.0"),
            "Field 'Content.Sender.Gender' is not an array"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Event.EventIdentifier.0"),
            "Field 'Content.Event.EventIdentifier' is not an array"
        );

        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.Event.0.EventIdentifier.0"),
            "Field 'Content.Event.EventIdentifier' is not an array"
        );
    }

    @Test
    public void testConvertInvalidHeader_ExternalFieldCannotBeAnObjectAndAValueField() {
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.MyExtension", "Content.MyExtension.SubField"),
            "Field 'Content.MyExtension' is not an object."
        );
        assertThatHeaderNamesAreInvalid(
            List.of("File", "Content.MyExtension.SubField", "Content.MyExtension"),
            "Field 'Content.MyExtension' is an object."
        );
    }

    private void assertThatHeaderNamesAreInvalid(List<String> headerNames, String errorMessage) {
        assertThatThrownBy(() -> csvMetadataValidator.validateHeaderNames(sedaSchemaInfoResolver, headerNames))
            .isInstanceOf(CollectInvalidCsvFormatException.class)
            .hasMessageContaining("Invalid header name")
            .hasMessageContaining(errorMessage);
    }

    private void assertThatHeaderNamesAreValid(List<String> headerNames) {
        assertThatCode(
            () -> csvMetadataValidator.validateHeaderNames(sedaSchemaInfoResolver, headerNames)
        ).doesNotThrowAnyException();
    }

    private RequestResponse<SchemaResponse> loadUnitSchema() throws InvalidParseOperationException, IOException {
        List<SchemaResponse> unitSchemaModels = JsonHandler.getFromInputStreamAsTypeReference(
            PropertiesUtils.getResourceAsStream("unit-schema-with-custom-fields.json"),
            new TypeReference<>() {}
        );
        return new RequestResponseOK<SchemaResponse>().addAllResults(unitSchemaModels);
    }
}
