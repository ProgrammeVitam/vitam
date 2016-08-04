/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.common.model;

/**
 * Enum StatusCode
 *
 * different constants status code for workflow , action handler and process
 *
 */
public enum OutcomeMessage {
    /**
     * OK : success message
     */
    CHECK_CONFORMITY_OK("Contrôle de conformité des objets réalisé avec succès"),
    
    /**
     * KO : fail message
     */
    CHECK_CONFORMITY_KO("Erreur de contrôle de conformité des objets"),
    
    /**
     * OK : success message
     */
    CHECK_OBJECT_NUMBER_OK("Contrôle du nombre des objets réalisé avec succès"),
    
    /**
     * KO : fail message
     */
    CHECK_OBJECT_NUMBER_KO("Erreur de contrôle du nombre des objets"),
    
    /**
     * OK : success message
     */
    CHECK_VERSION_OK("Contrôle des versions réalisé avec succès"),
    
    /**
     * KO : fail message
     */
    CHECK_VERSION_KO("Erreur de contrôle des versions"),
    
    /**
     * OK : success message
     */
    CHECK_MANIFEST_OK("Contrôle du bordereau réalisé avec succès"),
    
    /**
     * KO : fail message
     */
    CHECK_MANIFEST_KO("Erreur de contrôle du bordereau"),

    /**
     * OK : success message
     */
    EXTRACT_MANIFEST_OK("Extraction du bordereau réalisé avec succès"),
    
    /**
     * KO : fail message
     */
    EXTRACT_MANIFEST_KO("Erreur de l'extraction du bordereau"),
    
    /**
     * OK : success message
     */
    INDEX_UNIT_OK("Index unit réalisé avec succès"),
    
    /**
     * KO : fail message
     */
    INDEX_UNIT_KO("Erreur de l'index unit"),

    /**
     * OK : success message
     */
    INDEX_OBJECT_GROUP_OK("Index objectgroup réalisé avec succès"),
    
    /**
     * KO : fail message
     */
    INDEX_OBJECT_GROUP_KO("Erreur de l'index objectgroup"),   
    
    /**
     * KO : fail message
     */
    STORAGE_OFFER_KO_UNAVAILABLE("Offre de stockage non disponible"),
    
    /**
     * KO : fail message
     */
    STORAGE_OFFER_SPACE_KO("Disponibilité de l'offre de stockage insuffisante");
    

    private String value;

    private OutcomeMessage(String value) {
        this.value = value;
    }

    /**
     * value
     *
     * @return : value of status code
     */
    public String value() {
        return value;
    }

}
