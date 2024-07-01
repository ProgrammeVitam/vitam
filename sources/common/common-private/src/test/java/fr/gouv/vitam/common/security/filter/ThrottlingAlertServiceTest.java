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

package fr.gouv.vitam.common.security.filter;

import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.time.LogicalClockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ThrottlingAlertServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Mock
    private AlertService alertService;

    private ThrottlingAlertService throttlingAlertService;

    @Before
    public void init() {
        this.throttlingAlertService = new ThrottlingAlertService(alertService, "error_message", 60);
        this.logicalClock.freezeTime();
    }

    @Test
    public void testFirstAlert() {
        // Given

        // When
        throttlingAlertService.reportAlert();

        // Then
        verify(alertService, times(1)).createAlert("error_message");
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void testMultipleAlerts() {
        // Given

        // When
        for (int i = 0; i < 10; i++) {
            throttlingAlertService.reportAlert();
            logicalClock.logicalSleep(19, ChronoUnit.SECONDS);
        }

        // Then
        verify(alertService).createAlert("error_message");
        verify(alertService, times(2)).createAlert("4 redundant errors. error_message");
        verifyNoMoreInteractions(alertService);
    }

    @Test
    public void testMultipleSparseAlerts() {
        // Given

        // When
        for (int i = 0; i < 10; i++) {
            throttlingAlertService.reportAlert();
            logicalClock.logicalSleep(3, ChronoUnit.MINUTES);
        }

        // Then
        verify(alertService, times(10)).createAlert("error_message");
        verifyNoMoreInteractions(alertService);
    }
}
