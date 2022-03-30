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

import fr.gouv.vitam.common.PropertiesUtils;
import org.apache.commons.collections4.SetUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * Handles PersonalCertificatePermissionConfig loading &amp; validation
 */
public class PersonalCertificatePermissionConfigLoader {

    /**
     * Handles PersonalCertificatePermissionConfig loading and validation
     *
     * @param configFile config file path
     * @return
     * @throws IOException
     */
    public static PersonalCertificatePermissionConfig loadPersonalCertificatePermissionConfig(String configFile)
        throws IOException {

        final PersonalCertificatePermissionConfig config;
        try (final InputStream personalCertificatePermissionIS = PropertiesUtils.getConfigAsStream(
            configFile)) {

            config = PropertiesUtils.readYaml(
                personalCertificatePermissionIS, PersonalCertificatePermissionConfig.class);
        }

        if (config.getPermissionsRequiringPersonalCertificate() == null)
            config.setPermissionsRequiringPersonalCertificate(Collections.EMPTY_SET);

        if (config.getPermissionsWithoutPersonalCertificate() == null)
            config.setPermissionsWithoutPersonalCertificate(Collections.EMPTY_SET);

        validateConfiguration(config);

        return config;
    }

    private static void validateConfiguration(PersonalCertificatePermissionConfig config) throws IOException {

        SetUtils.SetView<String> intersection = SetUtils
            .intersection(config.getPermissionsRequiringPersonalCertificate(),
                config.getPermissionsWithoutPersonalCertificate());

        if (!intersection.isEmpty()) {
            throw new IllegalStateException(
                "Invalid configuration file. A permission cannot be both requiring and non requiring personal certificate. " +
                    intersection.toString());
        }
    }
}
