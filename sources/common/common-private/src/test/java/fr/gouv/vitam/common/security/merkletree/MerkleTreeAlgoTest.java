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
package fr.gouv.vitam.common.security.merkletree;

import fr.gouv.vitam.common.BaseXx;
import org.junit.Before;
import org.junit.Test;

import static fr.gouv.vitam.common.digest.DigestType.SHA512;
import static org.assertj.core.api.Assertions.assertThat;

public class MerkleTreeAlgoTest {

    private MerkleTreeAlgo merkleTreeAlgo;

    @Before
    public void init() {
        merkleTreeAlgo = new MerkleTreeAlgo(SHA512);
    }

    @Test
    public void shoud_compute_merkle_tree() {
        // Given
        merkleTreeAlgo.addLeaf("a");
        merkleTreeAlgo.addLeaf("b");
        merkleTreeAlgo.addLeaf("c");

        // When
        final MerkleTree mt = merkleTreeAlgo.generateMerkle();

        // Then
        assertThat(mt.getRoot()).isEqualTo(
            BaseXx.getFromBase64(
                "QC71vcS+qHQfQEr9kfpQ6Ud0O5myI2GacxkhrzY+jYAch4TFMIgH5nueosyQLLlM1fwGPU4Cah+o+RhWQYbj2w=="
            )
        );
    }

    @Test
    public void shoud_compute_merkle_tree_for_one_element() {
        // Given
        merkleTreeAlgo.addLeaf("a");

        // When
        final MerkleTree mt = merkleTreeAlgo.generateMerkle();

        // Then
        assertThat(mt.getRoot()).isEqualTo(
            BaseXx.getFromBase64(
                "H0D8ktokFpR1CXnubPWC8tXX0o4YM13gWrxU0FYOD1MChgxlK/CNVgJSql50IQVG82n7u86MEs/HlXsmUv6adQ=="
            )
        );
    }

    @Test
    public void should_add_thre_element_when_five_leafs() {
        // Given
        merkleTreeAlgo.addLeaf("a");
        merkleTreeAlgo.addLeaf("b");
        merkleTreeAlgo.addLeaf("c");
        merkleTreeAlgo.addLeaf("d");
        merkleTreeAlgo.addLeaf("e");

        // When
        merkleTreeAlgo.addPadding();

        // Then
        assertThat(merkleTreeAlgo.numberOfLeaves()).isEqualTo(8);
    }

    @Test
    public void should_not_add_element_when_four_leafs() {
        // Given
        merkleTreeAlgo.addLeaf("a");
        merkleTreeAlgo.addLeaf("b");
        merkleTreeAlgo.addLeaf("c");
        merkleTreeAlgo.addLeaf("d");

        // When
        merkleTreeAlgo.addPadding();

        // Then
        assertThat(merkleTreeAlgo.numberOfLeaves()).isEqualTo(4);
    }
}
