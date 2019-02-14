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


    public BusinessApplication(LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory, StorageClientFactory storageClientFactory,
        WorkspaceClientFactory workspaceClientFactory, AdminManagementClientFactory adminManagementClientFactory,
        MetaDataClientFactory metaDataClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory) {

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
            final AccessInternalConfiguration accessInternalConfiguration =
                PropertiesUtils.readYaml(yamlIS, AccessInternalConfiguration.class);
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
            singletons.add(new AccessInternalResourceImpl(logbookLifeCyclesClientFactory,
                logbookOperationsClientFactory, storageClientFactory,
                workspaceClientFactory, adminManagementClientFactory,
                metaDataClientFactory,
                processingManagementClientFactory));

            singletons.add(new LogbookInternalResourceImpl(logbookLifeCyclesClientFactory,
                logbookOperationsClientFactory, storageClientFactory,
                workspaceClientFactory, adminManagementClientFactory,
                metaDataClientFactory,
                processingManagementClientFactory));
        }

        singletons.add(new AccessContractIdContainerFilter());
    }

    @Override
    public Set<Class<?>> getClasses() {
        return commonBusinessApplication.getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
