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
package fr.gouv.vitam.logbook.common.traceability;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class TimeStampService {

    private final DigestType digestType;

    public TimeStampService() {
        this(VitamConfiguration.getDefaultTimestampDigestType());
    }

    @VisibleForTesting
    public TimeStampService(DigestType digestType) {
        this.digestType = digestType;
    }

    public byte[] getDigestFrom(byte[]... toCompute) {
        Digest digest = new Digest(digestType);

        for (byte[] bytes : toCompute) {
            if (bytes != null) {
                digest.update(bytes);
            }
        }

        return digest.digest();
    }

    public TimeStampToken getTimeStampFrom(String timeStampAsString) throws IOException, TSPException {
        try (
            ByteArrayInputStream is = new ByteArrayInputStream(Base64.decode(timeStampAsString.getBytes(US_ASCII)));
            ASN1InputStream timeStampAsn1Stream = new ASN1InputStream(is)
        ) {
            ASN1Primitive timeStampAsn1Primitive = timeStampAsn1Stream.readObject();
            if (timeStampAsn1Primitive == null) {
                throw new IOException("Cannot get ASN1Primitive.");
            }

            TimeStampToken timeStampToken = new TimeStampResponse(
                timeStampAsn1Primitive.getEncoded()
            ).getTimeStampToken();
            if (timeStampToken.getTimeStampInfo().getNonce() != null) {
                throw new IOException("Timestamp token nonce cannot be filled.");
            }

            return timeStampToken;
        }
    }

    public byte[] getDigestAsBytes(String property) {
        return Objects.isNull(property) || property.equals("null") ? null : BaseXx.getFromBase64(property);
    }

    public DigestType getDigestType() {
        return digestType;
    }
}
