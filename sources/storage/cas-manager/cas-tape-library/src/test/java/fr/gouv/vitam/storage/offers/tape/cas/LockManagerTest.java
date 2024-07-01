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
package fr.gouv.vitam.storage.offers.tape.cas;

import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class LockManagerTest {

    @Test
    public void givenMultipleLockedUnlockedItemsThenOK() {
        // Given
        LockManager<String> instance = new LockManager<>();

        // Then
        assertThat(instance.isLocked("item1")).isFalse();

        // When : Multiple locking / unlocking / re-locking
        LockHandle lock1 = instance.createLock(Set.of("item1"));
        LockHandle lock2 = instance.createLock(Set.of("item1", "item2", "item3", "item4"));
        LockHandle lock3 = instance.createLock(Set.of("item4", "item5"));
        lock2.release();
        LockHandle lock4 = instance.createLock(Set.of("item3"));

        // Then
        assertThat(instance.isLocked("item1")).isTrue();
        assertThat(instance.isLocked("item2")).isFalse();
        assertThat(instance.isLocked("item3")).isTrue();
        assertThat(instance.isLocked("item4")).isTrue();
        assertThat(instance.isLocked("item5")).isTrue();
        assertThat(instance.isLocked("item6")).isFalse();

        // When : Try releasing lock twice
        assertThatCode(lock2::release).doesNotThrowAnyException();

        // Then : Nothing
        assertThat(instance.isLocked("item1")).isTrue();
        assertThat(instance.isLocked("item2")).isFalse();
        assertThat(instance.isLocked("item4")).isTrue();

        // When : Releasing another lock
        lock4.release();

        // Then : Item unlocked
        assertThat(instance.isLocked("item3")).isFalse();

        // When : Releasing all remaining locks
        lock1.release();
        lock3.release();

        // Then : No more locks
        assertThat(instance.isLocked("item1")).isFalse();
        assertThat(instance.isLocked("item2")).isFalse();
        assertThat(instance.isLocked("item3")).isFalse();
        assertThat(instance.isLocked("item4")).isFalse();
        assertThat(instance.isLocked("item5")).isFalse();
        assertThat(instance.isLocked("item6")).isFalse();
    }
}
