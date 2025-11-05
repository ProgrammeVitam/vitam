/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.cold.server;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;
import fr.gouv.vitam.storage.cold.api.InaTapeProxyResource;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

public class InaTapeProxyApplication extends Application {

    private final CommonBusinessApplication commonBusinessApplication;
    private final Set<Object> singletons;

    public InaTapeProxyApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        InaTapeProxyConfiguration configuration = loadConfiguration(configurationFile);
        this.commonBusinessApplication = new CommonBusinessApplication();
        this.singletons = initResources(configuration);
    }

    private InaTapeProxyConfiguration loadConfiguration(String configurationFile) {
        try (InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            return PropertiesUtils.readYaml(yamlIS, InaTapeProxyConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }
    }

    private Set<Object> initResources(InaTapeProxyConfiguration configuration) {
        Set<Object> set = new HashSet<>(commonBusinessApplication.getResources());
        set.add(new InaTapeProxyResource(configuration));
        return set;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
