package fr.gouv.vitam.storage.engine.common.model.request;

import fr.gouv.vitam.storage.engine.common.model.Order;

/**
 * Offer Log request data.
 */
public class OfferLogRequest {

    /**
     * offset.
     */
    private Long offset;

    /**
     * max number of element.
     */
    private int limit;

    /**
     * order
     */
    private Order order;


    /**
     * Constructor for jackson
     */
    public OfferLogRequest() {
        super();
    }

    /**
     * Constructor
     * 
     * @param offset offset
     * @param limit limit
     * @param order order
     */
    public OfferLogRequest(Long offset, int limit, Order order) {
        super();
        this.offset = offset;
        this.limit = limit;
        this.order = order;
    }

    /**
     * @return current offset.
     */
    public Long getOffset() {
        return offset;
    }

    /**
     * 
     * @param offset
     */
    public void setOffset(Long offset) {
        this.offset = offset;
    }


    /**
     *
     * @return limit.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * 
     * @param limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @return the order
     */
    public Order getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(Order order) {
        this.order = order;
    }


}
