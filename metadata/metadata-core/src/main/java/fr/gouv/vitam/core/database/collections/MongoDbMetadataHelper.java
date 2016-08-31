/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
/**
 *
 */
package fr.gouv.vitam.core.database.collections;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTIONARGS;

/**
 * MongoDb Helper for Metadata
 */
public class MongoDbMetadataHelper {
    /**
     * Quick projection for ID Only
     */
    public static final BasicDBObject ID_PROJECTION = new BasicDBObject(MetadataDocument.ID, 1);

    protected static final String ADD_TO_SET = "$addToSet";

    /**
     * LRU Unit cache (limited to VITAM PROJECTION)
     */
    public static final UnitLRU LRU = new UnitLRU();
    

    private MongoDbMetadataHelper() {
        // Empty constructor
    }

    /**
     * Does not call getAfterLoad
     *
     * @param metadataCollections (not results except if already hashed)
     * @param ref
     * @return a MetadataDocument generic object from ID = ref value
     */
    @SuppressWarnings("rawtypes")
    public static final MetadataDocument findOneNoAfterLoad(final MetadataCollections metadataCollections, final String ref) {
        return (MetadataDocument<?>) metadataCollections.getCollection().find(eq(MetadataDocument.ID, ref)).first();
    }

    /**
     * Load a Document into MetadataDocument<?>. Calls getAfterLoad
     *
     * @param coll
     * @param obj
     * @return the MetadataDocument<?> casted object
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("rawtypes")
    public static final MetadataDocument loadFromDocument(final MetadataCollections coll, final Document obj)
        throws InstantiationException, IllegalAccessException {
        final MetadataDocument<?> vt = (MetadataDocument<?>) coll.getClasz().newInstance();
        vt.putAll(obj);
        vt.getAfterLoad();
        return vt;
    }

    /**
     * Calls getAfterLoad
     *
     * @param col (not Results except if already hashed)
     * @param field
     * @param ref
     * @return the MetadataDocument casted object using field = ref
     */
    @SuppressWarnings("rawtypes")
    public static final MetadataDocument findOne(final MetadataCollections col, final String field, final String ref) {
        final MetadataDocument<?> vitobj =
            (MetadataDocument<?>) col.getCollection().find(eq(field, ref)).first();
        if (vitobj == null) {
            return null;
        } else {
            vitobj.getAfterLoad();
        }
        return vitobj;
    }

    /**
     * Find the corresponding id in col collection if it exists. Calls getAfterLoad
     *
     * @param col (not results except if already hashed)
     * @param id
     * @return the MetadataDocument casted object using ID = id
     */
    @SuppressWarnings("rawtypes")
    public static final MetadataDocument findOne(final MetadataCollections col, final String id) {
        if (id == null || id.length() == 0) {
            return null;
        }
        return findOne(col, MetadataDocument.ID, id);
    }

    /**
     * OK with native id for Results
     *
     * @param col
     * @param id
     * @return True if one MetadataDocument object exists with this id
     */
    public static final boolean exists(final MetadataCollections col, final String id) {
        if (id == null || id.length() == 0) {
            return false;
        }
        return col.getCollection().find(eq(MetadataDocument.ID, id)).projection(MongoDbMetadataHelper.ID_PROJECTION)
            .first() != null;
    }

