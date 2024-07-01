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
package fr.gouv.vitam.batch.report.rest.repository;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import fr.gouv.vitam.batch.report.model.PreservationStatsModel;
import fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static fr.gouv.vitam.batch.report.model.PreservationStatus.KO;
import static fr.gouv.vitam.batch.report.model.PreservationStatus.WARNING;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.ACTION;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.ANALYSE_RESULT;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.CREATION_DATE_TIME;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.DETAIL_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.GRIFFIN_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.INPUT_OBJECT_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.OBJECT_GROUP_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.OUTPUT_OBJECT_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.PROCESS_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.SCENARIO_ID;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.STATUS;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.TENANT;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.UNIT_ID;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.ANALYSE;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.EXTRACT;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.GENERATE;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.IDENTIFY;

public class PreservationReportRepository {

    private final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationReportRepository.class);

    public static final String PRESERVATION_REPORT = "PreservationReport";
    private final MongoCollection<Document> collection;

    @VisibleForTesting
    public PreservationReportRepository(MongoDbAccess mongoDbAccess, String collectionName) {
        this.collection = mongoDbAccess.getMongoDatabase().getCollection(collectionName);
    }

    public PreservationReportRepository(MongoDbAccess mongoDbAccess) {
        this(mongoDbAccess, PRESERVATION_REPORT);
    }

    public void bulkAppendReport(List<PreservationReportEntry> reports) {
        List<WriteModel<Document>> preservationDocument = reports
            .stream()
            .distinct()
            .map(PreservationReportRepository::modelToWriteDocument)
            .collect(Collectors.toList());

        collection.bulkWrite(preservationDocument);
    }

    private static WriteModel<Document> modelToWriteDocument(PreservationReportEntry model) {
        return new UpdateOneModel<>(
            and(eq(PROCESS_ID, model.getProcessId()), eq(DETAIL_ID, model.getDetailId())),
            new Document("$set", Document.parse(JsonHandler.unprettyPrint(model))).append(
                "$setOnInsert",
                new Document("_id", GUIDFactory.newGUID().toString())
            ),
            new UpdateOptions().upsert(true)
        );
    }

    public MongoCursor<Document> findCollectionByProcessIdTenant(String processId, int tenantId) {
        return collection
            .aggregate(
                Arrays.asList(
                    match(and(eq(PROCESS_ID, processId), eq(TENANT, tenantId))),
                    Aggregates.project(
                        Projections.fields(
                            new Document(PROCESS_ID, "$processId"),
                            new Document(CREATION_DATE_TIME, "$creationDateTime"),
                            new Document(UNIT_ID, "$unitId"),
                            new Document(OBJECT_GROUP_ID, "$objectGroupId"),
                            new Document(STATUS, "$status"),
                            new Document(ACTION, "$actions"),
                            new Document(ANALYSE_RESULT, "$analyseResult"),
                            new Document(INPUT_OBJECT_ID, "$inputObjectId"),
                            new Document(OUTPUT_OBJECT_ID, "$outputObjectId"),
                            new Document(GRIFFIN_ID, "$griffinId"),
                            new Document(SCENARIO_ID, "$preservationScenarioId")
                        )
                    )
                )
            )
            .allowDiskUse(true)
            .iterator();
    }

    public PreservationStatsModel stats(String processId, int tenantId) {
        Bson eqProcessId = eq(PROCESS_ID, processId);
        Bson eqTenant = eq(TENANT, tenantId);

        int nbUnits = getUnitAndObjectGroupStats(and(eqProcessId, eqTenant), UNIT_ID);
        int nbObjectGroups = getUnitAndObjectGroupStats(and(eqProcessId, eqTenant), OBJECT_GROUP_ID);
        int nbStatusKos = getStats(and(eqTenant, eqProcessId, eq(STATUS, KO.name())), STATUS);
        int nbStatusWarning = getStats(and(eqTenant, eqProcessId, eq(STATUS, WARNING.name())), STATUS);

        int nbActionsAnaylse = getStats(and(eqTenant, eqProcessId, eq(ACTION, ANALYSE.name())), ACTION);
        int nbActionsGenerate = getStats(and(eqTenant, eqProcessId, eq(ACTION, GENERATE.name())), ACTION);
        int nbActionsIdentify = getStats(and(eqTenant, eqProcessId, eq(ACTION, IDENTIFY.name())), ACTION);
        int nbActionsExtract = getStats(and(eqTenant, eqProcessId, eq(ACTION, EXTRACT.name())), ACTION);

        Spliterator<SimpleEntry<String, Integer>> mapAnalyseResult = nbActionsAnaylse > 0
            ? collection
                .aggregate(
                    Arrays.asList(
                        match(and(eqTenant, eqProcessId)),
                        project(include("analyseResult")),
                        group("$" + ANALYSE_RESULT, sum("count", 1))
                    )
                )
                .allowDiskUse(true)
                .batchSize(VitamConfiguration.getBatchSize())
                .map(d -> new SimpleEntry<>(d.getString("_id"), d.get("count", Integer.class)))
                .spliterator()
            : Spliterators.emptySpliterator();

        Map<String, Integer> analyseResults = StreamSupport.stream(mapAnalyseResult, false).collect(
            Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)
        );

        return new PreservationStatsModel(
            nbUnits,
            nbObjectGroups,
            nbStatusKos,
            nbActionsAnaylse,
            nbActionsGenerate,
            nbActionsIdentify,
            nbActionsExtract,
            analyseResults,
            nbStatusWarning
        );
    }

    private Integer getStats(Bson matchee, String name) {
        Document result = collection
            .aggregate(Arrays.asList(match(matchee), group(String.format("$%s", name), sum("result", 1))))
            .first();
        return result != null ? result.getInteger("result") : 0;
    }

    private Integer getUnitAndObjectGroupStats(Bson matchee, String name) {
        Map<String, Object> group = new HashMap<>();
        group.put("_id", null);
        group.put("count", new Document("$sum", 1));
        Document first = collection
            .aggregate(Arrays.asList(match(matchee), group(String.format("$%s", name)), new Document("$group", group)))
            .first();
        if (first != null) {
            return first.getInteger("count");
        }
        return 0;
    }

    public void deleteReportByIdAndTenant(String processId, int tenantId) {
        DeleteResult deleteResult = collection.deleteMany(and(eq(PROCESS_ID, processId), eq(TENANT, tenantId)));
        LOGGER.info("Deleted document count: " + deleteResult.getDeletedCount() + " for process " + processId);
    }
}
