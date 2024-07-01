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
package fr.gouv.vitam.storage.offers.tape.exception;

public enum ReadWriteErrorCode {
    INTERNAL_ERROR_SERVER,
    TAPE_NOT_FOUND_IN_CATALOG,
    NO_EMPTY_SLOT_FOUND,
    KO_TAPE_IS_OUTSIDE,
    /**
     * Previous location should not be a drive, but a slot or mailbox
     * Current location should not be null
     */
    TAPE_LOCATION_CONFLICT_ON_LOAD,
    TAPE_LOCATION_CONFLICT_ON_UNLOAD,
    FILE_NOT_FOUND,
    NULL_CURRENT_TAPE,
    KO_DB_PERSIST,
    KO_ON_LOAD_TAPE,
    KO_ON_UNLOAD_TAPE,
    KO_ON_WRITE_TO_TAPE, // TODO CloseIncident Tape
    KO_ON_REWIND_TAPE,
    KO_ON_REWIND_FSF_BSF_TAPE,
    /**
     * Tape is full
     */
    KO_ON_END_OF_TAPE,

    /**
     * Ko on move to end of data according to file count of tape catalog
     */
    KO_ON_GOTO_FILE_COUNT,

    KO_ON_STATUS,
    KO_ON_LOAD_THEN_STATUS,
    KO_ON_UNLOAD_THEN_STATUS,
    KO_UNKNOWN_CURRENT_POSITION,
    KO_ON_READ_FROM_TAPE,
    KO_LABEL_DISCORDING_NOT_EMPTY_TAPE,
    KO_LABEL_DISCORDING,
    KO_REWIND_BEFORE_FORCE_OVERRIDE_NON_EMPTY_TAPE,
    KO_ON_READ_LABEL,
    TAPE_LOCATION_UNKNOWN,
    KO_TAPE_CURRENT_POSITION_GREATER_THAN_FILE_COUNT,
    KO_ON_GO_TO_POSITION,
    KO_REWIND_BEFORE_UNLOAD_TAPE,
    KO_ON_WRITE_TO_FS,
    KO_ON_MOVE_TO_CACHE,
    KO_ON_DELETE_ARCHIVED_BACKUP,
    KO_TAPE_IS_BUSY,
    KO_TAPE_CONFLICT_STATE,
    KO_DRIVE_STATUS_KO_AFTER_WRITE_ERROR,
}
