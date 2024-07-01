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
package fr.gouv.vitam.worker.core.plugin.reclassification.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GraphCycleDetectorTest {

    @Test
    public void testAddRelations_duplicatesOk() {
        // Given
        GraphCycleDetector instance = new GraphCycleDetector();

        // When
        instance.addRelations("1", Arrays.asList("2", "3"));
        instance.addRelations("1", Arrays.asList("2", "4"));

        // Then
        assertThat(instance.getChildToParents().keySet()).hasSize(1);
        assertThat(instance.getChildToParents().get("1")).containsExactlyInAnyOrder("2", "3", "4");

        assertThat(instance.getParentToChildren().keySet()).hasSize(3);
        assertThat(instance.getParentToChildren().get("2")).containsExactlyInAnyOrder("1");
        assertThat(instance.getParentToChildren().get("3")).containsExactlyInAnyOrder("1");
        assertThat(instance.getParentToChildren().get("4")).containsExactlyInAnyOrder("1");
    }

    @Test
    public void testAddAndRemoveRelations() {
        // Given
        GraphCycleDetector instance = new GraphCycleDetector();

        // When
        instance.addRelations("1", Arrays.asList("2", "3"));
        instance.addRelations("2", Arrays.asList("4", "5"));
        instance.removeRelations("2", Arrays.asList("5"));

        // Then
        assertThat(instance.getChildToParents().keySet()).hasSize(2);
        assertThat(instance.getChildToParents().get("1")).containsExactlyInAnyOrder("2", "3");
        assertThat(instance.getChildToParents().get("2")).containsExactlyInAnyOrder("4");

        assertThat(instance.getParentToChildren().keySet()).hasSize(3);
        assertThat(instance.getParentToChildren().get("2")).containsExactlyInAnyOrder("1");
        assertThat(instance.getParentToChildren().get("3")).containsExactlyInAnyOrder("1");
        assertThat(instance.getParentToChildren().get("4")).containsExactlyInAnyOrder("2");
    }

    @Test
    public void testRemoveRelations() {
        // Given
        GraphCycleDetector instance = new GraphCycleDetector();

        // When
        instance.addRelations("1", Arrays.asList("2", "3", "4"));
        instance.removeRelations("1", Arrays.asList("3"));
        instance.removeRelations("1", Arrays.asList("3"));

        // Then
        assertThat(instance.getChildToParents().keySet()).hasSize(1);
        assertThat(instance.getChildToParents().get("1")).containsExactlyInAnyOrder("2", "4");

        assertThat(instance.getParentToChildren().keySet()).hasSize(2);
        assertThat(instance.getParentToChildren().get("2")).containsExactlyInAnyOrder("1");
        assertThat(instance.getParentToChildren().get("4")).containsExactlyInAnyOrder("1");
    }

    @Test
    public void testCheckCycles_Empty() {
        // Given : empty graph
        GraphCycleDetector instance = new GraphCycleDetector();

        // When
        Set<String> cycles = instance.checkCycles();

        // Then
        assertThat(cycles).isEmpty();
    }

    @Test
    public void testCheckCycles_GraphWithoutCycles() {
        // Given : Graph without cycles
        /*
         *      1       6     8
         *      ↑ ↖   ↗ ↑
         *      2   3   7
         *      ↑ ↖ ↑
         *      4 ➝ 5
         */

        GraphCycleDetector instance = new GraphCycleDetector();
        instance.addRelations("1", Arrays.asList());
        instance.addRelations("2", Arrays.asList("1"));
        instance.addRelations("3", Arrays.asList("1", "6"));
        instance.addRelations("4", Arrays.asList("2", "5"));
        instance.addRelations("5", Arrays.asList("2", "3"));
        instance.addRelations("6", Arrays.asList());
        instance.addRelations("7", Arrays.asList("6"));
        instance.addRelations("8", Arrays.asList());

        // When
        Set<String> cycles = instance.checkCycles();

        // Then
        assertThat(cycles).isEmpty();
    }

    @Test
    public void testCheckCycles_SingletonGraphWithCycles() {
        // Given : A graph with self-referencing node
        GraphCycleDetector instance = new GraphCycleDetector();
        instance.addRelations("1", Arrays.asList("1"));

        // When
        Set<String> cycles = instance.checkCycles();

        // Then
        assertThat(cycles).containsExactlyInAnyOrder("1");
    }

    @Test
    public void testCheckCycles_BasicGraphWithCycles() {
        // Given : Basic cycle
        /*
         *     1
         *    ↗ ↘
         *   2 ← 3
         */
        GraphCycleDetector instance = new GraphCycleDetector();
        instance.addRelations("1", Arrays.asList("2"));
        instance.addRelations("2", Arrays.asList("3"));
        instance.addRelations("3", Arrays.asList("1"));

        // When
        Set<String> cycles = instance.checkCycles();

        // Then
        assertThat(cycles).containsExactlyInAnyOrder("1", "2", "3");
    }

    @Test
    public void testCheckCycles_GraphWithOneCycle() {
        // Given : Graph with one cycle
        /*
         *   1  4   5
         *   ↑ ↗  ↗
         *   2 ➝ 6
         *   ↑ ↙ ↑
         *   3   7
         *   ↑
         *   8
         */

        // Given : empty graph
        GraphCycleDetector instance = new GraphCycleDetector();
        instance.addRelations("1", Arrays.asList());
        instance.addRelations("2", Arrays.asList("1", "4", "6"));
        instance.addRelations("3", Arrays.asList("2"));
        instance.addRelations("4", Arrays.asList());
        instance.addRelations("5", Arrays.asList());
        instance.addRelations("6", Arrays.asList("3", "5"));
        instance.addRelations("7", Arrays.asList("6"));
        instance.addRelations("8", Arrays.asList("3"));

        // When
        Set<String> cycles = instance.checkCycles();

        // Then
        assertThat(cycles).containsExactlyInAnyOrder("2", "6", "3");
    }

    @Test
    public void testCheckCycles_GraphWithMultipleCycles() {
        // Given : Graph with multiple cycles
        /*
         *    1   4   5
         *    ↑ ↗   ↗ ↑
         *    2 ➝ 6   9 ➝ 10
         *    ↑ ↙ ↑   ↑ ↙
         *    3   7   11
         *    ↑       ↑
         *    8       12
         */
        GraphCycleDetector instance = new GraphCycleDetector();
        instance.addRelations("1", Arrays.asList());
        instance.addRelations("2", Arrays.asList("1", "4", "6"));
        instance.addRelations("3", Arrays.asList("2"));
        instance.addRelations("4", Arrays.asList());
        instance.addRelations("5", Arrays.asList());
        instance.addRelations("6", Arrays.asList("3", "5"));
        instance.addRelations("7", Arrays.asList("6"));
        instance.addRelations("8", Arrays.asList("3"));
        instance.addRelations("9", Arrays.asList("5,", "10"));
        instance.addRelations("10", Arrays.asList("11"));
        instance.addRelations("11", Arrays.asList("9"));
        instance.addRelations("12", Arrays.asList("11"));

        // When
        Set<String> cycles = instance.checkCycles();

        // Then
        assertThat(cycles).containsExactlyInAnyOrder("2", "6", "3", "9", "10", "11");
    }
}
