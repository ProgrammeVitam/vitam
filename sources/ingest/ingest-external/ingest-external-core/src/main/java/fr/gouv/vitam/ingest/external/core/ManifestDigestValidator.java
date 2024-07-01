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
package fr.gouv.vitam.ingest.external.core;

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.digest.DigestTypeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ingest.external.core.exception.ManifestDigestValidationException;

import java.io.IOException;
import java.io.InputStream;

public class ManifestDigestValidator {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ManifestDigestValidator.class);

    public void checkManifestDigest(
        InputStream manifestInputStream,
        String manifestDigestAlgo,
        String manifestDigestValue
    ) throws ManifestDigestValidationException, IOException {
        if (manifestDigestAlgo == null && manifestDigestValue == null) {
            LOGGER.debug("No manifest log check required");
            return;
        }

        if (manifestDigestAlgo == null) {
            throw new ManifestDigestValidationException("Missing manifest algorithm");
        }

        if (manifestDigestValue == null) {
            throw new ManifestDigestValidationException("Missing manifest digest");
        }

        DigestType digestType;
        try {
            digestType = DigestType.fromValue(manifestDigestAlgo);
        } catch (DigestTypeException e) {
            throw new ManifestDigestValidationException("Invalid manifest digest algorithm " + manifestDigestAlgo, e);
        }

        Digest digest = new Digest(digestType);
        digest.update(manifestInputStream);
        if (digest.digestHex().equals(manifestDigestValue)) {
            LOGGER.debug("Manifest digest validation succeeded");
        } else {
            throw new ManifestDigestValidationException(
                "Manifest digest validation failed. Expected digest is '" +
                manifestDigestValue +
                "', actual digest '" +
                digest.digestHex() +
                "' (" +
                digestType.getName() +
                ")"
            );
        }
    }
}
