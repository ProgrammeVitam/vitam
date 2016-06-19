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
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.UPDATEACTIONARGS;
import fr.gouv.vitam.core.database.collections.MongoDbAccess.VitamCollections;

/**
 * MongoDb Helper for Metadata
 */
public class MongoDbMetadataHelper {
    /**
     * Quick projection for ID Only
     */
    public static final BasicDBObject ID_PROJECTION = new BasicDBObject(VitamDocument.ID, 1);

    protected static final String ADD_TO_SET = "$addToSet";

    private MongoDbMetadataHelper() {
        // Empty constructor
    }

    /**
     * Does not call getAfterLoad
     *
     * @param collection (not results except if already hashed)
     * @param ref
     * @return a VitamDocument generic object from ID = ref value
     */
    @SuppressWarnings("rawtypes")
    public static final VitamDocument findOneNoAfterLoad(final VitamCollections collection, final String ref) {
        return (VitamDocument<?>) collection.getCollection().find(eq(VitamDocument.ID, ref)).first();
    }

    /**
     * Load a Document into VitamDocument<?>. Calls getAfterLoad
     *
     * @param coll
     * @param obj
     * @return the VitamDocument<?> casted object
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("rawtypes")
    public static final VitamDocument loadFromDocument(final VitamCollections coll, final Document obj)
        throws InstantiationException, IllegalAccessException {
        final VitamDocument<?> vt = (VitamDocument<?>) coll.getClasz().newInstance();
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
     * @return the VitamDocument casted object using field = ref
     */
    @SuppressWarnings("rawtypes")
    public static final VitamDocument findOne(final VitamCollections col, final String field, final String ref) {
        final VitamDocument<?> vitobj =
            (VitamDocument<?>) col.getCollection().find(eq(field, ref)).first();
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
     * @return the VitamDocument casted object using ID = id
     */
    @SuppressWarnings("rawtypes")
    public static final VitamDocument findOne(final VitamCollections col, final String id) {
        if (id == null || id.length() == 0) {
            return null;
        }
        return findOne(col, VitamDocument.ID, id);
    }

    /**
     * OK with native id for Results
     *
     * @param col
     * @param id
     * @return True if one VitamDocument object exists with this id
     */
    public static final boolean exists(final VitamCollections col, final String id) {
        if (id == null || id.length() == 0) {
            return false;
        }
        return col.getCollection().find(eq(VitamDocument.ID, id)).projection(MongoDbMetadataHelper.ID_PROJECTION)
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
    public static final FindIterable<?> select(final VitamCollections collection,
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
    public static final FindIterable<?> select(final VitamCollections collection,
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
     */
    public static final UpdateResult update(final VitamCollections collection,
        final Bson condition, final Bson data, int nb) {
        if (nb > 1) {
            return collection.getCollection().updateMany(condition, data);
        } else {
            return collection.getCollection().updateOne(condition, data);
        }
    }

    /**
     * @param collection domain of request
     * @param condition where condition
     * @param nb nb of item to delete
     * @return the DeleteResult on the update request based on the given collection
     */
    public static final DeleteResult delete(final VitamCollections collection,
        final Bson condition, int nb) {
        if (nb > 1) {
            return collection.getCollection().deleteMany(condition);
        } else {
            return collection.getCollection().deleteOne(condition);
        }
    }

    /**
     * @param targetIds
     * @param ancestorIds
     * @return the Filter condition to find if ancestorIds are ancestors of ObjectGroup targetIds
     */
    public static final Bson queryObjectGroupForAncestors(Set<String> targetIds, Set<String> ancestorIds) {
        return Filters.and(Filters.in(VitamDocument.OG, targetIds),
            Filters.or(Filters.in(VitamDocument.UP, ancestorIds), Filters.in(VitamDocument.ID, ancestorIds)));
    }

    /**
     * @param targetIds
     * @param ancestorIds
     * @return the Filter condition to find if ancestorIds are ancestors of targetIds
     */
    public static final Bson queryForAncestors(Set<String> targetIds, Set<String> ancestorIds) {
        return Filters.and(Filters.in(VitamDocument.ID, targetIds), Filters.in(VitamDocument.UP, ancestorIds));
    }

    /**
     * @param targetIds
     * @param ancestorIds
     * @return the Filter condition to find if ancestorIds are ancestors of targetIds or equals to targetIds
     */
    public static final Bson queryForAncestorsOrSame(Set<String> targetIds, Set<String> ancestorIds) {
        // FIXME REVIEW you change massively the code and the algorithm: it was
        // Filters.or(Filters.and(Filters.in(VitamDocument.ID, targetIds), Filters.in(VitamDocument.ID, ancestorIds)),
        // Filters.and(Filters.in(VitamDocument.ID, targetIds), Filters.in(VitamDocument.UP, ancestorIds)));
        ancestorIds.addAll(targetIds);
        // FIXME REVIEW : I understand why but not the possible reason of such value?
        ancestorIds.remove("");
        final int size = ancestorIds.size();
        if (size > 0) {
            return Filters.in(VitamDocument.ID, ancestorIds);
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
    protected static final BasicDBObject addLink(final VitamDocument obj1,
        final VitamLinks relation,
        final VitamDocument obj2) {
        switch (relation.type) {
            case ASYM_LINK_1:
                addAsymmetricLink(obj1, relation.field1to2, obj2);
                break;
            case SYM_LINK_11:
                addAsymmetricLink(obj1, relation.field1to2, obj2);
                return addAsymmetricLinkUpdate(obj2, relation.field2to1, obj1);
            case ASYM_LINK_N:
                addAsymmetricLinkset(obj1, relation.field1to2, obj2, false);
                break;
            case SYM_LINK_1N:
                return addSymmetricLink(obj1, relation.field1to2, obj2,
                    relation.field2to1);
            case SYM_LINK_N1:
                return addReverseSymmetricLink(obj1, relation.field1to2, obj2,
                    relation.field2to1);
            case SYM_LINK_NN:
                return addSymmetricLinkset(obj1, relation.field1to2, obj2,
                    relation.field2to1);
            case SYM_LINK_N_N:
                return addAsymmetricLinkset(obj2, relation.field2to1, obj1, true);
            default:
                break;
        }
        return null;
    }

    /**
     * Update the link
     *
     * @param obj1
     * @param vtReloaded
     * @param relation
     * @param src
     * @return the update part as { field : value }
     */
    @SuppressWarnings("rawtypes")
    protected static final BasicDBObject updateLink(final VitamDocument obj1,
        final VitamDocument vtReloaded,
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
     * Update the links
     *
     * @param obj1
     * @param vtReloaded
     * @param relation
     * @param src
     * @return the update part as { field : {$each : [value] } }
     */
    @SuppressWarnings("rawtypes")
    protected static final BasicDBObject updateLinks(final VitamDocument obj1,
        final VitamDocument vtReloaded,
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
     * Add an asymmetric relation (n-1) between Obj1 and Obj2
     *
     * @param obj1
     * @param obj1ToObj2
     * @param obj2
     * @param obj2ToObj1
     * @return a {@link BasicDBObject} for update as { field : value }
     */
    @SuppressWarnings("rawtypes")
    private static final BasicDBObject addReverseSymmetricLink(
        final VitamDocument obj1, final String obj1ToObj2,
        final VitamDocument obj2, final String obj2ToObj1) {
        addAsymmetricLinkset(obj1, obj1ToObj2, obj2, false);
        return addAsymmetricLinkUpdate(obj2, obj2ToObj1, obj1);
    }

    /**
     * Add an asymmetric relation (1-n) between Obj1 and Obj2
     *
     * @param obj1
     * @param obj1ToObj2
     * @param obj2
     * @param obj2ToObj1
     * @return a {@link BasicDBObject} for update as { $addToSet : { field : value } }
     */
    @SuppressWarnings("rawtypes")
    private static final BasicDBObject addSymmetricLink(final VitamDocument obj1,
        final String obj1ToObj2,
        final VitamDocument obj2, final String obj2ToObj1) {
        addAsymmetricLink(obj1, obj1ToObj2, obj2);
        return addAsymmetricLinkset(obj2, obj2ToObj1, obj1, true);
    }

    /**
     * Add a symmetric relation (n-n) between Obj1 and Obj2
     *
     * @param obj1
     * @param obj1ToObj2
     * @param obj2
     * @param obj2ToObj1
     * @return a {@link BasicDBObject} for update as { $addToSet : { field : value } }
     */
    @SuppressWarnings("rawtypes")
    private static final BasicDBObject addSymmetricLinkset(final VitamDocument obj1,
        final String obj1ToObj2,
        final VitamDocument obj2, final String obj2ToObj1) {
        addAsymmetricLinkset(obj1, obj1ToObj2, obj2, false);
        return addAsymmetricLinkset(obj2, obj2ToObj1, obj1, true);
    }

    /**
     * Add a single relation (1) from Obj1 to Obj2
     *
     * @param obj1
     * @param obj1ToObj2
     * @param obj2
     */
    @SuppressWarnings("rawtypes")
    private static final void addAsymmetricLink(final VitamDocument obj1,
        final String obj1ToObj2, final VitamDocument obj2) {
        final String refChild = (String) obj2.get(VitamDocument.ID);
        obj1.put(obj1ToObj2, refChild);
    }

    /**
     * Add a single relation (1) from Obj1 to Obj2 in update mode
     *
     * @param db
     * @param obj1
     * @param obj1ToObj2
     * @param obj2
     * @return a {@link BasicDBObject} for update as { field : value }
     */
    @SuppressWarnings("rawtypes")
    private static final BasicDBObject addAsymmetricLinkUpdate(
        final VitamDocument obj1, final String obj1ToObj2,
        final VitamDocument obj2) {
        final String refChild = (String) obj2.get(VitamDocument.ID);
        if (obj1.containsKey(obj1ToObj2) && obj1.get(obj1ToObj2).equals(refChild)) {
            return null;
        }
        obj1.put(obj1ToObj2, refChild);
        return new BasicDBObject(obj1ToObj2, refChild);
    }

    /**
     * Add a one way relation (n) from Obj1 to Obj2
     *
     * @param obj1
     * @param obj1ToObj2
     * @param obj2
     * @param toUpdate True if this element will be updated through $addToSet only
     * @return a {@link BasicDBObject} for update as { $addToSet : { field : value } }
     */
    @SuppressWarnings("rawtypes")
    private static final BasicDBObject addAsymmetricLinkset(final VitamDocument obj1,
        final String obj1ToObj2,
        final VitamDocument obj2, final boolean toUpdate) {
        @SuppressWarnings("unchecked")
        ArrayList<String> relation12 = (ArrayList<String>) obj1.get(obj1ToObj2);
        final String oid2 = (String) obj2.get(VitamDocument.ID);
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
}
