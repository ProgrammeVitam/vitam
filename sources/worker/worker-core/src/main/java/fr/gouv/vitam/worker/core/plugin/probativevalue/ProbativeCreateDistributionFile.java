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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.jsonl.JsonLineWriter;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.unit.SigningRoleType;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.SelectedUnit;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel.SIGNING_INFORMATION;
import static fr.gouv.vitam.common.model.unit.SigningInformationTypeModel.DETACHED_SIGNING_ROLE;
import static fr.gouv.vitam.common.model.unit.SigningInformationTypeModel.SIGNING_ROLE;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class ProbativeCreateDistributionFile extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProbativeCreateDistributionFile.class);

    private static final String HANDLER_ID = "PROBATIVE_VALUE_CREATE_DISTRIBUTION_FILE";

    public ProbativeCreateDistributionFile() {}

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            ProbativeValueRequest probativeValueRequest = loadRequest(handler);

            List<SelectedUnit> selectedUnitsWithInitialQuery = selectInitialQueryUnits(handler, probativeValueRequest);

            Collection<SelectedUnit> extendedResultSet = extendResultSetWithDetachedSigningInformation(
                handler,
                selectedUnitsWithInitialQuery,
                probativeValueRequest
            );

            String usageVersion = String.format(
                "%s_%s",
                probativeValueRequest.getUsage(),
                probativeValueRequest.getVersion()
            );
            File objectGroupsToCheck = generateDistributionFile(handler, extendedResultSet, usageVersion);

            handler.transferFileToWorkspace("distributionFile.jsonl", objectGroupsToCheck, false, false);

            return buildItemStatus(HANDLER_ID, OK, EventDetails.of("Creation of distribution file succeed."));
        } catch (Exception e) {
            LOGGER.error(e);
            return buildItemStatus(HANDLER_ID, KO, EventDetails.of("Creation of distribution file error."));
        }
    }

    private static ProbativeValueRequest loadRequest(HandlerIO handler) throws ProcessingStatusException {
        try {
            InputStream request = handler.getInputStreamFromWorkspace("request");
            return JsonHandler.getFromInputStream(request, ProbativeValueRequest.class);
        } catch (
            IOException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageServerException
            | InvalidParseOperationException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Cannot load request from workspace", e);
        }
    }

    private List<SelectedUnit> selectInitialQueryUnits(
        HandlerIO handlerIO,
        ProbativeValueRequest probativeValueRequest
    ) throws ProcessingStatusException {
        SelectMultiQuery initialSelectQuery = buildInitialSelectQuery(probativeValueRequest);
        return executeQuery(handlerIO, initialSelectQuery);
    }

    private SelectMultiQuery buildInitialSelectQuery(ProbativeValueRequest probativeValueRequest)
        throws ProcessingStatusException {
        boolean signingInformationAuditMode = includeDetachedSigningInformation(probativeValueRequest);

        try {
            JsonNode initialQuery = probativeValueRequest.getDslQuery();
            JsonNode restrictedQuery = getRestrictedQueryWithAccessContract(initialQuery);

            SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(restrictedQuery);
            SelectMultiQuery select = parser.getRequest();

            Query unitsHavingObjectGroupsQuery = QueryHelper.exists(VitamFieldsHelper.object());

            if (select.getQueries().isEmpty()) {
                select.getQueries().add(unitsHavingObjectGroupsQuery.setDepthLimit(0));
            } else {
                int lastQueryIndex = select.getQueries().size() - 1;
                Query lastQuery = select.getQueries().get(lastQueryIndex);
                int lastQueryDepth = lastQuery.getParserRelativeDepth();
                Query queryExistObject = and()
                    .add(lastQuery, unitsHavingObjectGroupsQuery)
                    .setDepthLimit(lastQueryDepth);
                select.getQueries().set(lastQueryIndex, queryExistObject);
            }

            // Add projections
            select.addUsedProjection(VitamFieldsHelper.id(), VitamFieldsHelper.object());
            if (signingInformationAuditMode) {
                select.addUsedProjection(SIGNING_INFORMATION + "." + DETACHED_SIGNING_ROLE);
            }

            return select;
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not build select query", e);
        }
    }

    private List<SelectedUnit> executeQuery(HandlerIO handlerIO, SelectMultiQuery selectQuery)
        throws ProcessingStatusException {
        try (MetaDataClient metadataClient = handlerIO.getMetaDataClient()) {
            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper.createUnitScrollSplitIterator(
                metadataClient,
                selectQuery
            );

            return scrollRequest
                .toStream()
                .filter(unit -> unit.hasNonNull(VitamFieldsHelper.object()))
                .map(SelectedUnit::fromUnit)
                .collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not execute select query", e);
        }
    }

    private Collection<SelectedUnit> extendResultSetWithDetachedSigningInformation(
        HandlerIO handlerIO,
        List<SelectedUnit> initialResultSet,
        ProbativeValueRequest probativeValueRequest
    ) throws ProcessingStatusException {
        if (!includeDetachedSigningInformation(probativeValueRequest)) {
            LOGGER.info("No need for Detached SigningInformation to be added to result set");
            return initialResultSet;
        }

        Map<String, SelectedUnit> totalSelectedUnitsByUnitId = initialResultSet
            .stream()
            .collect(Collectors.toMap(SelectedUnit::getUnitId, selectedUnit -> selectedUnit));

        Iterator<String> unitIdsWithDetachedSigningRoles = initialResultSet
            .stream()
            .filter(SelectedUnit::isHaveDetachedSigningRoles)
            .map(SelectedUnit::getUnitId)
            .iterator();

        Iterator<List<String>> partitionedUnitIdsWithDetachedSigningRoles = Iterators.partition(
            unitIdsWithDetachedSigningRoles,
            VitamConfiguration.getMaxElasticsearchBulk()
        );

        while (partitionedUnitIdsWithDetachedSigningRoles.hasNext()) {
            List<String> unitIds = partitionedUnitIdsWithDetachedSigningRoles.next();

            SelectMultiQuery select = buildSelectQueryForDetachedSigningInformationChildUnits(unitIds);
            List<SelectedUnit> detachedSigningInformationUnits = executeQuery(handlerIO, select);

            for (SelectedUnit unit : detachedSigningInformationUnits) {
                totalSelectedUnitsByUnitId.put(unit.getUnitId(), unit);
            }
        }
        return totalSelectedUnitsByUnitId.values();
    }

    private static SelectMultiQuery buildSelectQueryForDetachedSigningInformationChildUnits(Collection<String> unitIds)
        throws ProcessingStatusException {
        try {
            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(
                QueryHelper.and()
                    .add(
                        QueryHelper.in(VitamFieldsHelper.unitups(), unitIds.toArray(String[]::new)),
                        QueryHelper.exists(SIGNING_INFORMATION + "." + SIGNING_ROLE),
                        QueryHelper.ne(
                            SIGNING_INFORMATION + "." + SIGNING_ROLE,
                            SigningRoleType.SIGNED_DOCUMENT.getValue()
                        )
                    )
            );
            select.addUsedProjection(VitamFieldsHelper.id(), VitamFieldsHelper.object());
            select.addOrderByAscFilter(VitamFieldsHelper.object());

            return select;
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not select detached SigningInformation entries",
                e
            );
        }
    }

    /**
     * FIXME : For now, access-contract restrictions are "out of scope" of this US.
     *   We need to restrict query using access contract (see AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect)
     *   Important : Must be done for both "initial" AND "Detached SigningInformation child unit" queries
     */
    @Beta
    private static JsonNode getRestrictedQueryWithAccessContract(JsonNode query) {
        return query;
    }

    private static boolean includeDetachedSigningInformation(ProbativeValueRequest probativeValueRequest) {
        return Boolean.TRUE.equals(probativeValueRequest.getIncludeDetachedSigningInformation());
    }

    private File generateDistributionFile(
        HandlerIO handler,
        Collection<SelectedUnit> extendedResult,
        String usageVersion
    ) throws IOException, InvalidParseOperationException {
        File objectGroupsToCheck = handler.getNewLocalFile("OBJECT_GROUP_TO_CHECK.jsonl");
        try (
            FileOutputStream fileOutputStream = new FileOutputStream(objectGroupsToCheck);
            JsonLineWriter<JsonLineModel> writer = new JsonLineWriter<>(fileOutputStream)
        ) {
            // Group UnitIds by ObjectGroupId
            MultiValuedMap<String, String> unitIdsByObjectGroupId = new ArrayListValuedHashMap<>();
            for (SelectedUnit selectedUnit : extendedResult) {
                unitIdsByObjectGroupId.put(selectedUnit.getObjectGroupId(), selectedUnit.getUnitId());
            }

            // Append to distribution file
            for (String objectGroupId : unitIdsByObjectGroupId.keySet()) {
                Collection<String> unitIds = unitIdsByObjectGroupId.get(objectGroupId);
                writer.addEntry(toJsonLineDistribution(objectGroupId, usageVersion, unitIds));
            }
        }
        return objectGroupsToCheck;
    }

    private JsonLineModel toJsonLineDistribution(String objectId, String usageVersion, Collection<String> elementIds)
        throws InvalidParseOperationException {
        ObjectNode objectNode = createObjectNode();
        objectNode.set("unitIds", JsonHandler.toJsonNode(elementIds));
        objectNode.put("usageVersion", usageVersion);

        JsonLineModel model = new JsonLineModel();
        model.setId(objectId);
        model.setParams(objectNode);
        model.setDistribGroup(null);
        return model;
    }
}
