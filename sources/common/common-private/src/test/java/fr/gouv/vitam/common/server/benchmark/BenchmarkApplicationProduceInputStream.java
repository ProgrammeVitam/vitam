/**
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
package fr.gouv.vitam.common.server.benchmark;

import org.glassfish.jersey.server.ResourceConfig;

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;

/**
 * Benchmark web application
 */
public final class BenchmarkApplicationProduceInputStream
    extends AbstractVitamApplication<BenchmarkApplicationProduceInputStream, BenchmarkConfiguration> {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(BenchmarkApplicationProduceInputStream.class);

    private static String MODULE_NAME = ServerIdentity.getInstance().getRole();

    /**
     * Benchmark constructor
     *
     * @param configurationFile
     */
    protected BenchmarkApplicationProduceInputStream(String configurationFile) {
        super(BenchmarkConfiguration.class, configurationFile);
    }

    /**
     * Main method to run the application (doing start and join)
     *
     * @param args command line parameters
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            LOGGER.error(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT + MODULE_NAME);
            throw new IllegalArgumentException(CONFIG_FILE_IS_A_MANDATORY_ARGUMENT + MODULE_NAME);
        }
        try {
            final BenchmarkApplicationProduceInputStream application =
                new BenchmarkApplicationProduceInputStream(args[0]);
            application.run();
        } catch (final Exception e) {
            LOGGER.error("Can not start " + MODULE_NAME + " server: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    @Override
    protected void registerInResourceConfig(final ResourceConfig resourceConfig) {
        resourceConfig.register(new BenchmarkResourceProduceInputStream(getConfiguration()));
    }

    @Override
    protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
        // do nothing as @admin is not tested here
        return false;
    }

}
