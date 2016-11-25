/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.common.security.merkletree;

/**
 * MerkleTree
 */
public class MerkleTree {

    private byte[] root = new byte[0];
    private MerkleTree l;
    private MerkleTree r;


    /**
     * MerkleTree empty constructor
     */
    public MerkleTree() {}

    /**
     * Initialize the needed parameters for MerkleTree constructor
     *
     * @param root : the root of the MerkleTree
     * @param l : the left sheet of the MerkleTree
     * @param r : the right sheet of the MerkleTree
     */
    public MerkleTree(byte[] root, MerkleTree l, MerkleTree r) {
        this.root = root;
        this.l = l;
        this.r = r;
    }

    /**
     *
     * @return root for type byte[]
     */
    public byte[] getRoot() {
        return root;
    }

    /**
     * setter root
     *
     * @param root
     */
    public void setRoot(byte[] root) {
        this.root = root;
    }

    /**
     *
     * @return left sheet for type MerkleTree
     */
    public MerkleTree getL() {
        return l;
    }

    /**
     * setter left sheet
     *
     * @param l
     */
    public void setL(MerkleTree l) {
        this.l = l;
    }

    /**
     *
     * @return right sheet for type MerkleTree
     */
    public MerkleTree getR() {
        return r;
    }

    /**
     * setter right sheet
     *
     * @param r
     */
    public void setR(MerkleTree r) {
        this.r = r;
    }
}

