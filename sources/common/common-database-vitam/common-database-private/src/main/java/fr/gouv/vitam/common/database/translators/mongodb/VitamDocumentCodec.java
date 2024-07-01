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

import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;

import java.lang.reflect.Constructor;

/**
 * MongoDB Codec for all VitamDocument
 *
 * @param <T> The parameter of the class
 */
public class VitamDocumentCodec<T extends Document> implements CollectibleCodec<T> {

    private final DocumentCodec documentCodec;
    private final Class<T> clasz;
    private final Constructor<T> constructor;

    /**
     * Constructor
     *
     * @param clazz class of unknown type
     */
    public VitamDocumentCodec(Class<T> clazz) {
        try {
            documentCodec = new DocumentCodec();
            clasz = clazz;
            constructor = clazz.getConstructor(Document.class);
        } catch (final Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        documentCodec.encode(writer, value, encoderContext);
    }

    @Override
    public T generateIdIfAbsentFromDocument(T document) {
        if (!documentHasId(document)) {
            final GUID uuid = GUIDFactory.newGUID();
            document.put("_id", uuid.toString());
        }
        return document;
    }

    @Override
    public boolean documentHasId(T document) {
        return documentCodec.documentHasId(document);
    }

    @Override
    public BsonValue getDocumentId(T document) {
        return documentCodec.getDocumentId(document);
    }

    @Override
    public Class<T> getEncoderClass() {
        return clasz;
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        try {
            final Document document = documentCodec.decode(reader, decoderContext);
            return constructor.newInstance(document);
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
