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
package fr.gouv.vitam.security.internal.rest.service;

import com.google.common.collect.Sets;
import fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel;
import fr.gouv.vitam.security.internal.rest.server.PersonalCertificatePermissionConfig;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionServiceTest {

    public static final String PERM_1 = "perm1";
    public static final String PERM_2 = "perm2";
    public static final String PERM_3 = "perm3";
    public static final String PERM_4 = "perm4";

    @Test
    public void isPersonalCertificateRequiredForPermission() throws Exception {

        PersonalCertificatePermissionConfig config = new PersonalCertificatePermissionConfig();
        config.setPermissionsRequiringPersonalCertificate(Sets.newHashSet(PERM_1, PERM_2));
        config.setPermissionsWithoutPersonalCertificate(Sets.newHashSet(PERM_3));

        PermissionService instance = new PermissionService(config);

        assertThat(instance.isPersonalCertificateRequiredForPermission(PERM_1).getResponse()).isEqualTo(
            IsPersonalCertificateRequiredModel.Response.REQUIRED_PERSONAL_CERTIFICATE);

        assertThat(instance.isPersonalCertificateRequiredForPermission(PERM_3).getResponse()).isEqualTo(
            IsPersonalCertificateRequiredModel.Response.IGNORED_PERSONAL_CERTIFICATE);

        assertThat(instance.isPersonalCertificateRequiredForPermission(PERM_4).getResponse()).isEqualTo(
            IsPersonalCertificateRequiredModel.Response.ERROR_UNKNOWN_PERMISSION);
    }
}
