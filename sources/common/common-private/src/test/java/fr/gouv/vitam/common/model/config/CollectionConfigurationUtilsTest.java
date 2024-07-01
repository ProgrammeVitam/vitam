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

package fr.gouv.vitam.common.model.config;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CollectionConfigurationUtilsTest {

    @Test
    public void TestNullsAllowed() {
        CollectionConfiguration collectionConfiguration1 = new CollectionConfiguration(null, 10);
        CollectionConfiguration collectionConfiguration2 = new CollectionConfiguration(10, null);
        CollectionConfiguration collectionConfiguration3 = new CollectionConfiguration(null, null);
        CollectionConfiguration collectionConfiguration4 = new CollectionConfiguration(10, 10);

        assertThatCode(
            () -> CollectionConfigurationUtils.validate(collectionConfiguration1, true)
        ).doesNotThrowAnyException();

        assertThatCode(
            () -> CollectionConfigurationUtils.validate(collectionConfiguration2, true)
        ).doesNotThrowAnyException();

        assertThatCode(
            () -> CollectionConfigurationUtils.validate(collectionConfiguration3, true)
        ).doesNotThrowAnyException();

        assertThatCode(
            () -> CollectionConfigurationUtils.validate(collectionConfiguration4, true)
        ).doesNotThrowAnyException();
    }

    @Test
    public void TestNullsNotAllowed() {
        CollectionConfiguration collectionConfiguration1 = new CollectionConfiguration(null, 10);
        CollectionConfiguration collectionConfiguration2 = new CollectionConfiguration(10, null);
        CollectionConfiguration collectionConfiguration3 = new CollectionConfiguration(null, null);
        CollectionConfiguration collectionConfiguration4 = new CollectionConfiguration(10, 10);

        assertThatThrownBy(() -> CollectionConfigurationUtils.validate(collectionConfiguration1, false)).isInstanceOf(
            IllegalStateException.class
        );

        assertThatThrownBy(() -> CollectionConfigurationUtils.validate(collectionConfiguration2, false)).isInstanceOf(
            IllegalStateException.class
        );

        assertThatThrownBy(() -> CollectionConfigurationUtils.validate(collectionConfiguration3, false)).isInstanceOf(
            IllegalStateException.class
        );

        assertThatCode(
            () -> CollectionConfigurationUtils.validate(collectionConfiguration4, false)
        ).doesNotThrowAnyException();
    }

    @Test
    public void TestMinMaxShards() {
        CollectionConfiguration collectionConfiguration1 = new CollectionConfiguration(0, 10);
        CollectionConfiguration collectionConfiguration2 = new CollectionConfiguration(1, 10);
        CollectionConfiguration collectionConfiguration3 = new CollectionConfiguration(1000, 10);
        CollectionConfiguration collectionConfiguration4 = new CollectionConfiguration(1001, 10);

        assertThatThrownBy(() -> CollectionConfigurationUtils.validate(collectionConfiguration1, false)).isInstanceOf(
            IllegalStateException.class
        );

        assertThatCode(
            () -> CollectionConfigurationUtils.validate(collectionConfiguration2, false)
        ).doesNotThrowAnyException();

        assertThatCode(
            () -> CollectionConfigurationUtils.validate(collectionConfiguration3, false)
        ).doesNotThrowAnyException();

        assertThatThrownBy(() -> CollectionConfigurationUtils.validate(collectionConfiguration4, false)).isInstanceOf(
            IllegalStateException.class
        );
    }

    @Test
    public void TestMinMaxReplicas() {
        CollectionConfiguration collectionConfiguration1 = new CollectionConfiguration(10, -1);
        CollectionConfiguration collectionConfiguration2 = new CollectionConfiguration(10, 0);
        CollectionConfiguration collectionConfiguration3 = new CollectionConfiguration(10, 100);
        CollectionConfiguration collectionConfiguration4 = new CollectionConfiguration(10, 101);

        assertThatThrownBy(() -> CollectionConfigurationUtils.validate(collectionConfiguration1, false)).isInstanceOf(
            IllegalStateException.class
        );

        assertThatCode(
            () -> CollectionConfigurationUtils.validate(collectionConfiguration2, false)
        ).doesNotThrowAnyException();

        assertThatCode(
            () -> CollectionConfigurationUtils.validate(collectionConfiguration3, false)
        ).doesNotThrowAnyException();

        assertThatThrownBy(() -> CollectionConfigurationUtils.validate(collectionConfiguration4, false)).isInstanceOf(
            IllegalStateException.class
        );
    }
}
