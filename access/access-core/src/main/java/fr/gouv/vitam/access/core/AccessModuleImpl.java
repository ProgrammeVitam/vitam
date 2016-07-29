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
package fr.gouv.vitam.access.core;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.api.AccessModule;
import fr.gouv.vitam.access.common.exception.AccessExecutionException;
import fr.gouv.vitam.access.config.AccessConfiguration;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.client.MetaDataClient;
import fr.gouv.vitam.client.MetaDataClientFactory;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCycleClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.logbook.operations.client.LogbookClientFactory;

/**
 * AccessModuleImpl implements AccessModule
 */
public class AccessModuleImpl implements AccessModule {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessModuleImpl.class);
    private LogbookLifeCycleClient logbookLifeCycleClient;
    private LogbookClient logbookOperationClient;

    private final AccessConfiguration accessConfiguration;

    private MetaDataClientFactory metaDataClientFactory;

    private MetaDataClient metaDataClient;
    private static final String BLANK_REQUEST = "the request is blank";
    private static final String SANITY_CHECK_FAILED = "Sanity Check Failed ";
    private static final String ID_CHECK_FAILED = "the unit_id should be filled";
    private String eventType = "Update_archive_unit_unitary";           //Event Type

    //TODO setting in other place
    private Integer tenantId = 0;

    /**
     * AccessModuleImpl constructor
     *
     * @param configuration of mongoDB access
     */
    // constructor
    public AccessModuleImpl(AccessConfiguration configuration) {
        accessConfiguration = configuration;
    }

    /**
     * AccessModuleImpl constructor <br>
     * with metaDataClientFactory and configuration
     *
     * @param metaDataClientFactory {@link MetaDataClientFactory}
     * @param configuration {@link AccessConfiguration} access configuration
     */
    public AccessModuleImpl(MetaDataClientFactory metaDataClientFactory, AccessConfiguration configuration) {
        this.metaDataClientFactory = metaDataClientFactory;
        accessConfiguration = configuration;
    }

    /**
     * AccessModuleImpl constructor <br>
     * with metaDataClientFactory, configuration and logbook operation client and lifecycle
     *
     * @param metaDataClientFactory
     * @param configuration
     * @param pLogbookOperationClient
     * @param pLogbookLifeCycleClient
     */
    public AccessModuleImpl(MetaDataClientFactory metaDataClientFactory, AccessConfiguration configuration,
                            LogbookClient pLogbookOperationClient, LogbookLifeCycleClient pLogbookLifeCycleClient) {
        this.metaDataClientFactory = metaDataClientFactory;
        accessConfiguration = configuration;

        logbookOperationClient = pLogbookOperationClient==null ?
                LogbookClientFactory.getInstance().getLogbookOperationClient() : pLogbookOperationClient;
        logbookLifeCycleClient = pLogbookLifeCycleClient==null ?
                LogbookLifeCyclesClientFactory.getInstance().getLogbookLifeCyclesClient() : pLogbookLifeCycleClient;
    }

    /**
     * select Unit
     *
     * @param jsonQuery as String { $query : query}
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessExecutionException Throw if error occurs when send Unit to database
     */
    @Override
    public JsonNode selectUnit(JsonNode jsonQuery)
        throws IllegalArgumentException, InvalidParseOperationException, AccessExecutionException {

        JsonNode jsonNode = null;

        try {
            if (metaDataClientFactory == null) {
                metaDataClientFactory = new MetaDataClientFactory();
            }
            metaDataClient = metaDataClientFactory.create(accessConfiguration.getUrlMetaData());

            jsonNode = metaDataClient.selectUnits(jsonQuery.toString());

        } catch (final InvalidParseOperationException e) {
            LOGGER.error("parsing error", e);
            throw e;
        } catch (IllegalArgumentException e) {
            LOGGER.error("illegal argument", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("exeption thrown", e);
            throw new AccessExecutionException(e);
        }
        return jsonNode;
    }

    /**
     * select Unit by Id
     *
     * @param jsonQuery as String { $query : query}
     * @param unit_id as String
     * @throws IllegalArgumentException Throw if json format is not correct
     * @throws AccessExecutionException Throw if error occurs when send Unit to database
     */


    @Override
    public JsonNode selectUnitbyId(JsonNode jsonQuery, String unit_id)
        throws IllegalArgumentException, InvalidParseOperationException, AccessExecutionException {
        JsonNode jsonNode = null;

        if (StringUtils.isEmpty(unit_id)) {
            throw new IllegalArgumentException(ID_CHECK_FAILED);
        }
        try {

            if (metaDataClientFactory == null) {
                metaDataClientFactory = new MetaDataClientFactory();
            }
            metaDataClient = metaDataClientFactory.create(accessConfiguration.getUrlMetaData());

            jsonNode = metaDataClient.selectUnitbyId(jsonQuery.toString(), unit_id);

        } catch (final InvalidParseOperationException e) {
            LOGGER.error("parsing error", e);
            throw e;
        } catch (IllegalArgumentException e) {
            LOGGER.error("illegal argument", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("exeption thrown", e);
            throw new AccessExecutionException(e);
        }
        return jsonNode;
    }

    /**
     * update Unit by id
     *
     * @param queryJson json update query
     * @param id_unit   as String
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessExecutionException       Throw if error occurs when send Unit to database
     * @throws IllegalArgumentException       Throw if error occurs when checking argument
     */
    @Override
    public JsonNode updateUnitbyId(JsonNode queryJson, String id_unit) throws IllegalArgumentException, InvalidParseOperationException, AccessExecutionException {
        JsonNode jsonNode = null;
        LogbookOperationParameters logbookOpParamStart, logbookOpParamEnd;

        // TODO : Comment until to resolve lifeCycle update
        // LogbookLifeCycleUnitParameters logbookLCParamStart, logbookLCParamEnd;

        if (StringUtils.isEmpty(id_unit)) {
            throw new IllegalArgumentException(ID_CHECK_FAILED);
        }

        // eventidentifierprocess for lifecycle
        final GUID updateOpGuidStart = GUIDFactory.newOperationIdGUID(tenantId);

        try {

            if (metaDataClientFactory == null) {
                metaDataClientFactory = new MetaDataClientFactory();
            }
            metaDataClient = metaDataClientFactory.create(accessConfiguration!=null?accessConfiguration.getUrlMetaData():"");

            logbookOperationClient = logbookOperationClient == null
                ? LogbookClientFactory.getInstance().getLogbookOperationClient() : logbookOperationClient;

            // FIXME : Update lifeCycle issue aborted, only logbook operation is created

            // Create logbook operation
            logbookOpParamStart = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                LogbookOutcome.STARTED, "update archiveunit:" + id_unit, id_unit);
            logbookOperationClient.create(logbookOpParamStart);


            //call update
            jsonNode = metaDataClient.updateUnitbyId(queryJson.toString(), id_unit);

            logbookOpParamEnd = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                LogbookOutcome.OK, "update archiveunit:" + id_unit, id_unit);
            logbookOperationClient.update(logbookOpParamEnd);

        } catch (final InvalidParseOperationException ipoe) {
            rollBackLogbook(updateOpGuidStart, queryJson, id_unit);
            LOGGER.error("parsing error", ipoe);
            throw ipoe;
        } catch (IllegalArgumentException iae) {
            rollBackLogbook(updateOpGuidStart, queryJson, id_unit);
            LOGGER.error("illegal argument", iae);
            throw iae;
        } catch (MetaDataDocumentSizeException mddse) {
            rollBackLogbook(updateOpGuidStart, queryJson, id_unit);
            LOGGER.error("metadata document size error", mddse);
            throw  new AccessExecutionException(mddse);
        } catch (LogbookClientServerException lcse) {
            rollBackLogbook(updateOpGuidStart, queryJson, id_unit);
            LOGGER.error("document client server error", lcse);
            throw new AccessExecutionException(lcse);
        } catch (MetaDataExecutionException mdee) {
            rollBackLogbook(updateOpGuidStart, queryJson, id_unit);
            LOGGER.error("metadata execution execution error", mdee);
            throw new AccessExecutionException(mdee);
        } catch (LogbookClientNotFoundException lcnfe) {
            rollBackLogbook(updateOpGuidStart, queryJson, id_unit);
            LOGGER.error("logbook client not found error", lcnfe);
            throw new AccessExecutionException(lcnfe);
        } catch (LogbookClientBadRequestException lcbre) {
            rollBackLogbook(updateOpGuidStart, queryJson, id_unit);
            LOGGER.error("logbook client bad request error", lcbre);
            throw new AccessExecutionException(lcbre);
        } catch (LogbookClientAlreadyExistsException e) {
            LOGGER.error("logbook operation already exists", e);
            throw new AccessExecutionException(e);
        }
        return jsonNode;
    }

    private void rollBackLogbook(GUID updateOpGuidStart, JsonNode queryJson, String objectIdentifier) {
        try {
            LogbookOperationParameters logbookOpParamEnd = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    LogbookOutcome.ERROR, "Echec de l'écriture de la mise à jour des métadonnées", objectIdentifier);
            logbookOperationClient.update(logbookOpParamEnd);

            LogbookLifeCycleUnitParameters logbookParametersEnd = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, LogbookOutcome.ERROR,
                    queryJson.toString(), queryJson.toString(), objectIdentifier);
            logbookLifeCycleClient.rollback(logbookParametersEnd);
        } catch (LogbookClientBadRequestException lcbre) {
            LOGGER.error("bad request", lcbre);
        } catch (LogbookClientNotFoundException lcbre) {
            LOGGER.error("client not found", lcbre);
        } catch (LogbookClientServerException lcse) {
            LOGGER.error("client server error", lcse);
        }
    }

    private LogbookLifeCycleUnitParameters getLogbookLifeCycleUpdateUnitParameters(GUID eventIdentifierProcess, LogbookOutcome logbookOutcome, String outcomeDetail,
                                                                                   String outcomeDetailMessage, String objectIdentifier) {
        LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        final GUID updateGuid = GUIDFactory.newUnitGUID(tenantId); //eventidentifier

        LogbookLifeCycleUnitParameters parameters = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        parameters.putParameterValue(LogbookParameterName.eventIdentifier, updateGuid.toString());
        parameters.putParameterValue(LogbookParameterName.eventType, eventType);
        parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, eventIdentifierProcess!=null ? eventIdentifierProcess.toString() : "evtIdP NA");
        parameters.putParameterValue(LogbookParameterName.eventTypeProcess, eventTypeProcess.toString());
        parameters.putParameterValue(LogbookParameterName.outcome, logbookOutcome!=null ? logbookOutcome.toString() : "outcome NA");
        parameters.putParameterValue(LogbookParameterName.outcomeDetail, "update archive unit");
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, "update unit"+objectIdentifier.toString());
        parameters.putParameterValue(LogbookParameterName.objectIdentifier, objectIdentifier);

        return parameters;
    }

    private LogbookOperationParameters getLogbookOperationUpdateUnitParameters(GUID eventIdentifier, GUID eventIdentifierProcess, LogbookOutcome logbookOutcome,
                                                                               String outcomeDetailMessage, String eventIdentifierRequest) {

        LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;

        LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
        parameters.putParameterValue(LogbookParameterName.eventIdentifier, eventIdentifier!=null ? eventIdentifier.toString() : "evtId NA");
        parameters.putParameterValue(LogbookParameterName.eventType, eventType);
        parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, eventIdentifierProcess!=null ? eventIdentifierProcess.toString() : "evtIdP NA");
        parameters.putParameterValue(LogbookParameterName.eventTypeProcess, eventTypeProcess.toString());
        parameters.putParameterValue(LogbookParameterName.outcome, logbookOutcome!=null ? logbookOutcome.toString() : "outcome NA");
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage, outcomeDetailMessage);
        parameters.putParameterValue(LogbookParameterName.eventIdentifierRequest, eventIdentifierRequest);

        return parameters;
    }
}
