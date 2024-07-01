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
package fr.gouv.vitam.worker.core.plugin.reclassification.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReclassificationRequestDslParserTest {

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void parseReclassificationRequest_validQuery() throws Exception {
        // Given
        JsonNode jsonRequest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("Reclassification/dsl_parsing/reclassification_query_complete.json")
        );

        ReclassificationRequestDslParser instance = new ReclassificationRequestDslParser();

        // When
        ParsedReclassificationDslRequest parsedReclassificationDslRequest = instance.parseReclassificationRequest(
            jsonRequest
        );

        // Then
        assertThat(parsedReclassificationDslRequest.getEntries()).hasSize(4);
        ParsedReclassificationDslRequestEntry entry1 = parsedReclassificationDslRequest.getEntries().get(0);
        assertThat(entry1.getSelectMultiQuery().getRoots()).isEmpty();
        assertThat(entry1.getSelectMultiQuery().getQueries()).hasSize(1);
        assertThat(entry1.getSelectMultiQuery().getQueries().get(0).getQUERY().exactToken()).isEqualTo("$exists");
        assertThat(entry1.getAttachments()).containsExactlyInAnyOrder("aeaqaaaaaagdmvr3abnwoak7fzjq75qaaaca");
        assertThat(entry1.getDetachments()).containsExactlyInAnyOrder(
            "aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacc",
            "aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacb"
        );

        ParsedReclassificationDslRequestEntry entry2 = parsedReclassificationDslRequest.getEntries().get(1);
        assertThat(entry2.getSelectMultiQuery().getRoots()).containsExactlyInAnyOrder(
            "aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacc"
        );
        assertThat(entry2.getSelectMultiQuery().getQueries()).hasSize(1);
        assertThat(entry2.getSelectMultiQuery().getQueries().get(0).getQUERY().exactToken()).isEqualTo("$exists");
        assertThat(entry2.getAttachments()).containsExactlyInAnyOrder("aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacd");
        assertThat(entry2.getDetachments()).containsExactlyInAnyOrder(
            "aeaqaaaaaagdmvr3abnwoak7fzjq75qaaace",
            "aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacf"
        );

        ParsedReclassificationDslRequestEntry entry3 = parsedReclassificationDslRequest.getEntries().get(2);
        assertThat(entry3.getAttachments()).containsExactlyInAnyOrder("aeaqaaaaaagdmvr3abnwoak7fzjq75qaaach");
        assertThat(entry3.getDetachments()).isEmpty();

        ParsedReclassificationDslRequestEntry entry4 = parsedReclassificationDslRequest.getEntries().get(3);
        assertThat(entry4.getAttachments()).isEmpty();
        assertThat(entry4.getDetachments()).containsExactlyInAnyOrder("aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacj");
    }

    @Test
    public void parseReclassificationRequest_emptyQuery() throws Exception {
        // Given
        JsonNode jsonRequest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("Reclassification/dsl_parsing/reclassification_query_empty.json")
        );

        ReclassificationRequestDslParser instance = new ReclassificationRequestDslParser();

        // When // Then
        assertThatThrownBy(() -> instance.parseReclassificationRequest(jsonRequest)).isInstanceOf(
            InvalidParseOperationException.class
        );
    }
}
