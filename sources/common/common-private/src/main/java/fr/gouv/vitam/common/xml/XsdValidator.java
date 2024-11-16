/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */

package fr.gouv.vitam.common.xml;

import org.apache.xerces.util.XMLCatalogResolver;
import org.xml.sax.SAXException;

import javax.annotation.concurrent.ThreadSafe;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

@ThreadSafe
public class XsdValidator {

    /**
     * Filename of the catalog file ; should be found in the classpath.
     */
    private static final String CATALOG_FILENAME = "xsd_validation/catalog.xml";

    private final Schema schema;

    public XsdValidator(String internalXsdResourceFile) throws SAXException {
        // No need for explicit XXE validation of resource file (already handled by the schema factory)
        this.schema = getSchemaFactory()
            .newSchema(XsdValidator.class.getClassLoader().getResource(internalXsdResourceFile));
    }

    public XsdValidator(File xsdFile) throws SAXException, IOException {
        // No need for explicit XXE validation of xsd file (already handled by the schema factory)
        this.schema = getSchemaFactory().newSchema(xsdFile);
    }

    public void validate(File xmlFile) throws SAXException, IOException {
        // Manually validate xml files before using xsd validator
        SecureXMLFactoryUtils.validateXmlFile(xmlFile);

        @SuppressWarnings("deprecation")
        final Validator validator = SecureXMLFactoryUtils.internalCreateNotThatSecureSchemaValidator(this.schema);

        // Validate the XML file against the XSD
        validator.validate(new StreamSource(xmlFile));
    }

    public Schema getSchema() {
        return this.schema;
    }

    private SchemaFactory getSchemaFactory() {
        @SuppressWarnings("deprecation")
        SchemaFactory factory = SecureXMLFactoryUtils.internalCreateNotThatSecureXsdSchemaFactory();

        // Load catalog to resolve external schemas even offline.
        final URL catalogUrl = XsdValidator.class.getClassLoader().getResource(CATALOG_FILENAME);
        factory.setResourceResolver(
            new XMLCatalogResolver(new String[] { Objects.requireNonNull(catalogUrl).toString() }, false)
        );
        return factory;
    }
}
