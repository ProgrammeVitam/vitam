/**
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
 */
package fr.gouv.vitam.ingest.external.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;

/**
 * Archive Transfer Reply KO from primary Ingest external steps
 */
public class AtrKoBuilder {

    private static final String ATR_KO_DEFAULT_XML = "ATR_KO_DEFAULT.xml";
    private static final String DATE = "#MADATE#";
    private static final String MESSAGE_IDENTIFIER = "#MESSAGE_IDENTIFIER#";
    private static final String ARCHIVAL_AGENCY = "#ARCHIVAL_AGENCY#";
    private static final String TRANSFERRING_AGENCY = "#TRANSFERRING_AGENCY#";
    private static final String COMMENT = "#COMMENT#";
    private static final String OUTCOME_DETAIL_MESSAGE = "#OUTCOME_DETAIL_MESSAGE#";

    /**
     * private
     */
    private AtrKoBuilder() {
        // Empty
    }

    /**
     * To generate a default ATR KO from Ingest External on AV or MimeType checks.
     * 
     * @param messageIdentifier
     * @param archivalAgency
     * @param transferringAgency
     * @param detail
     * @return the corresponding InputStream with the ATR KO in XML format
     * @throws IngestExternalException 
     */
    public static InputStream buildAtrKo(String messageIdentifier, String archivalAgency, String transferringAgency,
        String detail) throws IngestExternalException {
        String xmlDefault;
        try {
            xmlDefault = FileUtil.readInputStream(PropertiesUtils.getResourceAsStream(ATR_KO_DEFAULT_XML));
        } catch (IOException e) {
            throw new IngestExternalException(e);
        }
        String finalXml =
            xmlDefault.replace(DATE, LocalDateUtil.now().toString()).replace(MESSAGE_IDENTIFIER, messageIdentifier)
                .replace(ARCHIVAL_AGENCY, archivalAgency).replace(TRANSFERRING_AGENCY, transferringAgency)
                .replace(COMMENT, detail).replace(OUTCOME_DETAIL_MESSAGE, detail);
        return new ByteArrayInputStream(finalXml.getBytes(CharsetUtils.UTF8));
    }
}
