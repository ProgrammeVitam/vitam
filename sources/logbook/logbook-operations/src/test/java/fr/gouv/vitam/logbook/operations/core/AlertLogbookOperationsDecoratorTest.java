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
package fr.gouv.vitam.logbook.operations.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;

public class AlertLogbookOperationsDecoratorTest {
    
    private LogbookOperationsImpl logbookOperationsImpl;
    AlertLogbookOperationsDecorator alertLogbookOperationsDecorator;
    private LogbookDbAccess mongoDbAccess;
    private AlertService alertService;
    private String eventType;
    private String outcome="OK";
    private LogbookOperationParameters logbookParameters;
    LogbookEvent logbookEvent=new LogbookEvent();
    List<LogbookEvent> alertEvents=new ArrayList<LogbookEvent>();
    final static GUID eip = GUIDFactory.newEventGUID(1);
    
    
    @Before
    public void setUp() {
        mongoDbAccess = mock(LogbookDbAccess.class);
        alertService=mock(AlertServiceImpl.class);       
        logbookOperationsImpl = new LogbookOperationsImpl(mongoDbAccess);
        eventType="STP_IMPORT_ACCESS_CONTRACT";      
        logbookParameters = LogbookParametersFactory.newLogbookOperationParameters();
        logbookParameters.putParameterValue(LogbookParameterName.eventType, eventType);
        logbookParameters.putParameterValue(LogbookParameterName.outcome, outcome);
        logbookEvent.setEvType(eventType);
        logbookEvent.setOutcome(outcome);        
        alertEvents.add(logbookEvent);
        alertLogbookOperationsDecorator=new AlertLogbookOperationsDecorator(logbookOperationsImpl, alertEvents,alertService);
    }

    @Test
    public void testCreate() throws Exception {
        alertLogbookOperationsDecorator.create(logbookParameters);  
        Mockito.verify(alertService).createAlert(Mockito.eq(VitamLogLevel.INFO),Mockito.anyString());
    }
    
    @Test
    public void testUpdate() throws Exception {
        alertLogbookOperationsDecorator.update(logbookParameters);  
        Mockito.verify(alertService).createAlert(Mockito.eq(VitamLogLevel.INFO),Mockito.anyString());
    }
    
    @Test
    public void testCreateBulkLogbookOperation() throws Exception {
        LogbookOperationParameters[] operationArray = {logbookParameters};
        alertLogbookOperationsDecorator.createBulkLogbookOperation(operationArray); 
        Mockito.verify(alertService).createAlert(Mockito.eq(VitamLogLevel.INFO),Mockito.anyString());
    }

    @Test
    public void testUpdateBulkLogbookOperation() throws Exception {
        LogbookOperationParameters[] operationArray = {logbookParameters};
        alertLogbookOperationsDecorator.updateBulkLogbookOperation(operationArray); 
        Mockito.verify(alertService).createAlert(Mockito.eq(VitamLogLevel.INFO),Mockito.anyString());
    }
    

    @Test
    public void testIsAlertEvent() throws Exception {
       boolean isAlertEvent=alertLogbookOperationsDecorator.isAlertEvent(logbookParameters);
       assertTrue(isAlertEvent);
    }
    
    @Test
    public void testIsAlertEventFalse() throws Exception {
        logbookParameters.putParameterValue(LogbookParameterName.outcome, "KO");
       boolean isAlertEvent=alertLogbookOperationsDecorator.isAlertEvent(logbookParameters);
       assertFalse(isAlertEvent);
    }
    
    
}
