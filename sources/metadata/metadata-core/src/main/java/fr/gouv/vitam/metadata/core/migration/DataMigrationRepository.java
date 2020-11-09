/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.metadata.core.migration;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Projections.computed;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.OG;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ORIGINATING_AGENCIES;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ORIGINATING_AGENCY;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.UP;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.GRAPH;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.PARENT_ORIGINATING_AGENCIES;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.UNITDEPTHS;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.UNITUPS;

/**
 * Repository for mongo data migration
 */
public class DataMigrationRepository {

    private static final BasicDBObject UNIT_VITAM_GRAPH_PROJECTION =
        new BasicDBObject(UP, 1)
            .append(UNITUPS, 1)
            .append(GRAPH, 1)
            .append(ORIGINATING_AGENCIES, 1)
            .append(UNITDEPTHS, 1)
            .append(ORIGINATING_AGENCY, 1)
            .append(PARENT_ORIGINATING_AGENCIES, 1)
            .append(ID, 1)
            .append(OG, 1);


    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DataMigrationService.class);

    private static final String NB_UNIT_UPS = "nbUS";

    private final int bulkSize;

    /**
     * Constructor
     */
    public DataMigrationRepository() {
        this(VitamConfiguration.getMigrationBulkSize());
    }

    /**
     * Constructor for testing
     */
    @VisibleForTesting
    DataMigrationRepository(int bulkSize) {
        this.bulkSize = bulkSize;
    }

    /**
     * Returns all unit ids to migrate sorted by top-down hierarchy level, by chunks of (at most) BULK_SIZE.
     */
    public CloseableIterator<List<Unit>> selectUnitBulkInTopDownHierarchyLevel() {

        MongoCursor<Unit> unitMongoCursor = selectUnitsInTopDownHierarchyLevel();

        PeekingIterator<Unit> unitPeekingIterator = new PeekingIterator<>(unitMongoCursor);

        return new CloseableIterator<>() {

            int lastLevel;

            @Override
            public void close() {
                unitMongoCursor.close();
            }

            @Override
            public boolean hasNext() {
                return unitPeekingIterator.hasNext();
            }

            @Override
            public List<Unit> next() {

                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                List<String> bulkUnitIds = new ArrayList<>();

                // Bulk processing
                while (unitMongoCursor.hasNext()) {

                    Unit unit = unitPeekingIterator.peek();

                    int unitLevel = unit.getInteger(DataMigrationRepository.NB_UNIT_UPS);

                    boolean shouldSplitUnitLevel = !bulkUnitIds.isEmpty() && unitLevel > lastLevel;
                    if (shouldSplitUnitLevel) {
                        lastLevel = unitLevel;
                        break;
                    }

                    unit = unitPeekingIterator.next();
                    bulkUnitIds.add(unit.getId());

                    boolean shouldSplitByBulkSize = bulkUnitIds.size() == bulkSize;
                    if (shouldSplitByBulkSize) {
                        break;
                    }
                }

                return getUnitsByIds(bulkUnitIds);
            }
        };
    }

    /**
     * Returns all units to migrate order by top-down hierarchy level.
     *
     * Sorting units by _us field size ensures top-down processing : A unit has strictly more ancestors that any of its
     * parent units.
     */
    private MongoCursor<Unit> selectUnitsInTopDownHierarchyLevel() {

        LOGGER.info("Selecting units to migrate");

        StopWatch stopWatch = StopWatch.createStarted();

        AggregateIterable<Unit> units =
            MetadataCollections.UNIT.<Unit>getCollection().aggregate(
                Arrays.asList(
                    // Skip already migrated data
                    match(
                        exists(Unit.GRAPH_LAST_PERSISTED_DATE, false)
                    ),
                    // Select _id and $size(_us)
                    project(fields(
                        include(Unit.ID),
                        computed(NB_UNIT_UPS, new BasicDBObject("$size", "$" + Unit.UNITUPS))
                    )),
                    // Order by $size(_us)
                    Aggregates.sort(Sorts.orderBy(
                        Sorts.ascending(NB_UNIT_UPS)
                    ))
                ))
                // Batch
                .batchSize(bulkSize)
                // Aggregation query requires more than 100MB to proceed.
                .allowDiskUse(true);

        MongoCursor<Unit> iterator = units.iterator();
        stopWatch.stop();
        LOGGER.info("Query executed in : " + stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms");
        return iterator;
    }

    /**
     * Returns all units by ids
     */
    private List<Unit> getUnitsByIds(Collection<String> unitIds) {

        List<Unit> unitsToUpdate = new ArrayList<>();

        MetadataCollections.UNIT.<Unit>getCollection()
            .find(in(Unit.ID, unitIds))
            .batchSize(unitIds.size())
            .forEach((Consumer<? super Unit>) unitsToUpdate::add);

        return unitsToUpdate;
    }

    /**
     * Returns units graph by Ids
     */
    public Map<String, Unit> getUnitGraphByIds(Collection<String> unitIds) {

        FindIterable<Unit> iterable = MetadataCollections.UNIT.<Unit>getCollection()
            .find(in(Unit.ID, unitIds))
            .batchSize(unitIds.size())
            .projection(UNIT_VITAM_GRAPH_PROJECTION);

        Map<String, Unit> directParentById = new HashMap<>();
        try (MongoCursor<Unit> cursor = iterable.iterator()) {
            while (cursor.hasNext()) {
                Unit unit = cursor.next();
                directParentById.put(unit.getId(), unit);
            }
        }

        return directParentById;
    }

    /**
     * Replaces all units in unordered bulk mode
     */
    public void bulkReplaceUnits(List<Unit> updatedUnits) {

        List<WriteModel<Unit>> updates = new ArrayList<>();
        for (Unit unit : updatedUnits) {
            ReplaceOneModel<Unit> replaceOneModel =
                new ReplaceOneModel<>(eq(Unit.ID, unit.getId()), unit,
                    new UpdateOptions().upsert(false));
            updates.add(replaceOneModel);
        }

        MetadataCollections.UNIT.getCollection().bulkWrite(updates, new BulkWriteOptions().ordered(false));
    }

    /**
     * Returns all object group ids to migrate by chunks of (at most) BULK_SIZE.
     */
    public CloseableIterator<List<String>> selectObjectGroupBulk() {

        MongoCursor<ObjectGroup> ogMongoCursor = selectObjectGroups();

        return new CloseableIterator<>() {

            @Override
            public void close() {
                ogMongoCursor.close();
            }

            @Override
            public boolean hasNext() {
                return ogMongoCursor.hasNext();
            }

            @Override
            public List<String> next() {

                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                List<String> bulkGotIds = new ArrayList<>();

                // Bulk processing
                while (ogMongoCursor.hasNext()) {

                    ObjectGroup got = ogMongoCursor.next();
                    bulkGotIds.add(got.getId());

                    boolean shouldSplitByBulkSize = bulkGotIds.size() == bulkSize;
                    if (shouldSplitByBulkSize) {
                        break;
                    }
                }

                return bulkGotIds;
            }
        };
    }

    /**
     * Returns all object groups to migrate.
     */
    private MongoCursor<ObjectGroup> selectObjectGroups() {

        LOGGER.info("Selecting object groups to migrate");

        StopWatch stopWatch = StopWatch.createStarted();

        FindIterable<ObjectGroup> objectGroups =
            MetadataCollections.OBJECTGROUP.<ObjectGroup>getCollection().find(
                exists(ObjectGroup.GRAPH_LAST_PERSISTED_DATE, false))
                // Batch
                .batchSize(bulkSize);

        MongoCursor<ObjectGroup> iterator = objectGroups.iterator();
        stopWatch.stop();
        LOGGER.info("Query executed in : " + stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms");
        return iterator;
    }

    /**
     * Replaces all units in unordered bulk mode
     */
    public void bulkUpgradeObjectGroups(List<String> objectGroupIds) {

        String graphLastPersistedDate = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());

        MetadataCollections.OBJECTGROUP.getCollection().updateMany(
            in(ObjectGroup.ID, objectGroupIds),
            Updates.set(ObjectGroup.GRAPH_LAST_PERSISTED_DATE, graphLastPersistedDate)
        );
    }
}
