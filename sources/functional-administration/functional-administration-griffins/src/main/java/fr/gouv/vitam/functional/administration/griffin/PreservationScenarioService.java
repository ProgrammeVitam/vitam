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
package fr.gouv.vitam.functional.administration.griffin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.PreservationScenarioModel;
import fr.gouv.vitam.common.server.HeaderIdHelper;
import fr.gouv.vitam.functional.administration.common.Griffin;
import fr.gouv.vitam.functional.administration.common.PreservationScenario;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.tenant;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.GRIFFIN;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.PRESERVATION_SCENARIO;
import static java.util.stream.Collectors.toSet;

public class PreservationScenarioService {

    private MongoDbAccessReferential mongoDbAccess;

    public PreservationScenarioService(MongoDbAccessReferential mongoDbAccess) {

        this.mongoDbAccess = mongoDbAccess;
    }

    public void importScenarios(@NotNull List<PreservationScenarioModel> listToImport)
        throws InvalidParseOperationException, SchemaValidationException, BadRequestException, ReferentialException,
        InvalidCreateOperationException {

        final List<String> listIdsToDelete = new ArrayList<>();
        final List<PreservationScenarioModel> listToUpdate = new ArrayList<>();
        final List<PreservationScenarioModel> listToInsert = new ArrayList<>();

        classifyDataInInsertUpdateOrDeleteLists(listToImport, listToInsert, listToUpdate, listIdsToDelete);

        insertScenarios(listToInsert);

        updateScenarios(listToUpdate);

        deleteScenarios(listIdsToDelete);
    }

    void classifyDataInInsertUpdateOrDeleteLists(@NotNull List<PreservationScenarioModel> listToImport,
        @NotNull List<PreservationScenarioModel> listToInsert,
        @NotNull List<PreservationScenarioModel> listToUpdate,
        @NotNull List<String> listToDelete)
        throws ReferentialException, BadRequestException, InvalidParseOperationException {

        final ObjectNode finalSelect = new Select().getFinalSelect();
        DbRequestResult result = mongoDbAccess.findDocuments(finalSelect, PRESERVATION_SCENARIO);
        final List<PreservationScenarioModel> allScenariosInDatabase =
            result.getDocuments(PreservationScenario.class, PreservationScenarioModel.class);

        Set<String> dataBaseIds =
            allScenariosInDatabase.stream().map(PreservationScenarioModel::getIdentifier).collect(toSet());
        final HashSet<String> updateIds = new HashSet<>(dataBaseIds);

        final Set<String> importIds =
            listToImport.stream().map(PreservationScenarioModel::getIdentifier).collect(toSet());
        updateIds.retainAll(importIds);

        final HashSet<String> removeIds = new HashSet<>(dataBaseIds);
        removeIds.removeAll(updateIds);

        listToDelete.addAll(removeIds);

        for (PreservationScenarioModel preservationScenarioModel : listToImport) {

            classifyModelToImportIntoInsertOrUpdateList(preservationScenarioModel, dataBaseIds, listToInsert,
                listToUpdate);
        }
    }

    private void classifyModelToImportIntoInsertOrUpdateList(@NotNull
        PreservationScenarioModel preservationScenarioModel,
        @NotNull Set<String> dataBaseIds,
        @NotNull List<PreservationScenarioModel> listToInsert,
        @NotNull List<PreservationScenarioModel> listToUpdate) {

        if (dataBaseIds.contains(preservationScenarioModel.getIdentifier())) {

            listToUpdate.add(preservationScenarioModel);
            return;
        }
        listToInsert.add(preservationScenarioModel);
    }

    private void insertScenarios(@NotNull List<PreservationScenarioModel> listToInsert)
        throws InvalidParseOperationException, ReferentialException, SchemaValidationException {

        if (listToInsert.isEmpty()) {
            return;
        }

        ArrayNode treatmentToInsert = JsonHandler.createArrayNode();

        for (PreservationScenarioModel preservationScenarioModel : listToInsert) {
            preservationScenarioModel.setTenant(HeaderIdHelper.getTenantId());

            (treatmentToInsert).add(toJson(preservationScenarioModel));
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
            modelNode.set("_tenant", hashTenant);
        }

        return modelNode;
    }

    private void deleteScenarios(@NotNull List<String> listIdsToDelete)
        throws ReferentialException, BadRequestException, SchemaValidationException, InvalidParseOperationException,
        InvalidCreateOperationException {

        for (String identifier : listIdsToDelete) {

            final Select select = new Select();
            select.setQuery(eq(Griffin.IDENTIFIER, identifier));
            mongoDbAccess.deleteDocument(select.getFinalSelect(), PRESERVATION_SCENARIO);
        }
    }

    private void updateScenarios(@NotNull List<PreservationScenarioModel> listToUpdate)
        throws ReferentialException, SchemaValidationException, BadRequestException, InvalidCreateOperationException,
        InvalidParseOperationException {

        for (PreservationScenarioModel preservationScenarioModel : listToUpdate) {

            JsonNode queryDslForUpdate = getUpdateDslQuery(preservationScenarioModel);

            mongoDbAccess.updateData(queryDslForUpdate, PRESERVATION_SCENARIO);
        }
    }

    private JsonNode getUpdateDslQuery(@NotNull PreservationScenarioModel preservationScenarioModel)
        throws InvalidCreateOperationException, InvalidParseOperationException {


        final List<SetAction> actions = new ArrayList<>();

        JsonNode jsonModel = JsonHandler.toJsonNode(preservationScenarioModel);

        for (String field : Lists.newArrayList(PreservationScenarioModel.alterableFields)) {

            JsonNode fieldNode = jsonModel.get(field);
            String value = fieldNode.textValue();
            SetAction action = new SetAction(field, value);
            actions.add(action);
        }

        SetAction[] setActions = actions.toArray(new SetAction[0]);
        final Update update = new Update();
        update.setQuery(eq(Griffin.IDENTIFIER, preservationScenarioModel.getIdentifier()));
        update.addActions(setActions);
        return update.getFinalUpdate();
    }
}
