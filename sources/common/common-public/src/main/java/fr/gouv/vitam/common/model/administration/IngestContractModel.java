/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.SingletonUtils;

import java.util.Set;

/**
 * Data Transfer Object Model of contract (DTO).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class IngestContractModel extends AbstractContractModel {

    /**
     * Archive Profiles
     */
    public static final String ARCHIVE_PROFILES = "ArchiveProfiles";
    /**
     * Attachment GUID
     */
    public static final String LINK_PARENT_ID = "LinkParentId";    
    /**
     * Activated control on parent id (ACTIVE / INACTOVE)
     */
    public static final String TAG_CHECK_PARENT_LINK = "CheckParentLink";

    @JsonProperty(LINK_PARENT_ID)
    private String linkParentId;

    @JsonProperty(ARCHIVE_PROFILES)
    private Set<String> archiveProfiles;

    @JsonProperty(TAG_CHECK_PARENT_LINK)
    private ActivationStatus checkParentLink;
    
    public IngestContractModel() {
        super();
    }
    
    
    /**
     * @return linkParentId
     */
    public String getLinkParentId() {
        return linkParentId;
    }

    /**
     * @param linkParentId
     */
    public void setLinkParentId(String linkParentId) {
        this.linkParentId = linkParentId;
    }

    public Set<String> getArchiveProfiles() {
        if (archiveProfiles == null) {
            return SingletonUtils.singletonSet();
        }
        return archiveProfiles;
    }

    public IngestContractModel setArchiveProfiles(Set<String> archiveProfiles) {
        this.archiveProfiles = archiveProfiles;
        return this;
    }
    
    /**
     * Get the check parent link status
     *
     * @return Check Parent Link status
     */
    public ActivationStatus getCheckParentLink() {
        return this.checkParentLink;
    }


    /**
     * Set or change the check parent link status
     *
     * @param status to set
     * @return this
     */
    public AbstractContractModel setCheckParentLink(ActivationStatus status) {
        this.checkParentLink = status;
        return this;
    }
    
}
