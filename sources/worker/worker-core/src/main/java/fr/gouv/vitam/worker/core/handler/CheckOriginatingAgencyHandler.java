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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handler class used to check the originating agency of SIP. </br>
 */
public class CheckOriginatingAgencyHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckOriginatingAgencyHandler.class);

    // IN RANK
    private static final int AGENT_RANK = 0;

    private static final String HANDLER_ID = "CHECK_AGENT";
    public static final String EMPTY_REQUIRED_FIELD = "EMPTY_REQUIRED_FIELD";
    private final AdminManagementClientFactory adminManagementClientFactory;

    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public CheckOriginatingAgencyHandler() {
        this(AdminManagementClientFactory.getInstance());
    }

    /**
     * Useful for inject mock in test class
     *
     * @param adminManagementClientFactory instance of adminManagementClientFactory or mock
     */
    @VisibleForTesting
    public CheckOriginatingAgencyHandler(AdminManagementClientFactory adminManagementClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    /**
     * Return Handler Id
     *
     * @return HANDLER_ID CHECK_AGENT
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) {
        checkMandatoryParameters(param);
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        Set<String> missingserviceAgents;

        try {
            checkMandatoryIOParameter(handlerIO);

            Map<String, Object> mandatoryValueMap = (Map<String, Object>) handlerIO.getInput(AGENT_RANK);

            Set<String> serviceAgentList = new HashSet<>();
            if (
                mandatoryValueMap.get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER) != null &&
                !Strings.isNullOrEmpty((String) mandatoryValueMap.get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER))
            ) {
                serviceAgentList.add((String) mandatoryValueMap.get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER));
            } else {
                ObjectNode evDetData = JsonHandler.createObjectNode();
                evDetData.put(SedaConstants.EV_DET_TECH_DATA, "Error originating agency not found in the manifest");
                itemStatus.setEvDetailData(JsonHandler.writeAsString(evDetData));
                itemStatus.increment(StatusCode.KO);
                itemStatus.setGlobalOutcomeDetailSubcode(EMPTY_REQUIRED_FIELD);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }

            if (
                mandatoryValueMap.get(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER) != null &&
                !Strings.isNullOrEmpty((String) mandatoryValueMap.get(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER))
            ) {
                serviceAgentList.add((String) mandatoryValueMap.get(SedaConstants.TAG_SUBMISSIONAGENCYIDENTIFIER));
            }

            missingserviceAgents = getMissingServiceAgents(serviceAgentList);
            if (!missingserviceAgents.isEmpty()) {
                itemStatus.increment(StatusCode.KO, missingserviceAgents.size());
                itemStatus.setGlobalOutcomeDetailSubcode("UNKNOWN");
                ObjectNode evDetData = JsonHandler.createObjectNode();
                ArrayNode serviceAgents = JsonHandler.createArrayNode();
                missingserviceAgents.forEach(agent -> serviceAgents.add(agent));
                evDetData.put(
                    SedaConstants.EV_DET_TECH_DATA,
                    "error originating agency validation" + JsonHandler.writeAsString(missingserviceAgents)
                );
                itemStatus.setEvDetailData(JsonHandler.writeAsString(evDetData));
            } else {
                itemStatus.increment(StatusCode.OK);
            }
        } catch (final ProcessingException | InvalidParseOperationException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private Set<String> getMissingServiceAgents(Set<String> serviceAgentList) throws ProcessingException {
        try (final AdminManagementClient client = adminManagementClientFactory.getClient()) {
            Set<String> missingServiceAgent = new HashSet<>();
            String[] serviceAgentArray = serviceAgentList.toArray(new String[serviceAgentList.size()]);
            Select select = new Select();
            select.setQuery(QueryHelper.in(AgenciesModel.TAG_IDENTIFIER, serviceAgentArray));
            JsonNode results = client.getAgencies(select.getFinalSelect());

            List<String> res = results.findValuesAsText(AgenciesModel.TAG_IDENTIFIER);
            for (String serviceAgent : serviceAgentList) {
                if (!res.contains(serviceAgent)) {
                    missingServiceAgent.add(serviceAgent);
                }
            }
            return missingServiceAgent;
        } catch (ReferentialException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Originating agency not found: ", e);
            throw new ProcessingException("Error while accessing referential AGENCIES");
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub

    }
}
