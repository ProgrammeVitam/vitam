package fr.vitam.plugin.custom;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class HelloWorldPluginTest {


    @Test
    public void testExecuteThenOK() {
        HelloWorldPlugin helloWorldPlugin = new HelloWorldPlugin();
        WorkerParameters parameters = WorkerParametersFactory.newWorkerParameters();
        HandlerIOImpl handlerIO = new HandlerIOImpl(GUIDFactory.newGUID().getId(), "workerId", Lists.newArrayList("objectId"));
        handlerIO.addInIOParameters(Lists.newArrayList((new IOParameter()
                .setUri(new ProcessingUri(UriPrefix.VALUE, "HelloWorldPluginTest")))));
        handlerIO.setCurrentObjectId("objectId");
        ItemStatus itemStatus = helloWorldPlugin.execute(parameters, handlerIO);
        assertTrue(itemStatus != null);
        assertTrue(itemStatus.getEvDetailData().contains("var_name"));
        assertTrue(itemStatus.getEvDetailData().contains("HelloWorldPluginTest"));
    }

    @Test
    public void testExecuteListThenOK() throws ContentAddressableStorageServerException, ProcessingException {
        HelloWorldPlugin helloWorldPlugin = new HelloWorldPlugin();
        WorkerParameters parameters = WorkerParametersFactory.newWorkerParameters();
        parameters.setObjectNameList( Lists.newArrayList("objectId"));
        HandlerIOImpl handlerIO = new HandlerIOImpl(GUIDFactory.newGUID().getId(), "workerId", parameters.getObjectNameList());
        handlerIO.addInIOParameters(Lists.newArrayList((new IOParameter()
                .setUri(new ProcessingUri(UriPrefix.VALUE, "HelloWorldPluginTest")))));
        handlerIO.setCurrentObjectId("objectId");
        List<ItemStatus> itemStatus = helloWorldPlugin.executeList(parameters, handlerIO);
        assertTrue(itemStatus != null);
        assertTrue(itemStatus.size() == 1);
        assertTrue(itemStatus.get(0).getEvDetailData().contains("var_name"));
        assertTrue(itemStatus.get(0).getEvDetailData().contains("HelloWorldPluginTest"));
    }
}