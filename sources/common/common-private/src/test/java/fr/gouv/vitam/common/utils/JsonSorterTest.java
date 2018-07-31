package fr.gouv.vitam.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Test;

import java.util.Arrays;

public class JsonSorterTest {

    @Test
    public void testSortJsonEntriesByKeys() throws Exception {

        JsonNode data = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream("json_sorter/input.json"));
        JsonSorter.sortJsonEntriesByKeys(data, Arrays.asList("field1", "field2"));

        JsonNode expected =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream("json_sorter/expected.json"));

        JsonAssert.assertJsonEquals(JsonHandler.unprettyPrint(data), JsonHandler.unprettyPrint(expected));
    }
}
