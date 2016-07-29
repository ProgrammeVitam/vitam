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
package fr.gouv.vitam.processing.common.utils;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.client.MetaDataClientFactory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCycleClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LogbookLifeCyclesClientFactory.class, WorkspaceClientFactory.class})
public class SedaUtilsLifeCycleExceptionsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String SIP = "sip1.xml";
	private static final String SIP_ARCHIVE_BEFORE_BDO = "SIP_Archive_Before_BDO.xml";
    private static final String OBJ = "obj";
    private WorkspaceClient workspaceClient;
    private final MetaDataClientFactory metadataFactory = new MetaDataClientFactory();
    private final InputStream seda = Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP);
	private final InputStream seda_2 = Thread.currentThread().getContextClassLoader()
			.getResourceAsStream(SIP_ARCHIVE_BEFORE_BDO);
    private SedaUtils utils;
    private final WorkParams params = new WorkParams().setGuuid(OBJ).setContainerName(OBJ)
        .setServerConfiguration(new ServerConfiguration().setUrlWorkspace(OBJ).setUrlMetada(OBJ))
        .setObjectName(OBJ);

    private static LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private static LogbookLifeCycleClient logbookLifeCycleClient;

    @BeforeClass
    public static void setup() {
        PowerMockito.mockStatic(LogbookLifeCyclesClientFactory.class);

        logbookLifeCyclesClientFactory = PowerMockito.mock(LogbookLifeCyclesClientFactory.class);
        logbookLifeCycleClient = org.mockito.Mockito.mock(LogbookLifeCycleClient.class);
        PowerMockito.when(LogbookLifeCyclesClientFactory.getInstance()).thenReturn(logbookLifeCyclesClientFactory);
        PowerMockito.when(LogbookLifeCyclesClientFactory.getInstance().getLogbookLifeCyclesClient())
				.thenReturn(logbookLifeCycleClient);
    }

    @Before
    public void setUp()
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        LogbookClientNotFoundException, IOException {
        workspaceClient = mock(WorkspaceClient.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);

        PowerMockito.doNothing().when(logbookLifeCycleClient).create(anyObject());
        PowerMockito.doNothing().when(logbookLifeCycleClient).update(anyObject());


        Map<String, String> binaryDataObjectIdToObjectGroupId = new HashMap<String, String>();
        Map<String, String> objectGroupIdToGuid = new HashMap<String, String>();
        binaryDataObjectIdToObjectGroupId.put("ID011", "ID006");
        objectGroupIdToGuid.put("ID006", OBJ);

        final File firstMapTmpFile = PropertiesUtils
            .fileFromTmpFolder(SedaUtils.BDO_TO_OBJECT_GROUP_ID_MAP_FILE_NAME_PREFIX + OBJ + SedaUtils.TXT_EXTENSION);
        final FileWriter firstMapTmpFileWriter = new FileWriter(firstMapTmpFile);
        firstMapTmpFileWriter.write(binaryDataObjectIdToObjectGroupId.toString());
        firstMapTmpFileWriter.flush();
        firstMapTmpFileWriter.close();

        final File secondMapTmpFile = PropertiesUtils
            .fileFromTmpFolder(SedaUtils.OBJECT_GROUP_ID_TO_GUID_MAP_FILE_NAME_PREFIX + OBJ + SedaUtils.TXT_EXTENSION);
        final FileWriter secondMapTmpFileWriter = new FileWriter(secondMapTmpFile);
        secondMapTmpFileWriter.write(objectGroupIdToGuid.toString());
        secondMapTmpFileWriter.flush();
        secondMapTmpFileWriter.close();
    }

	@Test(expected = ProcessingException.class)
	public void givenLogbookClientBadRequestExceptionWhenCheckConformityThenThrowError()
			throws ProcessingException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
			LogbookClientServerException, LogbookClientNotFoundException, URISyntaxException,
			ContentAddressableStorageException {
		when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
		PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
		PowerMockito.doThrow(new LogbookClientBadRequestException("LogbookClientBadRequestException"))
				.when(logbookLifeCycleClient).update(anyObject());

		utils = new SedaUtilsFactory().create(metadataFactory);
		utils.checkConformityBinaryObject(params);
	}

	@Test(expected = ProcessingException.class)
	public void givenLogbookClientNotFoundExceptionWhenCheckConformityThenThrowError() throws ProcessingException,
			LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
			LogbookClientNotFoundException, URISyntaxException, ContentAddressableStorageException {
		when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
		PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
		PowerMockito.doThrow(new LogbookClientNotFoundException("LogbookClientNotFoundException"))
				.when(logbookLifeCycleClient).update(anyObject());

		utils = new SedaUtilsFactory().create(metadataFactory);
		utils.checkConformityBinaryObject(params);
	}

	@Test(expected = ProcessingException.class)
	public void givenLogbookClientServerExceptionWhenCheckConformityThenThrowError() throws ProcessingException,
			LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
			LogbookClientNotFoundException, URISyntaxException, ContentAddressableStorageException {
		when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
		PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
		PowerMockito.doThrow(new LogbookClientServerException("LogbookClientServerException"))
				.when(logbookLifeCycleClient).update(anyObject());

		utils = new SedaUtilsFactory().create(metadataFactory);
		utils.checkConformityBinaryObject(params);
	}

	// Create LifeCycle with LogbookClientBadRequestException : Archive Unit
	// parsing
	@Test(expected = ProcessingException.class)
	public void givenLogbookClientBadRequestExceptionWhenExtractSedaArchiveUnitFirstProcessThenThrowError()
			throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
			ProcessingException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
			LogbookClientServerException {
		when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda_2);
		PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
		PowerMockito.doThrow(new LogbookClientBadRequestException("LogbookClientBadRequestException"))
				.when(logbookLifeCycleClient).create(anyObject());

		utils = new SedaUtilsFactory().create(metadataFactory);
		utils.extractSEDA(params);
	}

	// Create LifeCycle with LogbookClientAlreadyExistsException : Archive Unit
	// parsing
	@Test(expected = ProcessingException.class)
	public void givenLogbookClientAlreadyExistsExceptionWhenExtractSedaArchiveUnitFirstProcessThenThrowError()
			throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
			ProcessingException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
			LogbookClientServerException {
		when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda_2);
		PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
		PowerMockito.doThrow(new LogbookClientAlreadyExistsException("LogbookClientAlreadyExistsException"))
				.when(logbookLifeCycleClient).create(anyObject());

		utils = new SedaUtilsFactory().create(metadataFactory);
		utils.extractSEDA(params);
	}

	// Create LifeCycle with LogbookClientServerException : Archive Unit
	// parsing
	@Test(expected = ProcessingException.class)
	public void givenLogbookClientServerExceptionWhenExtractSedaArchiveUnitFirstProcessThenThrowError()
			throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
			ProcessingException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
			LogbookClientServerException {
		when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda_2);
		PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
		PowerMockito.doThrow(new LogbookClientServerException("LogbookClientServerException"))
				.when(logbookLifeCycleClient).create(anyObject());

		utils = new SedaUtilsFactory().create(metadataFactory);
		utils.extractSEDA(params);
	}

    // Create LifeCycle with LogbookClientBadRequestException
    @Test(expected = ProcessingException.class)
    public void givenLogbookClientBadRequestExceptionWhenExtractSedaThenThrowError()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        ProcessingException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        PowerMockito.doThrow(new LogbookClientBadRequestException("LogbookClientBadRequestException"))
            .when(logbookLifeCycleClient).create(anyObject());

        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.extractSEDA(params);
    }

    // Create LifeCycle with LogbookClientServerException
    @Test(expected = ProcessingException.class)
    public void givenLogbookClientServerExceptionWhenExtractSedaThenThrowError()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        ProcessingException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        PowerMockito.doThrow(new LogbookClientServerException("LogbookClientServerException"))
            .when(logbookLifeCycleClient).create(anyObject());

        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.extractSEDA(params);
    }

    // Create LifeCycle with LogbookClientAlreadyExistsException
    @Test(expected = ProcessingException.class)
    public void givenLogbookClientAlreadyExistsExceptionWhenExtractSedaThenThrowError()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        ProcessingException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);
        PowerMockito.doThrow(new LogbookClientAlreadyExistsException("LogbookClientAlreadyExistsException"))
            .when(logbookLifeCycleClient).create(anyObject());

        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.extractSEDA(params);
    }

    // Update LifeCycle with LogbookClientBadRequestException
    @Test(expected = ProcessingException.class)
    public void givenUpdateLogbookClientBadRequestExceptionWhenExtractSedaThenThrowError()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        ProcessingException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException, LogbookClientNotFoundException {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);

        PowerMockito.doThrow(new LogbookClientBadRequestException("LogbookClientBadRequestException"))
            .when(logbookLifeCycleClient).update(anyObject());

        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.extractSEDA(params);
    }

    // Update LifeCycle with LogbookClientNotFoundException
    @Test(expected = ProcessingException.class)
    public void givenUpdateLogbookClientNotFoundExceptionWhenExtractSedaThenThrowError()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        ProcessingException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException, LogbookClientNotFoundException {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);

        PowerMockito.doThrow(new LogbookClientNotFoundException("LogbookClientNotFoundException"))
            .when(logbookLifeCycleClient).update(anyObject());

        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.extractSEDA(params);
    }

    // Update LifeCycle with LogbookClientServerException
    @Test(expected = ProcessingException.class)
    public void givenUpdateLogbookClientServerExceptionWhenExtractSedaThenThrowError()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        ProcessingException, LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException, LogbookClientNotFoundException {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        PowerMockito.when(WorkspaceClientFactory.create(Mockito.anyObject())).thenReturn(workspaceClient);

        PowerMockito.doThrow(new LogbookClientServerException("LogbookClientServerException"))
            .when(logbookLifeCycleClient).update(anyObject());

        utils = new SedaUtilsFactory().create(metadataFactory);
        utils.extractSEDA(params);
    }
    
	@Test(expected = ProcessingException.class)
	public void givenUpdateLogbookClientServerExceptionWhenUpdateLifeCycleByStepThenThrowError()
			throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException,
			ProcessingException {

		LogbookParameters logbookLifecycleUnitParameters = createLogbookParametersInstance();

		PowerMockito.doThrow(new LogbookClientServerException("LogbookClientServerException"))
				.when(logbookLifeCycleClient).update(logbookLifecycleUnitParameters);

		utils = new SedaUtilsFactory().create(metadataFactory);
		params.setCurrentStep("TEST");
		utils.updateLifeCycleByStep(logbookLifecycleUnitParameters, params);
    }

	@Test(expected = ProcessingException.class)
	public void givenLogbookClientBadRequestExceptionWhenUpdateLifeCycleByStepThenThrowError()
			throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException,
			ProcessingException {

		LogbookParameters logbookLifecycleUnitParameters = createLogbookParametersInstance();

		PowerMockito.doThrow(new LogbookClientBadRequestException("LogbookClientBadRequestException"))
				.when(logbookLifeCycleClient).update(logbookLifecycleUnitParameters);

		utils = new SedaUtilsFactory().create(metadataFactory);
		params.setCurrentStep("TEST");
		utils.updateLifeCycleByStep(logbookLifecycleUnitParameters, params);
	}

	@Test(expected = ProcessingException.class)
	public void givenLogbookClientNotFoundExceptionWhenUpdateLifeCycleByStepThenThrowError()
			throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException,
			ProcessingException {

		LogbookParameters logbookLifecycleUnitParameters = createLogbookParametersInstance();

		PowerMockito.doThrow(new LogbookClientNotFoundException("LogbookClientNotFoundException"))
				.when(logbookLifeCycleClient).update(logbookLifecycleUnitParameters);

		utils = new SedaUtilsFactory().create(metadataFactory);
		params.setCurrentStep("TEST");
		utils.updateLifeCycleByStep(logbookLifecycleUnitParameters, params);
	}

	@Test(expected = ProcessingException.class)
	public void givenLogbookClientServerExceptionWhenSetLifeCycleFinalEventStatusThenThrowError()
			throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException,
			ProcessingException {
		LogbookParameters logbookLifecycleUnitParameters = createLogbookParametersInstance();

		PowerMockito.doThrow(new LogbookClientServerException("LogbookClientServerException"))
				.when(logbookLifeCycleClient).update(logbookLifecycleUnitParameters);

		utils = new SedaUtilsFactory().create(metadataFactory);
		params.setCurrentStep("TEST");
		utils.setLifeCycleFinalEventStatusByStep(logbookLifecycleUnitParameters, StatusCode.OK);
	}

	@Test(expected = ProcessingException.class)
	public void givenLogbookClientBadRequestExceptionWhenSetLifeCycleFinalEventStatusThenThrowError()
			throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException,
			ProcessingException {
		LogbookParameters logbookLifecycleUnitParameters = createLogbookParametersInstance();

		PowerMockito.doThrow(new LogbookClientBadRequestException("LogbookClientBadRequestException"))
				.when(logbookLifeCycleClient).update(logbookLifecycleUnitParameters);

		utils = new SedaUtilsFactory().create(metadataFactory);
		params.setCurrentStep("TEST");
		utils.setLifeCycleFinalEventStatusByStep(logbookLifecycleUnitParameters, StatusCode.OK);
	}

	@Test(expected = ProcessingException.class)
	public void givenLogbookClientNotFoundExceptionWhenSetLifeCycleFinalEventStatusThenThrowError()
			throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException,
			ProcessingException {
		LogbookParameters logbookLifecycleUnitParameters = createLogbookParametersInstance();

		PowerMockito.doThrow(new LogbookClientNotFoundException("LogbookClientNotFoundException"))
				.when(logbookLifeCycleClient).update(logbookLifecycleUnitParameters);

		utils = new SedaUtilsFactory().create(metadataFactory);
		params.setCurrentStep("TEST");
		utils.setLifeCycleFinalEventStatusByStep(logbookLifecycleUnitParameters, StatusCode.OK);
	}

	private LogbookParameters createLogbookParametersInstance() {
		LogbookParameters logbookLifecycleUnitParameters = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
		logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.objectIdentifier, OBJ);
		logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, OBJ);
		logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifier,
				GUIDFactory.newGUID().toString());
		logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
				SedaUtils.LIFE_CYCLE_EVENT_TYPE_PROCESS);
		logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventType,
				SedaUtils.UNIT_LIFE_CYCLE_CREATION_EVENT_TYPE);
		logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcome,
				LogbookOutcome.STARTED.toString());
		logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcomeDetail,
				LogbookOutcome.STARTED.toString());
		logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
				LogbookOutcome.STARTED.toString());

		return logbookLifecycleUnitParameters;
	}
}
