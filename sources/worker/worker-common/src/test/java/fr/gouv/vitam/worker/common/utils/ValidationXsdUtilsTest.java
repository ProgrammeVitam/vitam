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
package fr.gouv.vitam.worker.common.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import fr.gouv.vitam.common.PropertiesUtils;

public class ValidationXsdUtilsTest {

    private static final String SEDA_VALIDATION_FILE = "seda-2.0-main.xsd";
    private static final String SEDA_VITAM_VALIDATION_FILE = "seda-vitam/seda-vitam-2.0-main.xsd";
    private static final String SEDA_FILE = "sip1.xml";
    private static final String SEDA_FILE2 = "manifestOK.xml";
    private static final String SEDA_WRONG_FILE = "wrong_sip1.xml";
    private static final String SEDA_ARCHIVE_TRANSFER_REPLY = "ATR_example.xml";
    private static final String SEDA_ARCHIVE_TRANSFER_REPLY_NOTVALID = "ATR_example_notvalid.xml";
    private static final String WRONG_SEDA_MISSING_TITLE = "manifestKoOnTitleMissing.xml";

    @Test
    public void givenXmlCorrectWhenCheckXsdThenReturnTrue() throws XMLStreamException, SAXException, IOException {
        final ValidationXsdUtils valid = new ValidationXsdUtils();
        assertTrue(
            valid.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_FILE), SEDA_VALIDATION_FILE));
        assertTrue(
            valid.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_FILE2), SEDA_VALIDATION_FILE));
    }

    @Test(expected = SAXException.class)
    public void givenXmlWithInvalidContentWhenCheckXsdThenThrowSAXException()
        throws XMLStreamException, SAXException, IOException {
        final ValidationXsdUtils valid = new ValidationXsdUtils();
        assertFalse(valid.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_WRONG_FILE),
            SEDA_VALIDATION_FILE));
    }

    @Test(expected = SAXException.class)
    public void givenXmlWithMissingTitleWhenCheckXsdThenThrowSAXException()
        throws XMLStreamException, SAXException, IOException {
        final ValidationXsdUtils valid = new ValidationXsdUtils();
        assertFalse(valid.checkWithXSD(PropertiesUtils.getResourceAsStream(WRONG_SEDA_MISSING_TITLE),
            SEDA_VALIDATION_FILE));
    }

    @Test(expected = FileNotFoundException.class)
    public void givenXmlNotFoundWhenCheckXsdThenRaiseAnException()
        throws XMLStreamException, SAXException, IOException {
        final ValidationXsdUtils valid = new ValidationXsdUtils();
        valid.checkWithXSD(new FileInputStream(PropertiesUtils.getResourceFile("")), SEDA_VALIDATION_FILE);
    }

    @Test
    public void givenXmlARTCorrectWhenCheckXsdThenReturnTrue() throws XMLStreamException, SAXException, IOException {
        final ValidationXsdUtils valid = new ValidationXsdUtils();
        assertTrue(
            valid.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_ARCHIVE_TRANSFER_REPLY),
                SEDA_VITAM_VALIDATION_FILE));
    }

    @Test(expected = SAXException.class)
    public void givenXmlARTNotValidWhenCheckXsdThenReturnFalse() throws XMLStreamException, SAXException, IOException {
        final ValidationXsdUtils valid = new ValidationXsdUtils();
        assertFalse(valid.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_ARCHIVE_TRANSFER_REPLY_NOTVALID),
            SEDA_VITAM_VALIDATION_FILE));
    }

}
