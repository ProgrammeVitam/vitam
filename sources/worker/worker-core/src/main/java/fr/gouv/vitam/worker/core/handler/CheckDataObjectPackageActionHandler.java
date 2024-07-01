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
package fr.gouv.vitam.worker.core.handler;

import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Check HEADER Handler
 */
public class CheckDataObjectPackageActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckDataObjectPackageActionHandler.class);
    private static final String HANDLER_ID = "CHECK_DATAOBJECTPACKAGE";
    private static final int CHECK_NO_OBJECT_INPUT_RANK = 0;

    private final MetaDataClientFactory metaDataClientFactory;
    private final AdminManagementClientFactory adminManagementClientFactory;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private final SedaUtilsFactory sedaUtilsFactory;

    private final CheckNoObjectsActionHandler checkNoObjectsActionHandler;
    private final CheckObjectsNumberActionHandler checkObjectsNumberActionHandler;
    private final ExtractSedaActionHandler extractSedaActionHandler;
    private final CheckObjectUnitConsistencyActionHandler checkObjectUnitConsistencyActionHandler;

    public CheckDataObjectPackageActionHandler(
        CheckNoObjectsActionHandler checkNoObjectsActionHandler,
        CheckObjectsNumberActionHandler checkObjectsNumberActionHandler,
        ExtractSedaActionHandler extractSedaActionHandler,
        CheckObjectUnitConsistencyActionHandler checkObjectUnitConsistencyActionHandler
    ) {
        this(
            MetaDataClientFactory.getInstance(),
            AdminManagementClientFactory.getInstance(),
            LogbookLifeCyclesClientFactory.getInstance(),
            SedaUtilsFactory.getInstance(),
            checkNoObjectsActionHandler,
            checkObjectsNumberActionHandler,
            extractSedaActionHandler,
            checkObjectUnitConsistencyActionHandler
        );
    }

    public CheckDataObjectPackageActionHandler(
        MetaDataClientFactory metaDataClientFactory,
        AdminManagementClientFactory adminManagementClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        SedaUtilsFactory sedaUtilsFactory,
        CheckNoObjectsActionHandler checkNoObjectsActionHandler,
        CheckObjectsNumberActionHandler checkObjectsNumberActionHandler,
        ExtractSedaActionHandler extractSedaActionHandler,
        CheckObjectUnitConsistencyActionHandler checkObjectUnitConsistencyActionHandler
    ) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.sedaUtilsFactory = sedaUtilsFactory;
        this.checkNoObjectsActionHandler = checkNoObjectsActionHandler;
        this.checkObjectsNumberActionHandler = checkObjectsNumberActionHandler;
        this.extractSedaActionHandler = extractSedaActionHandler;
        this.checkObjectUnitConsistencyActionHandler = checkObjectUnitConsistencyActionHandler;
    }

    public static String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        try {
            if (Boolean.valueOf((String) handlerIO.getInput(CHECK_NO_OBJECT_INPUT_RANK))) {
                Stopwatch checkNoObject = Stopwatch.createStarted();

                ItemStatus checkNoObjectStatus = checkNoObjectsActionHandler.execute(params, handlerIO);
                PerformanceLogger.getInstance()
                    .log(
                        "STP_INGEST_CONTROL_SIP",
                        "CHECK_DATAOBJECTPACKAGE",
                        "checkNoObject",
                        checkNoObject.elapsed(TimeUnit.MILLISECONDS)
                    );

                itemStatus.setItemsStatus(CheckNoObjectsActionHandler.getId(), checkNoObjectStatus);

                Stopwatch checkObjectNumber = Stopwatch.createStarted();

                ItemStatus checkObjectNumberStatus = checkObjectsNumberActionHandler.execute(params, handlerIO);
                PerformanceLogger.getInstance()
                    .log(
                        "STP_INGEST_CONTROL_SIP",
                        "CHECK_DATAOBJECTPACKAGE",
                        "checkObjectNumber",
                        checkObjectNumber.elapsed(TimeUnit.MILLISECONDS)
                    );

                itemStatus.setItemsStatus(CheckObjectsNumberActionHandler.getId(), checkObjectNumberStatus);

                Stopwatch extractSeda = Stopwatch.createStarted();

                ItemStatus extractSedaStatus = extractSedaActionHandler.execute(params, handlerIO);

                PerformanceLogger.getInstance()
                    .log(
                        "STP_INGEST_CONTROL_SIP",
                        "CHECK_DATAOBJECTPACKAGE",
                        "extractSeda",
                        extractSeda.elapsed(TimeUnit.MILLISECONDS)
                    );

                itemStatus.setItemsStatus(ExtractSedaActionHandler.getId(), extractSedaStatus);

                if (extractSedaStatus.shallStop(true)) {
                    resetItemStatusMeter(itemStatus);
                    return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                }
            } else {
                try (
                    CheckVersionActionHandler checkVersionActionHandler = new CheckVersionActionHandler(
                        sedaUtilsFactory
                    );
                    CheckObjectsNumberActionHandler checkObjectsNumberActionHandler =
                        new CheckObjectsNumberActionHandler(sedaUtilsFactory);
                    ExtractSedaActionHandler extractSedaActionHandler = new ExtractSedaActionHandler(
                        metaDataClientFactory,
                        adminManagementClientFactory,
                        logbookLifeCyclesClientFactory
                    );
                    CheckObjectUnitConsistencyActionHandler checkObjectUnitConsistencyActionHandler =
                        new CheckObjectUnitConsistencyActionHandler()
                ) {
                    Stopwatch checkVersion = Stopwatch.createStarted();

                    ItemStatus checkVersionStatus = checkVersionActionHandler.execute(params, handlerIO);
                    PerformanceLogger.getInstance()
                        .log(
                            "STP_INGEST_CONTROL_SIP",
                            "CHECK_DATAOBJECTPACKAGE",
                            "checkVersion",
                            checkVersion.elapsed(TimeUnit.MILLISECONDS)
                        );

                    itemStatus.setItemsStatus(CheckVersionActionHandler.getId(), checkVersionStatus);

                    if (checkVersionStatus.shallStop(true)) {
                        resetItemStatusMeter(itemStatus);
                        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                    }

                    Stopwatch checkObjectNumber = Stopwatch.createStarted();

                    ItemStatus checkObjectNumberStatus = checkObjectsNumberActionHandler.execute(params, handlerIO);

                    PerformanceLogger.getInstance()
                        .log(
                            "STP_INGEST_CONTROL_SIP",
                            "CHECK_DATAOBJECTPACKAGE",
                            "checkObjectNumber",
                            checkObjectNumber.elapsed(TimeUnit.MILLISECONDS)
                        );

                    itemStatus.setItemsStatus(CheckObjectsNumberActionHandler.getId(), checkObjectNumberStatus);

                    if (checkObjectNumberStatus.shallStop(true)) {
                        resetItemStatusMeter(itemStatus);
                        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                    }

                    Stopwatch extractSeda = Stopwatch.createStarted();

                    ItemStatus extractSedaStatus = extractSedaActionHandler.execute(params, handlerIO);
                    PerformanceLogger.getInstance()
                        .log(
                            "STP_INGEST_CONTROL_SIP",
                            "CHECK_DATAOBJECTPACKAGE",
                            "extractSeda",
                            extractSeda.elapsed(TimeUnit.MILLISECONDS)
                        );

                    itemStatus.setItemsStatus(ExtractSedaActionHandler.getId(), extractSedaStatus);

                    if (extractSedaStatus.shallStop(true)) {
                        resetItemStatusMeter(itemStatus);
                        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
                    }

                    handlerIO.getInput().clear();
                    List<IOParameter> inputList = new ArrayList<>();
                    inputList.add(
                        new IOParameter().setUri(handlerIO.getOutput(ExtractSedaActionHandler.OG_ID_TO_UNID_ID_IO_RANK))
                    );
                    inputList.add(
                        new IOParameter()
                            .setUri(handlerIO.getOutput(ExtractSedaActionHandler.OG_ID_TO_GUID_IO_MEMORY_RANK))
                    );
                    handlerIO.addInIOParameters(inputList);
                    handlerIO.getOutput().clear();

                    Stopwatch checkObjectUnitConsistency = Stopwatch.createStarted();

                    ItemStatus checkObjectUnitConsistencyStatus = checkObjectUnitConsistencyActionHandler.execute(
                        params,
                        handlerIO
                    );

                    PerformanceLogger.getInstance()
                        .log(
                            "STP_INGEST_CONTROL_SIP",
                            "CHECK_DATAOBJECTPACKAGE",
                            "checkObjectUnitConsistency",
                            checkObjectUnitConsistency.elapsed(TimeUnit.MILLISECONDS)
                        );

                    itemStatus.setItemsStatus(
                        CheckObjectUnitConsistencyActionHandler.getId(),
                        checkObjectUnitConsistencyStatus
                    );
                }
            }
        } catch (ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        resetItemStatusMeter(itemStatus);

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}

    /**
     * Reset the statusMeter of the specified itemStatus
     *
     * @param itemStatus
     */
    private void resetItemStatusMeter(ItemStatus itemStatus) {
        itemStatus.reinitStatusMeter();
        //counter for DATAOBJECTPACKAGE is always 1
        itemStatus.setStatusMeterValue(itemStatus.getGlobalStatus(), 1);
    }
}
