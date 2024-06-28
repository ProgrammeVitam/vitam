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
package fr.gouv.vitam.logbook.lifecycles.core;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

public class LogbookLifeCyclesImplTest {

    private LogbookLifeCyclesImpl logbookLifeCyclesImpl;
    private LogbookDbAccess mongoDbAccess;
    private LogbookLifeCycleUnitParameters logbookLCUnitParameters;
    private LogbookLifeCycleObjectGroupParameters logbookLCOGParameters;
    static final GUID eip = GUIDFactory.newWriteLogbookGUID(0); // event identifier
    static final GUID iop = GUIDFactory.newWriteLogbookGUID(0); // identifier object
    static final GUID ioL = GUIDFactory.newUnitGUID(0);

    private static final LogbookLifeCycleUnitParameters getCompleteLifeCycleUnitParameters() {
        LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParameters;

        logbookLifeCyclesUnitParameters = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParameters.setStatus(StatusCode.OK);
        logbookLifeCyclesUnitParameters.putParameterValue(LogbookParameterName.eventIdentifier, eip.toString());
        logbookLifeCyclesUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, iop.toString());
        logbookLifeCyclesUnitParameters.putParameterValue(LogbookParameterName.objectIdentifier, ioL.toString());

        logbookLifeCyclesUnitParameters.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParameters.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParameters.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParameters.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        logbookLifeCyclesUnitParameters.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.nowFormatted()
        );
        logbookLifeCyclesUnitParameters.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );
        return logbookLifeCyclesUnitParameters;
    }

    private static final LogbookLifeCycleObjectGroupParameters getCompleteLifeCycleObjectGroupParameters() {
        LogbookLifeCycleObjectGroupParameters logbookLifeCycleObjectGroupParameters;

        logbookLifeCycleObjectGroupParameters = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCycleObjectGroupParameters.setStatus(StatusCode.OK);
        logbookLifeCycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, eip.toString());
        logbookLifeCycleObjectGroupParameters.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop.toString()
        ); // op
        logbookLifeCycleObjectGroupParameters.putParameterValue(LogbookParameterName.objectIdentifier, ioL.toString()); // lcid

        logbookLifeCycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCycleObjectGroupParameters.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCycleObjectGroupParameters.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        logbookLifeCycleObjectGroupParameters.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.nowFormatted()
        );
        logbookLifeCycleObjectGroupParameters.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );
        return logbookLifeCycleObjectGroupParameters;
    }

    @Before
    public void setUp() {
        mongoDbAccess = mock(LogbookDbAccess.class);
        logbookLCUnitParameters = getCompleteLifeCycleUnitParameters();
        logbookLCOGParameters = getCompleteLifeCycleObjectGroupParameters();
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenCreateLCUnitWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class).when(mongoDbAccess).createLogbookLifeCycleUnit(any(), any());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createUnit(iop.toString(), ioL.toString(), logbookLCUnitParameters);
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenUpdateLCUnitWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class)
            .when(mongoDbAccess)
            .updateLogbookLifeCycleUnit(any(), any(), any(), anyBoolean());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateUnit(iop.toString(), ioL.toString(), logbookLCUnitParameters);
    }

    @Test(expected = LogbookAlreadyExistsException.class)
    public void givenCreateLCUnitWhenLCUnitAlreadyExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookAlreadyExistsException.class)
            .when(mongoDbAccess)
            .createLogbookLifeCycleUnit(any(), any());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createUnit(iop.toString(), ioL.toString(), logbookLCUnitParameters);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenUpdateLCUnitWhenLCUnitNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class)
            .when(mongoDbAccess)
            .updateLogbookLifeCycleUnit(any(), any(), any(), anyBoolean());
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateUnit(iop.toString(), ioL.toString(), logbookLCUnitParameters);
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenCreateLCOGWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class)
            .when(mongoDbAccess)
            .createLogbookLifeCycleObjectGroup(any(), any());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createObjectGroup(iop.toString(), ioL.toString(), logbookLCOGParameters);
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenUpdateLCOGWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class)
            .when(mongoDbAccess)
            .updateLogbookLifeCycleObjectGroup(any(), any(), any());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateObjectGroup(iop.toString(), ioL.toString(), logbookLCOGParameters);
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenUpdateAndCommitLCOGWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class)
            .when(mongoDbAccess)
            .updateLogbookLifeCycleObjectGroup(any(), any(), any(), eq(true));

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateObjectGroup(iop.toString(), ioL.toString(), logbookLCOGParameters, true);
    }

    @Test(expected = LogbookAlreadyExistsException.class)
    public void givenCreateLCOGWhenLCOGAlreadyExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookAlreadyExistsException.class)
            .when(mongoDbAccess)
            .createLogbookLifeCycleObjectGroup(any(), any());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createObjectGroup(iop.toString(), ioL.toString(), logbookLCOGParameters);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenUpdateOGLCWhenOGLCNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class)
            .when(mongoDbAccess)
            .updateLogbookLifeCycleObjectGroup(any(), any(), any());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateObjectGroup(iop.toString(), ioL.toString(), logbookLCOGParameters);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenUpdateAndCommitOGLCWhenOGLCNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class)
            .when(mongoDbAccess)
            .updateLogbookLifeCycleObjectGroup(any(), any(), any(), eq(true));

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateObjectGroup(iop.toString(), ioL.toString(), logbookLCOGParameters, true);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenDeleteOGLCWhenOGLCNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class)
            .when(mongoDbAccess)
            .rollbackLogbookLifeCycleObjectGroup(any(), any());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.rollbackObjectGroup(iop.toString(), ioL.toString());
    }

    @Test
    public void givenDeleteOGLCWhenOGLCNotExistsAndPurgeDisabledThenDoNotThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class)
            .when(mongoDbAccess)
            .rollbackLogbookLifeCycleObjectGroup(any(), any());
        VitamConfiguration.setPurgeTemporaryLFC(false);
        try {
            logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
            logbookLifeCyclesImpl.rollbackObjectGroup(iop.toString(), ioL.toString());
        } finally {
            VitamConfiguration.setPurgeTemporaryLFC(true);
        }
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenDeleteLCOGWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class)
            .when(mongoDbAccess)
            .rollbackLogbookLifeCycleObjectGroup(any(), any());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.rollbackObjectGroup(iop.toString(), ioL.toString());
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenSelectObjectGroupLifeCycleWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class)
            .when(mongoDbAccess)
            .getLogbookLifeCycles(any(), anyBoolean(), any());
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.selectLifeCycles(null, false, LogbookCollections.LIFECYCLE_OBJECTGROUP);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenSelectObjectGroupLifeCycleWhenOperationNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class)
            .when(mongoDbAccess)
            .getLogbookLifeCycles(any(), anyBoolean(), any());
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.selectLifeCycles(null, true, LogbookCollections.LIFECYCLE_OBJECTGROUP);
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenSelectUnitLifeCycleWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class)
            .when(mongoDbAccess)
            .getLogbookLifeCycles(any(), anyBoolean(), any());
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.selectLifeCycles(null, true, LogbookCollections.LIFECYCLE_UNIT);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenSelectUnitLifeCycleWhenOperationNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class)
            .when(mongoDbAccess)
            .getLogbookLifeCycles(any(), anyBoolean(), any());
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.selectLifeCycles(null, true, LogbookCollections.LIFECYCLE_UNIT);
    }
}
