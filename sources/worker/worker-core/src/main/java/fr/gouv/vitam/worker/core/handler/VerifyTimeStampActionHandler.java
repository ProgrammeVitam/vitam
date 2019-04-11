/*
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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TSPValidationException;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Iterator;

public class VerifyTimeStampActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VerifyTimeStampActionHandler.class);

    private static final String HANDLER_ID = "VERIFY_TIMESTAMP";

    private static final int TRACEABILITY_EVENT_DETAIL_RANK = 0;

    private static final String TIMESTAMP_FILENAME = "token.tsp";

    private static JsonNode traceabilityEvent = null;

    private static final String HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP = "COMPARE_TOKEN_TIMESTAMP";
    private static final String HANDLER_SUB_ACTION_VALIDATE_TOKEN_TIMESTAMP = "VALIDATE_TOKEN_TIMESTAMP";

    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        InputStream tokenFile = null;
        // 1- Get TraceabilityEventDetail from Workspace
        try {
            traceabilityEvent =
                JsonHandler.getFromFile((File) handler.getInput(TRACEABILITY_EVENT_DETAIL_RANK));

            String operationFilePath = SedaConstants.TRACEABILITY_OPERATION_DIRECTORY + "/" +
                TIMESTAMP_FILENAME;
            tokenFile = handler.getInputStreamFromWorkspace(operationFilePath);
            String encodedTimeStampToken = IOUtils.toString(tokenFile, "UTF-8");

            // 1st part - lets check timestamp within the file is the same as the one saved in the traceabilityEvent
            final ItemStatus subItemStatusTokenComparison = new ItemStatus(HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP);
            try {
                compareTimeStamps(encodedTimeStampToken);
                itemStatus.setItemsStatus(HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP,
                    subItemStatusTokenComparison.increment(StatusCode.OK));
            } catch (ProcessingException e) {
                LOGGER.error("Timestamps are not equal", e);
                // lets stop the process and return an error
                itemStatus.setItemsStatus(HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP, subItemStatusTokenComparison.increment(StatusCode.KO));
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }

            // 2nd part - using bouncy castle, lets validate the timestamp
            final ItemStatus subItemStatusTokenValidation = new ItemStatus(HANDLER_SUB_ACTION_VALIDATE_TOKEN_TIMESTAMP);
            try {
                validateTimestamp(encodedTimeStampToken);
                itemStatus.setItemsStatus(HANDLER_SUB_ACTION_VALIDATE_TOKEN_TIMESTAMP,
                    subItemStatusTokenValidation.increment(StatusCode.OK));
            } catch (ProcessingException e) {
                LOGGER.error("Timestamps is not valid", e);
                // lets stop the process and return an error
                itemStatus.setItemsStatus(HANDLER_SUB_ACTION_VALIDATE_TOKEN_TIMESTAMP,
                    subItemStatusTokenValidation.increment(StatusCode.KO));
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }

        } catch (InvalidParseOperationException | ContentAddressableStorageNotFoundException | IOException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        } finally {
            IOUtils.closeQuietly(tokenFile);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private void compareTimeStamps(String timeStampToken) throws ProcessingException {
        String traceabilityTimeStamp = traceabilityEvent.get("TimeStampToken").asText();
        if (!timeStampToken.equals(traceabilityTimeStamp)) {
            throw new ProcessingException("TimeStamp tokens are different");
        }
    }

    private void validateTimestamp(String encodedTimeStampToken) throws ProcessingException {
        try {
            ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(
                Base64.decode(encodedTimeStampToken.getBytes())));
            ASN1Primitive obj = bIn.readObject();
            TimeStampResponse tsResp = new TimeStampResponse(obj.toASN1Primitive().getEncoded());
            TimeStampToken tsToken = tsResp.getTimeStampToken();

            AttributeTable table = tsToken.getSignedAttributes();
            Attribute attribute = table.get(PKCSObjectIdentifiers.id_aa_signingCertificate);
            if (attribute == null) {
                attribute = table.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2);
            }
            SigningCertificateV2 sigCertV2 = SigningCertificateV2.getInstance(attribute.getAttributeValues()[0]);

            // nonce should be null for now
            // TODO maybe nonce could be different than null ? If so, check what is set in LogbookAdministration >
            // generateTimeStampToken
            if (tsToken.getTimeStampInfo().getNonce() != null) {
                LOGGER.error("Nonce couldnt be not null");
                throw new ProcessingException("Nonce couldnt be not null");
            }

            try {

                Store storeTt = tsToken.getCertificates();
                Collection collTt = storeTt.getMatches(tsToken.getSID());
                Iterator certIt2 = collTt.iterator();
                X509CertificateHolder x509Certificate = (X509CertificateHolder) certIt2.next();

                SignerInformationVerifier sigVerifier =
                    new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(x509Certificate);
                if (tsToken.isSignatureValid(sigVerifier)) {
                    tsToken.validate(sigVerifier);
                } else {
                    LOGGER.error("Signature from timestamp token is incorrect");
                    throw new ProcessingException("Signature from timestamp token is incorrect");
                }

                DigestCalculatorProvider digestCalculatorProvider = new BcDigestCalculatorProvider();
                DigestCalculator digCalc =
                    digestCalculatorProvider
                        .get(new AlgorithmIdentifier(tsToken.getTimeStampInfo().getMessageImprintAlgOID()));
                OutputStream dOut = digCalc.getOutputStream();
                dOut.write(x509Certificate.getEncoded());
                dOut.close();
                byte[] certHash = digCalc.getDigest();
                if (!Arrays.areEqual(sigCertV2.getCerts()[0].getCertHash(), certHash)) {
                    LOGGER.error("Hash from certificates are different");
                    throw new ProcessingException("Hash from certificates are different");
                }

            } catch (TSPValidationException e) {
                LOGGER.error(e);
                throw new ProcessingException("TimeStampToken fails to validate", e);
            } catch (TSPException | IllegalArgumentException e) {
                LOGGER.error(e);
                throw new ProcessingException("Error while getting keystore", e);
            }
        } catch (TSPException | OperatorCreationException | CertificateException | IOException e) {
            LOGGER.error(e);
            throw new ProcessingException("TimeStamp tokens couldnt be validated", e);
        }
    }


    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub
    }
}
