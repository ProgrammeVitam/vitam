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
package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.PreservationScenarioModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.FileOutputStream;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class PreservationPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationActionPlugin.class);

    private static final String PRESERVATION_PREPARATION = "PRESERVATION_PREPARATION";

    private AdminManagementClientFactory adminManagementClientFactory;

    private MetaDataClientFactory metaDataClientFactory;


    public PreservationPreparationPlugin() {
        this(AdminManagementClientFactory.getInstance(), MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting PreservationPreparationPlugin(
        AdminManagementClientFactory adminManagementClientFactory,
        MetaDataClientFactory metaDataClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient();
            AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {


            JsonLineModel model = new JsonLineModel();
            model.setId("TEST_ID");
            model.setDistribGroup(1);
            JsonNode params = getFromString(
                "{\"formatId\": \"fmt/43\", \"griffinId\": \"griffinId\", \"actions\":[{\"type\":\"ANALYSE\", \"values\":null}], \"unitId\":\"Bobi\", \"objectId\": \"bobiObject\", \"debug\":true, \"timeout\":45 }");
            model.setParams(params);
            File objectGroupsToPreserve = handler.getNewLocalFile("object_groups_to_preserve.jsonl");

            try (JsonLineWriter writer = new JsonLineWriter(new FileOutputStream(objectGroupsToPreserve))) {
                writer.addEntry(model);
            }

            RequestResponse<PreservationScenarioModel> request =
                adminClient.findPreservationByID("PSC-000024");

            PreservationScenarioModel scenarioModel =
                ((RequestResponseOK<PreservationScenarioModel>) request).getFirstResult();

            ObjectNode eventDetails = createObjectNode();
            ObjectNode objectNode = JsonHandler.createObjectNode();

             JsonNode select = metaDataClient.selectUnits(objectNode);

            handler.transferFileToWorkspace("distributionFile.jsonl", objectGroupsToPreserve, false, false);


            return buildItemStatus(PRESERVATION_PREPARATION, StatusCode.OK, eventDetails);
        } catch (Exception e) {
            LOGGER.error(String.format("Preservation action failed with status [%s]", KO), e);
            ObjectNode error = createObjectNode().put("error", e.getMessage());
            return buildItemStatus(PRESERVATION_PREPARATION, KO, error);
        }

    }


}
