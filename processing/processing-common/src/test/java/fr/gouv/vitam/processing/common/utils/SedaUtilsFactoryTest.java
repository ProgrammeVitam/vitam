package fr.gouv.vitam.processing.common.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.gouv.vitam.client.MetaDataClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

public class SedaUtilsFactoryTest {

    @Test
    public void givenSedaUtilsFactoryWhenCallingCreateWithoutParamsThenReturnClient() {
        SedaUtilsFactory factory = new SedaUtilsFactory();
        assertTrue(factory.create() instanceof SedaUtils);
    }
    
    @Test
    public void givenSedaUtilsFactoryWhenCallingCreateWithParamsThenReturnClient() {
        SedaUtilsFactory factory = new SedaUtilsFactory();
        assertTrue(factory.create(new WorkspaceClientFactory(), new MetaDataClientFactory()) instanceof SedaUtils);
    }

}
