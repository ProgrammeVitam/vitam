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

import java.io.File;

public class OperationTraceabilityFiles {

    public static final String TRACEABILITY_DATA = "data.txt";
    public static final String TRACEABILITY_MERKLE_TREE = "merkleTree.json";
    public static final String TRACEABILITY_TOKEN = "token.tsp";
    public static final String TRACEABILITY_COMPUTING_INFORMATION = "computing_information.txt";
    public static final String TRACEABILITY_ADDITIONAL_INFORMATION = "additional_information.txt";

    public static final String TRACEABILITY_FILES_COMPLETE = "zip_complete.ready";
    public static final String TRACEABILITY_GENERAL_CHECKS_COMPLETE = "general_checks.ready";
    public static final String TRACEABILITY_GENERAL_CHECKS = "general_checks.json";

    private final File data;
    private final File merkleTree;
    private final File token;
    private final File computingInformation;
    private final File additionalInformation;

    public OperationTraceabilityFiles(
        File data,
        File merkleTree,
        File token,
        File computingInformation,
        File additionalInformation
    ) {
        this.data = data;
        this.merkleTree = merkleTree;
        this.token = token;
        this.computingInformation = computingInformation;
        this.additionalInformation = additionalInformation;
    }

    public File getData() {
        return data;
    }

    public File getMerkleTree() {
        return merkleTree;
    }

    public File getToken() {
        return token;
    }

    public File getComputingInformation() {
        return computingInformation;
    }

    public File getAdditionalInformation() {
        return additionalInformation;
    }

    public static final class OperationTraceabilityFilesBuilder {

        private File data;
        private File merkleTree;
        private File token;
        private File computingInformation;
        private File additionalInformation;

        private OperationTraceabilityFilesBuilder() {}

        public static OperationTraceabilityFilesBuilder anOperationTraceabilityFiles() {
            return new OperationTraceabilityFilesBuilder();
        }

        public OperationTraceabilityFilesBuilder with(String name, File file) {
            switch (name) {
                case TRACEABILITY_DATA:
                    this.data = file;
                    return this;
                case TRACEABILITY_MERKLE_TREE:
                    this.merkleTree = file;
                    return this;
                case TRACEABILITY_TOKEN:
                    this.token = file;
                    return this;
                case TRACEABILITY_COMPUTING_INFORMATION:
                    this.computingInformation = file;
                    return this;
                case TRACEABILITY_ADDITIONAL_INFORMATION:
                    this.additionalInformation = file;
                    return this;
                default:
                    throw new IllegalArgumentException(String.format("%s is illegal.", name));
            }
        }

        public OperationTraceabilityFiles build() {
            return new OperationTraceabilityFiles(data, merkleTree, token, computingInformation, additionalInformation);
        }
    }
}
