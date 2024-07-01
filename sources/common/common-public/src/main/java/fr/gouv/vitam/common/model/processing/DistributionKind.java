/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.processing;

/**
 * Enum of kind for distributor
 */
public enum DistributionKind {
    // TODO P1 comment on each lines + since each is the same enum = String, remove String

    /**
     * Distribution by reference, so 1 item only
     */
    REF("REF"),
    /**
     * Distribution by List (workspace or other kind of lists)
     */

    LIST_IN_FILE("LIST_IN_FILE"),

    /**
     * Distribution by List defined in a file.
     */
    LIST_ORDERING_IN_FILE("LIST_ORDERING_IN_FILE"),

    /**
     * Distribution by List defined in a directory.
     */
    LIST_IN_DIRECTORY("LIST_IN_DIRECTORY"),

    /**
     * Distribution by list defined in JSONL file.
     */
    LIST_IN_JSONL_FILE("LIST_IN_JSONL_FILE");

    /**
     * value
     */
    private String value;

    /**
     * DistributionKind
     *
     * @param value
     */
    DistributionKind(String value) {
        this.value = value;
    }

    /**
     * value(), get the value of DistributionKind
     *
     * @return the value as String
     */
    public String value() {
        return value;
    }

    /**
     * isDistributed
     *
     * @return
     */
    public boolean isDistributed() {
        return this != REF;
    }
}
