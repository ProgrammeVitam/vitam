package fr.gouv.vitam.worker.core.plugin.reclassification.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

public class ReclassificationRequestDslParserTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void parseReclassificationRequest_validQuery() throws Exception {

        // Given
        JsonNode jsonRequest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("Reclassification/dsl_parsing/reclassification_query_complete.json"));

        ReclassificationRequestDslParser instance = new ReclassificationRequestDslParser();

        // When
        ParsedReclassificationDslRequest parsedReclassificationDslRequest =
            instance.parseReclassificationRequest(jsonRequest);

        // Then
        assertThat(parsedReclassificationDslRequest.getEntries()).hasSize(4);
        ParsedReclassificationDslRequestEntry entry1 = parsedReclassificationDslRequest.getEntries().get(0);
        assertThat(entry1.getSelectMultiQuery().getRoots()).isEmpty();
        assertThat(entry1.getSelectMultiQuery().getQueries()).hasSize(1);
        assertThat(entry1.getSelectMultiQuery().getQueries().get(0).getQUERY().exactToken()).isEqualTo("$exists");
        assertThat(entry1.getAttachments()).containsExactlyInAnyOrder("aeaqaaaaaagdmvr3abnwoak7fzjq75qaaaca");
        assertThat(entry1.getDetachments()).containsExactlyInAnyOrder("aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacc",
            "aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacb");

        ParsedReclassificationDslRequestEntry entry2 = parsedReclassificationDslRequest.getEntries().get(1);
        assertThat(entry2.getSelectMultiQuery().getRoots()).containsExactlyInAnyOrder("aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacc");
        assertThat(entry2.getSelectMultiQuery().getQueries()).hasSize(1);
        assertThat(entry2.getSelectMultiQuery().getQueries().get(0).getQUERY().exactToken()).isEqualTo("$exists");
        assertThat(entry2.getAttachments()).containsExactlyInAnyOrder("aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacd");
        assertThat(entry2.getDetachments()).containsExactlyInAnyOrder("aeaqaaaaaagdmvr3abnwoak7fzjq75qaaace",
            "aeaqaaaaaagdmvr3abnwoak7fzjq75qaaacf");

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
            PropertiesUtils.getResourceAsStream("Reclassification/dsl_parsing/reclassification_query_empty.json"));

        ReclassificationRequestDslParser instance = new ReclassificationRequestDslParser();

        // When // Then
        assertThatThrownBy(() -> instance.parseReclassificationRequest(jsonRequest))
            .isInstanceOf(InvalidParseOperationException.class);
    }
}
