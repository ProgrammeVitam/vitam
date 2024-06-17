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
package fr.gouv.vitam.functional.administration.core.griffin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.preservation.DefaultGriffin;
import fr.gouv.vitam.common.model.administration.preservation.GriffinByFormat;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.Griffin;
import fr.gouv.vitam.functional.administration.common.PreservationScenario;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.format.model.FunctionalOperationModel;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.LocalDateUtil.getFormattedDateTimeForMongo;
import static fr.gouv.vitam.common.LocalDateUtil.now;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.tenant;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.getConcernedDiffLines;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.getUnifiedDiff;
import static fr.gouv.vitam.common.guid.GUIDReader.getGUID;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.functional.administration.common.Griffin.IDENTIFIER;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.GRIFFIN;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.PRESERVATION_SCENARIO;
import static fr.gouv.vitam.functional.administration.core.griffin.LogbookGriffinHelper.createLogbook;
import static fr.gouv.vitam.functional.administration.core.griffin.LogbookGriffinHelper.createLogbookEventKo;
import static fr.gouv.vitam.functional.administration.core.griffin.LogbookGriffinHelper.createLogbookEventSuccess;
import static fr.gouv.vitam.functional.administration.core.griffin.LogbookGriffinHelper.createLogbookEventWarning;
import static fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory.getInstance;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.Response.Status.CREATED;

public class GriffinService {

    private static final String GRIFFIN_BACKUP_EVENT = "STP_BACKUP_GRIFFIN";
    private static final String GRIFFIN_IMPORT_EVENT = "STP_IMPORT_GRIFFIN";
    private static final String GRIFFIN_REPORT = "GRIFFIN_REPORT";
    private static final String UND_TENANT = "_tenant";

    private final VitamCollection<PreservationScenario> preservationScenarioCollection;
    private final MongoDbAccessReferential mongoDbAccess;
    private final FunctionalBackupService functionalBackupService;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    @VisibleForTesting
    GriffinService(
        MongoDbAccessReferential mongoDbAccess,
        FunctionalBackupService functionalBackupService,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        VitamCollection<PreservationScenario> preservationScenarioCollection
    ) {
        this.mongoDbAccess = mongoDbAccess;
        this.functionalBackupService = functionalBackupService;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.preservationScenarioCollection = preservationScenarioCollection;
    }

    public GriffinService(MongoDbAccessAdminImpl mongoAccess, FunctionalBackupService functionalBackupService) {
        this(mongoAccess, functionalBackupService, getInstance(), PRESERVATION_SCENARIO.getVitamCollection());
    }

    public RequestResponse<GriffinModel> importGriffin(@NotNull List<GriffinModel> griffinsFromRequest)
        throws VitamException, InvalidCreateOperationException {
        String operationId = getVitamSession().getRequestId();
        GUID guid = getGUID(operationId);

        createLogbook(logbookOperationsClientFactory, guid, GRIFFIN_IMPORT_EVENT);

        try {
            validate(griffinsFromRequest);

            final Set<String> griffinIdsToDelete = new HashSet<>();
            final List<GriffinModel> griffinsToUpdate = new ArrayList<>();
            final List<GriffinModel> griffinsToInsert = new ArrayList<>();

            final ObjectNode finalSelect = new Select().getFinalSelect();
            DbRequestResult result = mongoDbAccess.findDocuments(finalSelect, GRIFFIN);
            final List<GriffinModel> allGriffins = result.getDocuments(Griffin.class, GriffinModel.class);

            classifyDataInInsertUpdateOrDeleteLists(
                griffinsFromRequest,
                griffinsToInsert,
                griffinsToUpdate,
                griffinIdsToDelete,
                allGriffins
            );

            checkDeletion(griffinIdsToDelete);

            List<String> griffinIdsToUpdate = griffinsToUpdate
                .stream()
                .map(GriffinModel::getIdentifier)
                .collect(Collectors.toList());
            Set<String> griffinsToUpdateUsedByScenario = getGriffinIdentifierUsedByScenario(griffinIdsToUpdate);

            insertGriffins(griffinsToInsert);
            updateGriffins(griffinsToUpdate);
            deleteGriffins(griffinIdsToDelete);

            Set<String> griffinsIdsToInsert = griffinsToInsert
                .stream()
                .map(GriffinModel::getIdentifier)
                .collect(toSet());

            GriffinReport griffinReport = generateReport(
                allGriffins,
                griffinsFromRequest,
                griffinIdsToUpdate,
                griffinIdsToDelete,
                griffinsIdsToInsert,
                griffinsToUpdateUsedByScenario
            );

            functionalBackupService.saveCollectionAndSequence(guid, GRIFFIN_BACKUP_EVENT, GRIFFIN, operationId);

            saveReport(guid, griffinReport);

            if (!griffinReport.getWarnings().isEmpty()) {
                createLogbookEventWarning(
                    logbookOperationsClientFactory,
                    guid,
                    GRIFFIN_IMPORT_EVENT,
                    GriffinReport.onlyWarning(griffinReport)
                );
            } else {
                createLogbookEventSuccess(logbookOperationsClientFactory, guid, GRIFFIN_IMPORT_EVENT);
            }

            return new RequestResponseOK<GriffinModel>()
                .addAllResults(griffinsFromRequest)
                .setHttpCode(CREATED.getStatusCode());
        } catch (InvalidCreateOperationException | VitamException e) {
            createLogbookEventKo(logbookOperationsClientFactory, guid, GRIFFIN_IMPORT_EVENT, e.getMessage());
            throw e;
        }
    }

