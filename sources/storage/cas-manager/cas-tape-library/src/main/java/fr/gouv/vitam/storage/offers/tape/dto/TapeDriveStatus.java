/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.storage.offers.tape.dto;

/**
 * For more information, @see http://manpagesfr.free.fr/man/man4/st.4.html
 */
public enum TapeDriveStatus {
    // Tape position is just after a filemark
    EOF("EOF"),

    // Begin of tape
    BOT("BOT"),

    /**
     * End of tape
     *
     * @deprecated EndOfTape status is not properly handled by MTX utility.
     * According to the man page of mtx utility : "In addition,  MTX  does  not handle the end of tape properly."
     */
    EOT("EOT"),

    // Tape position is at Setmark
    SM("SM"),

    // End of data
    EOD("EOD"),

    // Write Protection (read only)
    WR_PROT("WR_PROT"),

    //Tape is in drive markReady to read/write
    ONLINE("ONLINE"),

    // D_* density of drives
    D_6250("D_6250"),
    D_1600("D_1600"),
    D_800("D_800"),

    // Drive is empty (no tape)
    DR_OPEN("DR_OPEN"),

    // immediately report mode (1 if cache is enabled or no guarantee that data is physically written to tape. 0 if cache is disabled)
    IM_REP_EN("IM_REP_EN"),

    // Drive clean demand
    CLN("CLN");

    String status;

    TapeDriveStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
