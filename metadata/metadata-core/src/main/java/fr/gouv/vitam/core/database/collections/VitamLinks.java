package fr.gouv.vitam.core.database.collections;

import fr.gouv.vitam.core.database.collections.MongoDbAccess.VitamCollections;

/**
 *
 * Structure Access
 *
 */
enum VitamLinks {
    /**
     * Unit to Unit N-N link but asymmetric where only childs reference their fathers (so only "_up" link)
     */
    Unit2Unit(VitamCollections.Cunit, LinkType.SymLink_N_N, VitamDocument.UNUSED, VitamCollections.Cunit, VitamDocument.UP),
    /**
     * Unit to ObjectGroup 1-N link. This link is symmetric.
     */
    Unit2ObjectGroup(VitamCollections.Cunit, LinkType.SymLink1N, VitamDocument.OG, VitamCollections.Cobjectgroup, VitamDocument.UP);

    protected VitamCollections col1;
    protected LinkType type;
    protected String field1to2;
    protected VitamCollections col2;
    protected String field2to1;

    /**
     * @param col1
     * @param type
     * @param field1to2
     * @param col2
     * @param field2to1
     */
    private VitamLinks(final VitamCollections col1, final LinkType type,
        final String field1to2, final VitamCollections col2,
        final String field2to1) {
        this.col1 = col1;
        this.type = type;
        this.field1to2 = field1to2;
        this.col2 = col2;
        this.field2to1 = field2to1;
    }

}
