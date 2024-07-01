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
package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.timestamp.TimeStampSignature;
import fr.gouv.vitam.common.timestamp.TimeStampSignatureWithKeystore;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper;
import fr.gouv.vitam.logbook.common.traceability.TraceabilityService;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.VerifyTimeStampActionConfiguration;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * GenerateLifecycleTraceabilityAction Plugin
 */
public abstract class GenerateLifecycleTraceabilityActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        GenerateLifecycleTraceabilityActionPlugin.class
    );

    private final TimestampGenerator timestampGenerator;
    private static final String VERIFY_TIMESTAMP_CONF_FILE = "verify-timestamp.conf";
    public static final String TRACEABILITY_ZIP_FILE_NAME = "traceabilityFile.zip";
    public static final String TRACEABILITY_EVENT_FILE_NAME = "traceabilityEvent.json";

    /**
     * Empty constructor FinalizeLifecycleTraceabilityActionPlugin
     */
    public GenerateLifecycleTraceabilityActionPlugin() {
        TimeStampSignature timeStampSignature;
        VerifyTimeStampActionConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(
                PropertiesUtils.findFile(VERIFY_TIMESTAMP_CONF_FILE),
                VerifyTimeStampActionConfiguration.class
            );
        } catch (IOException e) {
            LOGGER.error("Processing exception", e);
        }
        if (configuration != null) {
            try {
                final File file = PropertiesUtils.findFile(configuration.getP12LogbookFile());
                timeStampSignature = new TimeStampSignatureWithKeystore(
                    file,
                    configuration.getP12LogbookPassword().toCharArray()
                );
            } catch (
                KeyStoreException
                | CertificateException
                | IOException
                | UnrecoverableKeyException
                | NoSuchAlgorithmException e
            ) {
                LOGGER.error("unable to instantiate TimeStampGenerator", e);
                // FIXME: Make a specific exception ?
                throw new RuntimeException(e);
            }
            timestampGenerator = new TimestampGenerator(timeStampSignature);
        } else {
            LOGGER.error("unable to instantiate TimeStampGenerator");
            throw new RuntimeException("Configuration is null");
        }
    }

    /**
     * Generation and storage of the secure file for lifecycles
     *
     * @param helper@throws TraceabilityException if any error occurs
     */
    protected void generateLifecycleTraceabilityFile(LogbookTraceabilityHelper helper) throws TraceabilityException {
        Integer tenantId = ParameterHelper.getTenantParameter();
        File tmpFolder = PropertiesUtils.fileFromTmpFolder("secure");

        TraceabilityService traceabilityService = new TraceabilityService(
            timestampGenerator,
            helper,
            tenantId,
            tmpFolder
        );

        traceabilityService.secureData(VitamConfiguration.getDefaultStrategy());
    }
}
