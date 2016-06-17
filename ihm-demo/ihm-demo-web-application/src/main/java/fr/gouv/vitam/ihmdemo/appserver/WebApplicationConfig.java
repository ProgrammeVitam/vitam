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
package fr.gouv.vitam.ihmdemo.appserver;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Web Application Configuration class 
 */
public class WebApplicationConfig {

    private int port;
    private String[] virtualHosts;
    private String defaultContext;
    private String staticContent;

    /**
     * @return port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port
     * @return WebApplicationConfig
     */
    public WebApplicationConfig setPort(int port) {
        ParametersChecker.checkParameter("port is mandatory", port);
        this.port = port;
        return this;
    }

    /**
     * @return virtualHosts
     */
    public String[] getVirtualHosts() {
        return virtualHosts;
    }

    /**
     * @param virtualHosts
     * @return
     */
    public WebApplicationConfig setVirtualHosts(String[] virtualHosts) {
        ParametersChecker.checkParameter("virtualHosts is mandatory", virtualHosts);
        this.virtualHosts = virtualHosts;
        return this;
    }

    /**
     * @return defaultContext
     */
    public String getDefaultContext() {
        return defaultContext;
    }

    /**
     * @param defaultContext
     * @return WebApplicationConfig
     */
    public WebApplicationConfig setDefaultContext(String defaultContext) {
        ParametersChecker.checkParameter("defaultContext is mandatory", defaultContext);
        this.defaultContext = defaultContext;
        return this;
    }

    /**
     * @return staticContent
     */
    public String getStaticContent() {
        return staticContent;
    }

    /**
     * @param staticContent
     * @return WebApplicationConfig
     */
    public WebApplicationConfig setStaticContent(String staticContent) {
        ParametersChecker.checkParameter("staticContent is mandatory", staticContent);
        this.staticContent = staticContent;
        return this;
    }
}
