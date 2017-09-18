package fr.gouv.vitam.access.internal.api;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.model.VitamAutoCloseable;

public interface DipService {

    /**
     * Transform the representation of an object (unit or objectGroup) to an xml format (DIP)
     * 
     * @param object the given representation of the object as Json (can be unit or objectGroup)
     * @param id The given id of the object to transform can be archiveUnit or objectGroup
     * @return xml representation of the object (Unit or Object Group)
     */
    Response jsonToXml(JsonNode object, String id);

}
