package fr.gouv.vitam.processing.common.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.xml.sax.SAXException;

import fr.gouv.vitam.common.PropertiesUtils;

public class ValidationXsdUtilsTest {
    
    private static final String SEDA_VALIDATION_FILE = "seda-2.0-main.xsd";
    private static final String SEDA_FILE = "sip1.xml";
    private static final String SEDA_WRONG_FILE = "wrong_sip1.xml";

    @Test
    public void givenXmlCorrectWhenCheckXsdThenReturnTrue() throws XMLStreamException, SAXException, IOException {
        ValidationXsdUtils valid = new ValidationXsdUtils();
        assertTrue(valid.checkWithXSD(new FileInputStream(PropertiesUtils.getResourcesFile(SEDA_FILE)), SEDA_VALIDATION_FILE));
    }
    
    @Test(expected = SAXException.class)
    public void givenXmlWithInvalidContentWhenCheckXsdThenThrowSAXException() throws XMLStreamException, SAXException, IOException{
        ValidationXsdUtils valid = new ValidationXsdUtils();
        assertFalse(valid.checkWithXSD(new FileInputStream(PropertiesUtils.getResourcesFile(SEDA_WRONG_FILE)), SEDA_VALIDATION_FILE));
    }
    
    @Test(expected = FileNotFoundException.class)
    public void givenXmlNotFoundWhenCheckXsdThenRaiseAnException() throws XMLStreamException, SAXException, IOException{
        ValidationXsdUtils valid = new ValidationXsdUtils();
        valid.checkWithXSD(new FileInputStream(PropertiesUtils.getResourcesFile("")), SEDA_VALIDATION_FILE);
    }
    
}
