/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.core.database.collections;

/**
 *
 * Structure Access
 *
 */
enum VitamLinks {
    /**
     * Unit to Unit N-N link but asymmetric where only childs reference their fathers (so only "_up" link)
     */
    UNIT_TO_UNIT(MetadataCollections.UNIT, LinkType.SYM_LINK_N_N, MetadataDocument.UNUSED, MetadataCollections.UNIT, MetadataDocument.UP),
    /**
     * Unit to ObjectGroup N-1 link. This link is symmetric.
     */
    UNIT_TO_OBJECTGROUP(MetadataCollections.UNIT, LinkType.SYM_LINK_N1, MetadataDocument.OG, MetadataCollections.OBJECTGROUP, MetadataDocument.UP);

    protected MetadataCollections col1;
    protected LinkType type;
    protected String field1to2;
    protected MetadataCollections col2;
    protected String field2to1;

    /**
     * @param col1
     * @param type
     * @param field1to2
     * @param col2
     * @param field2to1
     */
    private VitamLinks(final MetadataCollections col1, final LinkType type,
        final String field1to2, final MetadataCollections col2,
        final String field2to1) {
        this.col1 = col1;
        this.type = type;
        this.field1to2 = field1to2;
        this.col2 = col2;
        this.field2to1 = field2to1;
    }

}
