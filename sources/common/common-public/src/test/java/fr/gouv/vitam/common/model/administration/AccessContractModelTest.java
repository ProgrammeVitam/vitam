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
package fr.gouv.vitam.common.model.administration;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Data Transfer Object Model of access contract (DTO).
 */

public class AccessContractModelTest {

    private static final Integer TENANT_ID = 0;

    @Test
    public void testConstructor() {
        AccessContractModel contract = new AccessContractModel();
        final String id = "aeaqaaaaaahfrfvaaahrgak25v5fttiaaaaq";
        String name = "aName";
        String description = "aDescription of the contract";
        String creationdate = "08/12/2016";
        String lastupdate = "10/12/2016";
        String activationdate = "10/12/2016";
        String deactivationdate = "09/12/2016";
        Set<String> originatingAgencies = new HashSet<>();
        originatingAgencies.add("FR_FAKE");
        Set<String> rootUnits = Sets.newHashSet("guid");
        Set<String> excludedRootUnits = Sets.newHashSet("excludedGuid");
        Set<RuleType> RuleCategoryToFilter = Sets.newHashSet(RuleType.DisseminationRule);
        ActivationStatus accessLog = ActivationStatus.ACTIVE;
        contract
            .setId(id)
            .setTenant(TENANT_ID)
            .setName(name)
            .setDescription(description)
            .setStatus(ActivationStatus.ACTIVE)
            .setLastupdate(lastupdate)
            .setCreationdate(creationdate)
            .setActivationdate(activationdate)
            .setDeactivationdate(deactivationdate);
        contract
            .setOriginatingAgencies(originatingAgencies)
            .setEveryOriginatingAgency(true)
            .setRootUnits(rootUnits)
            .setExcludedRootUnits(excludedRootUnits)
            .setAccessLog(accessLog)
            .setRuleCategoryToFilter(RuleCategoryToFilter);

        assertEquals(id, contract.getId());
        assertEquals(name, contract.getName());
        assertEquals(description, contract.getDescription());
        assertEquals(creationdate, contract.getCreationdate());
        assertEquals(activationdate, contract.getActivationdate());
        assertEquals(deactivationdate, contract.getDeactivationdate());
        assertEquals(originatingAgencies, contract.getOriginatingAgencies());
        assertEquals(rootUnits, contract.getRootUnits());
        assertEquals(excludedRootUnits, contract.getExcludedRootUnits());
        assertEquals(accessLog, contract.getAccessLog());
        assertEquals(RuleCategoryToFilter, contract.getRuleCategoryToFilter());
    }

    @Test
    public void should_initialize_default_value() {
        // Given
        AccessContractModel accessContractModel = new AccessContractModel();

        // When
        accessContractModel.initializeDefaultValue();

        // Then
        assertThat(accessContractModel.getEveryOriginatingAgency()).isFalse();
        assertThat(accessContractModel.getWritingPermission()).isFalse();
        assertThat(accessContractModel.isEveryDataObjectVersion()).isFalse();
        assertThat(accessContractModel.getWritingRestrictedDesc()).isFalse();
        assertEquals(ActivationStatus.INACTIVE, accessContractModel.getAccessLog());
    }

    @Test
    public void should_not_initialize_default_value_if_already_present() {
        // Given
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryOriginatingAgency(true);
        accessContractModel.setEveryDataObjectVersion(true);
        accessContractModel.setWritingPermission(true);
        accessContractModel.setWritingRestrictedDesc(true);
        accessContractModel.setAccessLog(ActivationStatus.INACTIVE);

        // When
        accessContractModel.initializeDefaultValue();

        // Then
        assertThat(accessContractModel.getEveryOriginatingAgency()).isTrue();
        assertThat(accessContractModel.getWritingPermission()).isTrue();
        assertThat(accessContractModel.isEveryDataObjectVersion()).isTrue();
        assertThat(accessContractModel.getWritingRestrictedDesc()).isTrue();
        assertEquals(ActivationStatus.INACTIVE, accessContractModel.getAccessLog());
    }
}
