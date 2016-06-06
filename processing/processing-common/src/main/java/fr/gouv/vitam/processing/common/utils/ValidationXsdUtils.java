package fr.gouv.vitam.processing.common.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.util.XMLCatalogResolver;

/**
 * Class ValidationXsdUtils validate the file XML by XSD Method checkWithXSD return true if XSD validate the file XML,
 * else return false
 */
public class ValidationXsdUtils {

    static final Logger LOGGER = LoggerFactory.getLogger(ValidationXsdUtils.class);
    /**
     * Filename of the catalog file ; should be found in the classpath.
     */
    public static final String CATALOG_FILENAME = "catalog.xml";
    private XMLStreamReader xmlStreamReader;
    private Schema schema;

    /**
     * @param xmlFile
     * @param xsdFile
     * @return true(if XSD validate the file XML), false(if not)
     * @throws FileNotFoundException
     * @throws XMLStreamException
     * @throws SAXException
     */
    public boolean checkWithXSD(InputStream xmlFile, File xsdFile)
        throws FileNotFoundException, XMLStreamException, SAXException {

        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlStreamReader = xmlInputFactory.createXMLStreamReader(xmlFile, "UTF-8");
        schema = getSchema(xsdFile);

        try {
            Validator validator = schema.newValidator();
            validator.validate(new StAXSource(xmlStreamReader));
            return true;
        } catch (Exception e) {
            LOGGER.info("XSD cannnot validate the file XML");
            return false;
        }
    }

    private Schema getSchema(File xsdFile) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL catalogUrl = this.getClass().getClassLoader().getResource(CATALOG_FILENAME); // KWA : load catalog to
                                                                                         // resolve external schemas,
                                                                                         // even offline.
        factory.setResourceResolver(new XMLCatalogResolver(new String[] {catalogUrl.toString()}, false));
        return factory.newSchema(xsdFile);
    }
}
