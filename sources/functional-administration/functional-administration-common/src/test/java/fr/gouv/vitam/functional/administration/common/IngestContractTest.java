package fr.gouv.vitam.functional.administration.common;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

public class IngestContractTest {

    
    
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    private static final Integer TENANT_ID = 0;

    @Test
    @RunWithCustomExecutor
    public void testConstructor() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        IngestContract contract = new IngestContract();
        final String id = GUIDFactory.newIngestContractGUID(TENANT_ID).getId();
        String name = "aName";
        String description = "aDescription of the contract";
        String lastupdate = "10/12/2016";
        contract
            .setId(id)
            .setName(name)
            .setDescription(description).setStatus(ContractStatus.ACTIVE)
            .setLastupdate(lastupdate)
            .setCreationdate(lastupdate)
            .setActivationdate(lastupdate).
            setDeactivationdate(lastupdate);

        assertEquals(id, contract.getId());
        assertEquals(name, contract.getName());
        assertEquals(description, contract.getDescription());
        assertEquals(lastupdate, contract.getCreationdate());
        assertEquals(lastupdate, contract.getActivationdate());
        assertEquals(lastupdate, contract.getDeactivationdate());
    }

}
