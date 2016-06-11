package fr.gouv.vitam.core.database.collections.translator.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.bson.Document;
import org.junit.Test;

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

}
