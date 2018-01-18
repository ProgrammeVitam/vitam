/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.handler;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.timestamp.TimeStampSignature;
import fr.gouv.vitam.common.timestamp.TimeStampSignatureWithKeystore;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.traceability.TraceabilityService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.model.LogbookLifeCycleTraceabilityHelper;

/**
 * FinalizeLifecycleTraceabilityAction Plugin
 */
public class FinalizeLifecycleTraceabilityActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(FinalizeLifecycleTraceabilityActionHandler.class);

    private static final String HANDLER_ID = "FINALIZE_LC_TRACEABILITY";

    private HandlerIO handlerIO;

    private final TimestampGenerator timestampGenerator;
    private static final String VERIFY_TIMESTAMP_CONF_FILE = "verify-timestamp.conf";

    /**
     * Empty constructor FinalizeLifecycleTraceabilityActionPlugin
     *
     */
    public FinalizeLifecycleTraceabilityActionHandler() {
        TimeStampSignature timeStampSignature;
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        VerifyTimeStampActionConfiguration configuration = null;
        try {
            configuration =
                PropertiesUtils.readYaml(PropertiesUtils.findFile(VERIFY_TIMESTAMP_CONF_FILE),
                    VerifyTimeStampActionConfiguration.class);
        } catch (IOException e) {
            LOGGER.error("Processing exception", e);
        }
        if (configuration != null) {
            try {
                final File file = PropertiesUtils.findFile(configuration.getP12LogbookFile());
                timeStampSignature =
                    new TimeStampSignatureWithKeystore(file, configuration.getP12LogbookPassword().toCharArray());
            } catch (KeyStoreException | CertificateException | IOException | UnrecoverableKeyException |
                NoSuchAlgorithmException e) {
                LOGGER.error("unable to instanciate TimeStampGenerator", e);
                // FIXME: Make a specific exception ?
                throw new RuntimeException(e);
            }
            timestampGenerator = new TimestampGenerator(timeStampSignature);
        } else {
            LOGGER.error("unable to instanciate TimeStampGenerator");
            throw new RuntimeException("Configuration is null");
        }
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        handlerIO = handler;
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        try {
            finalizeLifecycles(params, itemStatus);
            itemStatus.increment(StatusCode.OK);
        } catch (TraceabilityException e) {
            LOGGER.error("Exception while finalizing", e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID,
            itemStatus);
    }

    /**
     * Generation and storage of the secure file for lifecycles
     * 
     * @param params worker parameters
     * @param itemStatus step itemStatus, would be updated by the current handler
     * @throws TraceabilityException if any error occurs
     */
    private void finalizeLifecycles(WorkerParameters params, ItemStatus itemStatus)
        throws TraceabilityException {
    	
        Integer tenantId = ParameterHelper.getTenantParameter();
        File tmpFolder = PropertiesUtils.fileFromTmpFolder("secure");
        final LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient();
        LogbookLifeCycleTraceabilityHelper helper =
        		new LogbookLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, params.getProcessId());
    	
    	TraceabilityService traceabilityService = 
    		new TraceabilityService(timestampGenerator, helper, tenantId, tmpFolder);
    	
    	traceabilityService.secureData();
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }
}
