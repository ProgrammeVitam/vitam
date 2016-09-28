package fr.gouv.vitam.common.server.application;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Interface VitamStatusService
 * 
 * Interface of Basic Functionality Management for Status Services
 * 
 */

interface VitamStatusService {

    /**
     * getResourcesStatus
     * 
     * return the overall status of this component with the constraint delay of less than 10ms.
     * 
     * @return boolean
     */
    boolean getResourcesStatus();

    /**
     * getAdminStatus
     * 
     * return the overall status of this component with the constraint delay of less than 10ms and shall return by
     * default empty JsonNode.
     * 
     * @return ServerIdentity
     * @throws InvalidParseOperationException
     */
    ObjectNode getAdminStatus() throws InvalidParseOperationException;
}
