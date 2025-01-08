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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.exceptions.CollectInvalidCsvFormatException;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doReturn;

public class CsvToJsonConverterTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    private SedaSchemaInfoResolver sedaSchemaInfoResolver;

    @Before
    public void setUp() throws Exception {
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(loadUnitSchema()).when(adminManagementClient).getUnitSchema();
        sedaSchemaInfoResolver = new SedaSchemaInfoResolver(adminManagementClientFactory);
    }

    @Test
    public void testNormalizedContentHeaderNames() throws CollectInternalException {
        // Given
        List<String> headerNames = List.of(
            "File",
            "Content.Title",
            "Content.Description.0",
            "Content.Description.0.attr",
            "Content.Event.0.EventIdentifier",
            "Content.Event.0.EventDateTime",
            "Content.Event.0.EventDetailData",
            "Content.Event.0.EventDetail",
            "Content.Event.0.EventTypeCode",
            "Content.Event.0.EventType",
            "Content.Event.0.OutcomeDetailMessage",
            "Content.Event.0.OutcomeDetail",
            "Content.Event.0.Outcome",
            "Content.Event.0.LinkingAgentIdentifier.LinkingAgentIdentifierType",
            "Content.Event.0.LinkingAgentIdentifier.LinkingAgentIdentifierValue",
            "Content.Event.1.LinkingAgentIdentifier.0.LinkingAgentRole",
            "Content.Event.1.LinkingAgentIdentifier.1.LinkingAgentRole",
            "Content.DescriptionLevel",
            "Content.Invoice.0.Provider.0.MyDate",
            "Content.Invoice.0.Provider.1.NonDeclaredSubField",
            "Content.SomeExternalField",
            "Content.SomeOtherExternalField.SomeSubField1",
            "Content.SomeOtherExternalField.SomeSubField2.0",
            "Content.SomeOtherExternalField.SomeSubField2.1",
            "Content.Signature.ReferencedObject.SignedObjectId",
            "Content.Signature.ReferencedObject.SignedObjectDigest",
            "Content.Signature.ReferencedObject.SignedObjectDigest.attr"
        );

        // When
        CsvToJsonConverter csvToJsonConverter = new CsvToJsonConverter(sedaSchemaInfoResolver, headerNames);
        Map<String, String> normalizedHeaderNames = csvToJsonConverter.getNormalizedContentHeaderMap();

        // Then
        assertThat(normalizedHeaderNames).hasSize(headerNames.size() - 4);
        assertThat(normalizedHeaderNames).doesNotContainKeys(
            "File",
            "Content.Title",
            "Content.Description.0",
            "Content.Description.0.attr"
        );
        assertThat(normalizedHeaderNames.keySet()).allSatisfy(headerNames::contains);

        assertThat(normalizedHeaderNames.get("Content.Event.0.EventIdentifier")).isEqualTo("Event[0].evId");
        assertThat(normalizedHeaderNames.get("Content.Event.0.EventDateTime")).isEqualTo("Event[0].evDateTime");
        assertThat(normalizedHeaderNames.get("Content.Event.0.EventDetailData")).isEqualTo("Event[0].evDetData");
        assertThat(normalizedHeaderNames.get("Content.Event.0.EventDetail")).isEqualTo("Event[0].evTypeDetail");
        assertThat(normalizedHeaderNames.get("Content.Event.0.EventTypeCode")).isEqualTo("Event[0].evTypeProc");
        assertThat(normalizedHeaderNames.get("Content.Event.0.EventType")).isEqualTo("Event[0].evType");
        assertThat(normalizedHeaderNames.get("Content.Event.0.OutcomeDetailMessage")).isEqualTo("Event[0].outMessg");
        assertThat(normalizedHeaderNames.get("Content.Event.0.OutcomeDetail")).isEqualTo("Event[0].outDetail");
        assertThat(normalizedHeaderNames.get("Content.Event.0.Outcome")).isEqualTo("Event[0].outcome");
        assertThat(
            normalizedHeaderNames.get("Content.Event.0.LinkingAgentIdentifier.LinkingAgentIdentifierType")
        ).isEqualTo("Event[0].linkingAgentIdentifier[0].LinkingAgentIdentifierType");
        assertThat(
            normalizedHeaderNames.get("Content.Event.0.LinkingAgentIdentifier.LinkingAgentIdentifierValue")
        ).isEqualTo("Event[0].linkingAgentIdentifier[0].LinkingAgentIdentifierValue");
        assertThat(normalizedHeaderNames.get("Content.Event.1.LinkingAgentIdentifier.0.LinkingAgentRole")).isEqualTo(
            "Event[1].linkingAgentIdentifier[0].LinkingAgentRole"
        );
        assertThat(normalizedHeaderNames.get("Content.Event.1.LinkingAgentIdentifier.1.LinkingAgentRole")).isEqualTo(
            "Event[1].linkingAgentIdentifier[1].LinkingAgentRole"
        );

        assertThat(normalizedHeaderNames.get("Content.DescriptionLevel")).isEqualTo("DescriptionLevel");
        assertThat(normalizedHeaderNames.get("Content.Invoice.0.Provider.0.MyDate")).isEqualTo(
            "Invoice[0].Provider[0].MyDate[0]"
        );
        assertThat(normalizedHeaderNames.get("Content.Invoice.0.Provider.1.NonDeclaredSubField")).isEqualTo(
            "Invoice[0].Provider[1].NonDeclaredSubField[0]"
        );
        assertThat(normalizedHeaderNames.get("Content.SomeExternalField")).isEqualTo("SomeExternalField[0]");
        assertThat(normalizedHeaderNames.get("Content.SomeOtherExternalField.SomeSubField1")).isEqualTo(
            "SomeOtherExternalField[0].SomeSubField1[0]"
        );
        assertThat(normalizedHeaderNames.get("Content.SomeOtherExternalField.SomeSubField2.0")).isEqualTo(
            "SomeOtherExternalField[0].SomeSubField2[0]"
        );
        assertThat(normalizedHeaderNames.get("Content.SomeOtherExternalField.SomeSubField2.1")).isEqualTo(
            "SomeOtherExternalField[0].SomeSubField2[1]"
        );

        assertThat(normalizedHeaderNames.get("Content.Signature.ReferencedObject.SignedObjectId")).isEqualTo(
            "Signature[0].ReferencedObject.SignedObjectId"
        );
        assertThat(normalizedHeaderNames.get("Content.Signature.ReferencedObject.SignedObjectDigest")).isEqualTo(
            "Signature[0].ReferencedObject.SignedObjectDigest.MessageDigest"
        );
        assertThat(normalizedHeaderNames.get("Content.Signature.ReferencedObject.SignedObjectDigest.attr")).isEqualTo(
            "Signature[0].ReferencedObject.SignedObjectDigest.Algorithm"
        );
    }

    @Test
    public void testConvertComplexDescription() throws Exception {
        // Given
        CSVParser parser = CsvHelper.createParser(
            PropertiesUtils.getResourceAsStream("csv/metadata_description_complex.csv")
        );
        List<String> headerNames = parser.getHeaderNames();
        List<CSVRecord> records = parser.getRecords();

        // When
        CsvToJsonConverter csvToJsonConverter = new CsvToJsonConverter(sedaSchemaInfoResolver, headerNames);
        ObjectNode unit0 = csvToJsonConverter.convertCsvRecordToJson(records.get(0));
        ObjectNode unit1 = csvToJsonConverter.convertCsvRecordToJson(records.get(1));
        ObjectNode unit2 = csvToJsonConverter.convertCsvRecordToJson(records.get(2));
        ObjectNode unit3 = csvToJsonConverter.convertCsvRecordToJson(records.get(3));
        ObjectNode unit4 = csvToJsonConverter.convertCsvRecordToJson(records.get(4));
        ThrowingCallable unit5Invocation = () -> csvToJsonConverter.convertCsvRecordToJson(records.get(5));
        ThrowingCallable unit6Invocation = () -> csvToJsonConverter.convertCsvRecordToJson(records.get(6));
        ThrowingCallable unit7Invocation = () -> csvToJsonConverter.convertCsvRecordToJson(records.get(7));
        ThrowingCallable unit8Invocation = () -> csvToJsonConverter.convertCsvRecordToJson(records.get(8));

        // Then
        assertJsonEquals(unit0, "csv/metadata_description_complex_expected_unit0.json");
        assertJsonEquals(unit1, "csv/metadata_description_complex_expected_unit1.json");
        assertJsonEquals(unit2, "csv/metadata_description_complex_expected_unit2.json");
        assertJsonEquals(unit3, "csv/metadata_description_complex_expected_unit3.json");
        assertJsonEquals(unit4, "csv/metadata_description_complex_expected_unit4.json");
        assertThatCode(unit5Invocation)
            .isInstanceOf(CollectInvalidCsvFormatException.class)
            .hasMessageContaining("Multiple values for 'Content.Description' header");
        assertThatCode(unit6Invocation)
            .isInstanceOf(CollectInvalidCsvFormatException.class)
            .hasMessageContaining("Multiple values for 'Content.Description' header with same lang attribute 'fr'");
        assertThatCode(unit7Invocation)
            .isInstanceOf(CollectInvalidCsvFormatException.class)
            .hasMessageContaining(
                "Invalid lang value '_illegal' for 'Content.Description.*': Field name cannot start with '_' or '-'"
            );
        assertThatCode(unit8Invocation)
            .isInstanceOf(CollectInvalidCsvFormatException.class)
            .hasMessageContaining("Invalid xml:lang attribute for header 'Content.Description.0.attr'");
    }

    @Test
    public void testConvertComplexTitle() throws Exception {
        // Given
        CSVParser parser = CsvHelper.createParser(
            PropertiesUtils.getResourceAsStream("csv/metadata_title_complex.csv")
        );
        List<String> headerNames = parser.getHeaderNames();
        List<CSVRecord> records = parser.getRecords();

        // When
        CsvToJsonConverter csvToJsonConverter = new CsvToJsonConverter(sedaSchemaInfoResolver, headerNames);
        ObjectNode unit0 = csvToJsonConverter.convertCsvRecordToJson(records.get(0));
        ObjectNode unit1 = csvToJsonConverter.convertCsvRecordToJson(records.get(1));
        ObjectNode unit2 = csvToJsonConverter.convertCsvRecordToJson(records.get(2));
        ObjectNode unit3 = csvToJsonConverter.convertCsvRecordToJson(records.get(3));
        ObjectNode unit4 = csvToJsonConverter.convertCsvRecordToJson(records.get(4));
        ThrowingCallable unit5Invocation = () -> csvToJsonConverter.convertCsvRecordToJson(records.get(5));
        ThrowingCallable unit6Invocation = () -> csvToJsonConverter.convertCsvRecordToJson(records.get(6));
        ThrowingCallable unit7Invocation = () -> csvToJsonConverter.convertCsvRecordToJson(records.get(7));
        ThrowingCallable unit8Invocation = () -> csvToJsonConverter.convertCsvRecordToJson(records.get(8));

        // Then
        assertJsonEquals(unit0, "csv/metadata_title_complex_expected_unit0.json");
        assertJsonEquals(unit1, "csv/metadata_title_complex_expected_unit1.json");
        assertJsonEquals(unit2, "csv/metadata_title_complex_expected_unit2.json");
        assertJsonEquals(unit3, "csv/metadata_title_complex_expected_unit3.json");
        assertJsonEquals(unit4, "csv/metadata_title_complex_expected_unit4.json");
        assertThatCode(unit5Invocation)
            .isInstanceOf(CollectInvalidCsvFormatException.class)
            .hasMessageContaining("Multiple values for 'Content.Title' header");
        assertThatCode(unit6Invocation)
            .isInstanceOf(CollectInvalidCsvFormatException.class)
            .hasMessageContaining("Multiple values for 'Content.Title' header with same lang attribute 'fr'");
        assertThatCode(unit7Invocation)
            .isInstanceOf(CollectInvalidCsvFormatException.class)
            .hasMessageContaining(
                "Invalid lang value '_illegal' for 'Content.Title.*': Field name cannot start with '_' or '-'"
            );
        assertThatCode(unit8Invocation)
            .isInstanceOf(CollectInvalidCsvFormatException.class)
            .hasMessageContaining("Invalid xml:lang attribute for header 'Content.Title.0.attr'");
    }

    @Test
    public void testConvertFullSedaContent() throws Exception {
        // Given
        CSVParser parser = CsvHelper.createParser(
            PropertiesUtils.getResourceAsStream("csv/metadata_full_seda_content.csv")
        );
        List<String> headerNames = parser.getHeaderNames();
        List<CSVRecord> records = parser.getRecords();

        // When
        CsvToJsonConverter csvToJsonConverter = new CsvToJsonConverter(sedaSchemaInfoResolver, headerNames);
        ObjectNode unit0 = csvToJsonConverter.convertCsvRecordToJson(records.get(0));
        ObjectNode unit1 = csvToJsonConverter.convertCsvRecordToJson(records.get(1));

        // Then
        assertJsonEquals(unit0, "csv/metadata_full_seda_content_expected_unit0.json");
        assertJsonEquals(unit1, "csv/metadata_full_seda_content_expected_unit1.json");
    }

    @Test
    public void testConvertFullSedaContentImplicitArrayIndexHeaders() throws Exception {
        // Given
        CSVParser parser = CsvHelper.createParser(
            PropertiesUtils.getResourceAsStream("csv/metadata_full_seda_content_implicit_index.csv")
        );
        List<String> headerNames = parser.getHeaderNames();
        List<CSVRecord> records = parser.getRecords();

        // When
        CsvToJsonConverter csvToJsonConverter = new CsvToJsonConverter(sedaSchemaInfoResolver, headerNames);
        ObjectNode unit0 = csvToJsonConverter.convertCsvRecordToJson(records.get(0));

        // Then
        assertJsonEquals(unit0, "csv/metadata_full_seda_content_implicit_index_expected_unit0.json");
    }

    public RequestResponse<SchemaResponse> loadUnitSchema() throws InvalidParseOperationException, IOException {
        List<SchemaResponse> unitSchemaModels = JsonHandler.getFromInputStreamAsTypeReference(
            PropertiesUtils.getResourceAsStream("unit-schema-with-custom-fields.json"),
            new TypeReference<>() {}
        );
        return new RequestResponseOK<SchemaResponse>().addAllResults(unitSchemaModels);
    }

    private static void assertJsonEquals(JsonNode actual, String expectedJsonResource)
        throws FileNotFoundException, InvalidParseOperationException {
        JsonAssert.assertJsonEquals(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(expectedJsonResource)),
            actual,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths("#management")
        );
    }
}
