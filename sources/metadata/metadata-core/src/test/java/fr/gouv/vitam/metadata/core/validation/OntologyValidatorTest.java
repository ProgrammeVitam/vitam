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
package fr.gouv.vitam.metadata.core.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.metadata.core.validation.OntologyValidator.stringExceedsMaxLuceneUtf8StorageSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class OntologyValidatorTest {

    private static final String JSON_FILE_WITH_REPLACEABLE_FIELDS = "archive_unit_for_replacement.json";

    private OntologyValidator ontologyValidator;

    @Before
    public void initialize() {
        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setIdentifier("text").setType(OntologyType.TEXT),
            new OntologyModel().setIdentifier("keyword").setType(OntologyType.KEYWORD),
            new OntologyModel().setIdentifier("date").setType(OntologyType.DATE),
            new OntologyModel().setIdentifier("long").setType(OntologyType.LONG),
            new OntologyModel().setIdentifier("double").setType(OntologyType.DOUBLE),
            new OntologyModel().setIdentifier("boolean").setType(OntologyType.BOOLEAN),
            new OntologyModel().setIdentifier("geopoint").setType(OntologyType.GEO_POINT),
            new OntologyModel().setIdentifier("enum").setType(OntologyType.ENUM)
        );

        this.ontologyValidator = new OntologyValidator(() -> ontologyModels);
    }

    @Test
    public void ontologyWhenStringAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("text", "value");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenBooleanAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("text", true);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("text", "true");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenLongAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("text", -1234567890L);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("text", "-1234567890");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenDateAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("text", "2000-01-01");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenDateWithFourDigitsSecondsThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("text", "1992-01-20T08:07:06.0492");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenDateWithFiveDigitsSecondsThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("text", "1992-01-20T08:07:06.04928");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenDateTimeAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("text", "2000-01-01T00:00:00");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenDoubleAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("text", -1.23);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("text", "-1.23");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenStringAsKeywordConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("keyword", "value");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenBooleanAsKeywordConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("keyword", true);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("keyword", "true");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenLongAsKeywordConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("keyword", -1234567890L);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("keyword", "-1234567890");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenDateAsKeywordConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("keyword", "2000-01-01");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenDateTimeAsKeywordConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("keyword", "2000-01-01T00:00:00");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenDoubleAsKeywordConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("keyword", -1.23);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("keyword", "-1.23");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenInvalidStringAsLongConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("long", "value");

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenValidLongStringAsLongConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("long", "-1234567890");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("long", -1234567890L);
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenBooleanAsLongConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("long", true);

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenLongAsLongConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("long", -1234567890L);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenDateAsLongConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("long", "2000-01-01");

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenDateTimeAsLongConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("long", "2000-01-01T00:00:00");

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenDoubleAsLongConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("long", -1.0);

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenValidDoubleStringAsDoubleConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("double", "-1.23");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("double", -1.23);
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenInvalidDoubleStringAsDoubleConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("double", "toto");

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenBooleanAsDoubleConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("double", true);

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenLongAsDoubleConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("double", -1234567890L);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("double", -1234567890.0);
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenDateAsDoubleConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("double", "2000-01-01");

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenDateTimeAsDoubleConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("double", "2000-01-01T00:00:00");

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenDoubleAsDoubleConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("double", -1.0);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenInvalidStringAsDateConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("date", "value");

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenBooleanAsDateConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("date", true);

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenLongAsDateConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("date", -1234567890L);

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenDateAsDateConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("date", "2000-01-01");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenDateTimeAsDateConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("date", "2000-01-01T00:00:00");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenDateTimeAsDateTimeConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("date", "2019-03-01T10:05:08.583594Z");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedConversion = JsonHandler.createObjectNode().put("date", "2019-03-01T10:05:08.583594");
        assertThat(updatedData).isEqualTo(expectedConversion);
    }

    @Test
    public void ontologyWhenDoubleAsDateConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("date", -1.23);

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenValidBooleanStringAsBooleanConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("boolean", "true");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("boolean", true);
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenInvalidBooleanStringAsBooleanConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("boolean", "toto");

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenBooleanAsBooleanConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("boolean", true);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenLongAsBooleanConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("boolean", -1234567890L);

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenDateAsBooleanConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("boolean", "2000-01-01");

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenDateTimeAsBooleanConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("boolean", "2000-01-01T00:00:00");

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Test
    public void ontologyWhenDoubleAsBooleanConversionThenKO() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("boolean", -1.0);

        // When / Then
        assertThatThrowsMetadataValidationException(data);
    }

    @Deprecated
    @Test
    public void ontologyWhenStringAsEnumConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("enum", "value");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Deprecated
    @Test
    public void ontologyWhenBooleanAsEnumConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("enum", true);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("enum", "true");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Deprecated
    @Test
    public void ontologyWhenLongAsEnumConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("enum", -1234567890L);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("enum", "-1234567890");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Deprecated
    @Test
    public void ontologyWhenDateAsEnumConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("enum", "2000-01-01");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Deprecated
    @Test
    public void ontologyWhenDateTimeAsEnumConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("enum", "2000-01-01T00:00:00");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Deprecated
    @Test
    public void ontologyWhenDoubleAsEnumConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("enum", -1.23);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("enum", "-1.23");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Deprecated
    @Test
    public void ontologyWhenStringAsGeoPointConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("geopoint", "value");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Deprecated
    @Test
    public void ontologyWhenBooleanAsGeoPointConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("geopoint", true);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("geopoint", "true");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Deprecated
    @Test
    public void ontologyWhenLongAsGeoPointConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("geopoint", -1234567890L);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("geopoint", "-1234567890");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Deprecated
    @Test
    public void ontologyWhenDateAsGeoPointConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("geopoint", "2000-01-01");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Deprecated
    @Test
    public void ontologyWhenDateTimeAsGeoPointConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("geopoint", "2000-01-01T00:00:00");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Deprecated
    @Test
    public void ontologyWhenDoubleAsGeoPointConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("geopoint", -1.23);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("geopoint", "-1.23");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenUnknownStringAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("unknownStr", "value");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenUnknownBooleanAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("unknownBoolean", true);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("unknownBoolean", "true");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenUnknownLongAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("unknownLong", 1234567890L);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("unknownLong", "1234567890");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyWhenUnknownDateAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("unknownDate", "2000-01-01");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenUnknownDateTimeAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("unknownDate", "2000-01-01T00:00:00");

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        assertThat(updatedData).isEqualTo(data);
    }

    @Test
    public void ontologyWhenUnknownDoubleAsTextConversionThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode().put("unknownDouble", -1.23);

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode().put("unknownDouble", "-1.23");
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void ontologyComplexObjectThenOK() throws Exception {
        // Given
        JsonNode data = JsonHandler.createObjectNode()
            .set(
                "obj1",
                JsonHandler.createObjectNode()
                    .set(
                        "array",
                        JsonHandler.createArrayNode()
                            .add(
                                JsonHandler.createObjectNode()
                                    .set(
                                        "obj2",
                                        JsonHandler.createObjectNode()
                                            .put("text", 1)
                                            .put("keyword", 12)
                                            .put("long", 123L)
                                            .put("boolean", "true")
                                            .put("double", 1234.0)
                                    )
                            )
                    )
            );

        // When
        ObjectNode updatedData = ontologyValidator.verifyAndReplaceFields(data);

        // Then
        JsonNode expectedData = JsonHandler.createObjectNode()
            .set(
                "obj1",
                JsonHandler.createObjectNode()
                    .set(
                        "array",
                        JsonHandler.createArrayNode()
                            .add(
                                JsonHandler.createObjectNode()
                                    .set(
                                        "obj2",
                                        JsonHandler.createObjectNode()
                                            .put("text", "1")
                                            .put("keyword", "12")
                                            .put("long", 123L)
                                            .put("boolean", true)
                                            .put("double", 1234.0)
                                    )
                            )
                    )
            );
        assertThat(updatedData).isEqualTo(expectedData);
    }

    @Test
    public void loopAndReplaceInJsonTests() throws Exception {
        // Given
        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setType(OntologyType.LONG).setIdentifier("extLong"),
            new OntologyModel().setType(OntologyType.DOUBLE).setIdentifier("extDouble"),
            new OntologyModel().setType(OntologyType.BOOLEAN).setIdentifier("extBoolean"),
            new OntologyModel().setType(OntologyType.DATE).setIdentifier("BirthDate"),
            new OntologyModel().setType(OntologyType.KEYWORD).setIdentifier("Title"),
            new OntologyModel().setType(OntologyType.TEXT).setIdentifier("Description")
        );
        final OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);

        JsonNode jsonOriginArcUnit = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(JSON_FILE_WITH_REPLACEABLE_FIELDS)
        );
        JsonNode jsonArcUnit = ontologyValidator.verifyAndReplaceFields(jsonOriginArcUnit);
        jsonArcUnit.get("ArchiveUnit").get("extLong").forEach(j -> assertThat(j.isLong()).isTrue());
        jsonArcUnit.get("ArchiveUnit").get("extDouble").forEach(j -> assertThat(j.isDouble()).isTrue());
        jsonArcUnit.get("ArchiveUnit").get("extBoolean").forEach(j -> assertThat(j.isBoolean()).isTrue());
        assertEquals("2016-09-26", jsonArcUnit.get("ArchiveUnit").get("Writer").get(0).get("BirthDate").asText());
        assertThat("2016-09-26").isEqualTo(
            jsonArcUnit.get("ArchiveUnit").get("Writer").get(0).get("BirthDate").asText()
        );

        JsonNode jsonOriginArcUnit2 = jsonOriginArcUnit.deepCopy();
        JsonNode jsonOriginArcUnit3 = jsonOriginArcUnit.deepCopy();
        JsonNode jsonOriginArcUnit4 = jsonOriginArcUnit.deepCopy();
        JsonNode jsonOriginArcUnit5 = jsonOriginArcUnit.deepCopy();
        JsonNode jsonOriginArcUnit6 = jsonOriginArcUnit.deepCopy();

        ((ArrayNode) jsonOriginArcUnit.get("ArchiveUnit").get("extLong")).set(0, new DoubleNode(8.324));
        assertThatThrownBy(() -> ontologyValidator.verifyAndReplaceFields(jsonOriginArcUnit))
            .isInstanceOf(MetadataValidationException.class)
            .hasMessageContaining("extLong");

        ((ArrayNode) jsonOriginArcUnit2.get("ArchiveUnit").get("extBoolean")).set(0, new TextNode("NOT_TRUE"));
        assertThatThrownBy(() -> ontologyValidator.verifyAndReplaceFields(jsonOriginArcUnit2))
            .isInstanceOf(MetadataValidationException.class)
            .hasMessageContaining("extBoolean");

        ((ArrayNode) jsonOriginArcUnit3.get("ArchiveUnit").get("Writer")).set(
                0,
                JsonHandler.getFromString("{\"BirthDate\":\"01/01/2001\"}")
            );
        assertThatThrownBy(() -> ontologyValidator.verifyAndReplaceFields(jsonOriginArcUnit3))
            .isInstanceOf(MetadataValidationException.class)
            .hasMessageContaining("BirthDate");

        ((ArrayNode) jsonOriginArcUnit4.get("ArchiveUnit").get("extDouble")).set(0, new TextNode("notADouble"));
        assertThatThrownBy(() -> ontologyValidator.verifyAndReplaceFields(jsonOriginArcUnit4))
            .isInstanceOf(MetadataValidationException.class)
            .hasMessageContaining("extDouble");

        ((ObjectNode) jsonOriginArcUnit5.get("ArchiveUnit")).set(
                "Title",
                new TextNode(
                    "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\n"
                )
            );
        assertThatThrownBy(() -> ontologyValidator.verifyAndReplaceFields(jsonOriginArcUnit5))
            .isInstanceOf(MetadataValidationException.class)
            .hasMessageContaining(
                "Error: <Not accepted value for the Keyword field (Title) whose UTF8 encoding is longer than the max length 32766> on field 'Title'."
            );

        ((ObjectNode) jsonOriginArcUnit6.get("ArchiveUnit")).set(
                "Description",
                new TextNode(
                    "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\n"
                )
            );
        assertThatThrownBy(() -> ontologyValidator.verifyAndReplaceFields(jsonOriginArcUnit6))
            .isInstanceOf(MetadataValidationException.class)
            .hasMessageContaining(
                "Error: <Not accepted value for the text field (Description) whose UTF8 encoding is longer than the max length 32766> on field 'Description'."
            );
    }

    @Test
    public void should_have_error_when_wrong_type_date() throws Exception {
        // Given
        List<OntologyModel> ontologyModels = Collections.singletonList(
            new OntologyModel().setType(OntologyType.DATE).setIdentifier("MyDate")
        );
        final OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        JsonNode jsonArcUnit = JsonHandler.getFromString("{\"MyDate\" : \"Everything is awesome\"}");

        assertThatThrownBy(() -> ontologyValidator.verifyAndReplaceFields(jsonArcUnit))
            .isInstanceOf(MetadataValidationException.class)
            .hasMessageContaining("MyDate");
    }

    @Test
    public void should_have_no_error_with_datetime_iso8601() throws Exception {
        // Given
        List<OntologyModel> ontologyModels = Collections.singletonList(
            new OntologyModel().setType(OntologyType.DATE).setIdentifier("MyDate")
        );
        final OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        JsonNode jsonArcUnit = JsonHandler.getFromString("{\"MyDate\" : \"2018-07-20T16:15:45.685\"}");

        // When
        ObjectNode result = ontologyValidator.verifyAndReplaceFields(jsonArcUnit);

        // Then
        assertThat(result).isEqualTo(jsonArcUnit);
    }

    @Test
    public void should_have_no_error_with_date_xsd_date_and_xsd_datetime() throws Exception {
        // Given
        List<OntologyModel> ontologyModels = Collections.singletonList(
            new OntologyModel().setType(OntologyType.DATE).setIdentifier("MyDate")
        );
        final OntologyValidator ontologyValidator = new OntologyValidator(() -> ontologyModels);
        JsonNode jsonArcUnit = JsonHandler.getFromString(
            "{\"MyField\":[{\"MyDate\":\"2016/10/12\"},{\"MyDate\":\"2004-04-12T13:20:15.5\"},{\"MyDate\":\"2004-04-12T13:20:00-05:00\"},{\"MyDate\":\"2004-04-12T13:20:00Z\"},{\"MyDate\":\"2004-04-12\"},{\"MyDate\":\"-0045-01-01\"},{\"MyDate\":\"12045-01-01\"},{\"MyDate\":\"2004-04-12-05:00\"},{\"MyDate\":\"2004-04-12Z\"}]}"
        );

        // When
        ObjectNode result = ontologyValidator.verifyAndReplaceFields(jsonArcUnit);

        // Then
        JsonNode expected = JsonHandler.getFromString(
            "{\"MyField\":[{\"MyDate\":\"2016-10-12\"},{\"MyDate\":\"2004-04-12T13:20:15.5\"},{\"MyDate\":\"2004-04-12T13:20:00\"},{\"MyDate\":\"2004-04-12T13:20:00\"},{\"MyDate\":\"2004-04-12\"},{\"MyDate\":\"-0045-01-01\"},{\"MyDate\":\"+12045-01-01\"},{\"MyDate\":\"2004-04-12\"},{\"MyDate\":\"2004-04-12\"}]}"
        );
        JsonAssert.assertJsonEquals(expected, result);
    }

    private void assertThatThrowsMetadataValidationException(JsonNode data) {
        assertThatThrownBy(() -> ontologyValidator.verifyAndReplaceFields(data))
            .isInstanceOf(MetadataValidationException.class)
            .extracting(x -> ((MetadataValidationException) x).getErrorCode())
            .isEqualTo(MetadataValidationErrorCode.ONTOLOGY_VALIDATION_FAILURE);
    }

    @Test
    public void testStringExceedsMaxLuceneUtf8StorageSize() {
        // Empty
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("", 6)).isFalse();

        // 1 char = 1 UTF8 byte
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("abcde", 6)).isFalse();
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("abcdef", 6)).isTrue();
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("abcdefg", 6)).isTrue();

        // 1 char = 2 UTF8 byte
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("éé", 6)).isFalse();
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("ééé", 6)).isTrue();
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("ت", 6)).isFalse();
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("تت", 6)).isFalse();
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("تتت", 6)).isTrue();

        // 1 char = 3 UTF8 byte
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("☃", 6)).isFalse();
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("☃☃", 6)).isTrue();

        // Special unicode code points that require 2x UTF-16 characters : 😊 = 2 chars = 2x2 UTF8 bytes
        assertThat("😊".length()).isEqualTo(2);
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("😊", 6)).isFalse();
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("a😊", 6)).isFalse();
        assertThat(stringExceedsMaxLuceneUtf8StorageSize("aa😊", 6)).isTrue();
    }
}
