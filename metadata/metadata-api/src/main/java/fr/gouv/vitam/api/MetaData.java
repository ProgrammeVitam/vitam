/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.api;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * MetaData interface for database operations
 */
public interface MetaData {

    // Unit
    /**
     * insert Unit
     *
     * @param insertRequest as String { $roots: roots, $query : query, $filter : multi, $data : data}
     *
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws MetaDataNotFoundException Throw if parent of this unit is not found
     * @throws MetaDataAlreadyExistException Throw if Unit id already exists
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     */
    public void insertUnit(JsonNode insertRequest)
        throws InvalidParseOperationException, IllegalArgumentException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataExecutionException, MetaDataDocumentSizeException;


    /**
     * Search UNITs by Select {@link Select}Query
     *
     * @param selectQuery
     * @return JsonNode {$hits{},$context{},$result:[{}....{}],} <br>
     *         $context will be added later (Access)</br>
     *         $result array of units(can be empty)
     * @throws InvalidParseOperationException Thrown when json format is not correct
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     * 
     */
    public JsonNode selectUnitsByQuery(String selectQuery)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException;

    /**
     * Search UNITs by Id {@link Select}Query <br>
     * for this method, the roots will be filled<br>
     * for example request :{
     * <h3>$roots:[{id:"id"}]</h3>,<br>
     * $query{}, ..}
     *
     * @param selectQuery
     * @param unitId
     * @return JsonNode {$hits{},$context{},$result:[{}....{}],} <br>
     *         $context will be added later (Access)</br>
     *         $result array of units(can be empty)
     * @throws InvalidParseOperationException Thrown when json format is not correct
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     * 
     */
    public JsonNode selectUnitsById(String selectQuery, String unitId)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException;

    /**
     * Update UNITs by Id {@link Update}Query <br>
     * for this method, the roots will be filled<br>
     * for example request :{
     * <h3>$roots:[{id:"id"}]</h3>,<br>
     * $query{}, ..}
     *
     * @param updateQuery
     * @param unitId
     * @return JsonNode {$hits{},$context{},$result:[{}....{}],} <br>
     *         $context will be added later (Access)</br>
     *         $result array of units(can be empty)
     * @throws InvalidParseOperationException Thrown when json format is not correct
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     * 
     */
    public JsonNode updateUnitbyId(String updateQuery, String unitId)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException;


    /**
     * @param objectRequest as JsonNode { $roots: roots, $query : query, $filter : multi, $data : data}
     *
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws MetaDataNotFoundException Throw if parent of this unit is not found
     * @throws MetaDataAlreadyExistException Throw if Unit id already exists
     * @throws MetaDataExecutionException Throw if error occurs when send Unit to database
     * @throws MetaDataDocumentSizeException Throw if Unit size is too big
     */
    void insertObjectGroup(JsonNode objectRequest) throws InvalidParseOperationException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataExecutionException, MetaDataDocumentSizeException;

}
