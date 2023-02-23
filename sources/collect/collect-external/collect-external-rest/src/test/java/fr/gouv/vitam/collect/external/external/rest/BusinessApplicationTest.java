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
package fr.gouv.vitam.collect.external.external.rest;

import fr.gouv.vitam.collect.internal.client.CollectInternalClientFactory;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.SecureEndpointScanner;
import fr.gouv.vitam.common.security.waf.SanityCheckerCommonFilter;
import fr.gouv.vitam.common.security.waf.SanityDynamicFeature;
import fr.gouv.vitam.common.server.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.server.application.resources.VitamStatusService;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;

public class BusinessApplicationTest extends Application {

    private final CommonBusinessApplication commonBusinessApplication;

    private Set<Object> singletons;
    private final CollectInternalClientFactory collectInternalClientFactory;
    private final IngestExternalClientFactory ingestExternalClientFactory;
    private final VitamStatusService vitamStatusService;

    public BusinessApplicationTest() {
        this.collectInternalClientFactory = mock(CollectInternalClientFactory.class);
        this.ingestExternalClientFactory = mock(IngestExternalClientFactory.class);
        this.vitamStatusService = mock(VitamStatusService.class);
        commonBusinessApplication = new CommonBusinessApplication(true);
        prepare();
    }

    public BusinessApplicationTest(@Context ServletConfig servletConfig) {
        this.collectInternalClientFactory = CollectInternalClientFactory.getInstance();
        this.ingestExternalClientFactory = IngestExternalClientFactory.getInstance();
        this.vitamStatusService = new BasicVitamStatusServiceImpl();
        commonBusinessApplication = new CommonBusinessApplication(true);
        prepare();
    }

    public void prepare() {
        SecureEndpointRegistry secureEndpointRegistry = new SecureEndpointRegistry();
        SecureEndpointScanner secureEndpointScanner = new SecureEndpointScanner(secureEndpointRegistry);

        final CollectExternalResource collectExternalResource =
            new CollectExternalResource(secureEndpointRegistry);
        final TransactionExternalResource transactionExternalResource = new TransactionExternalResource();

        final ProjectExternalResource projectExternalResource =
            new ProjectExternalResource(collectInternalClientFactory);
        singletons = new HashSet<>();
        singletons.addAll(commonBusinessApplication.getResources());
        singletons.add(collectExternalResource);
        singletons.add(projectExternalResource);
        singletons.add(transactionExternalResource);
        singletons.add(new SanityCheckerCommonFilter());
        singletons.add(new SanityDynamicFeature());
        singletons.add(secureEndpointScanner);
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    public CollectInternalClientFactory getCollectInternalClientFactory() {
        return collectInternalClientFactory;
    }

    public IngestExternalClientFactory getIngestExternalClientFactory() {
        return ingestExternalClientFactory;
    }

    public VitamStatusService getVitamStatusService() {
        return vitamStatusService;
    }
}
