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
package fr.gouv.vitam.worker.core.plugin.transfer.reply.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper for writing mixed level (distribution group) of json line entries into a single json line sorted by distribution group
 */
public class SortedLevelJsonLineWriter implements AutoCloseable {

    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<JsonLineModel>() {};
    private static final int MAX_LEVELS = 100;

    private final HandlerIO handler;
    private Map<Integer, File> filesByLevel = new HashMap<>();
    private Map<Integer, JsonLineWriter> writersByLevel = new HashMap<>();

    public SortedLevelJsonLineWriter(HandlerIO handler) {
        this.handler = handler;
    }

    public void addEntry(JsonLineModel line) throws IOException {
        if (line.getDistribGroup() == null) {
            throw new IllegalArgumentException("Null distribution group " + JsonHandler.unprettyPrint(line));
        }

        if (!filesByLevel.containsKey(line.getDistribGroup())) {
            if (filesByLevel.size() >= MAX_LEVELS) {
                throw new IllegalStateException("Too many levels " + MAX_LEVELS);
            }

            File newLocalFile = handler.getNewLocalFile(GUIDFactory.newGUID().toString());
            filesByLevel.put(line.getDistribGroup(), newLocalFile);
            writersByLevel.put(line.getDistribGroup(), new JsonLineWriter(new FileOutputStream(newLocalFile)));
        }

        JsonLineWriter jsonLineWriter = writersByLevel.get(line.getDistribGroup());
        jsonLineWriter.addEntry(line);
    }

    public void exportToWorkspace(String filename, boolean ascending) throws IOException, ProcessingException {
        // Flush / close all level writers
        for (JsonLineWriter value : this.writersByLevel.values()) {
            value.close();
        }

        File combinedSortedJsonLineFile = handler.getNewLocalFile(GUIDFactory.newGUID().toString());
        try {
            try (
                OutputStream outputStream = new FileOutputStream(combinedSortedJsonLineFile);
                JsonLineWriter writer = new JsonLineWriter(outputStream)
            ) {
                // Sort levels
                List<Integer> levels =
                    this.filesByLevel.keySet()
                        .stream()
                        .sorted(ascending ? Comparator.naturalOrder() : Comparator.reverseOrder())
                        .collect(Collectors.toList());

                // Append files by level
                for (Integer level : levels) {
                    File fileLevel = this.filesByLevel.get(level);

                    try (
                        InputStream is = new FileInputStream(fileLevel);
                        JsonLineGenericIterator<JsonLineModel> lineGenericIterator = new JsonLineGenericIterator<>(
                            is,
                            TYPE_REFERENCE
                        )
                    ) {
                        while (lineGenericIterator.hasNext()) {
                            writer.addEntry(lineGenericIterator.next());
                        }
                    }
                }
            }

            handler.transferFileToWorkspace(filename, combinedSortedJsonLineFile, true, false);
        } finally {
            for (File file : filesByLevel.values()) {
                FileUtils.deleteQuietly(file);
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (JsonLineWriter value : writersByLevel.values()) {
            value.close();
        }
        for (File file : filesByLevel.values()) {
            FileUtils.deleteQuietly(file);
        }
    }
}
