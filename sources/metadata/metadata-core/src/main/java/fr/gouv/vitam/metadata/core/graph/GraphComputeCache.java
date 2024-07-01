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
package fr.gouv.vitam.metadata.core.graph;

import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.cache.AbstractVitamCache;
import fr.gouv.vitam.common.cache.VitamCache;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Projections.include;

/**
 * This is a default implementation of VitamCache
 * You can given any other implementation using for example VitamCollection as parameter in the constructor
 * You can also implement with QueryDsl
 */
public class GraphComputeCache extends AbstractVitamCache<String, Document> {

    private static class SingletonHolder {

        private static final GraphComputeCache INSTANCE = new GraphComputeCache();
    }

    /**
     * Private constructor
     */
    private GraphComputeCache() {}

    @Override
    protected Map<String, Document> loadByKeys(Iterable<? extends String> keys) {
        Map<String, Document> docs = new HashMap<>();
        try (
            MongoCursor<Document> it = MetadataCollections.UNIT.getCollection()
                .find(in(Unit.ID, keys))
                .projection(include(Unit.UP, Unit.ORIGINATING_AGENCY))
                .iterator()
        ) {
            while (it.hasNext()) {
                final Document doc = it.next();
                docs.put(doc.get(Unit.ID, String.class), doc);
            }
        }
        return docs;
    }

    @Override
    protected Document loadByKey(String key) {
        return MetadataCollections.UNIT.getCollection()
            .find(eq(Unit.ID, key))
            .projection(include(Unit.UP, Unit.ORIGINATING_AGENCY))
            .first();
    }

    public static VitamCache<String, Document> getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
