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

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampResponseGenerator;
import org.bouncycastle.tsp.TimeStampTokenGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;

/**
 * Generate a time stamp token with a local PKCS12 keystore.
 */
public class TimeStampSignatureWithKeystore implements TimeStampSignature {

    private final DigestCalculatorProvider digestCalculatorProvider;
    private PrivateKey key;
    private final String tspPolicy;
    private Certificate[] certificateChain;

    /**
     * @param pkcs12Path file link to pkcs12 keystore
     * @param keystorePassword
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws UnrecoverableKeyException
     */
    public TimeStampSignatureWithKeystore(File pkcs12Path, char[] keystorePassword)
        throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
        digestCalculatorProvider = new BcDigestCalculatorProvider();

        final KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (FileInputStream fileInputStream = new FileInputStream(pkcs12Path)) {
            final String alias = loadKeystoreAndfindUniqueAlias(keystorePassword, keyStore, fileInputStream);

            key = (PrivateKey) keyStore.getKey(alias, keystorePassword);
            certificateChain = keyStore.getCertificateChain(alias);
        }

        tspPolicy = "1.1";
    }

    private String loadKeystoreAndfindUniqueAlias(
        char[] keystorePassword,
        KeyStore keyStore,
        FileInputStream fileInputStream
    ) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException {
        keyStore.load(fileInputStream, keystorePassword);

        final Enumeration<String> aliases = keyStore.aliases();
        final String alias = aliases.nextElement();
        if (aliases.hasMoreElements()) {
            throw new IllegalArgumentException("Keystore has many key");
        }
        return alias;
    }

    /**
     * @param request time stamp request
     * @return time stamp response
     * @throws OperatorCreationException
     * @throws TSPException
     * @throws CertificateEncodingException
     */
    @Override
    public TimeStampResponse sign(TimeStampRequest request)
        throws OperatorCreationException, TSPException, CertificateEncodingException {
        final DigestCalculator digestCalculator = digestCalculatorProvider.get(
            new AlgorithmIdentifier(request.getMessageImprintAlgOID())
        );
        final String tspAlgorithm = computeTspAlgorithm(key, VitamConfiguration.getDefaultTimestampDigestType());

        final SignerInfoGenerator signerInfoGen = new JcaSimpleSignerInfoGeneratorBuilder()
            .build(tspAlgorithm, key, (X509Certificate) certificateChain[0]);

        final ASN1ObjectIdentifier tsaPolicy = new ASN1ObjectIdentifier(tspPolicy);

        final TimeStampTokenGenerator tokenGen = new TimeStampTokenGenerator(
            signerInfoGen,
            digestCalculator,
            tsaPolicy
        );

        tokenGen.addCertificates(new JcaCertStore(Arrays.asList(certificateChain)));

        final TimeStampResponseGenerator timeStampResponseGenerator = new TimeStampResponseGenerator(
            tokenGen,
            TSPAlgorithms.ALLOWED
        );

        final Date currentDate = LocalDateUtil.getDate(LocalDateUtil.now());

        return timeStampResponseGenerator.generate(request, BigInteger.ONE, currentDate);
    }

    private String computeTspAlgorithm(PrivateKey privateKey, DigestType digestType) {
        return String.format("%sWith%s", digestType.name(), privateKey.getAlgorithm());
    }
}
