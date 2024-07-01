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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.security.merkletree.MerkleTree.EMPTY_LEAF;

/**
 * MerkleTreeAlgo
 */
public class MerkleTreeAlgo {

    private final DigestType digestType;
    private List<MerkleTree> leaves = new ArrayList<>();

    /**
     * @param digestType
     */
    public MerkleTreeAlgo(DigestType digestType) {
        this.digestType = digestType;
    }

    /**
     * adds leaf to the MerkleTree
     *
     * @param str
     */
    @VisibleForTesting
    void addLeaf(String str) {
        addLeaf(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * adds leaf to the MerkleTree
     *
     * @param data
     */
    public void addLeaf(byte[] data) {
        Digest digest = new Digest(digestType);
        MerkleTree tree = new MerkleTree(digest.update(data).digest(), null, null);
        leaves.add(tree);
    }

    @VisibleForTesting
    int numberOfLeaves() {
        return leaves.size();
    }

    /**
     * concat two hash to compute another one.
     *
     * @param left
     * @param right
     * @return byte[] generated Hash
     */
    private byte[] concat(byte[] left, byte[] right) {
        final Digest digest = new Digest(digestType);
        digest.update(left);
        digest.update(right);
        return digest.digest();
    }

    /**
     * adds padding when leaf number isn't 2^n
     */
    @VisibleForTesting
    void addPadding() {
        int numberOfLeaf = leaves.size();
        if (Long.bitCount(numberOfLeaf) == 1) {
            return;
        }

        long l = Long.highestOneBit(2 * numberOfLeaf);
        for (int j = 0; j < l - numberOfLeaf; j++) {
            leaves.add(EMPTY_LEAF);
        }
    }

    /**
     * @return MerkleTree
     */
    public MerkleTree generateMerkle() {
        addPadding();
        MerkleTree tree = Iterables.getFirst(leaves, null);
        while (leaves.size() > 1) {
            final List<MerkleTree> nextList = new ArrayList<>();
            for (int i = 0; i < leaves.size(); i = i + 2) {
                byte[] hash = concat(leaves.get(i).getRoot(), leaves.get(i + 1).getRoot());
                tree = new MerkleTree(hash, leaves.get(i), leaves.get(i + 1));
                nextList.add(tree);
            }
            leaves = nextList;
        }
        return tree;
    }
}
