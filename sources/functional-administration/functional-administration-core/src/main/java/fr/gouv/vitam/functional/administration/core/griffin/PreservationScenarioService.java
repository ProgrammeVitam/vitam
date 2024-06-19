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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
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
import fr.gouv.vitam.common.model.administration.preservation.GriffinByFormat;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.Griffin;
import fr.gouv.vitam.functional.administration.common.PreservationScenario;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.format.model.FileFormatModel;
import fr.gouv.vitam.functional.administration.core.format.model.FunctionalOperationModel;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.apache.commons.collections4.SetUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.tenant;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.getConcernedDiffLines;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.getUnifiedDiff;
import static fr.gouv.vitam.common.guid.GUIDReader.getGUID;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.functional.administration.common.PreservationScenario.IDENTIFIER;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.FORMATS;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.GRIFFIN;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.PRESERVATION_SCENARIO;
import static fr.gouv.vitam.functional.administration.core.griffin.LogbookGriffinHelper.createLogbook;
import static fr.gouv.vitam.functional.administration.core.griffin.LogbookGriffinHelper.createLogbookEventKo;
import static fr.gouv.vitam.functional.administration.core.griffin.LogbookGriffinHelper.createLogbookEventSuccess;
import static fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory.getInstance;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class PreservationScenarioService {

    private static final String SCENARIO_BACKUP_EVENT = "STP_BACKUP_SCENARIO";
    private static final String SCENARIO_IMPORT_EVENT = "STP_IMPORT_PRESERVATION_SCENARIO";
    private static final String SCENARIO_REPORT = "SCENARIO_REPORT";
    private static final String UND_TENANT = "_tenant";

    private final MongoDbAccessReferential mongoDbAccess;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final FunctionalBackupService functionalBackupService;

    PreservationScenarioService(
        MongoDbAccessReferential mongoDbAccess,
        FunctionalBackupService functionalBackupService,
        LogbookOperationsClientFactory logbookOperationsClientFactory
    ) {
        this.mongoDbAccess = mongoDbAccess;
        this.functionalBackupService = functionalBackupService;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    public PreservationScenarioService(
        MongoDbAccessAdminImpl mongoAccess,
        FunctionalBackupService functionalBackupService
    ) {
        this(mongoAccess, functionalBackupService, getInstance());
    }

    public RequestResponse<PreservationScenarioModel> importScenarios(
        @NotNull List<PreservationScenarioModel> listToImport
    ) throws VitamException {
        String operationId = getVitamSession().getRequestId();
        GUID guid = getGUID(operationId);

        createLogbook(logbookOperationsClientFactory, guid, SCENARIO_IMPORT_EVENT);

        try {
            validate(listToImport);

            final ObjectNode finalSelect = new Select().getFinalSelect();
            DbRequestResult result = mongoDbAccess.findDocuments(finalSelect, PRESERVATION_SCENARIO);
            final List<PreservationScenarioModel> allScenariosInDatabase = result.getDocuments(
                PreservationScenario.class,
                PreservationScenarioModel.class
            );

            Set<String> newIdentifiers = listToImport
                .stream()
                .map(PreservationScenarioModel::getIdentifier)
                .collect(toSet());
            Set<String> oldIdentifiers = allScenariosInDatabase
                .stream()
                .map(PreservationScenarioModel::getIdentifier)
                .collect(toSet());

            Set<String> updatedIdentifiers = SetUtils.intersection(newIdentifiers, oldIdentifiers);

            if (!updatedIdentifiers.isEmpty()) {
                updateScenarios(listToImport, updatedIdentifiers);
            }

            Set<String> identifierToDelete = SetUtils.difference(oldIdentifiers, newIdentifiers);
            if (!identifierToDelete.isEmpty()) {
                deleteScenarios(identifierToDelete);
            }

            Set<String> identifierToAdd = SetUtils.difference(newIdentifiers, oldIdentifiers);
            if (!identifierToAdd.isEmpty()) {
                insertScenarios(listToImport, identifierToAdd);
            }

            functionalBackupService.saveCollectionAndSequence(
                guid,
                SCENARIO_BACKUP_EVENT,
                PRESERVATION_SCENARIO,
                operationId
            );

            PreservationScenarioReport preservationScenarioReport = generateReport(
                allScenariosInDatabase,
                listToImport,
                updatedIdentifiers,
                identifierToDelete,
                identifierToAdd
            );
            saveReport(guid, preservationScenarioReport);
        } catch (VitamException e) {
            createLogbookEventKo(logbookOperationsClientFactory, guid, SCENARIO_IMPORT_EVENT, e.getMessage());
            throw e;
        }
        createLogbookEventSuccess(logbookOperationsClientFactory, guid, SCENARIO_IMPORT_EVENT);

        return new RequestResponseOK<PreservationScenarioModel>()
            .addAllResults(listToImport)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    private void checkGriffinByFormatIdentifier(PreservationScenarioModel scenario, List<String> griffinsIdentifiers)
        throws ReferentialException {
        for (GriffinByFormat griffinByFormat : scenario.getGriffinByFormat()) {
            if (!griffinsIdentifiers.contains(griffinByFormat.getGriffinIdentifier())) {
                throw new ReferentialException(
                    "Griffin '" + griffinByFormat.getGriffinIdentifier() + "' is not in database"
                );
            }
        }

        boolean defaultGriffinDoesNotExists =
            scenario.getDefaultGriffin() != null &&
            !griffinsIdentifiers.contains(scenario.getDefaultGriffin().getGriffinIdentifier());

        if (defaultGriffinDoesNotExists) {
            throw new ReferentialException(
                "Griffin '" + scenario.getDefaultGriffin().getGriffinIdentifier() + "' is not in database"
            );
        }
    }

    private void saveReport(GUID guid, PreservationScenarioReport griffinReport) throws StorageException {
        try (InputStream reportInputStream = JsonHandler.writeToInpustream(griffinReport)) {
            final String fileName = guid.getId() + ".json";

            functionalBackupService.saveFile(reportInputStream, guid, SCENARIO_REPORT, DataCategory.REPORT, fileName);
        } catch (IOException | VitamException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private void validate(List<PreservationScenarioModel> listToImport) throws VitamException {
        entryValidation(listToImport);

        functionalGriffinIdentifierValidation(listToImport);

        formatValidation(listToImport);
    }

    private void entryValidation(List<PreservationScenarioModel> listToImport) throws ReferentialException {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        List<String> identifiers = new ArrayList<>();
        for (PreservationScenarioModel model : listToImport) {
            if (identifiers.contains(model.getIdentifier())) {
                throw new ReferentialException("Duplicate scenario : '" + model.getIdentifier() + "'");
            }

            if ((model.getGriffinByFormat().isEmpty()) && model.getDefaultGriffin() == null) {
                throw new ReferentialException(
                    "Invalid scenario for : '" +
                    model.getIdentifier() +
                    "' : at least one griffin must be defined (griffin by format or default griffin)"
                );
            }

            Set<ConstraintViolation<PreservationScenarioModel>> constraints = validator.validate(model);
            if (!constraints.isEmpty()) {
                throw new ReferentialException(
                    "Invalid scenario  for  : '" + model.getIdentifier() + "' : " + getConstraintsStrings(constraints)
                );
            }

            identifiers.add(model.getIdentifier());
        }
    }

    private void functionalGriffinIdentifierValidation(List<PreservationScenarioModel> listToImport)
        throws BadRequestException, ReferentialException, InvalidParseOperationException {
        final ObjectNode finalSelect = new Select().getFinalSelect();
        DbRequestResult result = mongoDbAccess.findDocuments(finalSelect, GRIFFIN);

        final List<GriffinModel> allGriffinInDatabase = result.getDocuments(Griffin.class, GriffinModel.class);
        List<String> griffinsIdentifier = allGriffinInDatabase
            .stream()
            .map(GriffinModel::getIdentifier)
            .collect(toList());

        for (PreservationScenarioModel scenario : listToImport) {
            checkGriffinByFormatIdentifier(scenario, griffinsIdentifier);
        }
    }

    private void formatValidation(List<PreservationScenarioModel> listToImport) throws VitamException {
        ObjectNode finalSelect = new Select().getFinalSelect();
        DbRequestResult result = mongoDbAccess.findDocuments(finalSelect, FORMATS);

        Set<String> referencePuids = result
            .getDocuments(FileFormat.class, FileFormatModel.class)
            .stream()
            .map(FileFormatModel::getPuid)
            .collect(toSet());

        Set<String> puidsToCheck = listToImport
            .stream()
            .flatMap(s -> s.getGriffinByFormat().stream().flatMap(g -> g.getFormatList().stream()))
            .collect(toSet());

        puidsToCheck.removeAll(referencePuids);

        if (!puidsToCheck.isEmpty()) {
            throw new ReferentialException(
                String.format("List: %s does not exist in the database.", puidsToCheck.toString())
            );
        }
    }

    private String getConstraintsStrings(Set<ConstraintViolation<PreservationScenarioModel>> constraints) {
        List<String> result = new ArrayList<>();
        for (ConstraintViolation<PreservationScenarioModel> constraintViolation : constraints) {
            result.add("'" + constraintViolation.getPropertyPath() + "' : " + constraintViolation.getMessage());
        }
        return result.toString();
    }

    private FunctionalOperationModel retrieveOperationModel() {
        try {
            JsonNode result = logbookOperationsClientFactory
                .getClient()
                .selectOperationById(getVitamSession().getRequestId());

            return JsonHandler.getFromJsonNode(result.get(TAG_RESULTS).get(0), FunctionalOperationModel.class);
        } catch (LogbookClientException | InvalidParseOperationException e) {
            throw new VitamRuntimeException("Could not load operation data", e);
        }
    }

    private PreservationScenarioReport generateReport(
        List<PreservationScenarioModel> currentScenariosModels,
        List<PreservationScenarioModel> newScenarioModels,
        Set<String> updatedIdentifiers,
        Set<String> removedIdentifiers,
        Set<String> addedIdentifiers
    ) {
        PreservationScenarioReport report = new PreservationScenarioReport();

        FunctionalOperationModel operationModel = retrieveOperationModel();

        report.setOperation(operationModel);

        if (!currentScenariosModels.isEmpty()) {
            report.setPreviousScenariosCreationDate(currentScenariosModels.get(0).getCreationDate());
        }

        if (!newScenarioModels.isEmpty()) {
            report.setNewScenariosCreationDate(newScenarioModels.get(0).getCreationDate());
        }

        Map<String, PreservationScenarioModel> currentScenariosModelsByIdentifiers = newScenarioModels
            .stream()
            .collect(Collectors.toMap(PreservationScenarioModel::getIdentifier, model -> model));

        Map<String, PreservationScenarioModel> newScenariosModelByIdentifiers = newScenarioModels
            .stream()
            .collect(Collectors.toMap(PreservationScenarioModel::getIdentifier, model -> model));

        report.setRemovedIdentifiers(removedIdentifiers);

        report.setAddedIdentifiers(addedIdentifiers);

        reportUpdatedIdentifiers(
            updatedIdentifiers,
            report,
            currentScenariosModelsByIdentifiers,
            newScenariosModelByIdentifiers
        );

        reportVersioning(report);

        if (!removedIdentifiers.isEmpty()) {
            report.addWarning(removedIdentifiers.size() + " identifiers removed.");
        }

        if (report.getWarnings().isEmpty()) {
            return report.setStatusCode(StatusCode.OK);
        }

        return report.setStatusCode(StatusCode.WARNING);
    }

    private void reportVersioning(PreservationScenarioReport report) {
        if (report.getPreviousScenariosCreationDate() != null && report.getNewScenariosCreationDate() != null) {
            String previousDate = LocalDateUtil.getFormattedDateTimeForMongo(report.getPreviousScenariosCreationDate());
            String newDate = LocalDateUtil.getFormattedDateTimeForMongo(report.getNewScenariosCreationDate());

            if (previousDate.equals(newDate)) {
                report.addWarning("Same referential date: " + report.getNewScenariosCreationDate());
            }

            if (previousDate.compareTo(newDate) > 0) {
                report.addWarning(
                    "New imported referential date " +
                    report.getNewScenariosCreationDate() +
                    " is older than previous report date " +
                    report.getNewScenariosCreationDate()
                );
            }
        }
    }

    private void reportUpdatedIdentifiers(
        Set<String> updatedIdentifiers,
        PreservationScenarioReport report,
        Map<String, PreservationScenarioModel> currentScenariosModelsByIdentifiers,
        Map<String, PreservationScenarioModel> newScenariosModelByIdentifiers
    ) {
        for (String identifier : updatedIdentifiers) {
            PreservationScenarioModel currentScenarioModel = currentScenariosModelsByIdentifiers.get(identifier);
            PreservationScenarioModel newScenarioModel = newScenariosModelByIdentifiers.get(identifier);

            List<String> diff = diff(currentScenarioModel, newScenarioModel);
            report.addUpdatedIdentifiers(identifier, diff);
        }
    }

    private String toComparableString(PreservationScenarioModel scenarioModel) {
        try {
            ObjectNode currentJsonNode = (ObjectNode) toJsonNode(scenarioModel);
            // Exclude ignored fields from comparison
            currentJsonNode.remove(VitamDocument.ID);
            currentJsonNode.remove(PreservationScenario.VERSION);
            currentJsonNode.remove(PreservationScenarioModel.TAG_CREATION_DATE);
            currentJsonNode.remove(PreservationScenarioModel.TAG_LAST_UPDATE);
            return JsonHandler.prettyPrint(currentJsonNode);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private List<String> diff(
        PreservationScenarioModel currentScenarioModel,
        PreservationScenarioModel newScenarioModel
    ) {
        String after = toComparableString(newScenarioModel);
        String before = toComparableString(currentScenarioModel);

        List<String> concernedDiffLines = getConcernedDiffLines(getUnifiedDiff(before, after));
        concernedDiffLines.sort(Comparator.naturalOrder());
        return concernedDiffLines;
    }

    private void insertScenarios(@NotNull List<PreservationScenarioModel> listToImport, Set<String> newIdentifiers)
        throws InvalidParseOperationException, ReferentialException, SchemaValidationException, DocumentAlreadyExistsException {
        if (listToImport.isEmpty()) {
            return;
        }

        ArrayNode treatmentToInsert = JsonHandler.createArrayNode();

        for (PreservationScenarioModel preservationScenarioModel : listToImport) {
            if (newIdentifiers.contains(preservationScenarioModel.getIdentifier())) {
                preservationScenarioModel.setTenant(ParameterHelper.getTenantParameter());

                formatDateForMongo(preservationScenarioModel);

                treatmentToInsert.add(toJson(preservationScenarioModel));
            }
        }

        mongoDbAccess.insertDocuments(treatmentToInsert, PRESERVATION_SCENARIO);
    }

    private JsonNode toJson(@NotNull PreservationScenarioModel model) throws InvalidParseOperationException {
        ObjectNode modelNode = (ObjectNode) toJsonNode(model);

        JsonNode jsonNode = modelNode.remove(id());
        if (jsonNode != null) {
            modelNode.set("_id", jsonNode);
        }
        JsonNode hashTenant = modelNode.remove(tenant());
        if (hashTenant != null) {
            modelNode.set(UND_TENANT, hashTenant);
        }
        return modelNode;
    }

    private void deleteScenarios(@NotNull Set<String> listIdsToDelete)
        throws ReferentialException, BadRequestException, SchemaValidationException {
        try {
            for (String identifier : listIdsToDelete) {
                final Select select = new Select();
                select.setQuery(eq(PreservationScenario.IDENTIFIER, identifier));
                mongoDbAccess.deleteDocument(select.getFinalSelect(), PRESERVATION_SCENARIO);
            }
        } catch (InvalidCreateOperationException e) {
            throw new IllegalStateException("cannot Create Dsl");
        }
    }

    private void updateScenarios(@NotNull List<PreservationScenarioModel> listToImport, Set<String> identifierToUpdate)
        throws InvalidParseOperationException, DatabaseException, ReferentialException {
        for (PreservationScenarioModel preservationScenarioModel : listToImport) {
            if (identifierToUpdate.contains(preservationScenarioModel.getIdentifier())) {
                preservationScenarioModel.setLastUpdate(LocalDateUtil.nowFormatted());

                formatDateForMongo(preservationScenarioModel);

                preservationScenarioModel.setTenant(getVitamSession().getTenantId());
                ObjectNode scenario = (ObjectNode) toJsonNode(preservationScenarioModel);

                scenario.put(UND_TENANT, getVitamSession().getTenantId());
                mongoDbAccess.replaceDocument(
                    scenario,
                    preservationScenarioModel.getIdentifier(),
                    IDENTIFIER,
                    FunctionalAdminCollections.PRESERVATION_SCENARIO
                );
            }
        }
    }

    private void formatDateForMongo(PreservationScenarioModel preservationScenarioModel) throws ReferentialException {
        try {
            String lastUpdate = LocalDateUtil.nowFormatted();
            preservationScenarioModel.setLastUpdate(lastUpdate);
        } catch (DateTimeParseException e) {
            throw new ReferentialException(
                "Error in scenario '" +
                preservationScenarioModel.getIdentifier() +
                "' : field '" +
                PreservationScenarioModel.TAG_LAST_UPDATE +
                "' format is invalid",
                e
            );
        }

        if (preservationScenarioModel.getCreationDate() != null) {
            try {
                String creationDate = LocalDateUtil.getFormattedDateTimeForMongo(
                    preservationScenarioModel.getCreationDate()
                );
                preservationScenarioModel.setCreationDate(creationDate);
            } catch (DateTimeParseException e) {
                throw new ReferentialException(
                    "Error in scenario '" +
                    preservationScenarioModel.getIdentifier() +
                    "' : field '" +
                    PreservationScenarioModel.TAG_CREATION_DATE +
                    "' format is invalid",
                    e
                );
            }
        }
    }

    public RequestResponse<PreservationScenarioModel> findPreservationScenario(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        DbRequestResult documents = mongoDbAccess.findDocuments(queryDsl, PRESERVATION_SCENARIO);

        return documents.getRequestResponseOK(queryDsl, PreservationScenario.class, PreservationScenarioModel.class);
    }
}
