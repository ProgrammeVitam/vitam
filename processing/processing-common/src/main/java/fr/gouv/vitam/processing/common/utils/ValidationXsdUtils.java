/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.common.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
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

import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.util.XMLCatalogResolver;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;


/**
 * Class ValidationXsdUtils validate the file XML by XSD Method checkWithXSD return true if XSD validate the file XML,
 * else return false
 */
public class ValidationXsdUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ValidationXsdUtils.class);
    /**
     * Filename of the catalog file ; should be found in the classpath.
     */
    public static final String CATALOG_FILENAME = "catalog.xml";
    private XMLStreamReader xmlStreamReader;
    private Schema schema;

    /**
     * @param xmlFile the file to validate
     * @param xsdFile the xsd schema to validate with the file
     * @return true(if XSD validate the file XML)
     * @throws FileNotFoundException if the file is not found
     * @throws XMLStreamException if it couldnt create an xml stream reader
     * @throws SAXException if the file is not valid with the XSD, or the file is not an xml file
     * @throws IOException if the schema file could not be found
     */
    public boolean checkWithXSD(InputStream xmlFile, String xsdFile)
        throws SAXException, IOException, XMLStreamException {

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        try {
            xmlStreamReader = xmlInputFactory.createXMLStreamReader(xmlFile, "UTF-8");
            schema = getSchema(xsdFile);
            final Validator validator = schema.newValidator();
            validator.validate(new StAXSource(xmlStreamReader));
            return true;
        } catch (final SAXException e) {
            LOGGER.error("SAXException");
            throw e;
        } catch (final IOException e) {
            LOGGER.error("IOException");
            throw e;
        } catch (final XMLStreamException e) {
            LOGGER.error("XMLStreamException");
            throw e;
        }
    }

    private Schema getSchema(String xsdFile) throws SAXException {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // Load catalog to resolve external schemas even offline.
        final URL catalogUrl = this.getClass().getClassLoader().getResource(CATALOG_FILENAME);
        factory.setResourceResolver(new XMLCatalogResolver(new String[] {catalogUrl.toString()}, false));

        return factory.newSchema(this.getClass().getClassLoader().getResource(xsdFile));
    }
}
