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
package fr.gouv.vitam.common.timestamp;

import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.TimeStampException;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;

/**
 * Generate a time stamp token for a specific hash
 */
public class TimestampGenerator {

    private final TimeStampSignature timeStampSignature;

    /**
     * Constructor
     *
     * @param timeStampSignature
     */
    public TimestampGenerator(TimeStampSignature timeStampSignature) {
        this.timeStampSignature = timeStampSignature;
    }

    /**
     * @param hash the hash to timestamp
     * @param digestType algorithm use to generate the hash
     * @param nonce unique id to secure a timestamp request, can be null
     * @return timestamp token
     * @throws TimeStampException
     */
    public byte[] generateToken(byte[] hash, DigestType digestType, BigInteger nonce) throws TimeStampException {
        final TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        reqGen.setCertReq(true);
        final TimeStampRequest request = reqGen.generate(digestToOid(digestType), hash, nonce);

        try {
            TimeStampResponse timeStampResponse = timeStampSignature.sign(request);
            // lets validate de the timestamp
            timeStampResponse.validate(request);
            return timeStampResponse.getEncoded();
        } catch (OperatorCreationException | TSPException | CertificateEncodingException | IOException e) {
            throw new TimeStampException("unable to generate timestamp token", e);
        }
    }

    private ASN1ObjectIdentifier digestToOid(DigestType digestType) {
        switch (digestType) {
            case MD5:
                return PKCSObjectIdentifiers.md5;
            case SHA1:
                return OIWObjectIdentifiers.idSHA1;
            case SHA256:
                return NISTObjectIdentifiers.id_sha256;
            case SHA384:
                return NISTObjectIdentifiers.id_sha384;
            case SHA512:
                return NISTObjectIdentifiers.id_sha512;
            default:
                throw new IllegalArgumentException(String.format("digestType: %s has no oid value", digestType));
        }
    }
}
