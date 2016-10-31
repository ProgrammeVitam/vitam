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
package fr.gouv.vitam.ingest.internal.upload.rest;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server2.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server2.application.resources.AdminStatusResource;



/**
 * Ingest Internal web server application
 */
public class IngestInternalApplication
    extends AbstractVitamApplication<IngestInternalApplication, IngestInternalConfiguration> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalApplication.class);
    private static final String INGEST_INTERNAL_CONF_FILE_NAME = "ingest-internal.conf";
    private static final String MODULE_NAME = "ingest-internal";

    /**
     * Ingest Internal constructor
     */
    public IngestInternalApplication(String configuration) {
        super(IngestInternalConfiguration.class, configuration);
    }

    /**
     * Main method to run the application (doing start and join)
     *
     * @param args command line parameters
     * @throws IllegalStateException if the Vitam server cannot be launched
     */
    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                LOGGER.error(
                    String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT, INGEST_INTERNAL_CONF_FILE_NAME));
                throw new IllegalArgumentException(String.format(VitamServer.CONFIG_FILE_IS_A_MANDATORY_ARGUMENT,
                    INGEST_INTERNAL_CONF_FILE_NAME));
            }
            final IngestInternalApplication application = new IngestInternalApplication(args[0]);
            application.run();
        } catch (final Exception e) {
            LOGGER.error(String.format(VitamServer.SERVER_CAN_NOT_START, MODULE_NAME) + e.getMessage(), e);
            System.exit(1);
        }
    }

    @Override
    protected void registerInResourceConfig(ResourceConfig resourceConfig) {
        // Start by registering Multipart
        resourceConfig.register(MultiPartFeature.class);
        resourceConfig.register(new IngestInternalResource(getConfiguration()))
            .register(new AdminStatusResource());
    }
}
