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

package fr.gouv.vitam.worker.common.utils;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.common.xml.XsdValidator;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SedaXsdValidatorProviderTest {

    private static final String SEDA_FILE = "sip1.xml";
    private static final String SEDA_FILE2 = "manifestOK.xml";
    private static final String SEDA_FILE_WITH_AGENT = "manifestContainsAgentOK.xml";
    private static final String SEDA_FILE_WITH_PERSISTENT_IDENTIFIERS =
        "manifestContainsPersistentIdentifierOK-2.3.xml";
    private static final String SEDA_FILE_WITH_PERSISTENT_IDENTIFIERS_2_2 =
        "manifestContainsPersistentIdentifierOK-2.2.xml";
    private static final String SEDA_FILE_DATA_OBJ_REF = "manifestWithDataObjectGroupExistingReferenceId.xml";
    private static final String SEDA_WRONG_FILE = "wrong_sip1.xml";
    private static final String SEDA_ARCHIVE_TRANSFER_REPLY = "ATR_example.xml";
    private static final String SEDA_ARCHIVE_TRANSFER_REPLY_NOTVALID = "ATR_example_notvalid.xml";
    private static final String WRONG_SEDA_MISSING_TITLE = "manifestKoOnTitleMissing.xml";

    private static final String SEDA_UPDATE_VALID = "manifestOK_Update.xml";
    private static final String SEDA_UPDATE_NOT_VALID = "manifestKO_UpdateMissingSystemId.xml";
    private static final String SEDA_UPDATE_NOT_VALID_2 = "manifestKO_UpdateUnreferencedAU.xml";

    @Test
    public void givenXmlCorrectWhenCheckXsdThenReturnTrue() {
        XsdValidator seda21Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_1);
        XsdValidator seda22Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_2);
        XsdValidator seda23Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_3);
        assertThatCode(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(SEDA_FILE))
        ).doesNotThrowAnyException();
        assertThatCode(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(SEDA_FILE2))
        ).doesNotThrowAnyException();
        assertThatCode(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(SEDA_FILE_WITH_AGENT))
        ).doesNotThrowAnyException();
        assertThatCode(
            () -> seda22Validator.validate(PropertiesUtils.getResourceFile(SEDA_FILE_WITH_PERSISTENT_IDENTIFIERS_2_2))
        ).doesNotThrowAnyException();
        assertThatCode(
            () -> seda23Validator.validate(PropertiesUtils.getResourceFile(SEDA_FILE_WITH_PERSISTENT_IDENTIFIERS))
        ).doesNotThrowAnyException();
    }

    @Test
    public void givenXmlWithInvalidContentWhenCheckXsdThenThrowSAXException() {
        XsdValidator seda21Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_1);
        assertThatThrownBy(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(SEDA_WRONG_FILE))
        ).isInstanceOf(SAXException.class);
    }

    @Test
    public void givenXmlWithMissingTitleWhenCheckXsdThenValidationOk() {
        XsdValidator seda21Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_1);
        assertThatCode(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(WRONG_SEDA_MISSING_TITLE))
        ).doesNotThrowAnyException();
    }

    @Test
    public void givenXmlARTCorrectWhenCheckXsdThenReturnTrue() {
        XsdValidator seda21Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_1);
        assertThatCode(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(SEDA_ARCHIVE_TRANSFER_REPLY))
        ).doesNotThrowAnyException();
    }

    @Test
    public void givenXmlARTNotValidWhenCheckXsdThenReturnFalse() {
        XsdValidator seda21Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_1);
        // test an ATR xml file missing a MessageIdentifier Tag in it
        assertThatThrownBy(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(SEDA_ARCHIVE_TRANSFER_REPLY_NOTVALID))
        ).isInstanceOf(SAXException.class);
    }

    @Test
    public void givenXmlCorrectWithAddLinkWhenCheckXsdThenReturnTrue() {
        XsdValidator seda21Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_1);
        assertThatCode(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile("SIP_Add_Link.xml"))
        ).doesNotThrowAnyException();
    }

    @Test
    public void givenXmlCorrectWithUpdateWhenCheckXsdThenReturnTrue() {
        XsdValidator seda21Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_1);
        assertThatCode(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(SEDA_UPDATE_VALID))
        ).doesNotThrowAnyException();
    }

    @Test
    public void givenXmlUpdateWithoutSystemIdWhenCheckXsdThenThrowException() {
        XsdValidator seda21Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_1);
        assertThatThrownBy(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(SEDA_UPDATE_NOT_VALID))
        ).isInstanceOf(SAXException.class);
    }

    @Test
    public void givenXmlUpdateDeleteRefUnknownArchiveUnitWhenCheckXsdThenThrowException() {
        XsdValidator seda21Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_1);
        assertThatThrownBy(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(SEDA_UPDATE_NOT_VALID_2))
        ).isInstanceOf(SAXException.class);
    }

    @Test
    public void givenXmlWithDataObjectGroupExistingReferenceIdWhenCheckXsdThenOK() {
        XsdValidator seda21Validator = SedaXsdValidatorProvider.getInstance()
            .getValidator(SupportedSedaVersions.SEDA_2_1);
        assertThatCode(
            () -> seda21Validator.validate(PropertiesUtils.getResourceFile(SEDA_FILE_DATA_OBJ_REF))
        ).doesNotThrowAnyException();
    }
}
