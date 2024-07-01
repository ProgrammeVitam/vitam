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
package fr.gouv.vitam.common.client.configuration;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * SSL Key
 */
public class SSLKey {

    private static final String PARAMETERS = "SSLKey parameters";
    private String keyPath;
    private String keyPassword;

    /**
     * Empty constructor
     */
    public SSLKey() {
        // Empty
    }

    /**
     * @param keyPath
     * @param keyPassword
     * @throws IllegalArgumentException if keyPath/keyPassword is null or empty
     */
    public SSLKey(String keyPath, String keyPassword) {
        ParametersChecker.checkParameter(PARAMETERS, keyPath, keyPassword);

        this.keyPath = keyPath;
        this.keyPassword = keyPassword;
    }

    /**
     * @return the keyPath
     */
    public String getKeyPath() {
        return keyPath;
    }

    /**
     * @param keyPath the keyPath to set
     * @return this
     * @throws IllegalArgumentException if keyPath is null or empty
     */
    public SSLKey setKeyPath(String keyPath) {
        this.keyPath = keyPath;
        return this;
    }

    /**
     * @return the keyPassword
     */
    public String getKeyPassword() {
        return keyPassword;
    }

    /**
     * @param keyPassword the keyPassword to set
     * @return this
     * @throws IllegalArgumentException if keyPassword is null or empty
     */
    public SSLKey setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
        return this;
    }
}
