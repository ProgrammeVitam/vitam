/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.api;


import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * Access Resource REST API
 */
public interface AccessResource {

    /**
     * gets archive units with Json query
     *
     * @param dslQuery, null not allowed
     * @param headerParam X-Http-Method-Override header
     * @return a archive unit result list
     */
    // TODO fixer les commentaires: pas de ','
    public Response getUnits(String dslQuery, String headerParam);

    /**
     * gets archive units by Id with Json query
     * 
     * @param dslQuery DSK, null not allowed
     * @param headerParam X-Http-Method-Override header
     * @param unit_id units identifier
     * @return a archive unit result list
     */
    // TODO fixer les commentaires (DSK ?)
    // TODO respecter la casse Java lowerCamelCase (pas de '_')
    public Response getUnitById(String dslQuery, String headerParam, String unit_id);

    /**
     * update archive units by Id with Json query
     *
     * @param dslQuery DSK, null not allowed
     * @param unit_id units identifier
     * @return a archive unit result list
     */
    public Response updateUnitById(String dslQuery, String unit_id);

    /**
     * Retrieve an ObjectGroup by its id
     * 
     * @param idObjectGroup the ObjectGroup id
     * @param query the json query
     * @return an http response containing the objectGroup as json or a json serialized error
     */
    Response getObjectGroup(String idObjectGroup, String query);

    /**
     * POST version of getObjectGroup. Implicitly call getObjectGroup(String idObject, String query) if the "GET" value
     * is found in method override http header. Return an error otherwise.
     * 
     * @param xHttpOverride value of the associated header
     * @param idObjectGroup the ObjectGroup id
     * @param query the json query
     * @return an http response containing the objectGroup as json or a json serialized error
     */
    Response getObjectGroup(String xHttpOverride, String idObjectGroup, String query);

    /**
     * Retrieve an Object associated to the given ObjectGroup id based on given (via headers) Qualifier and Version
     * 
     * @param headers http request headers
     * @param idObjectGroup the ObjectGroup id
     * @param query the DSL query as json
     * @return an http response containing an InputStream of the Object if it is found or a json serialized error
     */
    Response getObjectStream(HttpHeaders headers, String idObjectGroup, String query);

    /**
     * POST version of getObjectStream. Implicitly call getObjectStream(HttpHeaders headers, String idObjectGroup,
     * String query) if the "GET" value is found in method override http header. Return an error otherwise.
     * 
     * @param headers http request headers
     * @param idObjectGroup the ObjectGroup id
     * @param query the DSL query as json
     * @return an http response containing an InputStream of the Object if it is found or a json serialized error
     */
    Response getObjectStreamPost(HttpHeaders headers, String idObjectGroup, String query);
}
