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
/*
 * Copyright Paul Merlin 2011 (Apache Licence v2.0)
 */
package fr.gouv.vitam.common.auth.core.authc;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.shiro.crypto.hash.Sha256Hash;

import java.security.cert.CertificateEncodingException;

/**
 * Based on work: Copyright Paul Merlin 2011 (Apache Licence v2.0)
 */
public class X509CredentialsSha256Matcher extends AbstractX509CredentialsMatcher {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(X509CredentialsSha256Matcher.class);

    @Override
    public boolean doX509CredentialsMatch(X509AuthenticationToken token, X509AuthenticationInfo info) {
        try {

            // FIXME : Arrays.equals !!!
            final String clientCertSha256 = new Sha256Hash(token.getX509Certificate().getEncoded()).toHex();
            final String subjectCertSha256 = new Sha256Hash(info.getX509Certificate().getEncoded()).toHex();

            final boolean match = clientCertSha256.equals(subjectCertSha256);

            if (match) {
                LOGGER.debug("Client certificate Sha256 hash match the one provided by the Realm, will return true");
            } else {
                LOGGER.debug(
                    "Client certificate Sha256 hash ({}) do not match the one provided by the Realm ({}), will return false",
                    clientCertSha256, subjectCertSha256);
            }

            return match;

        } catch (final CertificateEncodingException ex) {
            LOGGER.debug("Unable to do credentials matching", ex);
            return false;
        }

    }

}
