/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.server.benchmark;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.AbstractBenchmarkClientFactory;
import fr.gouv.vitam.common.server.VitamServerFactory;

/**
 * Benchmark Client factory
 */
public final class BenchmarkClientFactory extends AbstractBenchmarkClientFactory<BenchmarkClientRest> {
    private static final String RESOURCE_PATH = "/benchmark";

    /**
     * Default client operation type
     */
    private static final BenchmarkClientFactory BENCHMARK_CLIENT_FACTORY =
        new BenchmarkClientFactory(RESOURCE_PATH, false);
    /**
     * Default client operation type
     */
    private static final BenchmarkClientFactory BENCHMARK_CLIENT_MULTIPART_FACTORY =
        new BenchmarkClientFactory(RESOURCE_PATH, true);

    private BenchmarkClientFactory(String resourcePath, boolean allowMultipart) {
        super(VitamServerFactory.getDefaultPort(), resourcePath, allowMultipart);
    }

    /**
     * Set the BenchmarkClientFactory configuration
     *
     * @param port port to use
     * @throws IllegalArgumentException if type null or if type is OPERATIONS and server is null or empty or port <= 0
     */
    public static final void setConfiguration(int port) {
        ParametersChecker.checkValue("port", port, 1);
        getInstance().changeServerPort(port);
        getInstanceMultipart().changeServerPort(port);
    }

    /**
     * Get the LogbookClientFactory instance
     *
     * @return the instance
     */
    public static final BenchmarkClientFactory getInstance() {
        return BENCHMARK_CLIENT_FACTORY;
    }

    /**
     * Get the LogbookClientFactory instance
     *
     * @return the instance
     */
    public static final BenchmarkClientFactory getInstanceMultipart() {
        return BENCHMARK_CLIENT_MULTIPART_FACTORY;
    }

    @Override
    public BenchmarkClientRest getClient() {
        return new BenchmarkClientRest(this);
    }
}
