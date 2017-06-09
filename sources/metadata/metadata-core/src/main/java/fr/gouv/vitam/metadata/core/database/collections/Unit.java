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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Updates.addEachToSet;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.bson.BSONObject;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;

import fr.gouv.vitam.common.SingletonUtils;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTIONARGS;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.translators.mongodb.MongoDbHelper;
import fr.gouv.vitam.common.guid.GUIDObjectType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;

/**
 * Unit class:<br>
 *
 * @formatter:off { MD content, _id: UUID, _tenant: tenant, _profil: documentType,, _min: depthmin, _max: depthmax,
 *                _mgt. Management structure, _uds: { UUID1 : depth1, UUID2 : depth2, ... }, // not indexed and not to
 *                be in ES! _us: [ UUID1, UUID2, ... }, // indexed and equivalent to _uds _up: [ UUID1, UUID2, ... ], //
 *                limited to immediate parent _og: UUID, _nbc : immediateChildNb }
 * @formatter:on
 */
public class Unit extends MetadataDocument<Unit> {
    private static final String EXCEPTION_FOR = "Exception for ";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Unit.class);

    private static final long serialVersionUID = -4351321928647834270L;

    /**
     * UNITDEPTHS : { UUID1 : depth2, UUID2 : depth2 }
     */
    public static final String UNITDEPTHS = "_uds";
    /**
     * UNITUPS : [ UUID1, UUID2 ]
     */
    public static final String UNITUPS = "_us";
    /**
     * MINDEPTH : min
     */
    public static final String MINDEPTH = "_min";
    /**
     * MAXDEPTH : max
     */
    public static final String MAXDEPTH = "_max";
    /**
     * Number of Immediate child (Unit)
     */
    public static final String NBCHILD = "_nbc";
    /**
     * Management : { various rules per themes }
     */
    public static final String MANAGEMENT = "_mgt";
    /**
     * UnitType : nomal or holding scheme
     */
    public static final String UNIT_TYPE = "_unitType";

    /**
     * ES Mapping
     */
    public static final String TYPEUNIQUE = "typeunique";


    // TODO P1 add Nested objects or Parent/child relationships

    /**
     * Quick projection for ID and ObjectGroup Only
     */
    public static final BasicDBObject UNIT_OBJECTGROUP_PROJECTION =
            new BasicDBObject(MetadataDocument.ID, 1).append(MetadataDocument.OG, 1).append(TENANT_ID, 1);

    /**
     * Es projection (no UPS)
     */
    public static final BasicDBObject UNIT_ES_PROJECTION = new BasicDBObject(UNITDEPTHS, 0);

    /**
     * Unit Id, Vitam fields Only projection (no content nor management)
     */
    public static final BasicDBObject UNIT_VITAM_PROJECTION =
            new BasicDBObject(NBCHILD, 1).append(TYPE, 1).append(UNITUPS, 1).append(UNITDEPTHS, 1)
                    .append(MINDEPTH, 1).append(MAXDEPTH, 1)
                    .append(TENANT_ID, 1).append(MetadataDocument.UP, 1).append(MetadataDocument.ID, 1)
                    .append(ORIGINATING_AGENCIES, 1).append(MetadataDocument.OG, 1);
    /**
     * Unit Id, Vitam and Management fields Only projection (no content)
     */
    public static final BasicDBObject UNIT_VITAM_MANAGEMENT_PROJECTION =
            new BasicDBObject(UNIT_VITAM_PROJECTION)
                    .append(MANAGEMENT + ".$", 1);
    /**
     * Storage Rule
     */
    public static final String STORAGERULE = MANAGEMENT + ".StorageRule";
    /**
     * Appraisal Rule
     */
    public static final String APPRAISALRULE = MANAGEMENT + ".AppraisalRule";
    /**
     * Access Rule
     */
    public static final String ACCESSRULE = MANAGEMENT + ".AccessRule";
    /**
     * Dissemination Rule
     */
    public static final String DISSEMINATIONRULE = MANAGEMENT + ".DisseminationRule";
    /**
     * Reuse Rule
     */
    public static final String REUSERULE = MANAGEMENT + ".ReuseRule";
    /**
     * Classification Rule
     */
    public static final String CLASSIFICATIONRULE = MANAGEMENT + ".ClassificationRule";

    /**
     * Rule
     */
    public static final String RULE = ".Rules.Rule";
    /**
     * Rule end date (computed)
     */
    public static final String END = ".Rules._end";


    @SuppressWarnings("javadoc")
    public static final String STORAGERULES = STORAGERULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String STORAGEEND = STORAGERULE + END;
    @SuppressWarnings("javadoc")
    public static final String APPRAISALRULES = APPRAISALRULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String APPRAISALEND = APPRAISALRULE + END;
    @SuppressWarnings("javadoc")
    public static final String ACCESSRULES = ACCESSRULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String ACCESSEND = ACCESSRULE + END;
    @SuppressWarnings("javadoc")
    public static final String DISSEMINATIONRULES = DISSEMINATIONRULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String DISSEMINATIONEND = DISSEMINATIONRULE + END;
    @SuppressWarnings("javadoc")
    public static final String REUSERULES = REUSERULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String REUSEEND = REUSERULE + END;
    @SuppressWarnings("javadoc")
    public static final String CLASSIFICATIONRULES = CLASSIFICATIONRULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String CLASSIFICATIONEND = CLASSIFICATIONRULE + END;

    private static final BasicDBObject[] indexes = {
            new BasicDBObject(VitamLinks.UNIT_TO_UNIT.field2to1, 1),
            new BasicDBObject(VitamLinks.UNIT_TO_OBJECTGROUP.field1to2, 1),
            new BasicDBObject(TENANT_ID, 1),
            new BasicDBObject(UNITUPS, 1),
            new BasicDBObject(MINDEPTH, 1),
            new BasicDBObject(MAXDEPTH, 1),
            new BasicDBObject(OPS, 1),
            new BasicDBObject(STORAGERULES, 1),
            new BasicDBObject(STORAGEEND, 1),
            new BasicDBObject(APPRAISALRULES, 1),
            new BasicDBObject(APPRAISALEND, 1),
            new BasicDBObject(ACCESSRULES, 1),
            new BasicDBObject(ACCESSEND, 1),
            new BasicDBObject(DISSEMINATIONRULES, 1),
            new BasicDBObject(DISSEMINATIONEND, 1),
            new BasicDBObject(REUSERULES, 1),
            new BasicDBObject(REUSERULE, 1),
            new BasicDBObject(CLASSIFICATIONRULES, 1),
            new BasicDBObject(CLASSIFICATIONEND, 1),
            new BasicDBObject(TYPE, 1)};

    /**
     * Number of Immediate child (Unit)
     */
    private long nb = 0;

    /**
     * Empty constructor
     */
    public Unit() {
        // empty
    }

    /**
     * Constructor from Json
     *
     * @param content of type JsonNode for building Unit
     */
    public Unit(JsonNode content) {
        super(content);
    }

    /**
     * Constructor from Document
     *
     * @param content of type Document for building Unit
     */
    public Unit(Document content) {
        super(content);
    }

    /**
     * Constructor from Json as Text
     *
     * @param content of type String for building Unit
     */
    public Unit(String content) {
        super(content);
    }

    /**
     * @return the associated GUIDObjectType
     */
    public static final int getGUIDObjectTypeId() {
        return GUIDObjectType.UNIT_TYPE;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected MongoCollection<Unit> getCollection() {
        return MetadataCollections.C_UNIT.getCollection();
    }

    @Override
    protected MetadataCollections getMetadataCollections() {
        return MetadataCollections.C_UNIT;
    }

    /**
     * This (Unit) is a root
     */
    public final void setRoot() {
        GlobalDatasDb.ROOTS.add(getId());
    }

    @Override
    public Unit save() throws MetaDataExecutionException {
        putBeforeSave();
        getMaxDepth();
        getMinDepth();
        if (updated()) {
            LOGGER.debug("Updated: {}", this);
            return this;
        }
        LOGGER.debug("Save: {}", this);
        insert();
        return this;
    }

    @Override
    protected boolean updated() throws MetaDataExecutionException {
        // XXX TODO P1 only addition is taken into consideration there: removal shall be done elsewhere
        final Unit vt = (Unit) MongoDbMetadataHelper.findOneNoAfterLoad(getMetadataCollections(), getId());
        BasicDBObject update = null;
        if (vt != null) {
            LOGGER.debug("UpdateLinks: {}\n\t{}", this, vt);
            final List<BasicDBObject> listAddToSet = new ArrayList<>();
            final List<BasicDBObject> listset = new ArrayList<>();
            /*
             * Only parent link, not child link
             */
            BasicDBObject upd =
                    MongoDbMetadataHelper.updateLinkset(this, vt, VitamLinks.UNIT_TO_UNIT, false);
            if (upd != null) {
                listAddToSet.add(upd);
            }
            upd = MongoDbMetadataHelper.updateLink(this, vt, VitamLinks.UNIT_TO_OBJECTGROUP, true);
            if (upd != null) {
                listset.add(upd);
            }
            // UNITDEPTHS
            @SuppressWarnings("unchecked") final HashMap<String, Integer> vtDepths =
                    (HashMap<String, Integer>) vt.remove(UNITDEPTHS);
            @SuppressWarnings("unchecked")
            HashMap<String, Integer> depthLevels =
                    (HashMap<String, Integer>) get(UNITDEPTHS);
            if (depthLevels == null) {
                depthLevels = new HashMap<>();
            }
            final BasicDBObject vtDepthLevels = new BasicDBObject();
            if (vtDepths != null) {
                // remove all not in current but in vt as already updated, for
                // the others compare vt with current
                for (final String unit : vtDepths.keySet()) {
                    final Integer pastval = vtDepths.get(unit);
                    final Integer newval = depthLevels.get(unit);
                    if (newval != null) {
                        if (pastval > newval) {
                            // to be remotely updated
                            vtDepthLevels.append(unit, newval);
                        } else {
                            // to be remotely updated
                            vtDepthLevels.append(unit, pastval);
                            // update only locally
                            depthLevels.put(unit, pastval);
                        }
                    } else {
                        // to be remotely updated
                        vtDepthLevels.append(unit, pastval);
                        // update only locally
                        depthLevels.put(unit, pastval);
                    }
                }
                // now add into remote update from current, but only non
                // existing in vt (already done)
                for (final String unit : depthLevels.keySet()) {
                    // remove by default
                    final Integer srcobj = vtDepths.get(unit);
                    final Integer obj = depthLevels.get(unit);
                    if (srcobj == null) {
                        // to be remotely updated
                        vtDepthLevels.append(unit, obj);
                    }
                }
                // Update locally
                append(UNITDEPTHS, depthLevels);
            }
            if (!vtDepthLevels.isEmpty()) {
                upd = new BasicDBObject(UNITDEPTHS, vtDepthLevels);
                listset.add(upd);
            }
            // Compute UNITUPS
            @SuppressWarnings("unchecked") final List<String> vtUps = (List<String>) vt.remove(UNITUPS);
            @SuppressWarnings("unchecked")
            List<String> ups = (List<String>) get(UNITUPS);
            if (ups == null) {
                ups = new ArrayList<>();
            }
            if (vtUps != null) {
                // remove all not in vt but in current as newly added
                ups.removeAll(vtUps);
            }
            if (!ups.isEmpty()) {
                final BasicDBObject vtDepthsBson = new BasicDBObject(UNITUPS,
                        new BasicDBObject(UPDATEACTIONARGS.EACH.exactToken(), ups));
                listAddToSet.add(vtDepthsBson);
            }
            try {
                update = new BasicDBObject();
                if (!listAddToSet.isEmpty()) {
                    upd = new BasicDBObject();
                    for (final BasicDBObject dbObject : listAddToSet) {
                        upd.putAll((BSONObject) dbObject);
                    }
                    update = update.append(MongoDbMetadataHelper.ADD_TO_SET, upd);
                }
                if (!listset.isEmpty()) {
                    upd = new BasicDBObject();
                    for (final BasicDBObject dbObject : listset) {
                        upd.putAll((BSONObject) dbObject);
                    }
                    update = update.append(UPDATEACTION.SET.exactToken(), upd);
                }
                update = update.append(UPDATEACTION.INC.exactToken(),
                        new BasicDBObject(NBCHILD, nb));
                nb = 0;
                update(update);
            } catch (final MongoException e) {
                LOGGER.error(EXCEPTION_FOR + update, e);
                throw e;
            }
            listAddToSet.clear();
            listset.clear();
            return true;
        } else {
            MongoDbMetadataHelper.updateLinkset(this, null, VitamLinks.UNIT_TO_UNIT, false);
            append(NBCHILD, nb);
            append(UNITUPS, new ArrayList<>());
            nb = 0;
        }
        return false;
    }

    @Override
    public boolean load() {
        final Unit vt = (Unit) MongoDbMetadataHelper.findOneNoAfterLoad(getMetadataCollections(), getId());
        if (vt == null) {
            return false;
        }
        putAll(vt);
        getAfterLoad();
        return true;
    }

    @Override
    public Unit getAfterLoad() {
        return this;
    }

    @Override
    public Unit putBeforeSave() {
        return this;
    }

    /**
     * Used in ingest (get the next uds including itself with depth +1 for all)
     *
     * @return the new unitdepth for children
     */
    public List<Bson> getSubDepth() {
        final String id = getId();

        // addAll to temporary ArrayList
        @SuppressWarnings("unchecked") final ArrayList<Document> vtDomaineLevels =
                (ArrayList<Document>) get(UNITDEPTHS);
        final int size = vtDomaineLevels != null ? vtDomaineLevels.size() + 1 : 1;

        // must compute depth from parent
        final List<Bson> sublist = new ArrayList<>(size);
        if (vtDomaineLevels != null) {
            for (int i = 0; i < vtDomaineLevels.size(); i++) {
                final Document currentParent = vtDomaineLevels.get(i);
                sublist.addAll(currentParent
                        .entrySet().stream().map(entry -> new BasicDBObject(entry.getKey(), (Integer) entry.getValue() + 1))
                        .collect(Collectors.toList()));
            }
        }
        sublist.add(new BasicDBObject(id, 1));
        return sublist;
    }

    /**
     * Used in ingest (get the next ups including itself)
     *
     * @return the new UNITUPS
     */
    public List<String> getSubUnitUps() {
        @SuppressWarnings("unchecked") final List<String> subids = (List<String>) get(UNITUPS);
        List<String> subids2;
        if (subids != null) {
            subids2 = new ArrayList<>(subids.size() + 1);
            subids2.addAll(subids);
        } else {
            subids2 = new ArrayList<>(1);
        }
        subids2.add(getId());
        return subids2;
    }

    /**
     * @return the map of parent units with depth
     */
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getDepths() {
        final Map<String, Integer> map = (Map<String, Integer>) get(UNITDEPTHS);
        if (map == null) {
            return SingletonUtils.singletonMap();
        }
        return map;
    }

    /**
     * @return the max depth of this node from existing parents
     */
    public int getMaxDepth() {
        final Map<String, Integer> map = getDepths();
        int depth = 0;
        if (map != null) {
            for (final Integer integer : map.values()) {
                if (depth < integer) {
                    depth = integer;
                }
            }
        }
        depth++;
        put(MAXDEPTH, depth);
        return depth;
    }

    /**
     * @return the min depth of this node from existing parents
     */
    public int getMinDepth() {
        final Map<String, Integer> map = getDepths();
        int depth = this.getInteger(MINDEPTH, GlobalDatasParser.MAXDEPTH);
        if (map != null) {
            for (final Integer integer : map.values()) {
                if (depth > integer) {
                    depth = integer;
                }
            }
        }
        if (depth == GlobalDatasParser.MAXDEPTH) {
            depth = 1;
        }
        put(MINDEPTH, depth);
        return depth;
    }

    private void updateAfterAddingSubUnit() throws MetaDataExecutionException {
        final BasicDBObject update = new BasicDBObject()
                .append(UPDATEACTION.INC.exactToken(),
                        new BasicDBObject(NBCHILD, nb));
        nb = 0;
        update(update);
    }

    /**
     * Add the link (N)-N between this Unit and sub Unit (update only subUnit)
     *
     * @param unit for adding the link
     * @return Unit with link added
     * @throws MetaDataExecutionException when adding exception occurred
     */
    public Unit addUnit(final Unit unit) throws MetaDataExecutionException {
        Bson update = null;
        final List<String> ids = new ArrayList<>();
        LOGGER.debug(this + "->" + unit);
        final BasicDBObject update2 =
                MongoDbMetadataHelper.addLink(this, VitamLinks.UNIT_TO_UNIT, unit);
        if (update2 != null) {
            ids.add(unit.getId());
            update = update2;
        }
        if (!ids.isEmpty()) {
            final List<Bson> sublist = getSubDepth();
            final Bson updateSubDepth = addEachToSet(UNITDEPTHS, sublist);
            final List<String> subids = getSubUnitUps();
            final Bson updateSubUnits = addEachToSet(UNITUPS, subids);
            Integer val = this.getInteger(MINDEPTH);
            int min = 1;
            if (val != null) {
                min += val;
            }
            val = this.getInteger(MAXDEPTH);
            int max = 1;
            if (val != null) {
                max += val;
            }
            update = combine(update, updateSubDepth, updateSubUnits);
            if (min < unit.getInteger(MINDEPTH)) {
                update = combine(update, set(MINDEPTH, min));
            }
            if (max > unit.getInteger(MAXDEPTH)) {
                update = combine(update, set(MAXDEPTH, max));
            }

            List<String> sps = (List<String>) get(ORIGINATING_AGENCIES);
            if (sps != null) {
                update = combine(update, addEachToSet(ORIGINATING_AGENCIES, sps));
            }

            LOGGER.debug(this + "->" + unit + "\n" +
                    "\t" + MongoDbHelper.bsonToString(update, false) + "\n\t" + min + ":" + max);
            try {
                final long nbc = getCollection().updateOne(eq(ID, ids.get(0)),
                        update,
                        new UpdateOptions().upsert(false)).getMatchedCount();
                nb += nbc;
                sublist.clear();
                subids.clear();
                updateAfterAddingSubUnit();
            } catch (final MongoException e) {
                LOGGER.error(EXCEPTION_FOR + update, e);
                throw new MetaDataExecutionException(e);
            }
        }
        ids.clear();
        return this;
    }

    /**
     * Add the link (N)-N between Unit and List of sub Units (update only subUnits)
     *
     * @param units list of units for adding the link
     * @return Unit with links added
     * @throws MetaDataExecutionException when adding exception occurred
     */
    public Unit addUnits(final List<Unit> units) throws MetaDataExecutionException {
        Bson update = null;
        final List<String> ids = new ArrayList<>();
        for (final Unit unit : units) {
            final BasicDBObject update2 = MongoDbMetadataHelper.addLink(this, VitamLinks.UNIT_TO_UNIT, unit);
            if (update2 != null) {
                ids.add(unit.getId());
                update = update2;
            }
        }
        if (!ids.isEmpty()) {
            final List<Bson> sublist = getSubDepth();
            final Bson updateSubDepth = addEachToSet(UNITDEPTHS, sublist);
            final List<String> subids = getSubUnitUps();
            final Bson updateSubUnits = addEachToSet(UNITUPS, subids);
            update = combine(update, updateSubDepth, updateSubUnits);
            Integer val = this.getInteger(MINDEPTH);
            int min = 1;
            if (val != null) {
                min += val;
            }
            val = this.getInteger(MAXDEPTH);
            int max = 1;
            if (val != null) {
                max += val;
            }
            try {
                final long nbc = getCollection().updateMany(in(ID, ids),
                        update,
                        new UpdateOptions().upsert(false)).getMatchedCount();
                nb += nbc;
                sublist.clear();
                subids.clear();
                getCollection().updateMany(
                        and(in(ID, ids), lt(MAXDEPTH, max)),
                        new BasicDBObject(MAXDEPTH, max),
                        new UpdateOptions().upsert(false));
                getCollection().updateMany(
                        and(in(ID, ids), gt(MINDEPTH, min)),
                        new BasicDBObject(MINDEPTH, min),
                        new UpdateOptions().upsert(false));
                updateAfterAddingSubUnit();
            } catch (final MongoException e) {
                LOGGER.error(EXCEPTION_FOR + update, e);
                throw new MetaDataExecutionException(e);
            }
        }
        ids.clear();
        return this;
    }

    /**
     * @return the list of UUID of children (database access)
     */
    public List<String> getChildrenUnitIdsFromParent() {
        final BasicDBObject condition = new BasicDBObject(
                VitamLinks.UNIT_TO_UNIT.field2to1, getId());
        @SuppressWarnings("unchecked") final FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbMetadataHelper
                .select(getMetadataCollections(), condition, MongoDbMetadataHelper.ID_PROJECTION);
        final List<String> ids = new ArrayList<>();
        try (final MongoCursor<Unit> iterator = iterable.iterator()) {
            while (iterator.hasNext()) {
                final String mid = iterator.next().getId();
                ids.add(mid);
            }
        }
        return ids;
    }

    /**
     * @param remove if remove the link between units
     * @return the list of UUID of Unit parents (immediate)
     */
    @SuppressWarnings("unchecked")
    public List<String> getFathersUnitIds(final boolean remove) {
        List<String> list;
        if (remove) {
            list = (List<String>) remove(VitamLinks.UNIT_TO_UNIT.field2to1);
        } else {
            list = (List<String>) this.get(VitamLinks.UNIT_TO_UNIT.field2to1);
        }
        if (list == null) {
            return SingletonUtils.singletonList();
        }
        return list;
    }

    /**
     * Add the link 1-N between Unit and ObjectGroup (update both Unit and ObjectGroup)
     *
     * @param data the objectgroup for adding link
     * @return Unit with link added
     * @throws MetaDataExecutionException when adding exception occurred
     */
    public Unit addObjectGroup(final ObjectGroup data)
            throws MetaDataExecutionException {
        final String old = getObjectGroupId(false);
        final String newGOT = data.getId();
        // TODO P1 when update is ready: change Junit to reflect this case
        if (old != null && !old.isEmpty() && !old.equals(newGOT)) {
            throw new MetaDataExecutionException("Cannot change ObjectGroup of Unit without removing it first");
        }
        final BasicDBObject update =
                MongoDbMetadataHelper.addLink(this, VitamLinks.UNIT_TO_OBJECTGROUP, data);
        if (update != null) {
            data.update(update);
            updated();
        }
        return this;
    }

    /**
     * @param remove if remove the link
     * @return the ObjectGroup UUID (may return null)
     */
    public String getObjectGroupId(final boolean remove) {
        if (remove) {
            return (String) remove(VitamLinks.UNIT_TO_OBJECTGROUP.field1to2);
        } else {
            return (String) this.get(VitamLinks.UNIT_TO_OBJECTGROUP.field1to2);
        }
    }

    /**
     * Check if the current Unit has other Unit as immediate parent
     *
     * @param other a unit that could be immediate parent of current unit
     * @return True if immediate parent, else False (however could be a grand parent)
     */
    public boolean isImmediateParent(final String other) {
        final Map<String, Integer> depth = getDepths();
        return depth.get(other) == 1;
    }

    /**
     * Used in loop operation to clean the object
     *
     * @param all If true, all items are cleaned
     */
    public final void cleanStructure(final boolean all) {
        remove(VitamLinks.UNIT_TO_UNIT.field1to2);
        remove(VitamLinks.UNIT_TO_UNIT.field2to1);
        remove(VitamLinks.UNIT_TO_OBJECTGROUP.field1to2);
        remove(ID);
        if (all) {
            remove(UNITDEPTHS);
            remove(UNITUPS);
            remove(MINDEPTH);
            remove(MAXDEPTH);
            remove(TYPE);
            remove(NBCHILD);
        }
    }

    protected static void addIndexes() {
        // if not set, Unit and Tree are worst
        for (final BasicDBObject index : indexes) {
            MetadataCollections.C_UNIT.getCollection().createIndex(index);
        }
    }

    protected static void dropIndexes() {
        for (final BasicDBObject index : indexes) {
            MetadataCollections.C_UNIT.getCollection().dropIndex(index);
        }
    }

}
