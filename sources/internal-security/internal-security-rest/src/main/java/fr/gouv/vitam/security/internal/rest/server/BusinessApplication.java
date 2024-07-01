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
package fr.gouv.vitam.security.internal.rest.server;

import com.fasterxml.jackson.jaxrs.base.JsonParseExceptionMapper;
import com.mongodb.client.MongoClient;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.serverv2.ConfigurationApplication;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.security.internal.rest.mapper.CertificateExceptionMapper;
import fr.gouv.vitam.security.internal.rest.mapper.IllegalArgumentExceptionMapper;
import fr.gouv.vitam.security.internal.rest.mapper.PersonalCertificateExceptionMapper;
import fr.gouv.vitam.security.internal.rest.repository.IdentityRepository;
import fr.gouv.vitam.security.internal.rest.repository.PersonalRepository;
import fr.gouv.vitam.security.internal.rest.resource.CertificateResource;
import fr.gouv.vitam.security.internal.rest.resource.IdentityResource;
import fr.gouv.vitam.security.internal.rest.resource.PersonalCertificateResource;
import fr.gouv.vitam.security.internal.rest.service.IdentityService;
import fr.gouv.vitam.security.internal.rest.service.PermissionService;
import fr.gouv.vitam.security.internal.rest.service.PersonalCertificateService;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

/**
 * module declaring business resource
 */
public class BusinessApplication extends ConfigurationApplication {

    private final Set<Object> singletons;

    /**
     * Constructor
     *
     * @param servletConfig
     */
    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        singletons = new HashSet<>();

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final InternalSecurityConfiguration configuration = PropertiesUtils.readYaml(
                yamlIS,
                InternalSecurityConfiguration.class
            );

            final PersonalCertificatePermissionConfig personalCertificatePermissionConfig =
                PersonalCertificatePermissionConfigLoader.loadPersonalCertificatePermissionConfig(
                    configuration.getPersonalCertificatePermissionConfig()
                );

            MongoClient mongoClient = MongoDbAccess.createMongoClient(configuration);
            SimpleMongoDBAccess mongoDbAccess = new SimpleMongoDBAccess(mongoClient, configuration.getDbName());

            IdentityRepository identityRepository = new IdentityRepository(mongoDbAccess);
            IdentityService identityService = new IdentityService(identityRepository);

            PersonalRepository personalRepository = new PersonalRepository(mongoDbAccess);
            PersonalCertificateService personalCertificateService = new PersonalCertificateService(
                LogbookOperationsClientFactory.getInstance(),
                personalRepository
            );
            PermissionService permissionService = new PermissionService(personalCertificatePermissionConfig);

            CommonBusinessApplication commonBusinessApplication = new CommonBusinessApplication();
            singletons.addAll(commonBusinessApplication.getResources());

            singletons.add(new IdentityResource(identityService));
            singletons.add(new PersonalCertificateResource(permissionService, personalCertificateService));
            singletons.add(new CertificateResource(identityService, personalCertificateService));

            singletons.add(new CertificateExceptionMapper());
            singletons.add(new IllegalArgumentExceptionMapper());
            singletons.add(new JsonParseExceptionMapper());
            singletons.add(new PersonalCertificateExceptionMapper());
            singletons.add(new ApplicationStatusResource());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
