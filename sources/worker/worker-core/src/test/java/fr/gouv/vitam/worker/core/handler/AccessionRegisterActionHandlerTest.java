package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessingUri;
import fr.gouv.vitam.processing.common.model.UriPrefix;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({SedaUtils.class, SedaUtilsFactory.class})
public class AccessionRegisterActionHandlerTest {
    private static final String ARCHIVE_ID_TO_GUID_MAP = "ARCHIVE_ID_TO_GUID_MAP_obj.json";
    private static final String OBJECT_GROUP_ID_TO_GUID_MAP = "OBJECT_GROUP_ID_TO_GUID_MAP_obj.json";
    private static final String BDO_TO_BDO_INFO_MAP = "BDO_TO_BDO_INFO_MAP_obj.json";
    private static final String ATR_GLOBAL_SEDA_PARAMETERS = "globalSEDAParameters.json";
    private static final String FAKE_URL = "http://localhost:8080";
    AccessionRegisterActionHandler accessionRegisterHandler;
    private static final String HANDLER_ID = "ACCESSION_REGISTRATION";
    private HandlerIOImpl action;
    private GUID guid;
    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(SedaUtilsFactory.class);
        PowerMockito.mockStatic(SedaUtils.class);
        AdminManagementClientFactory.changeMode(null);
        guid = GUIDFactory.newGUID();
        params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
            .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        action = new HandlerIOImpl(guid.getId(), "workerId");
    }

    @After
    public void end() {
        action.partialClose();
    }
    
    @Test
    public void testResponseOK()
        throws Exception {
        SedaUtils sedaUtils = mock(SedaUtils.class);
        PowerMockito.when(SedaUtilsFactory.create(anyObject())).thenReturn(sedaUtils);
        when(sedaUtils.computeTotalSizeOfObjectsInManifest(anyObject())).thenReturn(new Long(1024));        
        AdminManagementClientFactory.getInstance().changeMode(null);        
        List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/ARCHIVE_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/BDO_TO_BDO_INFO_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "ATR/globalSEDAParameters.json")));
        action.addOutIOParameters(in);
        action.addOuputResult(0, PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP));
        action.addOuputResult(1, PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP));
        action.addOuputResult(2, PropertiesUtils.getResourceFile(BDO_TO_BDO_INFO_MAP));
        action.addOuputResult(3, PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS));
        action.reset();
        action.addInIOParameters(in);
        accessionRegisterHandler = new AccessionRegisterActionHandler();
        assertEquals(AccessionRegisterActionHandler.getId(), HANDLER_ID);
        final ItemStatus response = accessionRegisterHandler.execute(params, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

}
