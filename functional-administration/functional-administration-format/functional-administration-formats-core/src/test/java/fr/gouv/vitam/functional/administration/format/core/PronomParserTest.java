package fr.gouv.vitam.functional.administration.format.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.format.core.FileFormat;
import fr.gouv.vitam.functional.administration.format.core.PronomParser;

public class PronomParserTest {
    String FILE_TO_TEST = "FF-vitam.xml";
    String FILE_TO_TEST_KO = "FF-vitam-KO.xml";
    File pronomFile = null;
    File pronomFile_KO = null;
    FileFormat fileFormat = null;
    ArrayNode jsonFileFormat = null;
    PronomParser pronomParser;

    @Before
    public void setUp() throws FileNotFoundException {
        pronomFile = PropertiesUtils.findFile(FILE_TO_TEST);
    }

    @Test
    public void testPronomFormat() throws FileFormatException {
        jsonFileFormat = PronomParser.getPronom(pronomFile);
        assertTrue(jsonFileFormat.get(jsonFileFormat.size()-1).get("Name").toString().contains("RDF/XML"));
        assertEquals(jsonFileFormat.get(jsonFileFormat.size()-1).get("Puid").textValue(), "fmt/875");
        assertTrue(jsonFileFormat.get(jsonFileFormat.size()-1).get("MimeType").toString().contains("application/rdf+xml"));
    }
    
    @Test(expected = FileNotFoundException.class)
    public void testPronomFormatFileKO() throws FileNotFoundException, FileFormatException {
        pronomFile_KO = PropertiesUtils.findFile(FILE_TO_TEST_KO);
        jsonFileFormat = PronomParser.getPronom(pronomFile_KO);
    }
}
