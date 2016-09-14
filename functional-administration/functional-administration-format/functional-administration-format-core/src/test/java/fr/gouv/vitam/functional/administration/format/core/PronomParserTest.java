package fr.gouv.vitam.functional.administration.format.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;

public class PronomParserTest {
    String FILE_TO_TEST = "FF-vitam.xml";
    String FILE_TO_TEST_KO = "FF-vitam-KO.xml";
    ArrayNode jsonFileFormat = null;

    @Test
    public void testPronomFormat() throws FileFormatException, FileNotFoundException {
        jsonFileFormat = PronomParser.getPronom(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST)));
        JsonNode node = jsonFileFormat.get(jsonFileFormat.size() - 1);
        assertTrue(node.get("Name").toString().contains("RDF/XML"));
        assertEquals(node.get("PUID").textValue(), "fmt/875");
        assertTrue(node.get("MIMEType").toString().contains("application/rdf+xml"));
        assertFalse(node.get("Alert").asBoolean());
        assertEquals(node.get("Group").textValue(), "");
        assertEquals(node.get("Comment").textValue(), "");
    }

    @Test(expected = FileNotFoundException.class)
    public void testPronomFormatFileKO() throws FileNotFoundException, FileFormatException {
        jsonFileFormat = PronomParser.getPronom(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO)));
    }
}
