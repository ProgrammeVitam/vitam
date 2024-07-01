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
package fr.gouv.vitam.elastic.kibana.interceptor.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReplacePatternUtilsTest {

    private static final String UNDERSCORE_JSON = "underscore.json";
    private static final String SHARP_JSON = "sharp.json";

    @Test
    public void should_replace_underscore_character() throws Exception {
        // Given
        JsonNode fromInputStream = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(UNDERSCORE_JSON));
        List<String> whiteList = new ArrayList<>(Arrays.asList("tenant", "sp", "v", "storage"));
        ReplacePatternUtils replacePatternUtils = new ReplacePatternUtils(whiteList);
        // When
        String bodyWithoutUnderscore = replacePatternUtils.replaceUnderscoreBySharp(
            JsonHandler.unprettyPrint(fromInputStream)
        );
        // Then
        assertThat(bodyWithoutUnderscore).contains("#tenant").contains("#v").contains("#storage.#nbc");
        assertThat(bodyWithoutUnderscore).contains("_tenantx").contains("test._tenant");
    }

    @Test
    public void should_replace_sharp_character() throws Exception {
        // Given
        JsonNode fromInputStream = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(SHARP_JSON));
        List<String> whiteList = new ArrayList<>(Arrays.asList("tenant", "sp", "v", "storage"));
        ReplacePatternUtils replacePatternUtils = new ReplacePatternUtils(whiteList);
        // When
        String bodyWithoutSharp = replacePatternUtils.replaceSharpByUnderscore(
            JsonHandler.unprettyPrint(fromInputStream)
        );

        // Then
        assertThat(bodyWithoutSharp).contains("_tenant").contains("_v").contains("_storage._nbc");
        assertThat(bodyWithoutSharp).contains("#tenantx").contains("test.#tenant");
    }
}
