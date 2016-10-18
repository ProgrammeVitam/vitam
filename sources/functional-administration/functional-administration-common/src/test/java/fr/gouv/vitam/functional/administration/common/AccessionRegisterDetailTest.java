package fr.gouv.vitam.functional.administration.common;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Map;

import org.bson.Document;
import org.junit.Test;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;

public class AccessionRegisterDetailTest {

    private static final String TEST = "test";
    @Test
    public void testConstructor() throws Exception {
        RegisterValueDetail initialValue = new RegisterValueDetail().setTotal(0).setDeleted(0).setRemained(0);
        String id = GUIDFactory.newGUID().getId();
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

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("accession-register.json");
        Map<String, Object> documentMap = JsonHandler.getMapFromInputStream(stream);
        documentMap.put("_id", id);
        register = new AccessionRegisterDetail(new Document(documentMap));
    }

}
