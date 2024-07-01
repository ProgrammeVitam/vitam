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
package fr.gouv.vitam.worker.core.plugin.probativevalue.pojo;

import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.CheckedItem.EVENT_OBJECT_GROUP;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.CheckedItem.EVENT_OPERATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.CheckedItem.FILE_DIGEST;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.CheckedItem.MERKLE_TREE_ROOT_OBJECT_GROUP_DIGEST;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.CheckedItem.MERKLE_TREE_ROOT_OPERATION_DIGEST;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.CheckedItem.PREVIOUS_TIMESTAMP_OBJECT_GROUP;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.CheckedItem.PREVIOUS_TIMESTAMP_OPERATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.CheckedItem.TIMESTAMP_OBJECT_GROUP;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.CheckedItem.TIMESTAMP_OPERATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksAction.COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksAction.VALIDATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksSourceDestination.ADDITIONAL_TRACEABILITY;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksSourceDestination.COMPUTATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksSourceDestination.DATABASE;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksSourceDestination.OFFER;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksSourceDestination.TRACEABILITY_FILE;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksType.CHAIN;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksType.LOCAL_INTEGRITY;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksType.MERKLE_INTEGRITY;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksType.TIMESTAMP_CHECKING;

public enum ChecksInformation {
    // Local integrity checks
    FILE_DIGEST_OFFER_DATABASE_COMPARISON(
        LOCAL_INTEGRITY,
        OFFER,
        DATABASE,
        COMPARISON,
        FILE_DIGEST,
        "Comparing binary file digest stored in offers with binary file digest found in object group database."
    ),
    FILE_DIGEST_LFC_DATABASE_COMPARISON(
        LOCAL_INTEGRITY,
        DATABASE,
        DATABASE,
        COMPARISON,
        FILE_DIGEST,
        "Comparing binary file digest found in object group database with binary file digest found in logbook lifecycle object group database."
    ),
    FILE_DIGEST_DATABASE_TRACEABILITY_COMPARISON(
        LOCAL_INTEGRITY,
        DATABASE,
        TRACEABILITY_FILE,
        COMPARISON,
        FILE_DIGEST,
        "Comparing binary file digest found in object group database with binary file digest found in traceability secured file."
    ),
    EVENTS_OBJECT_GROUP_DIGEST_DATABASE_TRACEABILITY_COMPARISON(
        LOCAL_INTEGRITY,
        DATABASE,
        TRACEABILITY_FILE,
        COMPARISON,
        EVENT_OBJECT_GROUP,
        "Comparing object group lfc digest computed from logbook lifecycle object group database with object group lfc digest found in traceability secured file."
    ),
    EVENTS_OPERATION_DATABASE_TRACEABILITY_COMPARISON(
        LOCAL_INTEGRITY,
        DATABASE,
        TRACEABILITY_FILE,
        COMPARISON,
        EVENT_OPERATION,
        "Comparing operation ID found in database with operation ID found in traceability secured file."
    ),

    // Merkle integrity checks
    MERKLE_OPERATION_DIGEST_DATABASE_TRACEABILITY_COMPARISON(
        MERKLE_INTEGRITY,
        DATABASE,
        TRACEABILITY_FILE,
        COMPARISON,
        MERKLE_TREE_ROOT_OPERATION_DIGEST,
        "Comparing operation merkle digest found in database with operation merkle digest found in traceability secured file."
    ),
    MERKLE_OPERATION_DIGEST_COMPUTATION_TRACEABILITY_COMPARISON(
        MERKLE_INTEGRITY,
        COMPUTATION,
        TRACEABILITY_FILE,
        COMPARISON,
        MERKLE_TREE_ROOT_OPERATION_DIGEST,
        "Comparing operation merkle digest computed from secured data with operation merkle digest found in traceability secured file."
    ),
    MERKLE_OPERATION_DIGEST_COMPUTATION_ADDITIONAL_TRACEABILITY_COMPARISON(
        MERKLE_INTEGRITY,
        COMPUTATION,
        ADDITIONAL_TRACEABILITY,
        COMPARISON,
        MERKLE_TREE_ROOT_OPERATION_DIGEST,
        "Comparing operation merkle digest computed from secured data with operation merkle digest found in additional traceability secured file."
    ),
    MERKLE_OBJECT_GROUP_DIGEST_DATABASE_TRACEABILITY_COMPARISON(
        MERKLE_INTEGRITY,
        DATABASE,
        TRACEABILITY_FILE,
        COMPARISON,
        MERKLE_TREE_ROOT_OBJECT_GROUP_DIGEST,
        "Comparing object group merkle digest found in database with object group merkle digest found in traceability secured file."
    ),
    MERKLE_OBJECT_GROUP_DIGEST_COMPUTATION_TRACEABILITY_COMPARISON(
        MERKLE_INTEGRITY,
        COMPUTATION,
        TRACEABILITY_FILE,
        COMPARISON,
        MERKLE_TREE_ROOT_OBJECT_GROUP_DIGEST,
        "Comparing object group merkle digest computed from secured data with object group merkle digest found in traceability secured file."
    ),
    MERKLE_OBJECT_GROUP_DIGEST_COMPUTATION_ADDITIONAL_TRACEABILITY_COMPARISON(
        MERKLE_INTEGRITY,
        COMPUTATION,
        ADDITIONAL_TRACEABILITY,
        COMPARISON,
        MERKLE_TREE_ROOT_OBJECT_GROUP_DIGEST,
        "Comparing object group merkle digest computed from secured data with object group merkle digest found in additional traceability secured file."
    ),