    private void saveReport(GUID guid, GriffinReport griffinReport) throws StorageException {
        try (InputStream reportInputStream = JsonHandler.writeToInpustream(griffinReport)) {
            final String fileName = guid.getId() + ".json";

            functionalBackupService.saveFile(reportInputStream, guid, GRIFFIN_REPORT, DataCategory.REPORT, fileName);
        } catch (IOException | VitamException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private FunctionalOperationModel retrieveOperationModel() {
        try {
            JsonNode result = logbookOperationsClientFactory
                .getClient()
                .selectOperationById(VitamThreadUtils.getVitamSession().getRequestId());

            return JsonHandler.getFromJsonNode(result.get(TAG_RESULTS).get(0), FunctionalOperationModel.class);
        } catch (LogbookClientException | InvalidParseOperationException e) {
            throw new VitamRuntimeException("Could not load operation data", e);
        }
    }

    private GriffinReport generateReport(
        List<GriffinModel> currentGriffinsModels,
        List<GriffinModel> newGriffinsModels,
        List<String> updatedIdentifiers,
        Set<String> removedIdentifiers,
        Set<String> addedIdentifiers,
        Set<String> griffinIdentifiersUsedInPSC
    ) {
        GriffinReport report = new GriffinReport();

        FunctionalOperationModel operationModel = retrieveOperationModel();

        report.setOperation(operationModel);

        if (!currentGriffinsModels.isEmpty()) {
            report.setPreviousGriffinsVersion(currentGriffinsModels.get(0).getExecutableVersion());
            report.setPreviousGriffinsCreationDate(currentGriffinsModels.get(0).getCreationDate());
        }

        if (!newGriffinsModels.isEmpty()) {
            report.setNewGriffinsVersion(newGriffinsModels.get(0).getExecutableVersion());
            report.setPreviousGriffinsCreationDate(newGriffinsModels.get(0).getCreationDate());
        }

        Map<String, GriffinModel> currentGriffinsModelsByIdentifiers = newGriffinsModels
            .stream()
            .collect(Collectors.toMap(GriffinModel::getIdentifier, model -> model));

        Map<String, GriffinModel> newGriffinsModelByIdentifiers = newGriffinsModels
            .stream()
            .collect(Collectors.toMap(GriffinModel::getIdentifier, model -> model));

        report.setRemovedIdentifiers(removedIdentifiers);

        report.setAddedIdentifiers(addedIdentifiers);

        reportUpdatedIdentifiers(
            updatedIdentifiers,
            report,
            currentGriffinsModelsByIdentifiers,
            newGriffinsModelByIdentifiers
        );

        reportVersioning(report);

        if (!griffinIdentifiersUsedInPSC.isEmpty()) {
            report.addWarning(
                String.format(
                    " identifier(s) %s updated but they're already used in preservation scenarios.",
                    griffinIdentifiersUsedInPSC.toString()
                )
            );
        }

        if (report.getWarnings().isEmpty()) {
            return report.setStatusCode(StatusCode.OK);
        }

        return report.setStatusCode(StatusCode.WARNING);
    }

    private void reportVersioning(GriffinReport report) {
        if (report.getPreviousGriffinsCreationDate() != null && report.getNewGriffinsCreationDate() != null) {
            String previousDate = LocalDateUtil.getFormattedDateTimeForMongo(report.getPreviousGriffinsCreationDate());
            String newDate = LocalDateUtil.getFormattedDateTimeForMongo(report.getNewGriffinsCreationDate());

            if (previousDate.equals(newDate)) {
                report.addWarning("Same referential date: " + report.getNewGriffinsCreationDate());
            }

            if (previousDate.compareTo(newDate) > 0) {
                report.addWarning(
                    "New imported referential date " +
                    report.getNewGriffinsCreationDate() +
                    " is older than previous report date " +
                    report.getNewGriffinsCreationDate()
                );
            }
        }
    }

    private void reportUpdatedIdentifiers(
        List<String> updatedIdentifiers,
        GriffinReport report,
        Map<String, GriffinModel> currentGriffinsModelsByIdentifiers,
        Map<String, GriffinModel> newGriffinsModelByIdentifiers
    ) {
        for (String identifier : updatedIdentifiers) {
            GriffinModel currentGriffinModel = currentGriffinsModelsByIdentifiers.get(identifier);
            GriffinModel newGriffinModel = newGriffinsModelByIdentifiers.get(identifier);

            List<String> diff = diff(currentGriffinModel, newGriffinModel);
            report.addUpdatedIdentifiers(identifier, diff);
        }
    }

    private void validate(List<GriffinModel> listToImport) throws ReferentialException {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        List<String> identifiers = new ArrayList<>();
        for (GriffinModel model : listToImport) {
            if (identifiers.contains(model.getIdentifier())) {
                throw new ReferentialException(String.format("Duplicate griffin : '%s'.", model.getIdentifier()));
            }

            Set<ConstraintViolation<GriffinModel>> constraint = validator.validate(model);
            if (!constraint.isEmpty()) {
                throw new ReferentialException(
                    String.format(
                        "Invalid griffin for : '%s' : '%s'.",
                        model.getIdentifier(),
                        getConstraintsStrings(constraint)
                    )
                );
            }

            identifiers.add(model.getIdentifier());
        }
    }

    private void checkDeletion(Set<String> griffinsToDelete) throws ReferentialException {
        if (griffinsToDelete.isEmpty()) {
            return;
        }

        Set<String> griffinsToDeleteUsedByScenario = getGriffinIdentifierUsedByScenario(griffinsToDelete);

        if (!griffinsToDeleteUsedByScenario.isEmpty()) {
            throw new ReferentialException(
                String.format("Can not remove used griffin(s), %s.", String.join(", ", griffinsToDeleteUsedByScenario))
            );
        }
    }

    private Set<String> getGriffinIdentifierUsedByScenario(Collection<String> griffinIds) {
        if (griffinIds.isEmpty()) {
            return Collections.emptySet();
        }
        Spliterator<PreservationScenarioModel> preservationModels = preservationScenarioCollection
            .getCollection()
            .find()
            .map(PreservationScenario::toModel)
            .spliterator();

        return StreamSupport.stream(preservationModels, false)
            .flatMap(
                model ->
                    Stream.concat(
                        model.getGriffinByFormat().stream().map(GriffinByFormat::getGriffinIdentifier),
                        Stream.of(model.getDefaultGriffin()).map(DefaultGriffin::getGriffinIdentifier)
                    )
            )
            .filter(griffinIds::contains)
            .collect(Collectors.toSet());
    }

    private String getConstraintsStrings(Set<ConstraintViolation<GriffinModel>> constraints) {
        List<String> result = new ArrayList<>();
        for (ConstraintViolation<GriffinModel> constraintViolation : constraints) {
            result.add("'" + constraintViolation.getPropertyPath() + "' : " + constraintViolation.getMessage());
        }
        return result.toString();
    }

    private List<String> diff(GriffinModel griffinModel, GriffinModel newModel) {
        String after = toComparableString(newModel);
        String before = toComparableString(griffinModel);

        List<String> concernedDiffLines = getConcernedDiffLines(getUnifiedDiff(before, after));
        concernedDiffLines.sort(Comparator.naturalOrder());
        return concernedDiffLines;
    }

    private String toComparableString(GriffinModel griffinModel) {
        try {
            ObjectNode currentJsonNode = (ObjectNode) JsonHandler.toJsonNode(griffinModel);
            // Exclude ignored fields from comparison
            currentJsonNode.remove(VitamDocument.ID);
            currentJsonNode.remove(Griffin.VERSION);
            currentJsonNode.remove(GriffinModel.TAG_CREATION_DATE);
            currentJsonNode.remove(GriffinModel.TAG_LAST_UPDATE);
            return JsonHandler.prettyPrint(currentJsonNode);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    void classifyDataInInsertUpdateOrDeleteLists(
        @NotNull List<GriffinModel> listToImport,
        @NotNull List<GriffinModel> listToInsert,
        @NotNull List<GriffinModel> listToUpdate,
        @NotNull Set<String> listToDelete,
        List<GriffinModel> allGriffinInDatabase
    ) {
        Set<String> dataBaseIds = allGriffinInDatabase.stream().map(GriffinModel::getIdentifier).collect(toSet());
        final HashSet<String> updateIds = new HashSet<>(dataBaseIds);

        final Set<String> importIds = listToImport.stream().map(GriffinModel::getIdentifier).collect(toSet());
        updateIds.retainAll(importIds);

        final HashSet<String> removeIds = new HashSet<>(dataBaseIds);
        removeIds.removeAll(updateIds);

        listToDelete.addAll(removeIds);

        for (GriffinModel griffinModel : listToImport) {
            classifyModelToImportIntoInsertOrUpdateList(griffinModel, dataBaseIds, listToInsert, listToUpdate);
        }
    }

    private void classifyModelToImportIntoInsertOrUpdateList(
        @NotNull GriffinModel griffinModel,
        @NotNull Set<String> dataBaseIds,
        @NotNull List<GriffinModel> listToInsert,
        @NotNull List<GriffinModel> listToUpdate
    ) {
        if (dataBaseIds.contains(griffinModel.getIdentifier())) {
            listToUpdate.add(griffinModel);
            return;
        }
        listToInsert.add(griffinModel);
    }

    private void insertGriffins(@NotNull List<GriffinModel> listToInsert)
        throws InvalidParseOperationException, ReferentialException, SchemaValidationException, DocumentAlreadyExistsException {
        if (listToInsert.isEmpty()) {
            return;
        }
        ArrayNode griffinToInsert = JsonHandler.createArrayNode();

        for (GriffinModel griffinModel : listToInsert) {
            griffinModel.setTenant(ParameterHelper.getTenantParameter());

            formatDateForMongo(griffinModel);
            griffinToInsert.add(toJson(griffinModel));
        }

        mongoDbAccess.insertDocuments(griffinToInsert, GRIFFIN);
    }

    private JsonNode toJson(@NotNull GriffinModel model) throws InvalidParseOperationException {
        ObjectNode modelNode = (ObjectNode) toJsonNode(model);

        JsonNode hashTenant = modelNode.remove(tenant());
        if (hashTenant != null) {
            modelNode.set("_tenant", hashTenant);
        }

        JsonNode jsonNode = modelNode.remove(id());
        if (jsonNode != null) {
            modelNode.set("_id", jsonNode);
        }
        return modelNode;
    }

    private void deleteGriffins(@NotNull Set<String> listIdsToDelete)
        throws ReferentialException, BadRequestException, SchemaValidationException, InvalidCreateOperationException {
        for (String identifier : listIdsToDelete) {
            final Select select = new Select();
            select.setQuery(eq(IDENTIFIER, identifier));
            mongoDbAccess.deleteDocument(select.getFinalSelect(), GRIFFIN);
        }
    }

    private void updateGriffins(@NotNull List<GriffinModel> listToUpdate)
        throws InvalidParseOperationException, DatabaseException, ReferentialException {
        for (GriffinModel griffinModel : listToUpdate) {
            formatDateForMongo(griffinModel);

            ObjectNode griffin = (ObjectNode) toJsonNode(griffinModel);

            griffin.put(UND_TENANT, getVitamSession().getTenantId());
            mongoDbAccess.replaceDocument(
                griffin,
                griffinModel.getIdentifier(),
                IDENTIFIER,
                FunctionalAdminCollections.GRIFFIN
            );
        }
    }

    private void formatDateForMongo(GriffinModel griffinModel) throws ReferentialException {
        try {
            String lastUpdate = LocalDateUtil.getFormattedDateTimeForMongo(now());
            griffinModel.setLastUpdate(lastUpdate);

            String creationDate = griffinModel.getCreationDate();

            if (creationDate == null) {
                creationDate = LocalDateUtil.nowFormatted();
            }
            creationDate = getFormattedDateTimeForMongo(creationDate);
            griffinModel.setCreationDate(creationDate);
        } catch (DateTimeParseException e) {
            throw new ReferentialException(
                griffinModel.getIdentifier() +
                " Invalid " +
                GriffinModel.TAG_CREATION_DATE +
                " : " +
                griffinModel.getCreationDate(),
                e
            );
        }
    }

    public RequestResponse<GriffinModel> findGriffin(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        DbRequestResult documents = mongoDbAccess.findDocuments(queryDsl, GRIFFIN);

        return documents.getRequestResponseOK(queryDsl, Griffin.class, GriffinModel.class);
    }
}
