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
package fr.gouv.vitam.common.format.identification;

import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierBadRequestException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierInfo;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;

import java.nio.file.Path;
import java.util.List;

/**
 * Format identifier interface
 */
public interface FormatIdentifier {
    /**
     * Get format identifier status
     *
     * @return The format identifier software information containing version
     * @throws FormatIdentifierNotFoundException if the given identifier could not responds
     * @throws FormatIdentifierTechnicalException for any other technical exception
     */
    FormatIdentifierInfo status() throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException;

    /**
     * Identify the format of the file identified by its path
     *
     * @param pathToFile the path to the file to be identified
     * @return the FormatIdentifierResponse containing information about the format of the file
     * @throws FileFormatNotFoundException if a file format is not found by the identifier
     * @throws FormatIdentifierBadRequestException if the given path dont match a file
     * @throws FormatIdentifierNotFoundException if the given identifier could not responds
     * @throws FormatIdentifierTechnicalException for any other technical exception
     */
    List<FormatIdentifierResponse> analysePath(Path pathToFile)
        throws FileFormatNotFoundException, FormatIdentifierTechnicalException, FormatIdentifierBadRequestException, FormatIdentifierNotFoundException;
}
