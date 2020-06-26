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

package fr.gouv.vitam.common.metrics;

public class VitamMetricsNames {

    private VitamMetricsNames() {
        // This class is only for constants
    }

    /*
     * =================================
     *            Common
     * ==================================
     */

    /**
     * Vitam requests size in bytes per tenant and method
     * Type: Summary
     * Labels: "tenant", "method"
     */
    public static final String VITAM_REQUESTS_SIZE_BYTES = "vitam_requests_size_bytes";

    /**
     * Vitam responses size in bytes per tenant and method
     * Type: Summary
     * Labels: "tenant", "method
     */
    public static final String VITAM_RESPONSES_SIZE_BYTES = "vitam_responses_size_bytes";


    /**
     * Vitam storage upload objects to offers size in bytes per tenant, strategy, offer_id, data_category, origin (normal, bulk, offer_sync), and per attempt
     * Type: Summary
     * Labels: "tenant", "strategy", "offer_id", "data_category", "origin", "attempt"
     */
    public static final String VITAM_STORAGE_UPLOAD_SIZE_BYTES = "vitam_storage_upload_size_bytes";

    /**
     * Vitam storage download objects from offers size in bytes per tenant, strategy, offer_id, origin of request  (normal, traceability, offer_sync) and data_category
     * Type: Summary
     * Labels: "tenant", "strategy", "offer_id", "origin", "data_category"
     */
    public static final String VITAM_STORAGE_DOWNLOAD_SIZE_BYTES = "vitam_storage_download_size_bytes";


    /**
     * Vitam alert service counter per log_level
     * Type: Counter
     * Labels: "log_level"
     */
    public static final String VITAM_ALERT_COUNTER = "vitam_alert_count";


    /**
     * Vitam consistency errors counter
     * Type: Counter
     * Labels: "tenant", "service"
     */
    public static final String VITAM_CONSISTENCY_ERRORS_COUNT = "vitam_consistency_errors_count";

    /*
     * =================================
     *            Processing
     * ==================================
     */

    /**
     * Vitam operation count per state and status
     * Type: Gauge
     * Labels: "workflow", "state", "status"
     */
    public static final String VITAM_PROCESSING_WORKFLOW_OPERATION_TOTAL = "vitam_processing_workflow_operation_total";

    /**
     * Current number of worker tasks in the queue
     * Type: Gauge
     * Labels: "worker_family"
     */
    public static final String VITAM_PROCESSING_WORKER_TASK_IN_QUEUE_TOTAL =
        "vitam_processing_worker_task_in_queue_total";


    /**
     * Current number of worker tasks instantiated by the distributor. In queue or waiting to be added to the queue
     * Type: Gauge
     * Labels: "worker_family", "workflow", "step_name"
     */
    public static final String VITAM_PROCESSING_WORKER_CURRENT_TASK_TOTAL =
        "vitam_processing_worker_current_task_total";

    /**
     * Current number of workers
     * Type : Gauge
     * Labels: "worker_family"
     */
    public static final String VITAM_PROCESSING_WORKER_REGISTERED_TOTAL = "vitam_processing_worker_registered_total";

    /**
     * Worker tasks execution duration. From call of worker until receiving the response. Task contains one or collection of elements to send to workers
     * Type: Histogram
     * Labels: "worker_family", "worker_name", "workflow", "step_name"
     */
    public static final String VITAM_PROCESSING_WORKER_TASK_EXECUTION_DURATION_SECONDS =
        "vitam_processing_worker_task_execution_duration_seconds";

    /**
     * Worker tasks waiting time since task creation until task dequeue from the queue. Task contains one or collection of elements to send to workers
     * Type: Histogram
     * Labels: "worker_family", "workflow", "step_name"
     */
    public static final String VITAM_PROCESSING_WORKER_TASK_IDLE_DURATION_IN_QUEUE_SECONDS =
        "vitam_processing_worker_task_idle_duration_in_queue_seconds";


    /**
     * ProcessWorkflow step execution duration. From call of distributor until receiving the response
     * Type: Histogram
     * Labels: "workflow", "step_name"
     */
    public static final String VITAM_PROCESSING_WORKFLOW_STEP_EXECUTION_DURATION_SECONDS =
        "vitam_processing_workflow_step_execution_duration_seconds";


    /*
     * =================================
     *            Metadata
     * ==================================
     */

    /**
     * Vitam metadata effective log shipping histogram duration metric
     * Type: Histogram
     * Labels: "collection"
     */
    public static final String VITAM_METADATA_LOG_SHIPPING_DURATION = "vitam_metadata_log_shipping_duration";

    /**
     * Vitam metadata log shipping events counter for all events. Even for those with response already running
     * Type: Counter
     */
    public static final String VITAM_METADATA_LOG_SHIPPING_TOTAL = "vitam_metadata_log_shipping_total";

    /**
     * Vitam metadata reconstruction histogram metric
     * Type: Histogram
     * Labels: "tenant", "container"
     */
    public static final String VITAM_RECONSTRUCTION_DURATION = "vitam_reconstruction_duration";
}
