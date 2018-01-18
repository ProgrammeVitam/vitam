package fr.gouv.vitam.common.database.index.model;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;

public class IndexationResultTest {

    public static final String INDEX_RESULT_JSON = "indexationResult.json";

    @Test
    public void test() throws Exception {
        IndexationResult result = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(INDEX_RESULT_JSON),
            IndexationResult.class);
        Assert.assertNotNull(result);
        Assert.assertEquals("collection_name", result.getCollectionName());
        List<IndexOK> okList = result.getIndexOK();
        Assert.assertNotNull(okList);
        Assert.assertFalse(okList.isEmpty());
        Assert.assertEquals(2, okList.size());
        Assert.assertEquals("collection_name_0_date", okList.get(0).getIndexName());
        Assert.assertEquals((Integer) 0, okList.get(0).getTenant());
        Assert.assertEquals(2, okList.size());
        Assert.assertEquals("collection_name_2_date", okList.get(1).getIndexName());
        Assert.assertEquals((Integer) 2, okList.get(1).getTenant());

        List<IndexKO> koList = result.getIndexKO();
        Assert.assertNotNull(koList);
        Assert.assertFalse(koList.isEmpty());
        Assert.assertEquals(1, koList.size());
        Assert.assertEquals("collection_name_1_date", koList.get(0).getIndexName());
        Assert.assertEquals((Integer) 1, koList.get(0).getTenant());
        Assert.assertEquals("failed", koList.get(0).getMessage());
    }
}
