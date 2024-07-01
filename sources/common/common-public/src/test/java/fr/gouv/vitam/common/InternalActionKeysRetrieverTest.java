/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InternalActionKeysRetrieverTest {

    private final InternalActionKeysRetriever internalActionKeysRetriever = new InternalActionKeysRetriever();

    @Test
    public void should_return_internal_fields() throws Exception {
        // Given
        JsonNode query = JsonHandler.getFromInputStream(
            getClass()
                .getResourceAsStream(
                    "/preservation/internalActionKeysRetrieverTestFiles/queryActionSetInternalField.json"
                )
        );

        // When
        List<String> internalActionKeyFields = internalActionKeysRetriever.getInternalActionKeyFields(query);

        // Then
        assertThat(internalActionKeyFields).containsOnly("#sps");
    }

    @Test
    public void should_return_internal_fields_to_internal_object() throws Exception {
        // Given
        JsonNode query = JsonHandler.getFromInputStream(
            getClass()
                .getResourceAsStream(
                    "/preservation/internalActionKeysRetrieverTestFiles/queryActionSetInternalField2.json"
                )
        );

        // When
        List<String> internalActionKeyFields = internalActionKeysRetriever.getInternalActionKeyFields(query);

        // Then
        assertThat(internalActionKeyFields).containsExactly("#sps", "_id");
    }

    @Test
    public void should_return_internal_fields_to_internal_object_with_interpunct() throws Exception {
        // Given
        JsonNode query = JsonHandler.getFromInputStream(
            getClass()
                .getResourceAsStream(
                    "/preservation/internalActionKeysRetrieverTestFiles/queryActionSetInternalField3.json"
                )
        );

        // When
        List<String> internalActionKeyFields = internalActionKeysRetriever.getInternalActionKeyFields(query);

        // Then
        assertThat(internalActionKeyFields).containsExactly(
            "_firstExample",
            "_secondExample",
            "_thirdExample",
            "_fourthExample",
            "_fifthExample",
            "_sixthExample"
        );
    }

    @Test
    public void should_return_internal_fields_to_internal_object_with_interpuct2() throws Exception {
        // Given
        JsonNode query = JsonHandler.getFromInputStream(
            getClass()
                .getResourceAsStream(
                    "/preservation/internalActionKeysRetrieverTestFiles/queryActionSetInternalField4.json"
                )
        );

        // When
        List<String> internalActionKeyFields = internalActionKeysRetriever.getInternalActionKeyFields(query);

        // Then
        assertThat(internalActionKeyFields).containsExactly(
            "#firstExample",
            "#secondExample",
            "#thirdExample",
            "#fourthExample",
            "#fifthExample",
            "#sixthExample"
        );
    }

    @Test
    public void should_return_internal_fields_to_internal_object_with_interpuct3() throws Exception {
        // Given
        JsonNode query = JsonHandler.getFromInputStream(
            getClass()
                .getResourceAsStream(
                    "/preservation/internalActionKeysRetrieverTestFiles/queryActionSetInternalField5.json"
                )
        );

        // When
        List<String> internalActionKeyFields = internalActionKeysRetriever.getInternalActionKeyFields(query);

        // Then
        assertThat(internalActionKeyFields).containsExactly(
            "$firstExample",
            "$secondExample",
            "$thirdExample",
            "$fourthExample",
            "$fifthExample",
            "$sixthExample"
        );
    }

    @Test
    public void should_return_no_internal_fields() throws Exception {
        // Given
        JsonNode query = JsonHandler.getFromInputStream(
            getClass()
                .getResourceAsStream(
                    "/preservation/internalActionKeysRetrieverTestFiles/queryActionSetExternalField.json"
                )
        );

        // When
        List<String> internalActionKeyFields = internalActionKeysRetriever.getInternalActionKeyFields(query);

        // Then
        assertThat(internalActionKeyFields).isEmpty();
    }

    @Test
    public void should_return_internal_fields_for_regex() throws Exception {
        // Given
        JsonNode query = JsonHandler.getFromInputStream(
            getClass()
                .getResourceAsStream(
                    "/preservation/internalActionKeysRetrieverTestFiles/queryActionSetInternalFieldREGEX.json"
                )
        );

        // When
        List<String> internalActionKeyFields = internalActionKeysRetriever.getInternalActionKeyFields(query);

        // Then
        assertThat(internalActionKeyFields).containsOnly("_sedaVersion");
    }

    @Test
    public void should_return_any_forbidden_field_when_using_getInternalKeyFields() throws Exception {
        // Given
        JsonNode query = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/preservation/internalActionKeysRetrieverTestFiles/setAnyMetadata.json")
        );

        // When
        List<String> internalActionKeyFields = internalActionKeysRetriever.getInternalKeyFields(query);

        // Then
        assertThat(internalActionKeyFields).containsOnly(
            "$action",
            "$setregex",
            "$target",
            "$controlPattern",
            "$updatePattern",
            "$set",
            "$firstExample",
            "$secondExample",
            "$thirdExample",
            "$fourthExample",
            "$fifthExample",
            "$sixthExample",
            "#set",
            "#firstExample",
            "#secondExample",
            "#thirdExample",
            "#fourthExample",
            "#fifthExample",
            "#sixthExample",
            "_set",
            "_firstExample",
            "_secondExample",
            "_thirdExample",
            "_fourthExample",
            "_fifthExample",
            "_sixthExample"
        );
    }
}
