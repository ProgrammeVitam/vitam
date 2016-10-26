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
package fr.gouv.vitam.functional.administration.client;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * AdminManagementClient interface
 */
public interface AdminManagementClient extends MockOrRestClient {

    /**
     * @param stream as InputStream;
     * @return status
     * @throws ReferentialException when check exception occurs
     */
    Status checkFormat(InputStream stream) throws ReferentialException;


    /**
     * @param stream as InputStream
     * @throws ReferentialException when import exception occurs
     * @throws DatabaseConflictException conflict exception occurs
     */
    void importFormat(InputStream stream) throws ReferentialException, DatabaseConflictException;


    /**
     * @throws ReferentialException when delete exception occurs
     */
    // FIXME delete the collection without any check on legal to do so (does any object using this referential ?) ?
    // Il me semble que cette fonction devrait être interne et appelée par la méthode importFormat en interne de Vitam
    // et surtout pas en externe !!!
    // Fonctionnalité demandé par les POs pour la démo
    void deleteFormat() throws ReferentialException;

    /**
     * @param id as String
     * @return JsonNode
     * @throws ReferentialException check exception occurs
     * @throws InvalidParseOperationException when json exception occurs
     */
    JsonNode getFormatByID(String id) throws ReferentialException, InvalidParseOperationException;


    /**
     * @param query as JsonNode
     * @return JsonNode
     * @throws ReferentialException when referential format exception occurs
     * @throws InvalidParseOperationException when json exception occurs
     * @throws IOException when io data exception occurs
     */
    JsonNode getFormats(JsonNode query)
        throws ReferentialException, InvalidParseOperationException,
        IOException;

    /**
     *
     * @param stream
     * @return status
     * @throws FileRulesException
     * @throws AdminManagementClientServerException
     */

    Status checkRulesFile(InputStream stream) throws FileRulesException, AdminManagementClientServerException;

    /**
     *
     * @param stream
     * @throws FileRulesException when file rules exception occurs
     * @throws DatabaseConflictException when Database conflict exception occurs
     * @throws AdminManagementClientServerException
     */
    void importRulesFile(InputStream stream)
        throws FileRulesException, DatabaseConflictException, AdminManagementClientServerException;

    /**
     *
     * @throws FileRulesException
     * @throws AdminManagementClientServerException
     */

    void deleteRulesFile() throws FileRulesException, AdminManagementClientServerException;

    /**
     *
     * @param id ide de rule
     * @return Rule in JsonNode format
     * @throws FileRulesException when file rules exception occurs
     * @throws InvalidParseOperationException when a parse problem occurs
     * @throws AdminManagementClientServerException
     */
    JsonNode getRuleByID(String id)
        throws FileRulesException, InvalidParseOperationException, AdminManagementClientServerException;

    /**
     *
     * @param query
     * @return Rules in JsonNode format
     * @throws FileRulesException when file rules exception occurs
     * @throws InvalidParseOperationException when a parse problem occurs
     * @throws IOException when IO Exception occurs
     * @throws AdminManagementClientServerException
     */
    JsonNode getRule(JsonNode query)
        throws FileRulesException, InvalidParseOperationException,
        IOException, AdminManagementClientServerException;

    /**
     * @param register AccessionRegisterDetail 
     * @throws AccessionRegisterException when AccessionRegisterDetailexception occurs
     * @throws DatabaseConflictException when Database conflict exception occurs
     */
    void createorUpdateAccessionRegister(AccessionRegisterDetail register)
        throws AccessionRegisterException, DatabaseConflictException, AdminManagementClientServerException;

}
