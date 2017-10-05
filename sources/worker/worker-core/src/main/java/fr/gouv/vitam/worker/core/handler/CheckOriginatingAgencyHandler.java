package fr.gouv.vitam.worker.core.handler;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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

/**
 * Handler class used to check the originating agency of SIP. </br>
 *
 */
public class CheckOriginatingAgencyHandler extends ActionHandler {
    

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckOriginatingAgencyHandler.class);

    // IN RANK
    private static final int AGENT_RANK = 0;

    private static final String HANDLER_ID = "CHECK_AGENT";
    
    private HandlerIO handlerIO;
    final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public CheckOriginatingAgencyHandler() {
        
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }
    
    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) {
        
        checkMandatoryParameters(param);
        handlerIO = handler;
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        boolean serviceAgentValidity = false;

        try {
            checkMandatoryIOParameter(handler);
            Set<String> serviceAgentList = (Set<String>) handlerIO.getInput(AGENT_RANK);

            if (!serviceAgentList.isEmpty()) {
                serviceAgentValidity = checkServiceAgent(serviceAgentList);
            } else {
                serviceAgentValidity = false;
                LOGGER.info("There is no service agent in the SIP");
            }

            if (!serviceAgentValidity) {
                itemStatus.increment(StatusCode.KO);
                itemStatus.setData("error originating agency validation", serviceAgentList);
            } else {
                itemStatus.increment(StatusCode.OK);
            }
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }
    
    private boolean checkServiceAgent(Set<String> serviceAgentList) {
        try (final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            
          String[] serviceAgentArray = serviceAgentList.toArray(new String[serviceAgentList.size()]);
          Select select = new Select();
          select.setQuery(QueryHelper.in(AgenciesModel.TAG_IDENTIFIER, serviceAgentArray));
              
          JsonNode results = client.getAgencies(select.getFinalSelect());
          if (results.get("$results").size() != serviceAgentList.size()) {
              return false;
          }
          List<String> res = results.findValuesAsText("Identifier");
          for (String serviceAgent : serviceAgentList) {
              if (!res.contains(serviceAgent)) {
                  return false;
              }
          }
          return true;            
            
        } catch (ReferentialException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Originating agency not found: ", e);
        }
        return false;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub
        
    }

}
