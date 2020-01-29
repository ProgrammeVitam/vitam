/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.offers.tape.impl.catalog;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.impl.queue.QueueRepositoryImpl;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * repository for Tapes Catalog management in mongo.
 */
public class TapeCatalogRepository extends QueueRepositoryImpl {

    private static final String ALL_PARAMS_REQUIRED = "All params are required";

    String $_SET = "$set";
    String $_INC = "$inc";

    public TapeCatalogRepository(MongoCollection<Document> collection) {
        super(collection);
    }

    /**
     * create a tape model
     *
     * @param tapeCatalog
     * @throws InvalidParseOperationException
     */
    public String createTape(TapeCatalog tapeCatalog) throws TapeCatalogException {
        try {
            ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tapeCatalog);
            tapeCatalog.setVersion(0);
            String json = JsonHandler.unprettyPrint(tapeCatalog);
            collection.insertOne(Document.parse(json));
            return tapeCatalog.getId();
        } catch (Exception e) {
            throw new TapeCatalogException(e);
        }
    }

    /**
     * replace a tape model
     *
     * @param tapeCatalog
     * @throws InvalidParseOperationException
     */
    public boolean replaceTape(TapeCatalog tapeCatalog) throws TapeCatalogException {
        try {
            ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tapeCatalog);
            tapeCatalog.setVersion(tapeCatalog.getVersion() + 1);
            String json = JsonHandler.unprettyPrint(tapeCatalog);
            final UpdateResult result =
                collection.replaceOne(eq(TapeCatalog.ID, tapeCatalog.getId()), Document.parse(json));

            return result.getMatchedCount() == 1;
        } catch (Exception e) {
            throw new TapeCatalogException(e);
        }
    }

    /**
     * apply fields changes for tape tapeId
     *
     * @param tapeId
     * @param fields
     * @return true if changes have been applied otherwise false
     */
    public boolean updateTape(String tapeId, Map<String, Object> fields) throws TapeCatalogException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tapeId, fields);
        if (fields.isEmpty()) {
            throw new TapeCatalogException(ALL_PARAMS_REQUIRED);
        }

        try {
            Document update = new Document();
            fields.forEach((key, value) -> update.append(key, value));

            Document data = new Document($_SET, toBson(update))
                .append($_INC, new Document(TapeCatalog.VERSION, 1));

            UpdateResult result = collection.updateOne(eq(TapeCatalog.ID, tapeId), data);

            return result.getMatchedCount() == 1;
        } catch (Exception e) {
            throw new TapeCatalogException(e);
        }
    }

    /**
     * return tape models according to given fields
     *
     * @param criteria
     * @return
     */
    public List<TapeCatalog> findTapes(List<QueryCriteria> criteria) throws TapeCatalogException {
        if (criteria == null || criteria.isEmpty()) {
            throw new TapeCatalogException(ALL_PARAMS_REQUIRED);
        }

        List<Bson> filters = new ArrayList<>();
        for (QueryCriteria criterion : criteria) {
            switch (criterion.getOperator()) {
                case EQ:
                    filters.add(Filters.eq(criterion.getField(), criterion.getValue()));
                    break;
                case GT:
                    filters.add(Filters.gt(criterion.getField(), criterion.getValue()));
                    break;
                case GTE:
                    filters.add(Filters.gte(criterion.getField(), criterion.getValue()));
                    break;
                case LT:
                    filters.add(Filters.lt(criterion.getField(), criterion.getValue()));
                    break;
                case LTE:
                    filters.add(Filters.lte(criterion.getField(), criterion.getValue()));
                    break;
            }
        }

        List<TapeCatalog> result = new ArrayList<>();
        List<Document> documents = collection.find(Filters.and(filters)).into(new ArrayList<>());
        for (Document doc : documents) {
            try {
                result.add(JsonHandler.getFromString(BsonHelper.stringify(doc), TapeCatalog.class));
            } catch (InvalidParseOperationException e) {
                throw new TapeCatalogException(e);
            }
        }

        return result;
    }

    /**
     * return tape model according to given ID
     *
     * @param tapeId
     * @return
     */
    public TapeCatalog findTapeById(String tapeId) throws TapeCatalogException {
        ParametersChecker.checkParameter(ALL_PARAMS_REQUIRED, tapeId);
        FindIterable<Document> models =
            collection.find(eq(TapeCatalog.ID, tapeId));
        Document first = models.first();
        if (first == null) {
            return null;
        }
        try {
            return JsonHandler.getFromString(BsonHelper.stringify(first), TapeCatalog.class);
        } catch (InvalidParseOperationException e) {
            throw new TapeCatalogException(e);
        }
    }

    private Document toBson(Object object) {
        return Document.parse(JsonHandler.unprettyPrint(object));
    }

    private <T> T fromBson(Document document, Class<T> clazz)
        throws InvalidParseOperationException {
        return JsonHandler.getFromString(BsonHelper.stringify(document), clazz);
    }
}
