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
package fr.gouv.vitam.worker.common.utils;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCyclesClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SedaUtilsLifeCycleExceptionsTest {

    static final String UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE =
        "Check SIP – Units – Lifecycle Logbook Creation – Création du journal du cycle de vie des units";
    public static final String JSON_EXTENSION = ".json";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String OBJ = "obj";
    private final HandlerIO handlerIO = mock(HandlerIO.class);
    private final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
        .setWorkerGUID(GUIDFactory.newGUID().getId())
        .setUrlWorkspace("http://localhost:8083")
        .setUrlMetadata("http://localhost:8083")
        .setObjectName(OBJ)
        .setContainerName(OBJ)
        .setCurrentStep("TEST");

    private static LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private static LogbookLifeCyclesClient logbookLifeCycleClient;
    private final ItemStatus itemStatus = new ItemStatus("TEST");
    private LogbookLifeCyclesClientHelper helper;

    @BeforeClass
    public static void setup() {
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        logbookLifeCycleClient = mock(LogbookLifeCyclesClient.class);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCycleClient);
    }

    @Before
    public void setUp()
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException, LogbookClientNotFoundException, IOException {
        doNothing().when(logbookLifeCycleClient).create(any());
        doNothing().when(logbookLifeCycleClient).update(any());
        when(handlerIO.getLifecyclesClient()).thenReturn(logbookLifeCycleClient);

        helper = mock(LogbookLifeCyclesClientHelper.class);
        when(handlerIO.getHelper()).thenReturn(helper);
        when(handlerIO.getHelper()).thenReturn(helper);

        final Map<String, String> binaryDataObjectIdToObjectGroupId = new HashMap<>();
        final Map<String, String> objectGroupIdToGuid = new HashMap<>();
        binaryDataObjectIdToObjectGroupId.put("ID011", "ID006");
        objectGroupIdToGuid.put("ID006", OBJ);

        final File firstMapTmpFile = PropertiesUtils.fileFromTmpFolder(
            IngestWorkflowConstants.DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP_FILE_NAME_PREFIX + OBJ + JSON_EXTENSION
        );
        final FileWriter firstMapTmpFileWriter = new FileWriter(firstMapTmpFile);
        firstMapTmpFileWriter.write(binaryDataObjectIdToObjectGroupId.toString());
        firstMapTmpFileWriter.flush();
        firstMapTmpFileWriter.close();

        final File secondMapTmpFile = PropertiesUtils.fileFromTmpFolder(
            IngestWorkflowConstants.OBJECT_GROUP_ID_TO_GUID_MAP_FILE_NAME_PREFIX + OBJ + JSON_EXTENSION
        );
        final FileWriter secondMapTmpFileWriter = new FileWriter(secondMapTmpFile);
        secondMapTmpFileWriter.write(objectGroupIdToGuid.toString());
        secondMapTmpFileWriter.flush();
        secondMapTmpFileWriter.close();
        itemStatus.increment(StatusCode.OK);
    }

    @Test(expected = ProcessingException.class)
    public void givenUpdateLogbookClientServerExceptionWhenUpdateLifeCycleByStepThenThrowError()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException, ProcessingException {
        final LogbookLifeCycleParameters logbookLifecycleUnitParameters = createLogbookParametersInstance();

        doThrow(new LogbookClientNotFoundException("LogbookClientServerException"))
            .when(helper)
            .updateDelegate(logbookLifecycleUnitParameters);

        params.setCurrentStep("TEST");
        LogbookLifecycleWorkerHelper.updateLifeCycleStep(
            handlerIO.getHelper(),
            logbookLifecycleUnitParameters,
            params,
            null,
            LogbookTypeProcess.INGEST,
            StatusCode.OK
        );
        handlerIO.getLifecyclesClient().bulkUpdateUnit(OBJ, handlerIO.getHelper().removeUpdateDelegate(OBJ));
    }

    @Test(expected = ProcessingException.class)
    public void givenUpdateLogbookClientServerExceptionWhenUpdateLifeCycleForBeginingThenThrowError()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException, ProcessingException {
        final LogbookLifeCycleParameters logbookLifecycleUnitParameters = createLogbookParametersInstance();

        doThrow(new LogbookClientNotFoundException("LogbookClientServerException"))
            .when(helper)
            .updateDelegate(logbookLifecycleUnitParameters);

        params.setCurrentStep("TEST");
        LogbookLifecycleWorkerHelper.updateLifeCycleForBegining(
            handlerIO.getHelper(),
            logbookLifecycleUnitParameters,
            params,
            LogbookTypeProcess.INGEST
        );
        handlerIO.getLifecyclesClient().bulkUpdateUnit(OBJ, handlerIO.getHelper().removeUpdateDelegate(OBJ));
    }

    private LogbookLifeCycleParameters createLogbookParametersInstance() {
        final LogbookLifeCycleParameters logbookLifecycleUnitParameters =
            LogbookParameterHelper.newLogbookLifeCycleUnitParameters();
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.objectIdentifier, OBJ);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, OBJ);
        logbookLifecycleUnitParameters.putParameterValue(
            LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(0).getId()
        );
        logbookLifecycleUnitParameters.putParameterValue(
            LogbookParameterName.eventTypeProcess,
            LogbookTypeProcess.INGEST.name()
        );
        logbookLifecycleUnitParameters.putParameterValue(
            LogbookParameterName.eventType,
            UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE
        );
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcome, StatusCode.OK.toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcomeDetail, StatusCode.OK.toString());
        logbookLifecycleUnitParameters.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            StatusCode.OK.toString()
        );

        return logbookLifecycleUnitParameters;
    }
}
