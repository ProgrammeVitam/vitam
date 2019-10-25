/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

package fr.gouv.vitam.common.metrics;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertNotNull;

public class MetricsFeatureTest extends ResteasyTestApplication {

    private final static CommonBusinessApplication
        commonBusinessApplication = new CommonBusinessApplication();
    ;

    public static VitamServerTestRunner
        vitamServerTestRunner =
        new VitamServerTestRunner(MetricsFeatureTest.class);


    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(commonBusinessApplication.getResources(), new SimpleJerseyMetricsResource(),
            new AdvancedJerseyMetricsResource(), new ShouldNotWorkJerseyMetricsResource(),
            new MediaTypeJerseyMetricsResource());
    }

    @Override
    public Set<Class<?>> getClasses() {
        return commonBusinessApplication.getClasses();
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Test
    public void testSimpleJerseyMetricsResource() {
        final VitamMetrics metrics = commonBusinessApplication.metrics.get(VitamMetricsType.REST);

        assertNotNull("VitamMetrics", metrics);
        Assertions.assertThat(metrics.getRegistry().getMetrics().keySet())
            .containsAll(SimpleJerseyMetricsResource.expectedNames);
    }

    @Test
    public void testAdvancedJerseyMetricsResource() {
        final VitamMetrics metrics = commonBusinessApplication.metrics.get(VitamMetricsType.REST);

        assertNotNull("VitamMetrics", metrics);
        Assertions.assertThat(metrics.getRegistry().getMetrics().keySet())
            .containsAll(AdvancedJerseyMetricsResource.expectedNames);
    }

    @Test
    public void testMediaTypeJerseyMetricsResource() {
        final VitamMetrics metrics = commonBusinessApplication.metrics.get(VitamMetricsType.REST);

        assertNotNull("VitamMetrics", metrics);
        Assertions.assertThat(metrics.getRegistry().getMetrics().keySet())
            .containsAll(MediaTypeJerseyMetricsResource.expectedNames);
    }

    @Test
    public void testRegistrySize() {
        final VitamMetrics metrics = commonBusinessApplication.metrics.get(VitamMetricsType.REST);

        assertNotNull("VitamMetrics", metrics);

        // Multiply the expectedSize by 3 because we expect a timer, a meter and an exceptionMeter per name.
        Assertions.assertThat(42).isEqualTo(metrics.getRegistry().getMetrics().size());
    }
}
