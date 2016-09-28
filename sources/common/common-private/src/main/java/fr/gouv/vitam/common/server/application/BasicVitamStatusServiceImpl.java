package fr.gouv.vitam.common.server.application;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * BasicVitamStatusServiceImpl : Manage Basic Functionality of Status Service
 * 
 * 
 */
public class BasicVitamStatusServiceImpl implements VitamStatusService {

    @Override
    public boolean getResourcesStatus() {
        return true;
    }

    @Override
    public ObjectNode getAdminStatus() throws InvalidParseOperationException {
        return JsonHandler.createObjectNode();
    }

}
