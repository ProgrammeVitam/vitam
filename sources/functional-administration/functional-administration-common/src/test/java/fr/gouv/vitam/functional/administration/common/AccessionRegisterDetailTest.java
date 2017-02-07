package fr.gouv.vitam.functional.administration.common;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Map;

import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.model.RegisterValueDetailModel;

public class AccessionRegisterDetailTest {

    private static final String TEST = "test";
    private static final Integer TENANT_ID = 0;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Test
    @RunWithCustomExecutor
    public void testConstructor() throws Exception {
    	VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RegisterValueDetailModel initialValue = new RegisterValueDetailModel(0, 0, 0, null);
        final String id = GUIDFactory.newGUID().getId();
        AccessionRegisterDetail register = new AccessionRegisterDetail()
            .setOriginatingAgency(id)
            .setId(id)
            .setObjectSize(initialValue)
            .setSubmissionAgency(TEST)
            .setEndDate(TEST)
            .setStartDate(TEST)
            .setLastUpdate(TEST)
            .setStatus(AccessionRegisterStatus.STORED_AND_COMPLETED)
            .setTotalObjectGroups(initialValue)
            .setTotalObjects(initialValue)
            .setTotalUnits(initialValue);

        assertEquals(id, register.get("_id"));
        assertEquals(id, register.getOriginatingAgency());
        assertEquals(initialValue, register.getTotalObjectGroups());
        assertEquals(initialValue, register.getTotalObjectSize());
        assertEquals(initialValue, register.getTotalUnits());
        assertEquals(initialValue, register.getTotalObjects());
        assertEquals(TEST, register.getEndDate());

        final InputStream stream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("accession-register.json");
        final Map<String, Object> documentMap = JsonHandler.getMapFromInputStream(stream);
        documentMap.put("_id", id);
        register = new AccessionRegisterDetail(new Document(documentMap));
    }

}
