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
package fr.gouv.vitam.metadata.core.database.collections;

import java.util.ArrayList;
import java.util.List;

import org.bson.BSONObject;
import org.bson.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;

import fr.gouv.vitam.common.SingletonUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDObjectType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;

/**
 * ObjectGroup:<br>
 *
 * @formatter:off { MD technique globale (exemple GPS), _id : UUID, _tenant : tenant, _profil:
 *                audio|video|document|text|image|..., _up : [ UUIDUnit1, UUIDUnit2, ... ], _nbc : nb objects, _uses : [
 *                { strategy : conservationId, versions : [ { // Object _version : rank, _creadate : date, _id:
 *                UUIDObject, digest : { val : val, typ : type }, size: size, fmt: fmt, MD techniques, _copies : [ { sid
 *                : id, storageDigest: val }, { sid, ...}, ... ] }, { _version : N, ...}, ... ] }, { strategy :
 *                diffusion, ... }, ... ] }
 * @formatter:on
 */
public class ObjectGroup extends MetadataDocument<ObjectGroup> {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(ObjectGroup.class);
    private static final long serialVersionUID = -1761786017392977575L;

    /**
     * Number of copies
     */
    public static final String NB_COPY = "_nbc";
    /**
     * OriginatingAgency
     */
    public static final String ORIGINATINGAGENCY = "OriginatingAgency";
    /**
     * Usages
     */
    public static final String USAGES = "_qualifiers";
    /**
     * Storage Id
     */
    public static final String STORAGE = "_storage";
    
    /**
     * Unit Id, Vitam fields Only projection (no usage)
     */
    public static final BasicDBObject OBJECTGROUP_VITAM_PROJECTION =
        new BasicDBObject(NB_COPY, 1).append(TYPE, 1).append(ORIGINATINGAGENCY, 1)
            .append(TENANT_ID, 1).append(MetadataDocument.UP, 1).append(MetadataDocument.ID, 1);
    /**
     * Versions
     */
    // FIXME P2 WRONG
    public static final String VERSIONS = USAGES + ".*." + "versions";
    /**
     * DataObjectVersion
     */
    public static final String DATAOBJECTVERSION = VERSIONS + "." + "DataObjectVersion";

    /**
     * storage to objectGroup
     */
    public static final String VERSIONS_STORAGE = VERSIONS + "." + "storage";
    /**
     * Version
     */
    public static final String VERSION = VERSIONS + "." + "_version";
    /**
     * Object UUID
     */
    public static final String OBJECTID = VERSIONS + "." + "_id";
    /**
     * Object size
     */
    public static final String OBJECTSIZE = VERSIONS + "." + "Size";
    /**
     * Object format
     */
    public static final String OBJECTFORMAT = VERSIONS + "." + "FormatIdentification.FormatId";
    /**
     * Digest
     */
    public static final String OBJECTDIGEST = VERSIONS;
    /**
     * Digest Value
     */
    public static final String OBJECTDIGEST_VALUE = OBJECTDIGEST + "." + "MessageDigest";
    /**
     * Digest Type
     */
    public static final String OBJECTDIGEST_TYPE = OBJECTDIGEST + "." + "Algorithm";
    /**
     * Copies
     */
    public static final String COPIES = VERSIONS + "." + "_copies";
   

    private static final BasicDBObject[] indexes = {
        new BasicDBObject(VitamLinks.UNIT_TO_OBJECTGROUP.field2to1, 1),
        new BasicDBObject(TENANT_ID, 1),
        new BasicDBObject(ORIGINATINGAGENCY, 1),
        new BasicDBObject(VERSION, 1),
        new BasicDBObject(OPS, 1),
        new BasicDBObject(OBJECTID, 1),
        new BasicDBObject(OBJECTSIZE, 1),
        new BasicDBObject(OBJECTFORMAT, 1),
        new BasicDBObject(OBJECTDIGEST_VALUE, 1).append(OBJECTDIGEST_TYPE, 1),
        new BasicDBObject(STORAGE, 1),
        new BasicDBObject(DATAOBJECTVERSION, 1).append(VERSION, 1),
        new BasicDBObject(VERSIONS_STORAGE, 1)};

