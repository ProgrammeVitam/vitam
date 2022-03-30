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
package fr.gouv.vitam.functional.administration.contract.core;

import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.administration.AbstractContractModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;

/**
 * Common utils for contract services
 */
public class ContractHelper {

    /**
     * Find contracts by idenfier
     *
     * @param identifier identifier
     * @param collection mongo collection of the contract
     * @param mongoAccess mongo access service
     * @return DbRequestResult
     * @throws InvalidParseOperationException
     * @throws ReferentialException
     */
    public static DbRequestResult findByIdentifier(String identifier, FunctionalAdminCollections collection,
        MongoDbAccessReferential mongoAccess) throws InvalidParseOperationException, ReferentialException {
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(QueryHelper.eq("Identifier", identifier));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }
        return mongoAccess.findDocuments(parser.getRequest().getFinalSelect(), collection);
    }

    /**
     * Init idenfier in contract if slave not activated.
     *
     * @param slaveMode slaveMode
     * @param cm contract
     * @param vitamCounterService counter service
     * @param sequenceType sequence collection
     * @throws ReferentialException
     */
    public static void setIdentifier(boolean slaveMode, AbstractContractModel cm,
        VitamCounterService vitamCounterService, SequenceType sequenceType) throws ReferentialException {
        if (!slaveMode) {
            final String code = vitamCounterService.getNextSequenceAsString(ParameterHelper.getTenantParameter(),
                sequenceType);
            cm.setIdentifier(code);
        }
    }
}