    /**
     * Does not call getAfterLoad.
     *
     * @param collection domain of request
     * @param condition where condition
     * @param projection select condition
     * @return the FindIterable on the find request based on the given collection
     */
    public static final FindIterable<?> select(final MetadataCollections collection,
        final Bson condition,
        final Bson projection) {
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
    public static final FindIterable<?> select(final MetadataCollections collection,
        final Bson condition, final Bson projection, final Bson orderBy,
        final int offset, final int limit) {
        FindIterable<?> find = collection.getCollection().find(condition).skip(offset);
        if (projection != null) {
            find = find.projection(projection);
        }
        if (orderBy != null) {
            find = find.sort(orderBy);
        }
        if (limit > 0) {
            find = find.limit(limit);
        }
        return find;
    }

    /**
     * @param collection domain of request
     * @param condition where condition
     * @param data update
     * @param nb nb of item to update
     * @return the UpdateResult on the update request based on the given collection
     * @throws MetaDataExecutionException 
     */
    public static final UpdateResult update(final MetadataCollections collection,
        final Bson condition, final Bson data, int nb) 
            throws MetaDataExecutionException {
        try {
            if (nb > 1) {
                return collection.getCollection().updateMany(condition, data);
            } else {
                return collection.getCollection().updateOne(condition, data);
            }
        } catch (MongoException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    /**
     * @param collection domain of request
     * @param condition where condition
     * @param nb nb of item to delete
     * @return the DeleteResult on the update request based on the given collection
     * @throws MetaDataExecutionException 
     */
    public static final DeleteResult delete(final MetadataCollections collection,
        final Bson condition, int nb) 
            throws MetaDataExecutionException {
        try {
            if (nb > 1) {
                return collection.getCollection().deleteMany(condition);
            } else {
                return collection.getCollection().deleteOne(condition);
            }
        } catch (MongoException e) {
            throw new MetaDataExecutionException(e);
        }
    }

    /**
     * @param targetIds
     * @param ancestorIds
     * @return the Filter condition to find if ancestorIds are ancestors of ObjectGroup targetIds
     */
    public static final Bson queryObjectGroupForAncestors(Set<String> targetIds, Set<String> ancestorIds) {
        return Filters.and(Filters.in(MetadataDocument.OG, targetIds),
            Filters.or(Filters.in(MetadataDocument.UP, ancestorIds), Filters.in(MetadataDocument.ID, ancestorIds)));
    }

    /**
     * @param targetIds
     * @param ancestorIds
     * @return the Filter condition to find if ancestorIds are ancestors of targetIds
     */
    public static final Bson queryForAncestors(Set<String> targetIds, Set<String> ancestorIds) {
        return Filters.and(Filters.in(MetadataDocument.ID, targetIds), Filters.in(MetadataDocument.UP, ancestorIds));
    }

    /**
     * @param targetIds
     * @param ancestorIds
     * @return the Filter condition to find if ancestorIds are ancestors of targetIds or equals to targetIds
     */
    public static final Bson queryForAncestorsOrSame(Set<String> targetIds, Set<String> ancestorIds) {
        ancestorIds.addAll(targetIds);
        // TODO understand why it add empty string
        ancestorIds.remove("");
        final int size = ancestorIds.size();
        if (size > 0) {
            return Filters.or(Filters.and(Filters.in(MetadataDocument.ID, targetIds), Filters.in(MetadataDocument.ID, ancestorIds)),
                Filters.and(Filters.in(MetadataDocument.ID, targetIds), Filters.in(MetadataDocument.UP, ancestorIds)));
        }
        return new BasicDBObject();
    }

    /**
     * Add a Link according to relation defined, where the relation is defined in obj1->obj2 way by default (even if
     * symmetric)
     *
     * @param obj1
     * @param relation
     * @param obj2
     * @return a {@link BasicDBObject} that hold a possible update part (may be null) as { $addToSet : { field : value }
     *         } or { field : value }
     */
    @SuppressWarnings("rawtypes")
    protected static final BasicDBObject addLink(final MetadataDocument obj1,
        final VitamLinks relation,
        final MetadataDocument obj2) {
        switch (relation.type) {
            case SYM_LINK_N1:
                setAsymmetricLink(obj1, relation.field1to2, obj2);
                return addAsymmetricLinkset(obj2, relation.field2to1, obj1, true);
            case SYM_LINK_N_N:
                return addAsymmetricLinkset(obj2, relation.field2to1, obj1, true);
            default:
                break;
        }
        return null;
    }

    /**
     * Update the link (1 link type)
     *
     * @param obj1
     * @param vtReloaded
     * @param relation
     * @param src
     * @return the update part as { field : value }
     */
    @SuppressWarnings("rawtypes")
    protected static final BasicDBObject updateLink(final MetadataDocument obj1,
        final MetadataDocument vtReloaded,
        final VitamLinks relation, final boolean src) {
        final String fieldname = src ? relation.field1to2 : relation.field2to1;
        if (vtReloaded != null) {
            String srcOid = (String) vtReloaded.remove(fieldname);
            final String targetOid = (String) obj1.get(fieldname);
            if (srcOid != null && targetOid != null) {
                if (targetOid.equals(srcOid)) {
                    srcOid = null;
                } else {
                    srcOid = targetOid;
                }
            } else if (targetOid != null) {
                srcOid = targetOid;
            } else if (srcOid != null) {
                obj1.put(fieldname, srcOid);
                srcOid = null;
            }
            if (srcOid != null) {
                // need to add $set
                return new BasicDBObject(fieldname, srcOid);
            }
        } else {
            // nothing since save will be done just after
        }
        return null;
    }

    /**
     * Update the linkset (N link type)
     *
     * @param obj1
     * @param vtReloaded
     * @param relation
     * @param src
     * @return the update part as { field : {$each : [value] } }
     */
    @SuppressWarnings("rawtypes")
    protected static final BasicDBObject updateLinkset(final MetadataDocument obj1,
        final MetadataDocument vtReloaded,
        final VitamLinks relation, final boolean src) {
        final String fieldname = src ? relation.field1to2 : relation.field2to1;
        if (vtReloaded != null) {
            @SuppressWarnings("unchecked")
            final List<String> srcList = (List<String>) vtReloaded.remove(fieldname);
            @SuppressWarnings("unchecked")
            final List<String> targetList = (List<String>) obj1.get(fieldname);
            if (srcList != null && targetList != null) {
                targetList.removeAll(srcList);
            } else if (targetList != null) {
                // srcList empty
            } else {
                // targetList empty
                obj1.put(fieldname, srcList);
            }
            if (targetList != null && !targetList.isEmpty()) {
                // need to add $addToSet
                return new BasicDBObject(fieldname,
                    new BasicDBObject(UPDATEACTIONARGS.EACH.exactToken(), targetList));
            }
        } else {
            // nothing since save will be done just after, except checking array exists
            if (!obj1.containsKey(fieldname)) {
                obj1.put(fieldname, new ArrayList<>());
            }
        }
        return null;
    }

    /**
     * Add a single relation (1) from Obj1 to Obj2 (used in N-1 link)
     *
     * @param db
     * @param obj1
     * @param obj1ToObj2
     * @param obj2
     * @return a {@link BasicDBObject} for update as { field : value }
     */
    @SuppressWarnings("rawtypes")
    private static final BasicDBObject setAsymmetricLink( 
        final MetadataDocument obj1, final String obj1ToObj2,
        final MetadataDocument obj2) {
        final String refChild = obj2.getId();
        if (obj1.containsKey(obj1ToObj2) && obj1.get(obj1ToObj2).equals(refChild)) {
            return null;
        }
        obj1.put(obj1ToObj2, refChild);
        return new BasicDBObject(UPDATEACTION.SET.exactToken(), new BasicDBObject(obj1ToObj2, refChild));
    }

    /**
     * Add a one way relation (n) from Obj1 to Obj2 (used in N-(N) and N-1 links)
     *
     * @param obj1
     * @param obj1ToObj2
     * @param obj2
     * @param toUpdate True if this element will be updated through $addToSet only
     * @return a {@link BasicDBObject} for update as { $addToSet : { field : value } }
     */
    @SuppressWarnings("rawtypes")
    private static final BasicDBObject addAsymmetricLinkset(final MetadataDocument obj1,
        final String obj1ToObj2,
        final MetadataDocument obj2, final boolean toUpdate) {
        @SuppressWarnings("unchecked")
        ArrayList<String> relation12 = (ArrayList<String>) obj1.get(obj1ToObj2);
        final String oid2 = obj2.getId();
        if (relation12 == null) {
            if (toUpdate) {
                return new BasicDBObject(ADD_TO_SET,
                    new BasicDBObject(obj1ToObj2, oid2));
            }
            relation12 = new ArrayList<String>();
        }
        if (relation12.contains(oid2)) {
            return null;
        }
        if (toUpdate) {
            return new BasicDBObject(ADD_TO_SET, new BasicDBObject(obj1ToObj2, oid2));
        } else {
            relation12.add(oid2);
            obj1.put(obj1ToObj2, relation12);
            return null;
        }
    }
    
    /**
     * @param type to use
     * @return a new Result
     */
    public static Result createOneResult(FILTERARGS type) {
        return new ResultDefault(type);
    }

    /**
     * @param type
     * @param set
     * @return a new Result
     */
    public static Result createOneResult(FILTERARGS type, Set<String> set) {
        return new ResultDefault(type, set);
    }
    
}
