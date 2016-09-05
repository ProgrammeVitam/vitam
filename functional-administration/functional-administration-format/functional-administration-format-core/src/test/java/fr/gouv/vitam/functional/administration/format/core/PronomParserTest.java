package fr.gouv.vitam.functional.administration.format.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Test;

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
        assertTrue(jsonFileFormat.get(jsonFileFormat.size()-1).get("Name").toString().contains("RDF/XML"));
        assertEquals(jsonFileFormat.get(jsonFileFormat.size()-1).get("PUID").textValue(), "fmt/875");
        assertTrue(jsonFileFormat.get(jsonFileFormat.size()-1).get("MIMEType").toString().contains("application/rdf+xml"));
    }
    
    @Test(expected = FileNotFoundException.class)
    public void testPronomFormatFileKO() throws FileNotFoundException, FileFormatException {
        jsonFileFormat = PronomParser.getPronom(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO)));
    }
}
