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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper.newLogbookLifeCycleUnitParameters;
import static fr.gouv.vitam.worker.core.plugin.evidence.DataRectificationHelper.canDoCorrection;

/**
 * DataCorrectionService class
 */
public class DataRectificationService {

    private final StorageClientFactory storageClientFactory;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private static final String OBJECT_CORRECTIVE_AUDIT = "OBJECT_CORRECTIVE_AUDIT";
    private static final String UNIT_CORRECTIVE_AUDIT = "UNIT_CORRECTIVE_AUDIT";
    private static final String OBJECT_GROUP_CORRECTIVE_AUDIT = "OBJECT_GROUP_CORRECTIVE_AUDIT";

    @VisibleForTesting
    DataRectificationService(
        StorageClientFactory storageClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory
    ) {
        this.storageClientFactory = storageClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
    }

    DataRectificationService() {
        this(StorageClientFactory.getInstance(), LogbookLifeCyclesClientFactory.getInstance());
    }

    public Optional<IdentifierType> correctUnits(EvidenceAuditReportLine line, String containerName)
        throws InvalidParseOperationException, StorageServerClientException, LogbookClientNotFoundException, LogbookClientServerException, LogbookClientBadRequestException, InvalidGuidOperationException, StorageUnavailableDataFromAsyncOfferClientException {
        String securedHash = line.getSecuredHash();
        List<String> goodOffers = new ArrayList<>();
        List<String> badOffers = new ArrayList<>();

        if (!canDoCorrection(line.getOffersHashes(), securedHash, goodOffers, badOffers)) {
            return Optional.empty();
        }
        String message = String.format(
            "offer '%s'  has been corrected from offer %s ",
            badOffers.get(0),
            goodOffers.get(0)
        );
        storageClientFactory
            .getClient()
            .copyObjectFromOfferToOffer(
                line.getIdentifier() + ".json",
                DataCategory.UNIT,
                goodOffers.get(0),
                badOffers.get(0),
                line.getStrategyId()
            );
        updateLifecycleUnit(containerName, line.getIdentifier(), UNIT_CORRECTIVE_AUDIT, message);

        return Optional.of(new IdentifierType(line.getIdentifier(), DataCategory.UNIT.name()));
    }

    public List<IdentifierType> correctObjectGroups(EvidenceAuditReportLine line, String containerName)
        throws InvalidParseOperationException, StorageServerClientException, LogbookClientNotFoundException, LogbookClientServerException, LogbookClientBadRequestException, InvalidGuidOperationException, StorageUnavailableDataFromAsyncOfferClientException {
        String securedHash = line.getSecuredHash();
        List<IdentifierType> listCorrections = new ArrayList<>();
        List<String> goodOffers = new ArrayList<>();
        List<String> badOffers = new ArrayList<>();

        if (canDoCorrection(line.getOffersHashes(), securedHash, goodOffers, badOffers)) {
            String message = String.format(
                "offer '%s'  has been corrected from offer %s ",
                badOffers.get(0),
                goodOffers.get(0)
            );
            storageClientFactory
                .getClient()
                .copyObjectFromOfferToOffer(
                    line.getIdentifier() + ".json",
                    DataCategory.OBJECTGROUP,
                    goodOffers.get(0),
                    badOffers.get(0),
                    line.getStrategyId()
                );

            updateLifecycleObject(containerName, line.getIdentifier(), OBJECT_GROUP_CORRECTIVE_AUDIT, message);

            listCorrections.add(new IdentifierType(line.getIdentifier(), DataCategory.OBJECTGROUP.name()));
        }

        for (EvidenceAuditReportObject object : line.getObjectsReports()) {
            goodOffers.clear();
            badOffers.clear();
            securedHash = object.getSecuredHash();

            if (object.getEvidenceStatus() == EvidenceStatus.OK) {
                continue;
            }
            if (!canDoCorrection(object.getOffersHashes(), securedHash, goodOffers, badOffers)) {
                continue;
            }
            String message = String.format(
                "offer '%s'  has been corrected from offer %s  for object id %s ",
                badOffers.get(0),
                goodOffers.get(0),
                object.getIdentifier()
            );
            storageClientFactory
                .getClient()
                .copyObjectFromOfferToOffer(
                    object.getIdentifier(),
                    DataCategory.OBJECT,
                    goodOffers.get(0),
                    badOffers.get(0),
                    object.getStrategyId()
                );

            updateLifecycleObject(containerName, line.getIdentifier(), OBJECT_CORRECTIVE_AUDIT, message);

            listCorrections.add(new IdentifierType(line.getIdentifier(), DataCategory.OBJECT.name()));
        }
        return listCorrections;
    }

    private void updateLifecycleObject(String containerName, String identifier, String detail, String message)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException, InvalidGuidOperationException {
        final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;

        final GUID updateGuid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        StatusCode logbookOutcome = StatusCode.OK;
        LogbookLifeCycleObjectGroupParameters parameters = newLogbookLifeCycleObjectGroupParameters(
            updateGuid,
            VitamLogbookMessages.getEventTypeLfc(detail),
            GUIDReader.getGUID(containerName),
            eventTypeProcess,
            logbookOutcome,
            VitamLogbookMessages.getOutcomeDetailLfc(detail, logbookOutcome),
            VitamLogbookMessages.getCodeLfc(detail, logbookOutcome),
            GUIDReader.getGUID(identifier)
        );

        final ObjectNode object = JsonHandler.createObjectNode();
        object.put("Information", message);

        final String wellFormedJson = JsonHandler.unprettyPrint(object);

        parameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);

        logbookLifeCyclesClientFactory.getClient().update(parameters, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);
    }

    private void updateLifecycleUnit(String containerName, String identifier, String detail, String message)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException, InvalidGuidOperationException {
        final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;

        final GUID updateGuid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());

        StatusCode logbookOutcome = StatusCode.OK;
        LogbookLifeCycleUnitParameters parameters = newLogbookLifeCycleUnitParameters(
            updateGuid,
            VitamLogbookMessages.getEventTypeLfc(detail),
            GUIDReader.getGUID(containerName),
            eventTypeProcess,
            logbookOutcome,
            VitamLogbookMessages.getOutcomeDetailLfc(detail, logbookOutcome),
            VitamLogbookMessages.getCodeLfc(detail, logbookOutcome),
            GUIDReader.getGUID(identifier)
        );

        final ObjectNode object = JsonHandler.createObjectNode();

        object.put("Information", message);

        final String wellFormedJson = JsonHandler.unprettyPrint(object);

        parameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);

        logbookLifeCyclesClientFactory.getClient().update(parameters, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);
    }
}
