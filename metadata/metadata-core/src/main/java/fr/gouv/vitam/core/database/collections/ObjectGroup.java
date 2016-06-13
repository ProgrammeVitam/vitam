/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteConcernException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDObjectType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.core.database.collections.MongoDbAccess.VitamCollections;

/**
 * ObjectGroup:<br>
 * 
 * @formatter:off { MD technique globale (exemple GPS), _id : UUID, _dom : domainId (tenant), _type:
 *                audio|video|document|text|image|..., _up : [ UUIDUnit1, UUIDUnit2, ... ], _nb : nb objects, _uses : [
 *                { strategy : conservationId, versions : [ { // Object _version : rank, _creadate : date, _id:
 *                UUIDObject, digest : { val : val, typ : type }, size: size, fmt: fmt, MD techniques, _copies : [ { sid
 *                : id, storageDigest: val }, { sid, ...}, ... ] }, { _version : N, ...}, ... ] }, { strategy :
 *                diffusion, ... }, ... ] }
 * @formatter:on
 */
public class ObjectGroup extends VitamDocument<ObjectGroup> {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(ObjectGroup.class);
    private static final long serialVersionUID = -1761786017392977575L;

    /**
     * Number of copies
     */
    public static final String NB_COPY = "_nbc";
    /**
     * Usages
     */
    public static final String USAGES = "_uses";
    /**
     * Unit Id, Vitam fields Only projection (no usage)
     */
    public static final BasicDBObject OBJECTGROUP_VITAM_PROJECTION =
        new BasicDBObject(NB_COPY, 1).append(TYPE, 1)
            .append(DOMID, 1).append(VitamDocument.UP, 1).append(VitamDocument.ID, 1);
    /**
     * Strategy
     */
    public static final String STRATEGY = USAGES + "." + "strategy";
    /**
     * Versions
     */
    public static final String VERSIONS = USAGES + "." + "versions";
    /**
     * Version
     */
    public static final String VERSION = VERSIONS + "." + "_version";
    /**
     * Creation date
     */
    public static final String CREATED_DATE = VERSIONS + "." + "_creadate";
    /**
     * Object UUID
     */
    public static final String OBJECTID = VERSIONS + "." + "_id";
    /**
     * Object size
     */
    public static final String OBJECTSIZE = VERSIONS + "." + "size";
    /**
     * Object format
     */
    public static final String OBJECTFORMAT = VERSIONS + "." + "fmt";
    /**
     * Digest
     */
    public static final String OBJECTDIGEST = VERSIONS + "." + "digest";
    /**
     * Digest Value
     */
    public static final String OBJECTDIGEST_VALUE = OBJECTDIGEST + "." + "val";
    /**
     * Digest Type
     */
    public static final String OBJECTDIGEST_TYPE = OBJECTDIGEST + "." + "typ";
    /**
     * Copies
     */
    public static final String COPIES = VERSIONS + "." + "_copies";
    /**
     * Storage Id
     */
    public static final String STORAGE = COPIES + "." + "sid";
    /**
     * Digest
     */
    public static final String STORAGEDIGEST = COPIES + "." + "digest";

    private static final BasicDBObject[] indexes = {
        new BasicDBObject(VitamLinks.Unit2ObjectGroup.field2to1, 1),
        new BasicDBObject(DOMID, 1),
        new BasicDBObject(STRATEGY, 1),
        new BasicDBObject(VERSION, 1),
        new BasicDBObject(OBJECTID, 1),
        new BasicDBObject(OBJECTSIZE, 1),
        new BasicDBObject(OBJECTFORMAT, 1),
        new BasicDBObject(OBJECTDIGEST_VALUE, 1).append(OBJECTDIGEST_TYPE, 1),
        new BasicDBObject(STORAGE, 1),
        new BasicDBObject(STRATEGY, 1).append(VERSION, 1)};

    /**
     * Copy subclass
     */
    public static class Copy {
        String sid;
        String storageDigest;
    }
    /**
     * Object subclass
     */
    public static class ObjectItem {
        String _id;
        long size;
        Digest digest;
        int _version;
        Date _creadate;
        List<Copy> _copies;
        /**
         * @formatter:off Among them: formatIdentification{formatLitteral, mimeType}, fileInfo{filename,
         *                creatingApplicationName, creationApplicationVersion, dateCreatedByApplication, creatingOs,
         *                creatingOsVersion, lastModified}, metadata{according to Text, Document, Image, Audio, Video,
         *                ...), othermetadata(whatever)
         *
         *                or
         *
         *                physicalId, physicalDimensions(width, height, depth, shape, diameter, length, thickness,
         *                weight, numberOfPage), other(whatever)
         * @formatter:on
         */
        Map<String, Object> md;
    }

    /**
     * Total number of copies
     */
    private int nbCopy;

