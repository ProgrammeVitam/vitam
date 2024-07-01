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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionManager;

import java.util.Collection;

/**
 * Pagination Helper
 * <p>
 */
public class PaginationHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PaginationHelper.class);
    private static final String PARAMETERS = "PaginationHelper parameters";
    private static final String RESULT_SESSION_ATTRIBUTE = "$results";
    private static final String JSON_NODE_RESULT = "$results";
    private static final String JSON_NODE_HITS = "$hits";
    private static final String JSON_NODE_OFFSET = "offset";
    private static final String JSON_NODE_LIMIT = "limit";

    private static final PaginationHelper instance = new PaginationHelper();

    public static PaginationHelper getInstance() {
        return instance;
    }

    /**
     * @param sessionId
     * @param result
     * @throws VitamException
     */
    public void setResult(String sessionId, JsonNode result) throws VitamException {
        ParametersChecker.checkParameter(PARAMETERS, sessionId, result);
        final Session session = getSession(sessionId);
        session.setAttribute(RESULT_SESSION_ATTRIBUTE, result);
    }

    /**
     * @param sessionId
     * @param pagination
     * @return JsonNode
     * @throws VitamException
     */
    public JsonNode getResult(String sessionId, OffsetBasedPagination pagination) throws VitamException {
        final Session session = getSession(sessionId);
        final ObjectNode result = (ObjectNode) session.getAttribute(RESULT_SESSION_ATTRIBUTE);
        if (result != null) {
            return paginate(result, pagination);
        }
        return JsonHandler.createObjectNode();
    }

    /**
     * @param result
     * @param pagination
     * @return JsonNode
     * @throws VitamException
     */
    public JsonNode getResult(JsonNode result, OffsetBasedPagination pagination) throws VitamException {
        final ObjectNode jsonResult = (ObjectNode) result;
        return paginate(jsonResult, pagination);
    }

    private Session getSession(String sessionId) throws VitamException {
        try {
            final DefaultSecurityManager securityManager = (DefaultSecurityManager) SecurityUtils.getSecurityManager();
            final DefaultSessionManager sessionManager = (DefaultSessionManager) securityManager.getSessionManager();
            final Collection<Session> activeSessions = sessionManager.getSessionDAO().getActiveSessions();

            for (final Session session : activeSessions) {
                if (session.getId().equals(sessionId)) {
                    return session;
                }
            }

            throw new VitamException("Session Not Found Exception");
        } catch (final UnavailableSecurityManagerException e) {
            LOGGER.debug(e.getMessage(), e);
            throw new VitamException(e.getMessage(), e);
        }
    }

    public JsonNode paginate(ObjectNode result, OffsetBasedPagination pagination)
        throws InvalidParseOperationException {
        final ObjectNode jsonResult = (ObjectNode) JsonHandler.toJsonNode(result);
        JsonNode resultsPagination;
        if (pagination != null) {
            resultsPagination = JsonHandler.getSubArrayNode(
                (ArrayNode) jsonResult.get(JSON_NODE_RESULT),
                pagination.getOffset(),
                pagination.getLimit()
            );
        } else {
            resultsPagination = jsonResult.get(JSON_NODE_RESULT);
        }
        jsonResult.replace(JSON_NODE_RESULT, resultsPagination);

        final ObjectNode hits = (ObjectNode) jsonResult.get(JSON_NODE_HITS);
        if (pagination != null) {
            hits.put(JSON_NODE_OFFSET, pagination.getOffset());
            hits.put(JSON_NODE_LIMIT, pagination.getLimit());
        } else {
            hits.put(JSON_NODE_OFFSET, 0);
            hits.put(JSON_NODE_LIMIT, 10000);
        }
        jsonResult.replace(JSON_NODE_HITS, hits);

        return jsonResult;
    }
}
