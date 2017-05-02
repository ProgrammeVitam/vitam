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
 */

package fr.gouv.vitam.functional.administration.contract.api;


import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.functional.administration.client.model.AbstractContractModel;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

import java.util.List;

public interface ContractService<T extends AbstractContractModel> extends VitamAutoCloseable {


    /**
     * Create a collections of contracts After passing the validation steps. If all the contracts are valid, they are
     * stored in the collection and indexed. </BR> The access contract are valid in the following situations : </BR>
     * <ul>
     * <li>The collection contains 2 ore many contracts having the same name</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many contracts already exist in the database</li>
     *
     *
     * @param contractModelList
     * @return RequestResponseOK if success or VitamError
     * @throws VitamException if in error occurs while validating contracts
     */
    public RequestResponse<T> createContracts(List<T> contractModelList) throws VitamException;

    /**
     * Update contracts status after passing validation steps : </BR> 
     * Field modified : 
     * <ul> <li>- ActivationDate </li>
     * <li>- DesactivationDate </li>
     * <li>- LastUpdate </li>
     * <li>- Status</li>
     * 
     * 
     * @param queryDsl the given queryDsl for update
     * @return RequestResponseOK if success or VitamError
     * @throws VitamException if in error occurs while validating contracts
     */
    public RequestResponse<T> updateContract(JsonNode queryDsl) throws VitamException;


    /**
     * Find contract by id
     * 
     * @param id
     * @return
     */
    public T findOne(String id) throws ReferentialException, InvalidParseOperationException;


    /**
     * find contract by QueryDsl
     * 
     * @param queryDsl
     * @return
     */
    public List<T> findContracts(JsonNode queryDsl) throws ReferentialException, InvalidParseOperationException;



}