    /**
     * Total number of copies
     */
    private int nbCopy;


    /**
     * ES Mapping
     */
    public static final String TYPEUNIQUE = "typeunique";

    // TODO P1 add Nested objects or Parent/child relationships

    /**
     * depths
     */
    public static final String OGDEPTHS = "_ops";

    /**
     * Empty constructor
     */

    public ObjectGroup() {
        // Empty constructor
    }

    /**
     * Constructor from Json
     *
     * @param content the objectgroup of JsonNode format
     */
    public ObjectGroup(JsonNode content) {
        super(content);
    }

    /**
     * Constructor from Document
     *
     * @param content the objectgroup of Document format
     */
    public ObjectGroup(Document content) {
        super(content);
    }

    /**
     * Constructor from Json as Text
     *
     * @param content the objectgroup of String format
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
    protected MetadataCollections getMetadataCollections() {
        return MetadataCollections.C_OBJECTGROUP;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected MongoCollection<ObjectGroup> getCollection() {
        return MetadataCollections.C_OBJECTGROUP.getCollection();
    }

    @Override
    public MetadataDocument<ObjectGroup> newInstance(JsonNode content) {
        return new ObjectGroup(content);
    }

    /**
     *
     * @return a new sub Object GUID
     */
    public String newObjectGuid() {
        return GUIDFactory.newObjectGUID(getDomainId()).toString();
    }

    @Override
    public ObjectGroup save() throws MetaDataExecutionException {
        putBeforeSave();
        if (updated()) {
            return this;
        }
        insert();
        return this;
    }

    @Override
    protected boolean updated() throws MetaDataExecutionException {
        final ObjectGroup vt =
            (ObjectGroup) MongoDbMetadataHelper.findOneNoAfterLoad(getMetadataCollections(), getId());
        BasicDBObject update = null;
        if (vt != null) {
            final List<BasicDBObject> list = new ArrayList<>();
            BasicDBObject updAddToSet =
                MongoDbMetadataHelper.updateLinkset(this, vt, VitamLinks.UNIT_TO_OBJECTGROUP, false);
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
            MongoDbMetadataHelper.updateLinkset(this, null, VitamLinks.UNIT_TO_OBJECTGROUP, false);
        }
        return false;
    }

    @Override
    public boolean load() {
        final ObjectGroup vt =
            (ObjectGroup) MongoDbMetadataHelper.findOneNoAfterLoad(getMetadataCollections(), getId());
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
     * @param remove if remove the link unit/objectgroup
     * @return the list of parent Unit
     */
    @SuppressWarnings("unchecked")
    public List<String> getFathersUnitIds(final boolean remove) {
        List<String> list;
        if (remove) {
            list = (List<String>) remove(VitamLinks.UNIT_TO_OBJECTGROUP.field2to1);
        } else {
            list = (List<String>) this.get(VitamLinks.UNIT_TO_OBJECTGROUP.field2to1);
        }
        if (list == null) {
            return SingletonUtils.singletonList();
        }
        return list;
    }

    /**
     * Used in loop operation to clean the object
     *
     * @param all If true, all items are cleaned
     */
    public final void cleanStructure(final boolean all) {
        remove(VitamLinks.UNIT_TO_OBJECTGROUP.field2to1);
        remove(ID);
        if (all) {
            remove(NB_COPY);
        }
    }

    // TODO P1 add methods to add Object, incrementing NB_COPY

    /**
     * Check if the current ObjectGroup has Unit as immediate parent
     *
     * @param unit the unit could be immediate parent of objectgroup
     * @return True if immediate parent, else False (however could be a grand parent)
     */
    public boolean isImmediateParent(final String unit) {
        final List<String> parents = getFathersUnitIds(false);
        return parents.contains(unit);
    }

    protected static void addIndexes() {
        // if not set, Unit and Tree are worst
        for (final BasicDBObject index : indexes) {
            MetadataCollections.C_OBJECTGROUP.getCollection().createIndex(index);
        }
    }

    protected static void dropIndexes() {
        for (final BasicDBObject index : indexes) {
            MetadataCollections.C_OBJECTGROUP.getCollection().dropIndex(index);
        }
    }

}