    // Timestamp checks
    TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_VALIDATION(
        TIMESTAMP_CHECKING,
        DATABASE,
        TRACEABILITY_FILE,
        VALIDATION,
        TIMESTAMP_OPERATION,
        "Validating timestamp operation found in database 'AND' timestamp operation found in traceability secured file."
    ),
    TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_COMPARISON(
        TIMESTAMP_CHECKING,
        DATABASE,
        TRACEABILITY_FILE,
        COMPARISON,
        TIMESTAMP_OPERATION,
        "Comparing timestamp operation found in database with timestamp operation found in traceability secured file."
    ),
    TIMESTAMP_OPERATION_COMPUTATION_TRACEABILITY_COMPARISON(
        TIMESTAMP_CHECKING,
        COMPUTATION,
        TRACEABILITY_FILE,
        COMPARISON,
        TIMESTAMP_OPERATION,
        "Comparing timestamp operation computed from computing information traceability file with timestamp operation found in traceability secured file."
    ),
    TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_VALIDATION(
        TIMESTAMP_CHECKING,
        DATABASE,
        TRACEABILITY_FILE,
        VALIDATION,
        TIMESTAMP_OBJECT_GROUP,
        "Validating timestamp object group found in database 'AND' timestamp object group found in traceability secured file."
    ),
    TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_COMPARISON(
        TIMESTAMP_CHECKING,
        DATABASE,
        TRACEABILITY_FILE,
        COMPARISON,
        TIMESTAMP_OBJECT_GROUP,
        "Comparing timestamp object group found in database with timestamp object group found in secured file."
    ),
    TIMESTAMP_OBJECT_GROUP_COMPUTATION_TRACEABILITY_COMPARISON(
        TIMESTAMP_CHECKING,
        COMPUTATION,
        TRACEABILITY_FILE,
        COMPARISON,
        TIMESTAMP_OBJECT_GROUP,
        "Comparing timestamp object group computed from computing information traceability file with timestamp object group found in secured file."
    ),

    // Chain checks
    PREVIOUS_TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_VALIDATION(
        CHAIN,
        DATABASE,
        TRACEABILITY_FILE,
        VALIDATION,
        PREVIOUS_TIMESTAMP_OPERATION,
        "Validating previous timestamp operation found in database 'AND' previous timestamp operation found in traceability secured file."
    ),
    PREVIOUS_TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_COMPARISON(
        CHAIN,
        DATABASE,
        TRACEABILITY_FILE,
        COMPARISON,
        PREVIOUS_TIMESTAMP_OPERATION,
        "Comparing previous timestamp operation found in database with previous timestamp operation found in traceability secured file."
    ),
    PREVIOUS_TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_VALIDATION(
        CHAIN,
        DATABASE,
        TRACEABILITY_FILE,
        VALIDATION,
        PREVIOUS_TIMESTAMP_OBJECT_GROUP,
        "Validating previous timestamp object group found in database 'AND' previous timestamp object group found in traceability secured file."
    ),
    PREVIOUS_TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_COMPARISON(
        CHAIN,
        DATABASE,
        TRACEABILITY_FILE,
        COMPARISON,
        PREVIOUS_TIMESTAMP_OBJECT_GROUP,
        "Comparing previous timestamp object group found in database with previous timestamp object group found in traceability secured file."
    );

    public final ChecksType checksType;
    public final ChecksSourceDestination source;
    public final ChecksSourceDestination destination;
    public final ChecksAction action;
    public final CheckedItem item;
    public final String explanation;

    ChecksInformation(
        ChecksType checksType,
        ChecksSourceDestination source,
        ChecksSourceDestination destination,
        ChecksAction action,
        CheckedItem item,
        String explanation
    ) {
        this.checksType = checksType;
        this.source = source;
        this.destination = destination;
        this.action = action;
        this.item = item;
        this.explanation = explanation;
    }

    @Override
    public String toString() {
        return String.format(
            "'%s' of type '%s' with explanation '%s'.\n",
            this.name(),
            this.checksType,
            this.explanation
        );
    }

    public enum ChecksType {
        LOCAL_INTEGRITY,
        MERKLE_INTEGRITY,
        TIMESTAMP_CHECKING,
        CHAIN,
    }

    public enum ChecksSourceDestination {
        OFFER,
        DATABASE,
        TRACEABILITY_FILE,
        ADDITIONAL_TRACEABILITY,
        COMPUTATION,
    }

    public enum ChecksAction {
        COMPARISON,
        VALIDATION,
    }

    public enum CheckedItem {
        FILE_DIGEST,
        EVENT_LFC,
        EVENT_OBJECT_GROUP,
        EVENT_OPERATION,
        MERKLE_TREE_ROOT_LFC_DIGEST,
        MERKLE_TREE_ROOT_OPERATION_DIGEST,
        TIMESTAMP_LFC,
        TIMESTAMP_OPERATION,
        PREVIOUS_TIMESTAMP_LFC,
        PREVIOUS_TIMESTAMP_OPERATION,
        MERKLE_TREE_ROOT_OBJECT_GROUP_DIGEST,
        TIMESTAMP_OBJECT_GROUP,
        PREVIOUS_TIMESTAMP_OBJECT_GROUP,
    }
}
