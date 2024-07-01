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
package fr.gouv.vitam.access.internal.rest;

import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.access.internal.serve.filter.AccessContractIdContainerFilter;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

/**
 * Business application for access internal declaring resources and filters
 */
public class BusinessApplication extends Application {

    private final CommonBusinessApplication commonBusinessApplication;

    private Set<Object> singletons;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private StorageClientFactory storageClientFactory;
    private WorkspaceClientFactory workspaceClientFactory;
    private AdminManagementClientFactory adminManagementClientFactory;
    private MetaDataClientFactory metaDataClientFactory;
    private ProcessingManagementClientFactory processingManagementClientFactory;

    public BusinessApplication(
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        StorageClientFactory storageClientFactory,
        WorkspaceClientFactory workspaceClientFactory,
        AdminManagementClientFactory adminManagementClientFactory,
        MetaDataClientFactory metaDataClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory
    ) {
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;

        commonBusinessApplication = new CommonBusinessApplication();

        singletons = new HashSet<>();
        singletons.addAll(commonBusinessApplication.getResources());
        prepare(null);
    }

    /**
     * Constructor
     *
     * @param servletConfig the servlet configuration
     */
    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final AccessInternalConfiguration accessInternalConfiguration = PropertiesUtils.readYaml(
                yamlIS,
                AccessInternalConfiguration.class
            );
            commonBusinessApplication = new CommonBusinessApplication();
            singletons = new HashSet<>();
            singletons.addAll(commonBusinessApplication.getResources());

            prepare(accessInternalConfiguration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void prepare(AccessInternalConfiguration accessInternalConfiguration) {
        if (null != accessInternalConfiguration) {
            singletons.add(new AccessInternalResourceImpl(accessInternalConfiguration));
            singletons.add(new LogbookInternalResourceImpl());
        } else {
            singletons.add(
                new AccessInternalResourceImpl(
                    logbookLifeCyclesClientFactory,
                    logbookOperationsClientFactory,
                    storageClientFactory,
                    workspaceClientFactory,
                    adminManagementClientFactory,
                    metaDataClientFactory,
                    processingManagementClientFactory
                )
            );

            singletons.add(
                new LogbookInternalResourceImpl(
                    logbookLifeCyclesClientFactory,
                    logbookOperationsClientFactory,
                    storageClientFactory,
                    workspaceClientFactory,
                    adminManagementClientFactory,
                    metaDataClientFactory,
                    processingManagementClientFactory
                )
            );
        }

        singletons.add(new AccessContractIdContainerFilter());
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
