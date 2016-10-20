package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.api.HandlerIO;

public class AccessionRegisterActionHandlerTest {
    private static final String ARCHIVE_ID_TO_GUID_MAP = "ARCHIVE_ID_TO_GUID_MAP_obj.json";
    private static final String OBJECT_GROUP_ID_TO_GUID_MAP = "OBJECT_GROUP_ID_TO_GUID_MAP_obj.json";
    private static final String BDO_TO_BDO_INFO_MAP = "BDO_TO_BDO_INFO_MAP_obj.json";
    private static final String ATR_GLOBAL_SEDA_PARAMETERS = "globalSEDAParameters.json";
    private static final String FAKE_URL = "http://localhost:8080";
    AccessionRegisterActionHandler accessionRegisterHandler;
    private static final String HANDLER_ID = "ACCESSION_REGISTRATION";
    private HandlerIO action;
    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
            .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

    @Before
    public void setUp() throws Exception {
        action = new HandlerIO("containerName");
    }
    
    @Test
    public void testResponseOK()
        throws Exception {
        AdminManagementClientFactory.getInstance().changeMode(null);        
        action.addInput(PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP));
        action.addInput(PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP));
        action.addInput(PropertiesUtils.getResourceFile(BDO_TO_BDO_INFO_MAP));
        action.addInput(PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS));
        accessionRegisterHandler = new AccessionRegisterActionHandler();
        assertEquals(AccessionRegisterActionHandler.getId(), HANDLER_ID);
        final CompositeItemStatus response = accessionRegisterHandler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

}
