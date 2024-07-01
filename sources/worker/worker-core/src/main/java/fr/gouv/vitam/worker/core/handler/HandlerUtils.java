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
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Handler Utils class
 */
public class HandlerUtils {

    private HandlerUtils() {
        // private constructor
    }

    /**
     * Save the given map as specified by the rank output argument
     *
     * @param handlerIO the handler io
     * @param object the data object to write
     * @param rank the output rank
     * @throws IOException if cannot write file in json format
     * @throws ProcessingException if processing exception occurred
     */
    public static void save(HandlerIO handlerIO, Object object, int rank) throws IOException, ProcessingException {
        final String tmpFilePath = handlerIO.getOutput(rank).getPath();
        final File firstMapTmpFile = handlerIO.getNewLocalFile(tmpFilePath);
        try {
            JsonHandler.writeAsFile(object, firstMapTmpFile);
        } catch (final InvalidParseOperationException e) {
            throw new IOException(e);
        }
        handlerIO.addOutputResult(rank, firstMapTmpFile, true, false);
    }

    /**
     * Save the given map as specified by the rank output argument
     *
     * @param handlerIO the handler io
     * @param object the data object to write
     * @param workspacePath path to workspace
     * @throws IOException if cannot write file in json format
     * @throws ProcessingException if processing exception occurred
     */
    public static void save(HandlerIO handlerIO, Object object, String workspacePath)
        throws IOException, ProcessingException {
        final File firstMapTmpFile = handlerIO.getNewLocalFile(workspacePath);
        try {
            JsonHandler.writeAsFile(object, firstMapTmpFile);
        } catch (final InvalidParseOperationException e) {
            throw new IOException(e);
        }
        handlerIO.transferFileToWorkspace(workspacePath, firstMapTmpFile, true, false);
    }

    /**
     * Save the given map as specified by the rank output argument
     *
     * @param handlerIO the handler io
     * @param map the data map to write
     * @param rank the output rank
     * @param removeTmpFile if remove temp output file
     * @throws IOException if cannot write file in json format
     * @throws ProcessingException if processing exception occurred
     */
    public static void saveMap(
        HandlerIO handlerIO,
        Map<String, ?> map,
        int rank,
        boolean removeTmpFile,
        boolean asyncIO
    ) throws IOException, ProcessingException {
        final String tmpFilePath = handlerIO.getOutput(rank).getPath();
        final File firstMapTmpFile = handlerIO.getNewLocalFile(tmpFilePath);
        try {
            JsonHandler.writeAsFile(map, firstMapTmpFile);
        } catch (final InvalidParseOperationException e) {
            throw new IOException(e);
        }

        handlerIO.addOutputResult(rank, firstMapTmpFile, removeTmpFile, asyncIO);
    }

    /**
     * Save the given set as specified by the rank output argument
     *
     * @param handlerIO the handler io
     * @param set the data set to write
     * @param rank the output rank
     * @param removeTmpFile if remove temp output file
     * @throws IOException if cannot write file in json format
     * @throws ProcessingException if processing exception occurred
     */
    public static void saveSet(HandlerIO handlerIO, Set<?> set, int rank, boolean removeTmpFile, boolean asyncIO)
        throws IOException, ProcessingException {
        final String tmpFilePath = handlerIO.getOutput(rank).getPath();
        final File firstMapTmpFile = handlerIO.getNewLocalFile(tmpFilePath);
        try {
            JsonHandler.writeAsFile(set, firstMapTmpFile);
        } catch (final InvalidParseOperationException e) {
            throw new IOException(e);
        }

        handlerIO.addOutputResult(rank, firstMapTmpFile, removeTmpFile, asyncIO);
    }
}
