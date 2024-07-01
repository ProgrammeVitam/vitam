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
package fr.gouv.vitam.workspace.common;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CompressInformation POJO containing information on files to be compressed
 */
public class CompressInformation {

    @JsonProperty("files")
    private List<String> files = new ArrayList<>();

    @JsonProperty("outputFile")
    private String outputFile;

    @JsonProperty("outputContainer")
    private String outputContainer;

    /**
     * Default constructor
     */
    public CompressInformation() {
        // empty
    }

    /**
     * Constructor
     *
     * @param files list of files to be compressed
     * @param outputFile output file
     */
    public CompressInformation(List<String> files, String outputFile, String outputContainer) {
        this.files = files;
        this.outputFile = outputFile;
        this.outputContainer = outputContainer;
    }

    /**
     * get list of files to be compressed
     *
     * @return list of files as a list of string
     */
    public List<String> getFiles() {
        return files;
    }

    /**
     * Set list of files
     *
     * @param files list of files as string
     */
    public void setFiles(List<String> files) {
        this.files = files;
    }

    /**
     * Get Output File
     *
     * @return outpuFile
     */
    public String getOutputFile() {
        return outputFile;
    }

    /**
     * Set Output file
     *
     * @param outputFile
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getOutputContainer() {
        return outputContainer;
    }

    public CompressInformation setOutputContainer(String outputContainer) {
        this.outputContainer = outputContainer;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompressInformation that = (CompressInformation) o;
        return (
            Objects.equals(files, that.files) &&
            Objects.equals(outputFile, that.outputFile) &&
            Objects.equals(outputContainer, that.outputContainer)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(files, outputFile, outputContainer);
    }
}
