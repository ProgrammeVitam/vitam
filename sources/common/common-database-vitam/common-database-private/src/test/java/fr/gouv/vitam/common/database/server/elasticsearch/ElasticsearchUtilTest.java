package fr.gouv.vitam.common.database.server.elasticsearch;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.PropertyUtils;

public class ElasticsearchUtilTest {

    private static final String FILE_JSON = "/test.json";
    private static final String mapping = 
        "{\n" +
        "  \"properties\": {\n" +
        "    \"Tenant\": {\n" +
        "      \"type\": \"string\"\n" +
        "    }\n" +
        "  }\n" +
        "}";
    
    @Test
    public void test() throws IOException {
        String mappingTrans = ElasticsearchUtil.transferJsonToMapping(PropertyUtils.class.getResourceAsStream(FILE_JSON));
        System.out.println(mappingTrans);
        System.out.println(mapping);
        assertEquals(mapping, mappingTrans);
    }

}
