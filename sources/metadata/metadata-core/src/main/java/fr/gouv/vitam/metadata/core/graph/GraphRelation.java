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
package fr.gouv.vitam.metadata.core.graph;

import java.util.Objects;

/**
 * Contains information of relation child -&gt; parent
 * unit: the current treated unit
 * unitOriginatingAgency: the originatingAgency of the current unit
 * parent: the parent unit of the current treated unit
 * parentOriginatingAgency: the originatingAgency of the parent unit
 * depth: the depth of the relation
 * AU1
 * /  \  \
 * /    \  \
 * AU2  AU3  \
 * \    /    \
 * \  /      |
 * AU4      /
 * \   /
 * \ /
 * AU5
 *
 * AU1/AU2 depth = 1
 * AU1/AU5 depth = 1
 * AU1/AU3 depth = 1
 * AU2/AU4 depth = 2
 * AU3/AU4 depth = 2
 * AU4/AU5 depth = 3 (ignored as we have already /AU5 with depth 1)
 */
public class GraphRelation {

    private String unit;
    private String parent;
    private String parentOriginatingAgency;
    private Integer depth;

    /**
     * Constructor
     *
     * @param unit
     * @param parent
     * @param parentOriginatingAgency
     * @param depth
     */
    public GraphRelation(String unit, String parent, String parentOriginatingAgency, Integer depth) {
        this.unit = unit;
        this.parent = parent;
        this.parentOriginatingAgency = parentOriginatingAgency;
        this.depth = depth;
    }

    /**
     * Get unit
     *
     * @return unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Set unit
     *
     * @param unit
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Get parent unit
     *
     * @return parent
     */
    public String getParent() {
        return parent;
    }

    /**
     * Set parent unit
     *
     * @param parent
     */
    public void setParent(String parent) {
        this.parent = parent;
    }

    /**
     * Get parentOriginatingAgency
     *
     * @return parentOriginatingAgency
     */
    public String getParentOriginatingAgency() {
        return parentOriginatingAgency;
    }

    /**
     * Set parentOriginatingAgency
     *
     * @param parentOriginatingAgency
     */
    public void setParentOriginatingAgency(String parentOriginatingAgency) {
        this.parentOriginatingAgency = parentOriginatingAgency;
    }

    /**
     * Get depth
     *
     * @return depth
     */
    public Integer getDepth() {
        return depth;
    }

    /**
     * Set depth
     *
     * @param depth
     */
    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUnit(), getParent());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GraphRelation) {
            GraphRelation graphRelation = (GraphRelation) obj;
            return (
                Objects.equals(getUnit(), graphRelation.getUnit()) &&
                Objects.equals(getParent(), graphRelation.getParent())
            );
        }
        return false;
    }
}
