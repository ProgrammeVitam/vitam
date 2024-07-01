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
package fr.gouv.vitam.storage.engine.server.storagetraceability;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class OfferLogIteratorTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageDistribution storageDistribution;

    @Test
    public void testEmpty() throws Exception {
        // Given
        OfferLogIterator offerLogIterator = new OfferLogIterator(
            VitamConfiguration.getDefaultStrategy(),
            Order.DESC,
            DataCategory.UNIT,
            storageDistribution,
            1000
        );
        doReturn(new RequestResponseOK<OfferLog>())
            .when(storageDistribution)
            .getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 1000, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageDistribution).getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT,
            null,
            1000,
            Order.DESC
        );
        verifyNoMoreInteractions(storageDistribution);
    }

    @Test
    public void testOnePage() throws Exception {
        // Given
        OfferLogIterator offerLogIterator = new OfferLogIterator(
            VitamConfiguration.getDefaultStrategy(),
            Order.DESC,
            DataCategory.UNIT,
            storageDistribution,
            1000
        );

        doReturn(
            new RequestResponseOK<OfferLog>().addAllResults(
                Arrays.asList(
                    new OfferLog(500L, null, "0_unit", "file1", OfferLogAction.WRITE),
                    new OfferLog(400L, null, "0_unit", "file2", OfferLogAction.WRITE),
                    new OfferLog(300L, null, "0_unit", "file3", OfferLogAction.WRITE)
                )
            )
        )
            .when(storageDistribution)
            .getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 1000, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file1");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file2");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file3");
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageDistribution).getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT,
            null,
            1000,
            Order.DESC
        );
        verifyNoMoreInteractions(storageDistribution);
    }

    @Test
    public void testExactlyOnePage() throws Exception {
        // Given
        OfferLogIterator offerLogIterator = new OfferLogIterator(
            VitamConfiguration.getDefaultStrategy(),
            Order.DESC,
            DataCategory.UNIT,
            storageDistribution,
            2
        );

        doReturn(
            new RequestResponseOK<OfferLog>().addAllResults(
                Arrays.asList(
                    new OfferLog(500L, null, "0_unit", "file1", OfferLogAction.WRITE),
                    new OfferLog(400L, null, "0_unit", "file2", OfferLogAction.WRITE)
                )
            )
        )
            .when(storageDistribution)
            .getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 2, Order.DESC);

        doReturn(new RequestResponseOK<OfferLog>())
            .when(storageDistribution)
            .getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, 399L, 2, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file1");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file2");
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageDistribution).getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT,
            null,
            2,
            Order.DESC
        );
        verify(storageDistribution).getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT,
            399L,
            2,
            Order.DESC
        );
        verifyNoMoreInteractions(storageDistribution);
    }

    @Test
    public void testMultiPages() throws Exception {
        // Given
        OfferLogIterator offerLogIterator = new OfferLogIterator(
            VitamConfiguration.getDefaultStrategy(),
            Order.DESC,
            DataCategory.UNIT,
            storageDistribution,
            2
        );

        doReturn(
            new RequestResponseOK<OfferLog>().addAllResults(
                Arrays.asList(
                    new OfferLog(500L, null, "0_unit", "file1", OfferLogAction.WRITE),
                    new OfferLog(400L, null, "0_unit", "file2", OfferLogAction.WRITE)
                )
            )
        )
            .when(storageDistribution)
            .getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, null, 2, Order.DESC);

        doReturn(
            new RequestResponseOK<OfferLog>().addAllResults(
                Arrays.asList(new OfferLog(300L, null, "0_unit", "file3", OfferLogAction.WRITE))
            )
        )
            .when(storageDistribution)
            .getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, 399L, 2, Order.DESC);

        // When / Then
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file1");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file2");
        assertThat(offerLogIterator.hasNext()).isTrue();
        assertThat(offerLogIterator.next().getFileName()).isEqualTo("file3");
        assertThat(offerLogIterator.hasNext()).isFalse();
        assertThatThrownBy(offerLogIterator::next).isInstanceOf(NoSuchElementException.class);

        verify(storageDistribution).getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT,
            null,
            2,
            Order.DESC
        );
        verify(storageDistribution).getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT,
            399L,
            2,
            Order.DESC
        );
        verifyNoMoreInteractions(storageDistribution);
    }
}
