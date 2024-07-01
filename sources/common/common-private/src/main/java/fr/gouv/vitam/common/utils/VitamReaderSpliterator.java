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
package fr.gouv.vitam.common.utils;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Vitam reader Spliterator.
 */
public class VitamReaderSpliterator implements Spliterator<String> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamReaderSpliterator.class);

    /**
     * reader
     */
    private final BufferedReader reader;

    /**
     * Constructor.
     *
     * @param reader
     */
    public VitamReaderSpliterator(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
        try {
            String line = reader.readLine();
            if (line != null) {
                action.accept(line);
                return true;
            } else return false;
        } catch (IOException e) {
            LOGGER.error(e);
            return false;
        }
    }

    @Override
    public Spliterator<String> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return DISTINCT | NONNULL | IMMUTABLE;
    }
}
