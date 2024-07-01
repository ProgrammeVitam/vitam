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
package fr.gouv.vitam.common.stream;

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import org.apache.commons.io.input.ProxyInputStream;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.io.IOUtils.EOF;

/**
 * Input stream that checks input stream digest
 */
public class ExactDigestValidatorInputStream extends ProxyInputStream {

    private final String expectedDigest;
    private final Digest digest;

    public ExactDigestValidatorInputStream(final InputStream in, DigestType digestType, String expectedDigest) {
        super(null);
        this.digest = new Digest(digestType);
        super.in = this.digest.getDigestInputStream(in);
        this.expectedDigest = expectedDigest;
    }

    @Override
    protected void afterRead(int n) throws IOException {
        if (n == EOF) {
            String digestHex = this.digest.digestHex();
            if (!digestHex.equals(expectedDigest)) {
                throw new IOException("Invalid stream digest. Expected=" + expectedDigest + ", actual=" + digestHex);
            }
        }

        super.afterRead(n);
    }
}
