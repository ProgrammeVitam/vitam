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
package fr.gouv.vitam.collect.internal.core.helpers;

import fr.gouv.vitam.collect.internal.core.helpers.builders.DbVersionsModelBuilder;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class DbVersionsModelBuilderTest {

    @Test
    public void should_generate_appropriate_DbVersionsModel_with_correct_inputs() {
        // GIVEN
        String fileName = "memoire_nationale.txt";
        String versionId = "aebbaaaaacaltpovaewckal62ukh4ml5a67q";
        DataObjectVersionType usage = DataObjectVersionType.BINARY_MASTER;
        int version = 1;

        // WHEN
        DbVersionsModel versionsModel = new DbVersionsModelBuilder().build(versionId, fileName, usage, version);

        // THEN
        assertThat(versionsModel).isNotNull();
        assertThat(versionsModel.getDataObjectVersion()).isEqualTo(usage.getName() + "_" + version);
    }

    @Test
    public void should_throws_an_error_when_usage_is_not_provided() {
        // GIVEN
        String fileName = "memoire_nationale.txt";
        String versionId = "aebbaaaaacaltpovaewckal62ukh4ml5a67q";
        int version = 1;

        // WHEN // THEN
        assertThatThrownBy(() -> new DbVersionsModelBuilder().build(versionId, fileName, null, version)).hasMessage(
            "Usage can't be null"
        );
    }

    @Test
    public void should_throws_an_error_when_fileName_is_not_provided() {
        // GIVEN
        String versionId = "aebbaaaaacaltpovaewckal62ukh4ml5a67q";
        DataObjectVersionType usage = DataObjectVersionType.BINARY_MASTER;
        int version = 1;

        // WHEN // THEN
        assertThatThrownBy(() -> new DbVersionsModelBuilder().build(versionId, null, usage, version)).hasMessage(
            "FileName can't be null"
        );
    }

    @Test
    public void should_throws_an_error_when_versionId_is_not_provided() {
        // GIVEN
        String fileName = "memoire_nationale.txt";
        DataObjectVersionType usage = DataObjectVersionType.BINARY_MASTER;
        int version = 1;

        // WHEN // THEN
        assertThatThrownBy(() -> new DbVersionsModelBuilder().build(null, fileName, usage, version)).hasMessage(
            "VersionId can't be null"
        );
    }
}
