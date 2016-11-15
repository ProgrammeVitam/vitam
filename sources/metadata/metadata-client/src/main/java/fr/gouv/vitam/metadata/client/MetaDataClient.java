/**
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
 **/

package fr.gouv.vitam.metadata.client;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.client2.BasicClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;

/**
 * Metadata client interface
 */
public interface MetaDataClient extends BasicClient {

    /**
     * @param insertQuery as String <br>
     *        null is not allowed
     * @return the result as JsonNode
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataDocumentSizeException
     * @throws MetaDataClientServerException
     */
    JsonNode insertUnit(String insertQuery) throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataNotFoundException, MetaDataAlreadyExistException, MetaDataDocumentSizeException,
        MetaDataClientServerException;

    /**
     * Search units by select query (DSL)
     *
     * @param selectQuery : select query {@link fr.gouv.vitam.common.database.builder.request.multiple.Select} as String
     *        <br>
     *        Null is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws MetaDataClientServerException
     */
    JsonNode selectUnits(JsonNode selectQuery) throws MetaDataExecutionException, MetaDataDocumentSizeException,
        InvalidParseOperationException, MetaDataClientServerException;

    /**
     * Search units by query (DSL) and path unit id
     *
     * @param selectQuery : select query {@link fr.gouv.vitam.common.database.builder.request.single.Select} as String
     *        <br>
     *        Null is not allowed
     * @param unitId : unit id <br>
     *        null and blank is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws MetaDataClientServerException
     */
    JsonNode selectUnitbyId(String selectQuery, String unitId) throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException;

    /**
     * Search Object Group by query (DSL) and path objectGroup id
     *
     * @param selectQuery : select query {@link fr.gouv.vitam.common.database.builder.request.single.Select} as String
     *        <br>
     *        Null is not allowed
     * @param objectGroupId : objectGroup id <br>
     *        null and blank is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException thrown when the Query is badly formatted or objectGroupId is empty
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws MetadataInvalidSelectException thrown when objectGroupId or selectQuery id is null or blank
     * @throws MetaDataClientServerException
     */
    JsonNode selectObjectGrouptbyId(String selectQuery, String objectGroupId) throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetadataInvalidSelectException, MetaDataClientServerException;

    /**
     * Update units by query (DSL) and path unit id
     *
     * @param updateQuery  update query {@link fr.gouv.vitam.common.database.builder.request.single.Select} as String
     *        <br>
     *        Null is not allowed
     * @param unitId  unit id <br>
     *        null and blank is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws MetaDataClientServerException
     */
    JsonNode updateUnitbyId(String updateQuery, String unitId) throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException;

    /**
     * @param insertQuery as String
     * @return response as JsonNode contains the request result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataDocumentSizeException
     * @throws MetaDataClientServerException
     */
    JsonNode insertObjectGroup(String insertQuery) throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataNotFoundException, MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException;
}
