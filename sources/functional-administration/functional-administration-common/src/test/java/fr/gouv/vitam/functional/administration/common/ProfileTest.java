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
package fr.gouv.vitam.functional.administration.common;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.administration.ProfileFormat;
import fr.gouv.vitam.common.model.administration.ProfileStatus;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */

public class ProfileTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final Integer TENANT_ID = 0;

    @Test
    @RunWithCustomExecutor
    public void testConstructor() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Profile profile = new Profile();
        final String id = GUIDFactory.newProfileGUID(TENANT_ID).getId();
        String identifier = "aIdentifier";
        String name = "aName";
        String description = "aDescription of the contract";
        String lastupdate = "10/12/2016";
        profile
            .setId(id)
            .setIdentifier(identifier)
            .setName(name)
            .setDescription(description)
            .setStatus(ProfileStatus.ACTIVE)
            .setFormat(ProfileFormat.XSD)
            .setLastupdate(lastupdate)
            .setCreationdate(lastupdate)
            .setActivationdate(lastupdate)
            .setDeactivationdate(lastupdate);

        assertEquals(id, profile.getId());
        assertEquals(identifier, profile.getIdentifier());
        assertEquals(name, profile.getName());
        assertEquals(description, profile.getDescription());
        assertEquals(ProfileStatus.ACTIVE, profile.getStatus());
        assertEquals(ProfileFormat.XSD, profile.getFormat());
        assertEquals(lastupdate, profile.getCreationdate());
        assertEquals(lastupdate, profile.getActivationdate());
        assertEquals(lastupdate, profile.getDeactivationdate());
    }
}
