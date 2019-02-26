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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.xml.ValidationXsdUtils;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ValidationXsdUtilsTest {

    private static final String SEDA_VITAM_VALIDATION_FILE = "seda-vitam-2.1-main.xsd";
    private static final String SEDA_VALIDATION_FILE = "SEDA_2-1_fast_dl/seda-2.1-main.xsd";
    private static final String SEDA_FILE = "sip1.xml";
    private static final String SEDA_FILE2 = "manifestOK.xml";
    private static final String SEDA_FILE_DATA_OBJ_REF = "manifestWithDataObjectGroupExistingReferenceId.xml";
    private static final String SEDA_WRONG_FILE = "wrong_sip1.xml";
    private static final String SEDA_ARCHIVE_TRANSFER_REPLY = "ATR_example.xml";
    private static final String SEDA_ARCHIVE_TRANSFER_REPLY_NOTVALID = "ATR_example_notvalid.xml";
    private static final String WRONG_SEDA_MISSING_TITLE = "manifestKoOnTitleMissing.xml";

    private static final String SEDA_UPDATE_VALID = "manifestOK_Update.xml";
    private static final String SEDA_UPDATE_NOT_VALID = "manifestKO_UpdateMissingSystemId.xml";
    private static final String SEDA_UPDATE_NOT_VALID_2 = "manifestKO_UpdateUnreferencedAU.xml";

    @Test
    public void givenXmlCorrectWhenCheckXsdThenReturnTrue() throws XMLStreamException, SAXException, IOException {
        assertTrue(
            ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_FILE), SEDA_VITAM_VALIDATION_FILE));
        assertTrue(
            ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_FILE2), SEDA_VITAM_VALIDATION_FILE));        
    }

    @Test(expected = SAXException.class)
    public void givenXmlWithInvalidContentWhenCheckXsdThenThrowSAXException()
        throws XMLStreamException, SAXException, IOException {
        assertFalse(ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_WRONG_FILE),
            SEDA_VITAM_VALIDATION_FILE));
    }

    @Test
    public void givenXmlWithMissingTitleWhenCheckXsdThenValidationOk()
        throws XMLStreamException, SAXException, IOException {
        assertTrue(ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream(WRONG_SEDA_MISSING_TITLE),
            SEDA_VITAM_VALIDATION_FILE));
    }

    @Test(expected = FileNotFoundException.class)
    public void givenXmlNotFoundWhenCheckXsdThenRaiseAnException()
        throws XMLStreamException, SAXException, IOException {
        ValidationXsdUtils.checkWithXSD(new FileInputStream(PropertiesUtils.getResourceFile("")), SEDA_VITAM_VALIDATION_FILE);
    }

    @Test
    public void givenXmlARTCorrectWhenCheckXsdThenReturnTrue() throws XMLStreamException, SAXException, IOException {
        assertTrue(
            ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_ARCHIVE_TRANSFER_REPLY),
                SEDA_VALIDATION_FILE));
    }

    @Test(expected = SAXException.class)
    public void givenXmlARTNotValidWhenCheckXsdThenReturnFalse() throws XMLStreamException, SAXException, IOException {
        // test an ATR xml file missing a MessageIdentifier Tag in it 
        ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_ARCHIVE_TRANSFER_REPLY_NOTVALID),
            SEDA_VALIDATION_FILE);
    }

    @Test
    public void givenXmlCorrectWithAddLinkWhenCheckXsdThenReturnTrue()
        throws XMLStreamException, SAXException, IOException {
        assertTrue(
            ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream("SIP_Add_Link.xml"), SEDA_VITAM_VALIDATION_FILE));
    }

    @Test
    public void givenXmlCorrectWithUpdateWhenCheckXsdThenReturnTrue()
        throws XMLStreamException, SAXException, IOException {
        assertTrue(
            ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_UPDATE_VALID), SEDA_VITAM_VALIDATION_FILE));
    }

    @Test(expected = SAXException.class)
    public void givenXmlUpdateWithoutSystemIdWhenCheckXsdThenThrowException()
        throws XMLStreamException, SAXException, IOException {
        ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_UPDATE_NOT_VALID), SEDA_VITAM_VALIDATION_FILE);
    }
    
    @Test(expected = SAXException.class)
    public void givenXmlUpdateDeleteRefUnknownArchiveUnitWhenCheckXsdThenThrowException()
        throws XMLStreamException, SAXException, IOException {
        ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_UPDATE_NOT_VALID_2), SEDA_VITAM_VALIDATION_FILE);
    }
    
    
    @Test
    public void givenXmlWithDataObjectGroupExistingReferenceIdWhenCheckXsdThenOK()
        throws XMLStreamException, SAXException, IOException {
        assertTrue(
            ValidationXsdUtils.checkWithXSD(PropertiesUtils.getResourceAsStream(SEDA_FILE_DATA_OBJ_REF),
                SEDA_VITAM_VALIDATION_FILE));
    }
    
    @Test
    public void testValidRNGOK()
        throws Exception {
        try {
            ValidationXsdUtils.checkFileRNG(PropertiesUtils.getResourceAsStream("manifest_ok_profile.xml"), 
                PropertiesUtils.getResourceFile("Profil20.rng")); 
        } catch (SAXException e) {
            fail("should be valid");
        }

    }
}
