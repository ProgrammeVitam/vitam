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
package fr.gouv.vitam.logbook.lifecycles.core;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

public class LogbookLifeCyclesImplTest {

    private LogbookLifeCyclesImpl logbookLifeCyclesImpl;
    private LogbookDbAccess mongoDbAccess;
    private LogbookLifeCycleUnitParameters logbookLCUnitParameters;
    private LogbookLifeCycleObjectGroupParameters logbookLCOGParameters;
    final static GUID eip = GUIDFactory.newWriteLogbookGUID(0);// event identifier
    final static GUID iop = GUIDFactory.newWriteLogbookGUID(0);// identifier object
    final static GUID ioL = GUIDFactory.newUnitGUID(0);

    private static final LogbookLifeCycleUnitParameters getCompleteLifeCycleUnitParameters() {
        LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersStart;


        logbookLifeCyclesUnitParametersStart = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.STARTED);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());

        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        return logbookLifeCyclesUnitParametersStart;

    }

    private static final LogbookLifeCycleObjectGroupParameters getCompleteLifeCycleObjectGroupParameters() {

        LogbookLifeCycleObjectGroupParameters logbookLifeCycleObjectGroupParametersStart;


        logbookLifeCycleObjectGroupParametersStart =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCycleObjectGroupParametersStart.setStatus(StatusCode.STARTED);
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());// op
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());// lcid

        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCycleObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetail,
            "outcomeDetail");
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        return logbookLifeCycleObjectGroupParametersStart;

    }

    @Before
    public void setUp() {
        mongoDbAccess = mock(LogbookDbAccess.class);
        logbookLCUnitParameters = getCompleteLifeCycleUnitParameters();
        logbookLCOGParameters = getCompleteLifeCycleObjectGroupParameters();
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenCreateLCUnitWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class).when(mongoDbAccess).createLogbookLifeCycleUnit(anyObject(),
            anyObject());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createUnit(iop.toString(), ioL.toString(), logbookLCUnitParameters);
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenUpdateLCUnitWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class).when(mongoDbAccess).updateLogbookLifeCycleUnit(anyObject(),
            anyObject());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateUnit(iop.toString(), ioL.toString(), logbookLCUnitParameters);
    }


    @Test(expected = LogbookAlreadyExistsException.class)
    public void givenCreateLCUnitWhenLCUnitAlreadyExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookAlreadyExistsException.class).when(mongoDbAccess).createLogbookLifeCycleUnit(anyObject(),
            anyObject());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createUnit(iop.toString(), ioL.toString(), logbookLCUnitParameters);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenUpdateLCUnitWhenLCUnitNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class).when(mongoDbAccess).updateLogbookLifeCycleUnit(anyObject(),
            anyObject());
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateUnit(iop.toString(), ioL.toString(), logbookLCUnitParameters);
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenCreateLCOGWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class).when(mongoDbAccess)
            .createLogbookLifeCycleObjectGroup(anyObject(), anyObject());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createObjectGroup(iop.toString(), ioL.toString(), logbookLCOGParameters);
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenUpdateLCOGWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class).when(mongoDbAccess)
            .updateLogbookLifeCycleObjectGroup(anyObject(), anyObject());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateObjectGroup(iop.toString(), ioL.toString(), logbookLCOGParameters);
    }

    @Test(expected = LogbookAlreadyExistsException.class)
    public void givenCreateLCOGWhenLCOGAlreadyExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookAlreadyExistsException.class).when(mongoDbAccess)
            .createLogbookLifeCycleObjectGroup(anyObject(), anyObject());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.createObjectGroup(iop.toString(), ioL.toString(), logbookLCOGParameters);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenUpdateOGLCWhenOGLCNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class).when(mongoDbAccess)
            .updateLogbookLifeCycleObjectGroup(anyObject(), anyObject());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.updateObjectGroup(iop.toString(), ioL.toString(), logbookLCOGParameters);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenDeleteOGLCWhenOGLCNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class).when(mongoDbAccess)
            .rollbackLogbookLifeCycleObjectGroup(anyObject(), anyObject());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.rollbackObjectGroup(iop.toString(), ioL.toString());
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenDeleteLCOGWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class).when(mongoDbAccess)
            .rollbackLogbookLifeCycleObjectGroup(anyObject(), anyObject());

        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.rollbackObjectGroup(iop.toString(), ioL.toString());
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenSelectObjectGroupLifeCycleWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class).when(mongoDbAccess)
            .getLogbookLifeCycleObjectGroups(anyObject());
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.selectObjectGroup(null);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenSelectObjectGroupLifeCycleWhenOperationNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class).when(mongoDbAccess)
            .getLogbookLifeCycleObjectGroups(anyObject());
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.selectObjectGroup(null);
    }

    @Test(expected = LogbookDatabaseException.class)
    public void givenSelectUnitLifeCycleWhenErrorInMongoThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookDatabaseException.class).when(mongoDbAccess)
            .getLogbookLifeCycleUnits(anyObject());
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.selectUnit(null);
    }

    @Test(expected = LogbookNotFoundException.class)
    public void givenSelectUnitLifeCycleWhenOperationNotExistsThenThrowLogbookException() throws Exception {
        Mockito.doThrow(LogbookNotFoundException.class).when(mongoDbAccess)
            .getLogbookLifeCycleUnits(anyObject());
        logbookLifeCyclesImpl = new LogbookLifeCyclesImpl(mongoDbAccess);
        logbookLifeCyclesImpl.selectUnit(null);
    }
}


