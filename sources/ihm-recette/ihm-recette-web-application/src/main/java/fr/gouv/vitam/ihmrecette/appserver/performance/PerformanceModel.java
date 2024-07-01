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
package fr.gouv.vitam.ihmrecette.appserver.performance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PerformanceModel {

    /**
     * number of ingest in parallel
     */
    private final Integer parallelIngest;
    /**
     * name of the SIP to upload.
     */
    private String fileName;

    /**
     * number of ingest.
     */
    private int numberOfIngest;

    /**
     * maximum of retry to wait end of pulling of each ingest.
     */
    private Integer numberOfRetry;

    /**
     * delay between two ingest in millisecond.
     */
    private Integer delay;

    @JsonCreator
    public PerformanceModel(
        @JsonProperty("fileName") String fileName,
        @JsonProperty("parallelIngest") Integer parallelIngest,
        @JsonProperty("delay") Integer delay,
        @JsonProperty("numberOfIngest") int numberOfIngest,
        @JsonProperty("numberOfRetry") Integer numberOfRetry
    ) {
        this.fileName = fileName;
        this.parallelIngest = parallelIngest;
        this.numberOfIngest = numberOfIngest;
        this.numberOfRetry = numberOfRetry;
        this.delay = delay;
    }

    public static PerformanceModel createPerformanceTestInParallel(
        String fileName,
        int parallelIngest,
        int numberOfIngest,
        Integer numberOfRetry
    ) {
        return new PerformanceModel(fileName, parallelIngest, null, numberOfIngest, numberOfRetry);
    }

    public static PerformanceModel createPerformanceTestInSequence(
        String fileName,
        int delay,
        int numberOfIngest,
        Integer numberOfRetry
    ) {
        return new PerformanceModel(fileName, null, delay, numberOfIngest, numberOfRetry);
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getParallelIngest() {
        return parallelIngest;
    }

    public int getNumberOfIngest() {
        return numberOfIngest;
    }

    public Integer getNumberOfRetry() {
        return numberOfRetry;
    }

    public Integer getDelay() {
        return delay;
    }
}
