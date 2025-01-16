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

package fr.gouv.vitam.collect.internal.core.csv;

import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.ARRAY_INDEX_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.SEPARATOR;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.buildPath;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.matchesPattern;

public class CsvHeaderFieldNameIterable implements Iterable<CsvHeaderFieldNameIterable.FieldEntry> {

    private final String[] fieldNames;

    public CsvHeaderFieldNameIterable(String headerName) {
        this.fieldNames = StringUtils.splitPreserveAllTokens(headerName, SEPARATOR);
    }

    @Override
    public Iterator<FieldEntry> iterator() {
        return new Iterator<>() {
            private int index = 0;
            private String simpleSedaPath = null;
            private String fullSedaPath = null;

            @Override
            public boolean hasNext() {
                return index < fieldNames.length;
            }

            @Override
            public FieldEntry next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                String fieldName = fieldNames[index];

                String parentSimpleSedaPath = simpleSedaPath;
                simpleSedaPath = buildPath(simpleSedaPath, fieldName);

                String parentFullSedaPath = fullSedaPath;
                fullSedaPath = buildPath(fullSedaPath, fieldName);
                String fullSedaPathWithoutLastArrayIndex = fullSedaPath;

                Integer arrayIndex = null;
                if (index + 1 < fieldNames.length && matchesPattern(fieldNames[index + 1], ARRAY_INDEX_PATTERN)) {
                    String arrayIndexStr = fieldNames[index + 1];
                    arrayIndex = Integer.valueOf(arrayIndexStr);
                    fullSedaPath = buildPath(fullSedaPath, arrayIndexStr);
                    index++;
                }
                index++;
                boolean isDeclaredAsObject = index < fieldNames.length;

                return new FieldEntry(
                    fieldName,
                    arrayIndex,
                    isDeclaredAsObject,
                    simpleSedaPath,
                    fullSedaPath,
                    fullSedaPathWithoutLastArrayIndex,
                    parentFullSedaPath,
                    parentSimpleSedaPath
                );
            }
        };
    }

    public record FieldEntry(
        // Declared array index (ex "Content.Title" --> "Title", "Content.Title.0" --> "Title")
        String sedaFieldName,
        // Declared array index (ex "Content.Title" --> null, "Content.Title.0" --> 0)
        Integer arrayIndex,
        boolean isDeclaredAsObject,
        // Seda Path without array indexes (ex "Content.Writer.0.FullName.0" --> "Content.Writer.FullName")
        String simpleSedaPath,
        // Seda Path with declared array indexes (ex "Content.Writer.0.FullName.0" --> "Content.Writer.0.FullName.0")
        String fullSedaPath,
        // Seda Path with declared array indexes (ex "Content.Writer.0.FullName.0" --> "Content.Writer.0.FullName")
        String fullSedaPathWithoutLastArrayIndex,
        // Parent Seda Path with declared array indexes (ex "Content.Writer.0.FullName.0" --> "Content.Writer.0")
        String parentFullSedaPath,
        // Parent Seda Path without array indexes (ex "Content.Writer.0.FullName.0" --> "Content.Writer")
        String parentSimpleSedaPath
    ) {
        public boolean isDeclaredAsArray() {
            return arrayIndex != null;
        }
    }
}
