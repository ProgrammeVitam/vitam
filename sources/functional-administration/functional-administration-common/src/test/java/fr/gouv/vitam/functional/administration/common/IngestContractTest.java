package fr.gouv.vitam.functional.administration.common;

import static org.junit.Assert.assertEquals;

import fr.gouv.vitam.common.model.administration.IngestContractCheckState;
import org.junit.Rule;
import org.junit.Test;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import java.util.HashSet;
import java.util.Set;

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
        final String id = GUIDFactory.newContractGUID(TENANT_ID).getId();
        String name = "aName";
        String description = "aDescription of the contract";
        String lastupdate = "10/12/2016";
        Set<String> archiveProfiles = new HashSet<>();
        archiveProfiles.add("FR_FAKE");
        String managementContractId = "MC-00001";
        contract
            .setId(id)
            .setName(name)
            .setDescription(description).setStatus(ActivationStatus.ACTIVE)
            .setCheckParentLink(IngestContractCheckState.AUTHORIZED)
            .setLastupdate(lastupdate)
            .setCreationdate(lastupdate)
            .setActivationdate(lastupdate)
            .setDeactivationdate(lastupdate)
            .setArchiveProfiles(archiveProfiles)
            .setManagementContractId(managementContractId);

        assertEquals(id, contract.getId());
        assertEquals(name, contract.getName());
        assertEquals(description, contract.getDescription());
        assertEquals(lastupdate, contract.getCreationdate());
        assertEquals(lastupdate, contract.getActivationdate());
        assertEquals(lastupdate, contract.getDeactivationdate());
        assertEquals(archiveProfiles, contract.getArchiveProfiles());
        assertEquals(managementContractId, contract.getManagementContractId());

    }

}
