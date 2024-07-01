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
package fr.gouv.vitam.functionaltest.configuration;

import fr.gouv.vitam.common.ParametersChecker;

import java.util.List;

/**
 * TnrClientConfiguration
 */
public class TnrClientConfiguration {

    private static final String IS_A_MANDATORY_PARAMETER = " is a mandatory parameter";
    private List<Integer> tenants;
    private Integer adminTenant;

    /**
     * Empty ClientConfiguration constructor for YAMLFactory
     */
    public TnrClientConfiguration() {
        //Nothing to-do
    }

    /**
     * @return urlWorkspace
     */
    public String getUrlWorkspace() {
        return urlWorkspace;
    }

    /**
     * @param urlWorkspace
     */
    public void setUrlWorkspace(String urlWorkspace) {
        this.urlWorkspace = urlWorkspace;
    }

    /**
     * @return vitamSecret
     */
    public String getVitamSecret() {
        return vitamSecret;
    }

    /**
     * @param vitamSecret
     */
    public void setVitamSecret(String vitamSecret) {
        this.vitamSecret = vitamSecret;
    }

    /**
     * url workspace
     */
    protected String urlWorkspace;
    /**
     * tenants List on wich Testing
     */
    protected List<Integer> tenantsTest;

    /**
     * vitam secret
     */
    protected String vitamSecret;

    /**
     * TNR tenants List
     *
     * @return The list
     */
    public List<Integer> getTenantsTest() {
        return tenantsTest;
    }

    /**
     * TNR tenants list setter
     *
     * @param tenants
     */
    public void setTenantsTest(List<Integer> tenants) {
        ParametersChecker.checkParameter("Tenant id" + IS_A_MANDATORY_PARAMETER, tenants);
        this.tenantsTest = tenants;
    }

    public List<Integer> getTenants() {
        return tenants;
    }

    public Integer getAdminTenant() {
        return adminTenant;
    }

    public void setTenants(List<Integer> tenants) {
        ParametersChecker.checkParameter("Mandatory", tenants);
        this.tenants = tenants;
    }

    public void setAdminTenant(Integer adminTenant) {
        ParametersChecker.checkParameter("Mandatory", adminTenant);
        this.adminTenant = adminTenant;
    }
}
