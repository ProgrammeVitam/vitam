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
package fr.gouv.vitam.metadata.core.database.collections;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import org.bson.conversions.Bson;

import java.util.Collection;

import static com.mongodb.client.model.Filters.eq;

/**
 * MongoDb Helper for Metadata
 *
 * @deprecated use {@link MongoDbMetadataRepository}
 */
@Deprecated
public class MongoDbMetadataHelper {

    /**
     * Quick projection for ID Only
     */
    static final BasicDBObject ID_PROJECTION = new BasicDBObject(MetadataDocument.ID, 1);

    private MongoDbMetadataHelper() {
        // Empty constructor
    }

    /**
     * Find the corresponding id in col collection if it exists. Calls getAfterLoad
     *
     * @param col (not results except if already hashed) the working collection
     * @param id the id value for searching in collection field
     * @return the MetadataDocument casted object using ID = id
     */
    @SuppressWarnings("rawtypes")
    public static final MetadataDocument findOne(final MetadataCollections col, final String id) {
        if (id == null || id.length() == 0) {
            return null;
        }
        final MetadataDocument<?> result = (MetadataDocument<?>) col
            .getCollection()
            .find(eq(VitamDocument.ID, id))
            .first();
        return result;
    }

    /**
     * Does not call getAfterLoad.
     *
     * @param collection domain of request
     * @param condition where condition
     * @param projection select condition
     * @return the FindIterable on the find request based on the given collection
     */
    public static final FindIterable<?> select(
        final MetadataCollections collection,
        final Bson condition,
        final Bson projection
    ) {
        if (projection != null) {
            return collection.getCollection().find(condition).projection(projection);
        } else {
            return collection.getCollection().find(condition);
        }
    }

    /**
     * Does not call getAfterLoad.
     *
     * @param collection domain of request
     * @param condition where condition
     * @param projection select condition
     * @param orderBy orderBy condition
     * @param offset offset (0 by default)
     * @param limit limit (0 for no limit)
     * @return the FindIterable on the find request based on the given collection
     */
    public static final FindIterable<?> select(
        final MetadataCollections collection,
        final Bson condition,
        final Bson projection,
        final Bson orderBy,
        final int offset,
        final int limit
    ) {
        FindIterable<?> find = collection.getCollection().find(condition);
        if (projection != null) {
            find = find.projection(projection);
        }
        return selectFiltered(find, orderBy, offset, limit);
    }

    /**
     * Aff orderBy and offset and limit if not null or not -1
     *
     * @param find
     * @param orderBy
     * @param offset
     * @param limit
     * @return the modified FindIterable
     */
    public static final FindIterable<?> selectFiltered(
        FindIterable<?> find,
        final Bson orderBy,
        final int offset,
        final int limit
    ) {
        if (offset != -1) {
            find.skip(offset);
        }
        if (orderBy != null) {
            find.sort(orderBy);
        }
        if (limit > 0) {
            find.limit(limit);
        }
        return find;
    }

    /**
     * @param collection domain of request
     * @param condition where condition
     * @param nb nb of item to delete
     * @return the DeleteResult on the update request based on the given collection
     * @throws MetaDataExecutionException if a mongo operation exception occurred
     */
    public static final DeleteResult delete(final MetadataCollections collection, final Bson condition, int nb)
        throws MetaDataExecutionException {
        try {
            if (nb > 1) {
                return collection.getCollection().deleteMany(condition);
            } else {
                return collection.getCollection().deleteOne(condition);
            }
        } catch (final MongoException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    /**
     * Used to filter Units/OG according to some Units ancestors
     *
     * @param targetIds set of target ids
     * @param ancestorIds set of ancestor ids
     * @return the Filter condition to find if ancestorIds are ancestors of targetIds or equals to targetIds
     */
    public static final Bson queryForAncestorsOrSame(Collection<String> targetIds, Collection<String> ancestorIds) {
        ancestorIds.addAll(targetIds);
        // TODO P1 understand why it add empty string
        ancestorIds.remove("");
        final int size = ancestorIds.size();
        if (size > 0) {
            return Filters.or(
                Filters.and(Filters.in(MetadataDocument.ID, targetIds), Filters.in(MetadataDocument.ID, ancestorIds)),
                Filters.and(Filters.in(MetadataDocument.ID, targetIds), Filters.in(MetadataDocument.UP, ancestorIds)),
                Filters.and(Filters.in(MetadataDocument.ID, targetIds), Filters.in(Unit.UNITUPS, ancestorIds))
            );
        }
        return new BasicDBObject();
    }

    /**
     * @param type of filter
     * @return a new Result
     */
    public static Result createOneResult(FILTERARGS type) {
        return new ResultDefault(type);
    }

    /**
     * @param type of filter
     * @param set of collection for creating Result
     * @return a new Result
     */
    public static Result createOneResult(FILTERARGS type, Collection<String> set) {
        return new ResultDefault(type, set);
    }
}
