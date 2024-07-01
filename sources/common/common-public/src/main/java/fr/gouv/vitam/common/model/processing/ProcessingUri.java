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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Processing Uri format
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ProcessingUri {

    @JsonProperty("prefix")
    private UriPrefix prefix;

    @JsonProperty("path")
    private String path;

    /**
     * Empty Constructor
     */
    public ProcessingUri() {
        // empty constructor
    }

    /**
     * Constructor with prefix and path
     *
     * @param prefix of processing uri
     * @param path of processing uri
     */
    public ProcessingUri(String prefix, String path) {
        this(UriPrefix.valueOf(prefix), path);
    }

    /**
     * Constructor with prefix and path
     *
     * @param prefix of processing uri
     * @param path of processing uri
     */
    public ProcessingUri(UriPrefix prefix, String path) {
        setPrefix(prefix);
        setPath(path);
    }

    /**
     * Constructor with String
     *
     * @param uri of processing
     */
    public ProcessingUri(String uri) {
        this(uri.split(":")[0], uri.split(":")[1]);
    }

    /**
     * @return the prefix
     */
    public UriPrefix getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the prefix to set
     * @return this
     */
    public ProcessingUri setPrefix(UriPrefix prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     * @return this
     */
    public ProcessingUri setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public String toString() {
        return prefix + ":" + path;
    }
}
