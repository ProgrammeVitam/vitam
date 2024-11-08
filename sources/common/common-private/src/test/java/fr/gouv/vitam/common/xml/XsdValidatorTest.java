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

import fr.gouv.vitam.common.PropertiesUtils;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XsdValidatorTest {

    private static final String XML_FILE_WITH_XXE = "xml/xml_document_with_xxe.xml";
    private static final String XML_FILE_OK = "xml/xml_document_ok.xml";
    private static final String XML_FILE_KO = "xml/xml_document_ko.xml";
    private static final String XSD_FILE_WITH_XXE = "xml/xsd_with_xxe.xml";
    private static final String XSD_FILE_OK = "xml/xsd_ok.xml";
    public static final String XSD_FILE_KO = "xml/xsd_ko.xml";
    public static final String NON_XML_FILE = "xml/non_xml_file.txt";

    @Test
    public void testCreateValidatorWithValidXsdThenOK() {
        assertThatCode(() -> new XsdValidator(PropertiesUtils.getResourceFile(XSD_FILE_OK))).doesNotThrowAnyException();
    }

    @Test
    public void testCreateValidatorWithInvalidXsdThenKO() {
        assertThatThrownBy(() -> new XsdValidator(PropertiesUtils.getResourceFile(XSD_FILE_KO))).isInstanceOf(
            SAXParseException.class
        );
    }

    @Test
    public void testCreateValidatorWithBadFormatXmlXsdThenKO() {
        assertThatThrownBy(() -> new XsdValidator(PropertiesUtils.getResourceFile(NON_XML_FILE))).isInstanceOf(
            SAXParseException.class
        );
    }

    @Test
    public void testCreateValidatorWithXsdFileWithXXEThenRejected() {
        assertThatThrownBy(() -> new XsdValidator(PropertiesUtils.getResourceFile(XSD_FILE_WITH_XXE)))
            .isInstanceOf(SAXException.class)
            .hasMessageContaining("DOCTYPE is disallowed when the feature");
    }

    @Test
    public final void shouldXsdValidationOfValidXmlFileThenOK() throws Exception {
        XsdValidator xsdValidator = new XsdValidator(PropertiesUtils.getResourceFile(XSD_FILE_OK));

        assertThatCode(
            () -> xsdValidator.validate(PropertiesUtils.getResourceFile(XML_FILE_OK))
        ).doesNotThrowAnyException();
    }

    @Test
    public final void shouldXsdValidationOfXmlFileWithExternalEntityThenKO() throws Exception {
        XsdValidator xsdValidator = new XsdValidator(PropertiesUtils.getResourceFile(XSD_FILE_OK));

        assertThatThrownBy(() -> xsdValidator.validate(PropertiesUtils.getResourceFile(XML_FILE_WITH_XXE)))
            .isInstanceOf(InvalidXmlException.class)
            .hasMessageContaining("DOCTYPE is disallowed");
    }

    @Test
    public final void shouldXsdValidationOfInvalidXmlFileThenKO() throws Exception {
        XsdValidator xsdValidator = new XsdValidator(PropertiesUtils.getResourceFile(XSD_FILE_OK));

        assertThatThrownBy(
            () -> xsdValidator.validate(PropertiesUtils.getResourceFile(XML_FILE_KO))
        ).hasMessageContaining("It must be a value from the enumeration");
    }

    @Test
    public final void shouldXsdValidationOfNonXmlFileThenKO() throws Exception {
        XsdValidator xsdValidator = new XsdValidator(PropertiesUtils.getResourceFile(XSD_FILE_OK));

        assertThatThrownBy(() -> xsdValidator.validate(PropertiesUtils.getResourceFile(NON_XML_FILE))).isInstanceOf(
            InvalidXmlException.class
        );
    }
}
