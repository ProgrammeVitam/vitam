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
package fr.gouv.vitam.access.external.api;

public class AccessExtAPI {

    private AccessExtAPI() {}
    
    public static final String ACCESSION_REGISTERS = "accession-registers";
    public static final String ACCESSION_REGISTERS_API = "/" + ACCESSION_REGISTERS;
    public static final String ACCESSION_REGISTERS_API_UPDATE = "/accession-registers";
    
    public static final String ACCESSION_REGISTERS_DETAIL = "accession-register-detail";
    
    public static final String ENTRY_CONTRACT = "entrycontracts";
    public static final String AGENCIES = "agencies";

    public static final String ENTRY_CONTRACT_API = "/" + ENTRY_CONTRACT;
    public static final String ENTRY_CONTRACT_API_UPDATE = "/entrycontracts";
    
    public static final String ACCESS_CONTRACT = "accesscontracts";
    public static final String ACCESS_CONTRACT_API = "/" + ACCESS_CONTRACT;
    public static final String ACCESS_CONTRACT_API_UPDATE = "/accesscontracts";
    
    public static final String PROFILES = "profiles";
    public static final String PROFILES_API = "/" + PROFILES;
    public static final String PROFILES_API_UPDATE = "profiles";
    
    public static final String CONTEXTS = "contexts";
    public static final String CONTEXTS_API = "/" + CONTEXTS;
    public static final String CONTEXTS_API_UPDATE = "contexts";
    
    public static final String FORMATS = "formats";
    
    public static final String RULES = "rules";
    
    public static final String TRACEABILITY = "traceability";
    public static final String TRACEABILITY_API = "/" + TRACEABILITY;
    
    public static final String AUDITS = "audits";
    public static final String AUDITS_API = "/" + AUDITS;

    public static final String SECURITY_PROFILES = "securityprofiles";
}