    /**
     * Empty constructor
     */
    public ObjectGroup() {
        // Empty constructor
    }

    /**
     * Constructor from Json
     * 
     * @param content
     */
    public ObjectGroup(JsonNode content) {
        super(content);
    }

    /**
     * Constructor from Document
     * 
     * @param content
     */
    public ObjectGroup(Document content) {
        super(content);
    }

    /**
     * Constructor from Json as Text
     * 
     * @param content
     */
    public ObjectGroup(String content) {
        super(content);
    }

    /**
     *
     * @return the associated GUIDObjectType
     */
    public static final int getGUIDObjectTypeId() {
        return GUIDObjectType.OBJECTGROUP_TYPE;
    }

    @Override
    protected VitamCollections getVitamCollections() {
        return MongoDbAccess.VitamCollections.Cobjectgroup;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected MongoCollection<ObjectGroup> getCollection() {
        return (MongoCollection<ObjectGroup>) MongoDbAccess.VitamCollections.Cobjectgroup.getCollection();
    }

    /**
     *
     * @return a new sub Object GUID
     */
    public String newObjectGuid() {
        return GUIDFactory.newObjectGUID(getDomainId()).toString();
    }

    @Override
    public ObjectGroup save() throws MongoWriteException, MongoWriteConcernException, MongoException {
        putBeforeSave();
        if (updated()) {
            return this;
        }
        insert();
        return this;
    }

    @Override
    protected boolean updated() throws MongoWriteException, MongoWriteConcernException, MongoException {
        final ObjectGroup vt =
            (ObjectGroup) MongoDbMetadataHelper.findOneNoAfterLoad(getVitamCollections(), getId());
        BasicDBObject update = null;
        if (vt != null) {
            final List<BasicDBObject> list = new ArrayList<>();
            BasicDBObject updAddToSet = MongoDbMetadataHelper.updateLinks(this, vt, VitamLinks.Unit2ObjectGroup, false);
            if (updAddToSet != null) {
                list.add(updAddToSet);
            }
            if (!list.isEmpty()) {
                try {
                    update = new BasicDBObject();
                    updAddToSet = new BasicDBObject();
                    for (final BasicDBObject dbObject : list) {
                        updAddToSet.putAll((BSONObject) dbObject);
                    }
                    update = update.append(MongoDbMetadataHelper.ADD_TO_SET, updAddToSet);
                    update(update);
                } catch (final MongoException e) {
                    LOGGER.error("Exception for " + update, e);
                    throw e;
                }
                list.clear();
            }
            return true;
        } else {
            MongoDbMetadataHelper.updateLinks(this, null, VitamLinks.Unit2ObjectGroup, false);
        }
        return false;
    }

    @Override
    public boolean load() {
        final ObjectGroup vt =
            (ObjectGroup) MongoDbMetadataHelper.findOneNoAfterLoad(getVitamCollections(), getId());
        if (vt == null) {
            return false;
        }
        putAll(vt);
        getAfterLoad();
        return true;
    }

    @Override
    public ObjectGroup getAfterLoad() {
        nbCopy = this.getInteger(NB_COPY, 0);
        return this;
    }

    @Override
    public ObjectGroup putBeforeSave() {
        put(NB_COPY, nbCopy);
        return this;
    }

    /**
     * @param remove
     * @return the list of parent Unit
     */
    @SuppressWarnings("unchecked")
    public List<String> getFathersUnitIds(final boolean remove) {
        if (remove) {
            return (List<String>) remove(VitamLinks.Unit2ObjectGroup.field2to1);
        } else {
            return (List<String>) this.get(VitamLinks.Unit2ObjectGroup.field2to1);
        }
    }

    /**
     * Used in loop operation to clean the object
     *
     * @param all If true, all items are cleaned
     */
    public final void cleanStructure(final boolean all) {
        remove(VitamLinks.Unit2ObjectGroup.field2to1);
        remove(ID);
        if (all) {
            remove(NB_COPY);
        }
    }

    // TODO add methods to add Object, incrementing NB_COPY

    /**
     * Check if the current ObjectGroup has Unit as immediate parent
     *
     * @param unit
     * @return True if immediate parent, else False (however could be a grand parent)
     */
    public boolean isImmediateParent(final String unit) {
        final List<String> parents = getFathersUnitIds(false);
        return parents.contains(unit);
    }

    protected static void addIndexes() {
        // if not set, Unit and Tree are worst
        for (final BasicDBObject index : indexes) {
            MongoDbAccess.VitamCollections.Cobjectgroup.getCollection().createIndex(index);
        }
    }

    protected static void dropIndexes() {
        for (final BasicDBObject index : indexes) {
            MongoDbAccess.VitamCollections.Cobjectgroup.getCollection().dropIndex(index);
        }
    }
}
