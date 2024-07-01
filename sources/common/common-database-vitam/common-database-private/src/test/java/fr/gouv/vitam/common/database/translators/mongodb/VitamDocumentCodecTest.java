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
package fr.gouv.vitam.common.database.translators.mongodb;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.json.JsonHandler;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class VitamDocumentCodecTest {

    private static class PseudoClass extends Document {

        /**
         *
         */
        private static final long serialVersionUID = 7416418759857986750L;

        public PseudoClass() {
            append("_id", "id01");
        }

        public PseudoClass(Document doc) {
            super(doc);
        }
    }

    @Test
    public final void testVitamDocumentCodec() {
        try {
            new VitamDocumentCodec<>(Document.class);
            fail("should failed");
        } catch (final IllegalArgumentException e) {
            // Ignore
        }
        final VitamDocumentCodec<PseudoClass> vitamDocumentCodec = new VitamDocumentCodec<>(PseudoClass.class);
        final PseudoClass document = new PseudoClass();
        assertTrue(vitamDocumentCodec.documentHasId(document));
        assertNotNull(vitamDocumentCodec.getDocumentId(document));
        assertNotNull(vitamDocumentCodec.generateIdIfAbsentFromDocument(document));
        assertEquals(PseudoClass.class, vitamDocumentCodec.getEncoderClass());
        document.remove("_id");
        assertFalse(vitamDocumentCodec.documentHasId(document));
        try {
            vitamDocumentCodec.getDocumentId(document);
            fail("should failed");
        } catch (final IllegalStateException e) {
            // Ignore
        }
        assertNotNull(vitamDocumentCodec.generateIdIfAbsentFromDocument(document));
        assertTrue(vitamDocumentCodec.documentHasId(document));
        assertNotNull(vitamDocumentCodec.getDocumentId(document));
    }

    @Test
    public final void testdiff() {
        ObjectNode obNode1 = JsonHandler.createObjectNode();
        obNode1.put("Name", "MyName \" é ");
        obNode1.put("_v", 0);
        obNode1.put("_a", "test\"");
        obNode1.set("Title_", JsonHandler.createObjectNode().put("FR", "$Title_FR"));
        obNode1.set(
            "elems",
            JsonHandler.createArrayNode().addAll(List.of(TextNode.valueOf("1"), TextNode.valueOf("A")))
        );

        ObjectNode obNode2 = JsonHandler.createObjectNode();
        obNode2.put("Name", "MyModifiedName \" é ");
        obNode2.put("_v", 1);
        obNode2.put("_a", "test1\"");
        obNode2.set("Title_", JsonHandler.createObjectNode().put("FR", "@Title_FR/New"));
        obNode2.set(
            "elems",
            JsonHandler.createArrayNode().addAll(List.of(TextNode.valueOf("Z"), TextNode.valueOf("A")))
        );

        List<String> list = VitamDocument.getUnifiedDiff(
            JsonHandler.prettyPrint(obNode1),
            JsonHandler.prettyPrint(obNode2)
        );

        List<String> result = VitamDocument.getConcernedDiffLines(list);

        List<String> expected = List.of(
            "-  \"Name\" : \"MyName \\\" \\u00E9 \"",
            "-  \"_v\" : 0",
            "-  \"_a\" : \"test\\\"\"",
            "-  \"Title_.FR\" : \"$Title_FR\"",
            "-  \"elems\" : [ \"1\", \"A\" ]",
            "+  \"Name\" : \"MyModifiedName \\\" \\u00E9 \"",
            "+  \"_v\" : 1",
            "+  \"_a\" : \"test1\\\"\"",
            "+  \"Title_.FR\" : \"@Title_FR/New\"",
            "+  \"elems\" : [ \"Z\", \"A\" ]"
        );

        Assertions.assertThat(result).containsExactly(expected.toArray(String[]::new));
    }
}
