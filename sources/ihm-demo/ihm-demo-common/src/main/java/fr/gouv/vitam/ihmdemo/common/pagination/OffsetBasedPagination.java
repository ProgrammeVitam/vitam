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
package fr.gouv.vitam.ihmdemo.common.pagination;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.ihmdemo.common.api.IhmDataRest;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * Offset-based pagination using HTTP Headers
 */
public class OffsetBasedPagination {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OffsetBasedPagination.class);
    private static final String PARAMETERS = "OffsetBasedPagination parameters";

    public static final String HEADER_LIMIT = IhmDataRest.X_LIMIT;
    public static final String HEADER_OFFSET = IhmDataRest.X_OFFSET;
    public static final String HEADER_TOTAL = IhmDataRest.X_TOTAL;

    private int offset = PaginationParameters.DEFAULT_OFFSET;
    private int limit = PaginationParameters.DEFAULT_LIMIT;
    private int total = 0;

    /**
     * Empty Pagination Constructor
     */
    public OffsetBasedPagination() {
        // Empty
    }

    /**
     * @param request
     * @throws VitamException
     */
    public OffsetBasedPagination(HttpServletRequest request) throws VitamException {
        parseHttpHeaders(request);
    }

    /**
     * @param offset
     * @param limit
     */
    public OffsetBasedPagination(int offset, int limit) {
        this(offset, limit, 0);
    }

    /**
     * @param offset
     * @param limit
     * @param total
     */
    public OffsetBasedPagination(int offset, int limit, int total) {
        ParametersChecker.checkParameter(PARAMETERS, offset, limit, total);
        setLimit(limit).setOffset(offset).setTotal(total);
    }

    /**
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @param offset the offset to set
     * @return this
     */
    public OffsetBasedPagination setOffset(int offset) {
        ParametersChecker.checkParameter(PARAMETERS, offset);
        this.offset = offset;
        return this;
    }

    /**
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param limit the limit to set
     * @return this
     */
    public OffsetBasedPagination setLimit(int limit) {
        ParametersChecker.checkParameter(PARAMETERS, limit);
        this.limit = limit;
        return this;
    }

    /**
     * @return the total
     */
    public int getTotal() {
        return total;
    }

    /**
     * @param total the total to set
     * @return this
     */
    public OffsetBasedPagination setTotal(int total) {
        ParametersChecker.checkParameter(PARAMETERS, total);
        this.total = total;
        return this;
    }

    /**
     * parse From Headers
     *
     * @param request
     * @throws VitamException
     */
    private OffsetBasedPagination parseHttpHeaders(final HttpServletRequest request) throws VitamException {
        ParametersChecker.checkParameter(PARAMETERS, request);
        final List<String> offsetValues = Collections.list(request.getHeaders(IhmWebAppHeader.OFFSET.getName()));
        if (offsetValues != null) {
            if (offsetValues.size() == 1) {
                try {
                    offset = Integer.parseInt(offsetValues.get(0));
                    if (offset < 0 || offset > PaginationParameters.MAXIMUM_OFFSET) {
                        LOGGER.debug("Offset exceeded acceptable range" + offsetValues.get(0));
                        throw new VitamException("Offset exceeded acceptable range " + offsetValues.get(0));
                    }
                } catch (NumberFormatException | NullPointerException e) {
                    LOGGER.debug("Error parsing offset from {}", offsetValues.get(0), e);
                    throw new VitamException("Error parsing Offset from supplied value: " + offsetValues.get(0));
                }
            }
        }
        final List<String> limitValues = Collections.list(request.getHeaders(IhmWebAppHeader.LIMIT.getName()));
        if (limitValues != null) {
            if (limitValues.size() == 1) {
                try {
                    limit = Integer.parseInt(limitValues.get(0));
                    if (limit < 1 || limit > PaginationParameters.MAXIMUM_LIMIT) {
                        LOGGER.debug("Limit exceeded acceptable range " + limitValues.get(0));
                        throw new VitamException("Limit exceeded acceptable range " + limitValues.get(0));
                    }
                } catch (NumberFormatException | NullPointerException e) {
                    LOGGER.debug("Error parsing limit from {}", limitValues, e);
                    throw new VitamException("Error parsing Limit from supplied value: " + limitValues.get(0));
                }
            }
        }

        return this;
    }
}
