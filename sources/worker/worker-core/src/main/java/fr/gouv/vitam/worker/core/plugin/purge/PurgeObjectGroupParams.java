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
package fr.gouv.vitam.worker.core.plugin.purge;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import org.apache.commons.collections4.ListUtils;

import java.util.List;
import java.util.stream.Collectors;

public class PurgeObjectGroupParams {

    @JsonProperty("id")
    private String id;

    @JsonProperty("strategyId")
    private String strategyId;

    @JsonProperty("objects")
    private List<PurgeObjectParams> objects;

    public String getId() {
        return id;
    }

    public PurgeObjectGroupParams setId(String id) {
        this.id = id;
        return this;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public PurgeObjectGroupParams setStrategyId(String strategyId) {
        this.strategyId = strategyId;
        return this;
    }

    public List<PurgeObjectParams> getObjects() {
        return objects;
    }

    public PurgeObjectGroupParams setObjects(List<PurgeObjectParams> objects) {
        this.objects = objects;
        return this;
    }

    public static PurgeObjectGroupParams fromObjectGroup(ObjectGroupResponse objectGroup) {
        List<PurgeObjectParams> objectParams = ListUtils.emptyIfNull(objectGroup.getQualifiers())
            .stream()
            .flatMap(qualifier -> ListUtils.emptyIfNull(qualifier.getVersions()).stream())
            .filter(version -> version.getPhysicalId() == null)
            .map(
                version ->
                    new PurgeObjectParams().setId(version.getId()).setStrategyId(version.getStorage().getStrategyId())
            )
            .collect(Collectors.toList());

        return new PurgeObjectGroupParams()
            .setId(objectGroup.getId())
            .setStrategyId(objectGroup.getStorage().getStrategyId())
            .setObjects(objectParams);
    }
}
