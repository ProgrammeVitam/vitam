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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;

/**
 * MerkleTreeAlgo
 */
public class MerkleTreeAlgo {

    private DigestType digestType;
    private Map<Integer, List<MerkleTree>> myClassListMap = new LinkedHashMap<Integer, List<MerkleTree>>();
    private List<MerkleTree> niveau0 = new ArrayList<>();

    /**
     * 
     * @param digestType
     */
    public MerkleTreeAlgo(DigestType digestType) {
        this.digestType = digestType;
    }

    /**
     * adds sheet to the MerkleTree
     * 
     * @param str
     */
    public void addSheet(String str) {
        MerkleTree tree = new MerkleTree();
        Digest digest = new Digest(digestType);
        tree.setRoot(digest.update(str.getBytes()).digest());
        tree.setL(null);
        tree.setR(null);
        niveau0.add(tree);
    }

    /**
     * 
     * @param operations
     */
    private boolean isPowerOfTwo(List<MerkleTree> operations) {
        if (Long.bitCount(operations.size()) == 1)
            return true;
        return false;
    }

    /**
     * aggregates two args to generate their Hash
     * 
     * @param arg0
     * @param arg1
     * @return byte[] generated Hash
     */
    private byte[] compute(byte[] arg0, byte[] arg1) {
        Digest digest = new Digest(digestType);
        digest.update(arg0);
        digest.update(arg1);
        return digest.digest();
    }

    /**
     * adds padding when sheet number isn't 2^n
     * 
     */
    public void addPadding() {
        if (!isPowerOfTwo(niveau0)) {
            while (!isPowerOfTwo(niveau0)) {
                niveau0.add(new MerkleTree());
            }
        }
        myClassListMap.put(Integer.valueOf(0), niveau0);
    }

    /**
     * 
     * @return MerkleTree
     */
    public MerkleTree generateMerkle() {
        addPadding();
        MerkleTree arbre = new MerkleTree();
        Double niv = (Math.log(niveau0.size()) / Math.log(2));
        int n = 0;
        do {
            List<MerkleTree> arbreNiveau = new ArrayList<>();
            for (int i = 0; i < myClassListMap.get(n).size(); i = i + 2) {
                List<MerkleTree> niveau = myClassListMap.get(n);
                arbre = new MerkleTree();
                arbre.setRoot(compute(niveau.get(i).getRoot(), niveau.get(i + 1).getRoot()));
                arbre.setR(niveau.get(i + 1));
                arbre.setL(niveau.get(i));
                arbreNiveau.add(arbre);
            }
            n++;
            myClassListMap.put(Integer.valueOf(n), arbreNiveau);
        } while (n < niv.intValue());
        return arbre;
    }

}
