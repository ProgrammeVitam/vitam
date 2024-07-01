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
package fr.gouv.vitam.worker.core.plugin.traceability;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.TraceabilityError;
import fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.logbook.common.traceability.TimeStampService;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.HandlerUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.IOUtils;
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
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.WorkspaceConstants.ERROR_FLAG;
import static fr.gouv.vitam.common.model.WorkspaceConstants.TRACEABILITY_OPERATION_DIRECTORY;
import static fr.gouv.vitam.logbook.common.model.TraceabilityFile.currentHash;
import static fr.gouv.vitam.logbook.common.model.TraceabilityFile.previousTimestampToken;
import static fr.gouv.vitam.logbook.common.model.TraceabilityFile.previousTimestampTokenMinusOneMonth;
import static fr.gouv.vitam.logbook.common.model.TraceabilityFile.previousTimestampTokenMinusOneYear;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_COMPUTING_INFORMATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_MERKLE_TREE;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public class VerifyTimeStampActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VerifyTimeStampActionHandler.class);

    private static final String HANDLER_ID = "VERIFY_TIMESTAMP";

    private static final int TRACEABILITY_EVENT_DETAIL_RANK = 0;

    private static final String TIMESTAMP_FILENAME = "token.tsp";
    private static final String HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP = "COMPARE_TOKEN_TIMESTAMP";
    private static final String HANDLER_SUB_ACTION_VALIDATE_TOKEN_TIMESTAMP = "VALIDATE_TOKEN_TIMESTAMP";
    private static final String HANDLER_SUB_ACTION_VERIFY_TOKEN_TIMESTAMP = "VERIFY_TOKEN_TIMESTAMP";

    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) throws ProcessingException {
        if (handler.isExistingFileInWorkspace(params.getObjectName() + File.separator + ERROR_FLAG)) {
            return buildItemStatus(HANDLER_ID, KO);
        }
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        // 1- Get TraceabilityEventDetail from Workspace
        try {
            JsonNode traceabilityEvent = JsonHandler.getFromFile(
                (File) handler.getInput(TRACEABILITY_EVENT_DETAIL_RANK)
            );

            String encodedTimeStampToken = getEncodedTimeStampToken(params, handler);

            // 1st part - lets check timestamp within the file is the same as the one saved in the traceabilityEvent
            final ItemStatus subItemStatusTokenComparison = new ItemStatus(HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP);
            try {
                compareTimeStamps(encodedTimeStampToken, traceabilityEvent);
                itemStatus.setItemsStatus(
                    HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP,
                    subItemStatusTokenComparison.increment(StatusCode.OK)
                );
            } catch (ProcessingException e) {
                LOGGER.error("Timestamps are not equal", e);
                // lets stop the process and return an error
                itemStatus.setItemsStatus(
                    HANDLER_SUB_ACTION_COMPARE_TOKEN_TIMESTAMP,
                    subItemStatusTokenComparison.increment(StatusCode.KO)
                );
                updateReport(
                    params,
                    handler,
                    t ->
                        t
                            .setStatus(itemStatus.getGlobalStatus().name())
                            .setError(TraceabilityError.INEQUAL_TIMESTAMP)
                            .setMessage("Timestamps are not equal")
                );
                HandlerUtils.save(handler, "", params.getObjectName() + File.separator + ERROR_FLAG);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }

            // 2nd part - using bouncy castle, lets validate the timestamp
            final ItemStatus subItemStatusTokenValidation = new ItemStatus(HANDLER_SUB_ACTION_VALIDATE_TOKEN_TIMESTAMP);
            try {
                validateTimestamp(encodedTimeStampToken);
                itemStatus.setItemsStatus(
                    HANDLER_SUB_ACTION_VALIDATE_TOKEN_TIMESTAMP,
                    subItemStatusTokenValidation.increment(StatusCode.OK)
                );
            } catch (ProcessingException e) {
                LOGGER.error("Timestamps is not valid", e);
                // lets stop the process and return an error
                itemStatus.setItemsStatus(
                    HANDLER_SUB_ACTION_VALIDATE_TOKEN_TIMESTAMP,
                    subItemStatusTokenValidation.increment(StatusCode.KO)
                );
                updateReport(
                    params,
                    handler,
                    t ->
                        t
                            .setStatus(itemStatus.getGlobalStatus().name())
                            .setError(TraceabilityError.INVALID_TIMESTAMP)
                            .setMessage("Timestamps is not valid")
                );
                HandlerUtils.save(handler, "", params.getObjectName() + File.separator + ERROR_FLAG);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }

            final ItemStatus subItemStatusTokenVerification = new ItemStatus(HANDLER_SUB_ACTION_VERIFY_TOKEN_TIMESTAMP);
            try {
                String computingInformationPath =
                    TRACEABILITY_OPERATION_DIRECTORY +
                    File.separator +
                    params.getObjectName() +
                    File.separator +
                    TRACEABILITY_COMPUTING_INFORMATION;
                String merkleTreePath =
                    TRACEABILITY_OPERATION_DIRECTORY +
                    File.separator +
                    params.getObjectName() +
                    File.separator +
                    TRACEABILITY_MERKLE_TREE;
                verifyTimestamp(encodedTimeStampToken, computingInformationPath, merkleTreePath, handler);
                itemStatus.setItemsStatus(
                    HANDLER_SUB_ACTION_VERIFY_TOKEN_TIMESTAMP,
                    subItemStatusTokenVerification.increment(StatusCode.OK)
                );
            } catch (ProcessingException e) {
                LOGGER.error("Timestamps is not valid", e);
                // lets stop the process and return an error
                itemStatus.setItemsStatus(
                    HANDLER_SUB_ACTION_VERIFY_TOKEN_TIMESTAMP,
                    subItemStatusTokenVerification.increment(StatusCode.KO)
                );
                updateReport(
                    params,
                    handler,
                    t ->
                        t
                            .setStatus(itemStatus.getGlobalStatus().name())
                            .setError(TraceabilityError.INVALID_TIMESTAMP)
                            .setMessage("Timestamps is not valid")
                );
                HandlerUtils.save(handler, "", params.getObjectName() + File.separator + ERROR_FLAG);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }
            updateReport(params, handler, t -> t.setStatus(itemStatus.getGlobalStatus().name()));
        } catch (
            InvalidParseOperationException
            | ContentAddressableStorageNotFoundException
            | IOException
            | ContentAddressableStorageServerException e
        ) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private void updateReport(WorkerParameters param, HandlerIO handlerIO, Consumer<TraceabilityReportEntry> updater)
        throws IOException, ProcessingException, InvalidParseOperationException {
        String path = param.getObjectName() + File.separator + WorkspaceConstants.REPORT;
        TraceabilityReportEntry traceabilityReportEntry = JsonHandler.getFromJsonNode(
            handlerIO.getJsonFromWorkspace(path),
            TraceabilityReportEntry.class
        );
        updater.accept(traceabilityReportEntry);
        HandlerUtils.save(handlerIO, traceabilityReportEntry, path);
    }

    private String getEncodedTimeStampToken(WorkerParameters param, HandlerIO handler)
        throws IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        String operationFilePath =
            TRACEABILITY_OPERATION_DIRECTORY +
            File.separator +
            param.getObjectName() +
            File.separator +
            TIMESTAMP_FILENAME;
        try (InputStream tokenFile = handler.getInputStreamFromWorkspace(operationFilePath)) {
            return IOUtils.toString(tokenFile, StandardCharsets.UTF_8);
        }
    }

    private void verifyTimestamp(
        String encodedTimeStampToken,
        String computingInformationPath,
        String merkleTreePath,
        HandlerIO handler
    ) throws ProcessingException {
        try (
            InputStream computingInformation = handler.getInputStreamFromWorkspace(computingInformationPath);
            InputStream merkleTreeInformation = handler.getInputStreamFromWorkspace(merkleTreePath)
        ) {
            Properties computingProperties = new Properties();
            computingProperties.load(computingInformation);

            String merkleTreeDigest = JsonHandler.getFromInputStream(merkleTreeInformation).get("Root").asText();
            String computingCurrentDigest = computingProperties.getProperty(currentHash);

            if (!Objects.equals(merkleTreeDigest, computingCurrentDigest)) {
                throw new ProcessingException(
                    String.format("Not same digest %s %s.", merkleTreeDigest, computingCurrentDigest)
                );
            }

            TimeStampService timeStampService = new TimeStampService();

            byte[] rootMerkleTree = timeStampService.getDigestAsBytes(computingCurrentDigest);
            byte[] prevTimeStampToken = timeStampService.getDigestAsBytes(
                computingProperties.getProperty(previousTimestampToken)
            );
            byte[] prevTimestampTokenMinusOneMonth = timeStampService.getDigestAsBytes(
                computingProperties.getProperty(previousTimestampTokenMinusOneMonth)
            );
            byte[] prevTimestampTokenMinusOneYear = timeStampService.getDigestAsBytes(
                computingProperties.getProperty(previousTimestampTokenMinusOneYear)
            );

            TimeStampToken timeStampToken = timeStampService.getTimeStampFrom(encodedTimeStampToken);

            byte[] timeStampDataFromFile = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
            byte[] computedTimeStampData = timeStampService.getDigestFrom(
                rootMerkleTree,
                prevTimeStampToken,
                prevTimestampTokenMinusOneMonth,
                prevTimestampTokenMinusOneYear
            );

            if (!Arrays.equals(timeStampDataFromFile, computedTimeStampData)) {
                throw new ProcessingException(
                    String.format(
                        "Not same digest %s %s.",
                        BaseXx.getBase16(timeStampDataFromFile),
                        BaseXx.getBase16(computedTimeStampData)
                    )
                );
            }
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    private void compareTimeStamps(String timeStampToken, JsonNode traceabilityEvent) throws ProcessingException {
        String traceabilityTimeStamp = traceabilityEvent.get("TimeStampToken").asText();
        if (!timeStampToken.equals(traceabilityTimeStamp)) {
            throw new ProcessingException("TimeStamp tokens are different");
        }
    }

    private void validateTimestamp(String encodedTimeStampToken) throws ProcessingException {
        try {
            TimeStampService timeStampService = new TimeStampService();
            TimeStampToken tsToken = timeStampService.getTimeStampFrom(encodedTimeStampToken);

            AttributeTable table = tsToken.getSignedAttributes();
            Attribute attribute = table.get(PKCSObjectIdentifiers.id_aa_signingCertificate);
            if (attribute == null) {
                attribute = table.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2);
            }
            SigningCertificateV2 sigCertV2 = SigningCertificateV2.getInstance(attribute.getAttributeValues()[0]);

            try {
                X509CertificateHolder x509Certificate = retriveCertificate(tsToken);

                SignerInformationVerifier sigVerifier = new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider(PROVIDER_NAME)
                    .build(x509Certificate);
                if (tsToken.isSignatureValid(sigVerifier)) {
                    tsToken.validate(sigVerifier);
                } else {
                    LOGGER.error("Signature from timestamp token is incorrect");
                    throw new ProcessingException("Signature from timestamp token is incorrect");
                }

                DigestCalculatorProvider digestCalculatorProvider = new BcDigestCalculatorProvider();
                DigestCalculator digCalc = digestCalculatorProvider.get(
                    new AlgorithmIdentifier(tsToken.getTimeStampInfo().getMessageImprintAlgOID())
                );
                OutputStream dOut = digCalc.getOutputStream();
                dOut.write(x509Certificate.getEncoded());
                dOut.close();
                byte[] certHash = digCalc.getDigest();
                if (!Arrays.equals(sigCertV2.getCerts()[0].getCertHash(), certHash)) {
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

    private X509CertificateHolder retriveCertificate(TimeStampToken tsToken) {
        Store<X509CertificateHolder> storeTt = tsToken.getCertificates();
        Selector<X509CertificateHolder> sid = tsToken.getSID();
        Collection<X509CertificateHolder> collTt = storeTt.getMatches(sid);
        Iterator<X509CertificateHolder> certIt2 = collTt.iterator();
        return certIt2.next();
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub
    }
}
