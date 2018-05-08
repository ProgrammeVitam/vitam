package fr.gouv.vitam.metadata.core.database.collections;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;

/**
 * Repository to access to metadata collection
 */
public class MongoDbMetadataRepository {


    public MetadataDocument findOne(MetadataCollections col, String id) {
        return MongoDbMetadataHelper.findOne(col, id);
    }

    /**
     * Does not call getAfterLoad.
     *
     * @param collection domain of request
     * @param projection select condition
     * @param directParents list of parents
     * @return the FindIterable on the find request based on the given collection
     */
    public <T extends VitamDocument> Collection<T> selectByIds(MetadataCollections collection,
                                                               Bson projection, Set<String> directParents) {
        FindIterable<T> iterable =  (FindIterable<T>) MongoDbMetadataHelper.select(collection, in(ID, directParents), projection);

        List<T> vitamDocuments = new ArrayList<>();

        try (final MongoCursor<T> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                final T vitamDocument = cursor.next();
                vitamDocuments.add(vitamDocument);
            }
        }

        return vitamDocuments;
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
    public FindIterable<?> selectByIds(MetadataCollections collection,
                                       Bson condition, Bson projection, Bson orderBy,
                                       int offset, int limit) {
        return MongoDbMetadataHelper.select(collection, condition, projection, orderBy, offset, limit);
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
    public FindIterable<?> selectFiltered(FindIterable<?> find, Bson orderBy,
                                                       int offset, int limit) {
        return MongoDbMetadataHelper.selectFiltered(find, orderBy, offset, limit);
    }

    /**
     * @param collection domain of request
     * @param condition where condition
     * @param nb nb of item to delete
     * @return the DeleteResult on the update request based on the given collection
     * @throws MetaDataExecutionException if a mongo operation exception occurred
     */
    public DeleteResult delete(MetadataCollections collection,
                                            Bson condition, int nb)
        throws MetaDataExecutionException {

        return MongoDbMetadataHelper.delete(collection, condition, nb);
    }

    /**
     * Used to filter Units/OG according to some Units ancestors
     *
     * @param targetIds   set of target ids
     * @param ancestorIds set of ancestor ids
     *
     * @return the Filter condition to find if ancestorIds are ancestors of targetIds or equals to targetIds
     */
    public Bson queryForAncestorsOrSame(Collection<String> targetIds, Collection<String> ancestorIds) {
        return MongoDbMetadataHelper.queryForAncestorsOrSame(targetIds, ancestorIds);
    }

    /**
     * @param type of filter
     * @return a new Result
     */
    public Result createOneResult(BuilderToken.FILTERARGS type) {
        return MongoDbMetadataHelper.createOneResult(type);
    }

    /**
     * @param type of filter
     * @param set of collection for creating Result
     * @return a new Result
     */
    public Result createOneResult(BuilderToken.FILTERARGS type, Collection<String> set) {
        return createOneResult(type, set);
    }

}
