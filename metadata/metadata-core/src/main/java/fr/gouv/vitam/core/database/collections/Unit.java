/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections;

import static com.mongodb.client.model.Updates.*;
import static com.mongodb.client.model.Filters.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteConcernException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.UPDATEACTION;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.UPDATEACTIONARGS;
import fr.gouv.vitam.core.database.collections.MongoDbAccess.VitamCollections;
import fr.gouv.vitam.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Unit class:<br>
 * @formatter:off
 * { 
 *   MD content, 
 *   _id: UUID, _dom: DomainId (tenant), _type: documentType,, _min: depthmin, _max: depthmax,
 *   _mgt. Management structure, 
 *   _uds: { UUID1 : depth1, UUID2 : depth2, ... }, // not indexed  and not to be in ES!
 *   _us: [ UUID1, UUID2, ... }, // indexed and equivalent to _uds 
 *   _up: [ UUID1, UUID2, ... ], // limited to immediate parent
 *   _og: UUID, _nb : immediateChildNb 
 * }
 * @formatter:on
 */
public class Unit extends VitamDocument<Unit> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Unit.class);

    private static final long serialVersionUID = -4351321928647834270L;

    /**
     * UNITDEPTHS : { UUID1 : depth2, UUID2 : depth2 }
     */
    public static final String UNITDEPTHS = "_uds";
    /**
     * UNITUPS : [ UUID1, UUID2 }
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
     * Quick projection for ID and ObjectGroup Only
     */
    public static final BasicDBObject UNIT_OBJECTGROUP_PROJECTION = 
            new BasicDBObject(VitamDocument.ID, 1).append(VitamDocument.OG, 1).append(DOMID, 1);
    /**
     * Unit Id, Vitam fields Only projection (no content nor management)
     */
    public static final BasicDBObject UNIT_VITAM_PROJECTION =
            new BasicDBObject(NBCHILD, 1).append(TYPE, 1).append(UNITUPS, 1).append(UNITDEPTHS, 1)
            .append(MINDEPTH, 1).append(MAXDEPTH, 1)
            .append(DOMID, 1).append(VitamDocument.UP, 1).append(VitamDocument.ID, 1);
    /**
     * Unit Id, Vitam and Management fields Only projection (no content)
     */
    public static final BasicDBObject UNIT_VITAM_MANAGEMENT_PROJECTION =
            new BasicDBObject(UNIT_VITAM_PROJECTION)
            .append(MANAGEMENT+".$", 1);
    /**
     * Storage Rule
     */
    public static final String STORAGERULE = MANAGEMENT+".storageRule";
    /**
     * Appraisal Rule
     */
    public static final String APPRAISALRULE = MANAGEMENT+".appraisalRule";
    /**
     * Access Rule
     */
    public static final String ACCESSRULE = MANAGEMENT+".accessRule";
    /**
     * Dissemination Rule
     */
    public static final String DISSEMINATIONRULE = MANAGEMENT+".disseminationRule";
    /**
     * Reuse Rule
     */
    public static final String REUSERULE = MANAGEMENT+".reuseRule";
    /**
     * Classification Rule
     */
    public static final String CLASSIFICATIONRULE = MANAGEMENT+".classificationRule";

    /**
     * Rule
     */
    public static final String RULE = ".rules.rule";
    /**
     * Rule end date (computed)
     */
    public static final String END = ".rules._end";

    @SuppressWarnings("javadoc")
    public static final String STORAGERULES = STORAGERULE+RULE;
    @SuppressWarnings("javadoc")
    public static final String STORAGEEND = STORAGERULE+END;
    @SuppressWarnings("javadoc")
    public static final String APPRAISALRULES = APPRAISALRULE+RULE;
    @SuppressWarnings("javadoc")
    public static final String APPRAISALEND = APPRAISALRULE+END;
    @SuppressWarnings("javadoc")
    public static final String ACCESSRULES = ACCESSRULE+RULE;
    @SuppressWarnings("javadoc")
    public static final String ACCESSEND = ACCESSRULE+END;
    @SuppressWarnings("javadoc")
    public static final String DISSEMINATIONRULES = DISSEMINATIONRULE+RULE;
    @SuppressWarnings("javadoc")
    public static final String DISSEMINATIONEND = DISSEMINATIONRULE+END;
    @SuppressWarnings("javadoc")
    public static final String REUSERULES = REUSERULE+RULE;
    @SuppressWarnings("javadoc")
    public static final String REUSEEND = REUSERULE+END;
    @SuppressWarnings("javadoc")
    public static final String CLASSIFICATIONRULES = CLASSIFICATIONRULE+RULE;
    @SuppressWarnings("javadoc")
    public static final String CLASSIFICATIONEND = CLASSIFICATIONRULE+END;
    
    private static final BasicDBObject[] indexes = {
            new BasicDBObject(VitamLinks.Unit2Unit.field2to1, 1),
            new BasicDBObject(VitamLinks.Unit2ObjectGroup.field1to2, 1),
            new BasicDBObject(DOMID, 1),
            new BasicDBObject(UNITUPS, 1),
            new BasicDBObject(MINDEPTH, 1),
            new BasicDBObject(MAXDEPTH, 1),
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
            new BasicDBObject(TYPE, 1) };

    /**
     * Default Rule usage Class
     */
    public static class RuleUsage {
        String rule;
        Date startDate;
        Date _end;
    }
    /**
     * Default Rule Class
     */
    public static class RuleType {
        List<RuleUsage> rules;
        boolean preventInheritance;
        List<String> refNonRuleIds;
    }
    /**
     * Default Rule with Action Class
     */
    public static class RuleActionType {
        List<RuleUsage> rules;
        boolean preventInheritance;
        List<String> refNonRuleIds;
        String finalAction;
    }
    /**
     * Classification Rule Class
     */
    public static class RuleClassificationType {
        List<RuleUsage> rules;
        boolean preventInheritance;
        List<String> refNonRuleIds;
        String classificationLevel;
        String classificationOwner;
        Date classificationReassessingDate;
        boolean needReassessingAuthorization;
    }
    /**
     * Management Class
     */
    public static class Management {
        RuleActionType storageRule;
        RuleActionType appraisalRule;
        RuleType accessRule;
        RuleType disseminationRule;
        RuleType reuseRule;
        RuleClassificationType classificationRule;
        boolean needAuthorization;
    }
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
     * @param content
     */
    public Unit(JsonNode content) {
        super(content);
    }

    /**
     * Constructor from Document
     * @param content
     */
    public Unit(Document content) {
        super(content);
    }

    /**
     * Constructor from Json as Text
     * @param content
     */
    public Unit(String content) {
        super(content);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected MongoCollection<Unit> getCollection() {
        return (MongoCollection<Unit>) MongoDbAccess.VitamCollections.Cunit.getCollection();
    }

	@Override
	protected VitamCollections getVitamCollections() {
		return MongoDbAccess.VitamCollections.Cunit;
	}

    /**
     * This (Unit) is a root
     */
    public final void setRoot() {
        GlobalDatasDb.ROOTS.add(this.getId());
    }

    @Override
    public Unit save() throws MongoWriteException, MongoWriteConcernException, MongoException {
        putBeforeSave();
        getMaxDepth();
        getMinDepth();
        if (updated()) {
            LOGGER.debug("Updated: {}", this);
            return this;
        }
        LOGGER.debug("Save: {}", this);
        insert();
        MongoDbAccess.LRU.put(getId(), this);
        return this;
    }

    @Override
    protected boolean updated() throws MongoWriteException, MongoWriteConcernException, MongoException {
        // XXX FIXME only addition is taken into consideration there: removal shall be done elsewhere
        final Unit vt = (Unit) MongoDbHelper.findOneNoAfterLoad(getVitamCollections(), getId());
        BasicDBObject update = null;
        if (vt != null) {
            LOGGER.debug("UpdateLinks: {}\n\t{}", this, vt);
            final List<BasicDBObject> listAddToSet = new ArrayList<>();
            final List<BasicDBObject> listset = new ArrayList<>();
            /*
             * Only parent link, not child link
             */
            BasicDBObject upd =
            		MongoDbHelper.updateLinks(this, vt, VitamLinks.Unit2Unit, false);
            if (upd != null) {
                listAddToSet.add(upd);
            }
            upd = MongoDbHelper.updateLink(this, vt, VitamLinks.Unit2ObjectGroup, true);
            if (upd != null) {
                listset.add(upd);
            }
            // UNITDEPTHS
            @SuppressWarnings("unchecked")
            final HashMap<String, Integer> vtDepths =
                    (HashMap<String, Integer>) vt.remove(UNITDEPTHS);
            @SuppressWarnings("unchecked")
            HashMap<String, Integer> depthLevels =
                    (HashMap<String, Integer>) get(UNITDEPTHS);
            if (depthLevels == null) {
                depthLevels = new HashMap<String, Integer>();
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
                            vtDepthLevels.append(unit, newval); // to be remotely updated
                        } else {
                            vtDepthLevels.append(unit, pastval); // to be remotely updated
                            depthLevels.put(unit, pastval); // update only locally
                        }
                    } else {
                        vtDepthLevels.append(unit, pastval); // to be remotely updated
                        depthLevels.put(unit, pastval); // update only locally
                    }
                }
                // now add into remote update from current, but only non
                // existing in vt (already done)
                for (final String unit : depthLevels.keySet()) {
                    // remove by default
                    final Integer srcobj = vtDepths.get(unit);
                    final Integer obj = depthLevels.get(unit);
                    if (srcobj == null) {
                        vtDepthLevels.append(unit, obj); // will be updated remotely
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
            @SuppressWarnings("unchecked")
            final List<String> vtUps = (List<String>) vt.remove(UNITUPS);
            @SuppressWarnings("unchecked")
            List<String> ups = (List<String>) get(UNITUPS);
            if (ups == null) {
                ups = new ArrayList<String>();
            }
            if (vtUps != null) {
                // remove all not in vt but in current as newly added
                ups.removeAll(vtUps);
            }
            if (! ups.isEmpty()) {
                BasicDBObject vtDepthsBson = new BasicDBObject(UNITUPS, 
                        new BasicDBObject(UPDATEACTIONARGS.each.exactToken(), ups));
                listAddToSet.add(vtDepthsBson);
            }
            try {
                update = new BasicDBObject();
                if (!listAddToSet.isEmpty()) {
                    upd = new BasicDBObject();
                    for (final BasicDBObject dbObject : listAddToSet) {
                        upd.putAll((BSONObject) dbObject);
                    }
                    update = update.append(MongoDbHelper.ADD_TO_SET, upd);
                }
                if (!listset.isEmpty()) {
                    upd = new BasicDBObject();
                    for (final BasicDBObject dbObject : listset) {
                        upd.putAll((BSONObject) dbObject);
                    }
                    update = update.append(UPDATEACTION.set.exactToken(), upd);
                }
                update = update.append(UPDATEACTION.inc.exactToken(),
                        new BasicDBObject(NBCHILD, nb));
                nb = 0;
                update(update);
                MongoDbAccess.LRU.put(getId(), this);
            } catch (final MongoException e) {
                LOGGER.error("Exception for " + update, e);
                throw e;
            }
            listAddToSet.clear();
            listset.clear();
            return true;
        } else {
            // MongoDbAccess.updateLinks(this, null, VitamLinks.Unit2Unit,
            // true);
        	MongoDbHelper.updateLinks(this, null, VitamLinks.Unit2Unit, false);
            append(NBCHILD, nb);
            nb = 0;
        }
        return false;
    }

    @Override
    public boolean load() {
        final Unit vt = (Unit) MongoDbHelper.findOneNoAfterLoad(getVitamCollections(), getId());
        if (vt == null) {
            return false;
        }
        this.putAll(vt);
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
        // addAll to temporary HashMap
        @SuppressWarnings("unchecked")
        final HashMap<String, Integer> vtDomaineLevels =
                (HashMap<String, Integer>) get(UNITDEPTHS);
        int size = vtDomaineLevels != null ? vtDomaineLevels.size() + 1 : 1;
        // must compute depth from parent
        List<Bson> sublist = new ArrayList<Bson>(size);
        if (vtDomaineLevels != null) {
            for (final java.util.Map.Entry<String, Integer> entry : vtDomaineLevels
                    .entrySet()) {
                sublist.add(new BasicDBObject(entry.getKey(), entry.getValue() + 1));
            }
        }
        sublist.add(new BasicDBObject(id, 1));
        return sublist;
    }

    /**
     * Used in ingest (get the next ups including itself)
     * @return the new UNITUPS
     */
    public List<String> getSubUnitUps() {
        @SuppressWarnings("unchecked")
        List<String> subids = (List<String>) get(UNITUPS);
        List<String> subids2;
        if (subids != null) {
        	subids2 = new ArrayList<String>(subids.size()+1);
            subids2.addAll(subids);
        } else {
        	subids2 = new ArrayList<String>(1);
        }
        subids2.add(getId());
        return subids2;
    }
    /**
     *
     * @return the map of parent units with depth
     */
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getDepths() {
        return (Map<String, Integer>) get(UNITDEPTHS);
    }

    /**
     * 
     * @return the max depth of this node from existing parents
     */
    public int getMaxDepth() {
        Map<String, Integer> map = getDepths();
        int depth = 0;
        if (map != null) {
	        for (Iterator<Integer> iterator = map.values().iterator(); iterator.hasNext();) {
	            Integer type = (Integer) iterator.next();
	            if (depth < type) {
	                depth = type;
	            }
	        }
        }
        depth++;
        this.put(MAXDEPTH, depth);
        return depth;
    }

    /**
     * 
     * @return the min depth of this node from existing parents
     */
    public int getMinDepth() {
        Map<String, Integer> map = getDepths();
        int depth = this.getInteger(MINDEPTH, GlobalDatasDb.MAXDEPTH);
        if (map != null) {
	        for (Iterator<java.util.Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
	                iterator.hasNext();) {
	            java.util.Map.Entry<String, Integer> entry = iterator.next();
	            if (entry.getValue() == 1) {
	                Unit parent = MongoDbAccess.LRU.get(entry.getKey());
	                int parentDepth = parent.getInteger(MINDEPTH) + 1;
	                if (depth > parentDepth) {
	                    depth = parentDepth;
	                }
	            }
	        }
        }
        if (depth == GlobalDatasDb.MAXDEPTH) {
        	depth = 1;
        }
        this.put(MINDEPTH, depth);
        return depth;
    }

    
    /**
     * Add the link N-N between this Unit and sub Unit
     *
     * @param unit
     * @return this
     */
    public Unit addUnit(final Unit unit) throws MongoWriteException, MongoWriteConcernException, MongoException {
        Bson update = null;
        final List<String> ids = new ArrayList<>();
        LOGGER.debug(this+"->"+unit);
        final BasicDBObject update2 =
        		MongoDbHelper.addLink(this, VitamLinks.Unit2Unit, unit);
        if (update2 != null) {
            ids.add(unit.getId());
            update = update2;
        }
        if (!ids.isEmpty()) {
            List<Bson> sublist = getSubDepth();
            Bson updateSubDepth = addEachToSet(UNITDEPTHS, sublist);
            List<String> subids = getSubUnitUps();
            Bson updateSubUnits = addEachToSet(UNITUPS, subids);
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
            LOGGER.debug(this+"->"+unit+"\n"+
            		"\t"+MongoDbHelper.bsonToString(update, false)+"\n\t"+min+":"+max);
            try {
                long nbc = getCollection().updateOne(in(ID, ids),
                        update,
                        new UpdateOptions().upsert(false)).getMatchedCount();
                nb += nbc;
                sublist.clear();
                subids.clear();
            } catch (final MongoException e) {
                LOGGER.error("Exception for " + update, e);
                throw e;
            }
        }
        ids.clear();
        return this;
    }
    
    /**
     * Add the link N-N between Unit and List of sub Unit
     *
     * @param units
     * @return this
     */
    public Unit addUnit(final List<Unit> units) throws MongoWriteException, MongoWriteConcernException, MongoException {
        Bson update = null;
        final List<String> ids = new ArrayList<>();
        for (final Unit unit : units) {
            final BasicDBObject update2 =
            		MongoDbHelper.addLink(this, VitamLinks.Unit2Unit, unit);
            if (update2 != null) {
                ids.add(unit.getId());
                update = update2;
            }
        }
        if (!ids.isEmpty()) {
            List<Bson> sublist = getSubDepth();
            Bson updateSubDepth = addEachToSet(UNITDEPTHS, sublist);
            List<String> subids = getSubUnitUps();
            Bson updateSubUnits = addEachToSet(UNITUPS, subids);
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
                long nbc = getCollection().updateMany(in(ID, ids),
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
            } catch (final MongoException e) {
                LOGGER.error("Exception for " + update, e);
                throw e;
            }
        }
        ids.clear();
        return this;
    }

    /**
     *
     * @return the list of UUID of children (database access)
     */
    public List<String> getChildrenUnitIdsFromParent() {
        BasicDBObject condition = new BasicDBObject(
                VitamLinks.Unit2Unit.field2to1, this.getId());
        @SuppressWarnings("unchecked")
        FindIterable<Unit> iterable = (FindIterable<Unit>) MongoDbHelper
                .select(getVitamCollections(), condition, MongoDbHelper.ID_PROJECTION);
        final List<String> ids = new ArrayList<>();
        MongoCursor<Unit> iterator = iterable.iterator();
        try {
            while (iterator.hasNext()) {
                final String mid = iterator.next().getId();
                ids.add(mid);
            }
        } finally {
            iterator.close();
        }
        return ids;
    }

    /**
     *
     * @param remove
     * @return the list of UUID of Unit parents (immediate)
     */
    @SuppressWarnings("unchecked")
    public List<String> getFathersUnitIds(final boolean remove) {
        if (remove) {
            return (List<String>) remove(VitamLinks.Unit2Unit.field2to1);
        } else {
            return (List<String>) this.get(VitamLinks.Unit2Unit.field2to1);
        }
    }

    /**
     * Add the link 1-N between Unit and ObjectGroup
     *
     * @param data
     * @return this
     */
    public Unit addObjectGroup(final ObjectGroup data) throws MongoWriteException, MongoWriteConcernException, MongoException {
        final BasicDBObject update =
        		MongoDbHelper.addLink(this, VitamLinks.Unit2ObjectGroup, data);
        if (update != null) {
            data.update(update);
        }
        return this;
    }

    /**
     *
     * @param remove
     * @return the ObjectGroup UUID
     */
    public String getObjectGroupId(final boolean remove) {
        if (remove) {
            return (String) remove(VitamLinks.Unit2ObjectGroup.field1to2);
        } else {
            return (String) this.get(VitamLinks.Unit2ObjectGroup.field1to2);
        }
    }

    /**
     * Check if the current Unit has other Unit as immediate parent
     *
     * @param other
     * @return True if immediate parent, else False (however could be a grand
     *         parent)
     */
    public boolean isImmediateParent(final String other) {
        Map<String, Integer> depth = getDepths();
        return (depth.get(other) == 1);
    }

    /**
     * Used in loop operation to clean the object
     *
     * @param all
     *            If true, all items are cleaned
     */
    public final void cleanStructure(final boolean all) {
        remove(VitamLinks.Unit2Unit.field1to2);
        remove(VitamLinks.Unit2Unit.field2to1);
        remove(VitamLinks.Unit2ObjectGroup.field1to2);
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
        for (BasicDBObject index : indexes) {
            MongoDbAccess.VitamCollections.Cunit.getCollection().createIndex(index);
        }
    }

    protected static void dropIndexes() {
        for (BasicDBObject index : indexes) {
            MongoDbAccess.VitamCollections.Cunit.getCollection().dropIndex(index);
        }
    }
}
