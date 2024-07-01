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
package fr.gouv.vitam.common.database.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is the result of DbRequestSingle's execution
 */
public class DbRequestResult implements VitamAutoCloseable {

    private long offset;
    private long limit;
    private long count;
    private long total;
    private final Map<String, List<String>> diffs;
    private MongoCursor<VitamDocument<?>> cursor;
    private List<VitamDocument<?>> documents;

    /**
     * empty constructor
     */
    public DbRequestResult() {
        count = 0;
        total = 0;
        diffs = new HashMap<>();
    }

    public DbRequestResult(DbRequestResult requestResult) {
        count = requestResult.count;
        total = requestResult.total;
        diffs = new HashMap<>(requestResult.diffs);
        limit = requestResult.limit;
        offset = requestResult.offset;
    }

    /**
     * @return the count
     */
    public long getCount() {
        return count;
    }

    /**
     * @param count the count to set
     * @return this
     */
    public DbRequestResult setCount(long count) {
        this.count = count;
        return this;
    }

    /**
     * @return the possible total result (select)
     */
    public long getTotal() {
        return total;
    }

    /**
     * @param total the possible total result (select)
     * @return this
     */
    public DbRequestResult setTotal(long total) {
        this.total = total;
        return this;
    }

    /**
     * @return the diffs
     */
    public Map<String, List<String>> getDiffs() {
        return new HashMap<>(diffs);
    }

    /**
     * @return the offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * @param offset the offset to set
     * @return this
     */
    public DbRequestResult setOffset(long offset) {
        this.offset = offset;
        return this;
    }

    /**
     * @return the limit
     */
    public long getLimit() {
        return limit;
    }

    /**
     * @param limit the limit to set
     * @return this
     */
    public DbRequestResult setLimit(long limit) {
        this.limit = limit;
        return this;
    }

    /**
     * @param diffs the diffs to set
     * @return this
     */
    public DbRequestResult setDiffs(Map<String, List<String>> diffs) {
        this.diffs.putAll(diffs);
        return this;
    }

    /**
     * @return True if the query has at least one result
     */
    public boolean hasResult() {
        if (cursor != null) {
            return cursor.hasNext();
        }
        return count > 0;
    }

    /**
     * @param cursor the cursor to set
     * @return this
     */
    public DbRequestResult setCursor(MongoCursor<VitamDocument<?>> cursor) {
        this.cursor = cursor;
        return this;
    }

    /**
     * Returns directly the VitamDocuments list
     *
     * @param cls source class
     * @param <T> the original class used by the collection extending VitamCollection
     * @return the documents (copy)
     */
    public <T extends VitamDocument<?>> List<T> getDocuments(Class<T> cls) {
        if (documents == null) {
            documents = new ArrayList<>();
            if (cursor != null) {
                while (cursor.hasNext()) {
                    VitamDocument<?> document = cursor.next();
                    filterFinalResponse(document);
                    documents.add(document);
                }
                cursor.close();
                cursor = null;
            }
        }
        count = documents.size();
        return (List<T>) new ArrayList<>(documents);
    }

    /**
     * Return directly the clsFomJson items list
     *
     * @param cls source class
     * @param clsFromJson target class
     * @param <T> the original class used by the collection extending VitamCollection
     * @param <V> the target class using Json native decoding
     * @return the documents using the target class
     * @throws InvalidParseOperationException if parsing is in error
     */
    public <T extends VitamDocument<T>, V> List<V> getDocuments(Class<T> cls, Class<V> clsFromJson)
        throws InvalidParseOperationException {
        if (documents == null) {
            documents = new ArrayList<>();
            final List<V> newList = new ArrayList<>();
            if (cursor != null) {
                while (cursor.hasNext()) {
                    final T doc = (T) cursor.next();
                    documents.add(doc);
                    filterFinalResponse(doc);

                    newList.add(BsonHelper.fromDocumentToObject(doc, clsFromJson));
                }
                cursor.close();
                cursor = null;
            }
            count = documents.size();
            return newList;
        }
        final List<V> newList = new ArrayList<>();
        for (final VitamDocument<?> doc : documents) {
            newList.add(BsonHelper.fromDocumentToObject(doc, clsFromJson));
        }

        return newList;
    }

    /**
     * Return directly a RequestResponseOk from result using default VitamDocument
     *
     * @param cls Native MongoDb Class
     * @param <T> the original class used by the collection extending VitamCollection
     * @return a RequestResponseOK with Hits and Results sets (not request)
     */
    public <T extends VitamDocument<T>> RequestResponseOK<T> getRequestResponseOK(JsonNode query, Class<T> cls) {
        final RequestResponseOK<T> response = new RequestResponseOK<>(query);
        // Save before addAll
        DatabaseCursor currentCursor = getDatabaseCursor();
        response.addAllResults(getDocuments(cls)).setHits(currentCursor);
        close();
        return response;
    }

    /**
     * This method will modify the document argument in order to filter as output all _varname to corresponding #varname
     * according to ParserTokens
     *
     * @param document of type Document to be modified
     */
    public final void filterFinalResponse(VitamDocument<?> document) {
        for (final ParserTokens.PROJECTIONARGS projection : ParserTokens.PROJECTIONARGS.values()) {
            switch (projection) {
                case ID:
                    replace(document, VitamDocument.ID, VitamFieldsHelper.id());
                    break;
                case TENANT:
                    replace(document, VitamDocument.TENANT_ID, VitamFieldsHelper.tenant());
                    break;
                case VERSION:
                    replace(document, VitamDocument.VERSION, VitamFieldsHelper.version());
                    break;
                case SEDAVERSION:
                    replace(document, VitamDocument.SEDAVERSION, VitamFieldsHelper.sedaVersion());
                    break;
                case IMPLEMENTATIONVERSION:
                    replace(document, VitamDocument.IMPLEMENTATIONVERSION, VitamFieldsHelper.implementationVersion());
                    break;
                default:
                    break;
            }
        }
    }

    /*

     */
    private final void replace(VitamDocument<?> document, String originalFieldName, String targetFieldName) {
        final Object value = document.remove(originalFieldName);
        if (value != null) {
            document.append(targetFieldName, value);
        }
    }

    /**
     * Return directly a RequestResponseOk from result using clsFromJson class
     *
     * @param cls Native MongoDb Class
     * @param clsFromJson target class
     * @param <T> the original class used by the collection extending VitamCollection
     * @param <V> the target class using Json native decoding
     * @return a RequestResponseOK with Hits and Results sets (not request)
     * @throws InvalidParseOperationException
     */
    public <T extends VitamDocument<T>, V> RequestResponseOK<V> getRequestResponseOK(
        JsonNode query,
        Class<T> cls,
        Class<V> clsFromJson
    ) throws InvalidParseOperationException {
        final RequestResponseOK<V> response = new RequestResponseOK<>(query);
        // Save before addAll
        DatabaseCursor currentCursor = getDatabaseCursor();
        response.addAllResults(getDocuments(cls, clsFromJson)).setHits(currentCursor);
        close();
        return response;
    }

    /**
     * @return the corresponding DatabaseCursor
     */
    public DatabaseCursor getDatabaseCursor() {
        return new DatabaseCursor(total, offset, limit, count);
    }

    @Override
    public void close() {
        diffs.clear();
        if (documents != null) {
            documents.clear();
            documents = null;
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }
}
