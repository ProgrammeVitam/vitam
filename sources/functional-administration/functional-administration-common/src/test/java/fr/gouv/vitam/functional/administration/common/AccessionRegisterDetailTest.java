package fr.gouv.vitam.functional.administration.common;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.javacrumbs.jsonunit.JsonAssert;
import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.AccessionRegisterStatus;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

public class AccessionRegisterDetailTest {

    private static final String TEST = "test";
    private static final String DATE = "2017-01-01";
    private static final Integer TENANT_ID = 0;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Test
    @RunWithCustomExecutor
    public void testConstructor() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RegisterValueDetailModel initialValue = new RegisterValueDetailModel().setIngested(0).setDeleted(0);
        final String id = GUIDFactory.newGUID().getId();
        List<String> ids = new ArrayList<>();
        ids.add(id);
        AccessionRegisterDetail register = new AccessionRegisterDetail()
            .setOriginatingAgency(id)
            .setId(id)
            .setObjectSize(initialValue)
            .setSubmissionAgency(TEST)
            .setArchivalAgreement(TEST)
            .setEndDate(DATE)
            .setStartDate(DATE)
            .setLastUpdate(DATE)
            .setStatus(AccessionRegisterStatus.STORED_AND_COMPLETED)
            .setTotalObjectGroups(initialValue)
            .setTotalObjects(initialValue)
            .setTotalUnits(initialValue)
            .setOperationIds(ids)
            .setAcquisitionInformation(TEST)
            .setLegalStatus(TEST);

        assertEquals(id, register.get("_id"));
        assertEquals(id, register.getOriginatingAgency());
        JsonAssert.assertJsonEquals(initialValue, register.getTotalObjectGroups());
        JsonAssert.assertJsonEquals(initialValue, register.getTotalObjectSize());
        JsonAssert.assertJsonEquals(initialValue, register.getTotalUnits());
        JsonAssert.assertJsonEquals(initialValue, register.getTotalObjects());
        assertEquals(LocalDateUtil.getFormattedDateForMongo(DATE), register.getEndDate());
        assertEquals(TEST, register.getAcquisitionInformation());
        assertEquals(TEST, register.getLegalStatus());

        final InputStream stream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream("accession-register.json");
        final Map<String, Object> documentMap = JsonHandler.getMapFromInputStream(stream);
        documentMap.put("_id", id);
        register = new AccessionRegisterDetail(new Document(documentMap));
    }

}
