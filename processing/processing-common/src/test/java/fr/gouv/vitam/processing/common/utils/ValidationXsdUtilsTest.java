package fr.gouv.vitam.processing.common.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.xml.sax.SAXException;

import fr.gouv.vitam.common.PropertiesUtils;

public class ValidationXsdUtilsTest {

    @Test
    public void givenXmlCorrectWhenCheckXsdThenReturnTrue() throws XMLStreamException, SAXException, IOException {
        ValidationXsdUtils valid = new ValidationXsdUtils();
        File xsd = PropertiesUtils.getResourcesFile("seda-2.0-main.xsd");
        assertTrue(valid.checkWithXSD(new FileInputStream(PropertiesUtils.getResourcesFile("sip1.xml")), xsd));
    }
    
    @Test
    public void givenXmlWithInvalidContentWhenCheckXsdThenReturnFalse() throws XMLStreamException, SAXException, IOException{
        ValidationXsdUtils valid = new ValidationXsdUtils();
        File xsd = PropertiesUtils.getResourcesFile("seda-2.0-main.xsd");
        assertFalse(valid.checkWithXSD(new FileInputStream(PropertiesUtils.getResourcesFile("wrong_sip1.xml")), xsd));
    }
    
    @Test(expected = FileNotFoundException.class)
    public void givenXmlNotFoundWhenCheckXsdThenRaiseAnException() throws XMLStreamException, SAXException, IOException{
        ValidationXsdUtils valid = new ValidationXsdUtils();
        File xsd = PropertiesUtils.getResourcesFile("seda-2.0-main.xsd");
        valid.checkWithXSD(new FileInputStream(PropertiesUtils.getResourcesFile("wrong_sip.xml")), xsd);
    }
    
}
