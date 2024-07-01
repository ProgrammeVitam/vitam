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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.CheckedItem;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksAction;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksSourceDestination;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.ChecksType;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;

public class ProbativeCheck {

    private final String name;
    private final String details;
    private final ChecksType type;
    private final ChecksSourceDestination source;
    private final ChecksSourceDestination destination;
    private final String sourceComparable;
    private final String destinationComparable;
    private final ChecksAction action;
    private final CheckedItem item;
    private final StatusCode status;

    @JsonCreator
    public ProbativeCheck(
        @JsonProperty("name") String name,
        @JsonProperty("details") String details,
        @JsonProperty("type") ChecksType type,
        @JsonProperty("source") ChecksSourceDestination source,
        @JsonProperty("destination") ChecksSourceDestination destination,
        @JsonProperty("sourceComparable") String sourceComparable,
        @JsonProperty("destinationComparable") String destinationComparable,
        @JsonProperty("action") ChecksAction action,
        @JsonProperty("item") CheckedItem item,
        @JsonProperty("status") StatusCode status
    ) {
        this.name = name;
        this.details = details;
        this.type = type;
        this.source = source;
        this.destination = destination;
        this.sourceComparable = sourceComparable;
        this.destinationComparable = destinationComparable;
        this.action = action;
        this.item = item;
        this.status = status;
    }

    @JsonIgnore
    public static ProbativeCheck from(
        ChecksInformation information,
        String source,
        String destination,
        StatusCode status
    ) {
        return new ProbativeCheck(
            information.name(),
            information.explanation,
            information.checksType,
            information.source,
            information.destination,
            source,
            destination,
            information.action,
            information.item,
            status
        );
    }

    @JsonIgnore
    public static ProbativeCheck okFrom(ChecksInformation information, String source, String destination) {
        return new ProbativeCheck(
            information.name(),
            information.explanation,
            information.checksType,
            information.source,
            information.destination,
            source,
            destination,
            information.action,
            information.item,
            OK
        );
    }

    @JsonIgnore
    public static ProbativeCheck koFrom(ChecksInformation information, String source, String destination) {
        return new ProbativeCheck(
            information.name(),
            information.explanation,
            information.checksType,
            information.source,
            information.destination,
            source,
            destination,
            information.action,
            information.item,
            KO
        );
    }

    @JsonIgnore
    public static ProbativeCheck warnFrom(ChecksInformation information, String source, String destination) {
        return new ProbativeCheck(
            information.name(),
            information.explanation,
            information.checksType,
            information.source,
            information.destination,
            source,
            destination,
            information.action,
            information.item,
            WARNING
        );
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("details")
    public String getDetails() {
        return details;
    }

    @JsonProperty("type")
    public ChecksType getType() {
        return type;
    }

    @JsonProperty("source")
    public ChecksSourceDestination getSource() {
        return source;
    }

    @JsonProperty("destination")
    public ChecksSourceDestination getDestination() {
        return destination;
    }

    @JsonProperty("sourceComparable")
    public String getSourceComparable() {
        return sourceComparable;
    }

    @JsonProperty("destinationComparable")
    public String getDestinationComparable() {
        return destinationComparable;
    }

    @JsonProperty("action")
    public ChecksAction getAction() {
        return action;
    }

    @JsonProperty("item")
    public CheckedItem getItem() {
        return item;
    }

    @JsonProperty("status")
    public StatusCode getStatus() {
        return status;
    }
}
