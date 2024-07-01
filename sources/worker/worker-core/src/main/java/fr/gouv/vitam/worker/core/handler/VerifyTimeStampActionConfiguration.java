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
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * TnrClientConfiguration
 */
public class VerifyTimeStampActionConfiguration {

    private static final String IS_A_MANDATORY_PARAMETER = " is a mandatory parameter";

    /**
     * VerifyTimeStampActionConfiguration constructor
     *
     * @param p12LogbookPassword
     * @param p12LogbookFile
     */
    public VerifyTimeStampActionConfiguration(String p12LogbookPassword, String p12LogbookFile) {
        ParametersChecker.checkParameter("p12LogbookPassword" + IS_A_MANDATORY_PARAMETER, p12LogbookPassword);
        ParametersChecker.checkParameter("p12LogbookFile" + IS_A_MANDATORY_PARAMETER, p12LogbookFile);
        this.p12LogbookPassword = p12LogbookPassword;
        this.p12LogbookFile = p12LogbookFile;
    }

    /**
     * Empty VerifyTimeStampActionConfiguration constructor for YAMLFactory
     */
    public VerifyTimeStampActionConfiguration() {
        // Nothing to-do
    }

    private String p12LogbookPassword;

    private String p12LogbookFile;

    /**
     * @return password of p12
     */
    public String getP12LogbookPassword() {
        return p12LogbookPassword;
    }

    /**
     * @param p12LogbookPassword file to set
     */
    public void setP12LogbookPassword(String p12LogbookPassword) {
        this.p12LogbookPassword = p12LogbookPassword;
    }

    /**
     * @return p12 logbook file
     */
    public String getP12LogbookFile() {
        return p12LogbookFile;
    }

    /**
     * @param p12LogbookFile file to set
     */
    public void setP12LogbookFile(String p12LogbookFile) {
        this.p12LogbookFile = p12LogbookFile;
    }
}
