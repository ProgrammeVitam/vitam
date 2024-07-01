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

package fr.gouv.vitam.processing.common.metrics;

import fr.gouv.vitam.common.metrics.VitamMetricsNames;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

public class CommonProcessingMetrics {

    /**
     * Count the number of worker task in the queue
     * If the queue is empty, means there no more operation.
     * => if per month for example we have empty queue, means that we can reduce the number of workers
     * If the queue is full, means that worker cant consume all tasks
     * => perhaps, we have to add more workers
     */
    public static final Gauge WORKER_TASKS_IN_QUEUE = Gauge.build()
        .name(VitamMetricsNames.VITAM_PROCESSING_WORKER_TASK_IN_QUEUE_TOTAL)
        .labelNames("worker_family")
        .help("Current number of worker tasks in the queue")
        .register();

    /**
     * Count the number of tasks created by the distributor and not yet completed
     * The tasks maybe in the queue or waiting to be enqueued
     */
    public static final Gauge CURRENTLY_INSTANTIATED_TASKS = Gauge.build()
        .name(VitamMetricsNames.VITAM_PROCESSING_WORKER_CURRENT_TASK_TOTAL)
        .labelNames("worker_family", "workflow", "step_name")
        .help(
            "Current number of worker tasks instantiated by the distributor. In queue or waiting to be added to the queue"
        )
        .register();

    /**
     * Count the number of registered workers
     * If we know that we have 2 workers, but metrics says 1 worker
     * => this means that we have to analyse why we have only 1 instead of 2
     */
    public static final Gauge REGISTERED_WORKERS = Gauge.build()
        .name(VitamMetricsNames.VITAM_PROCESSING_WORKER_REGISTERED_TOTAL)
        .labelNames("worker_family")
        .help("Current number of workers")
        .register();

    /**
     * Worker task execution duration
     * From call of worker until receiving the response.
     * Task contains one or collection of elements to send to workers
     */
    public static final Histogram WORKER_TASKS_EXECUTION_DURATION_HISTOGRAM = Histogram.build()
        .name(VitamMetricsNames.VITAM_PROCESSING_WORKER_TASK_EXECUTION_DURATION_SECONDS)
        .help(
            "Worker tasks execution duration. From call of worker until receiving the response. Task contains one or collection of elements to send to workers"
        )
        .labelNames("worker_family", "worker_name", "workflow", "step_name")
        .buckets(.01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10, 30, 60, 120, 180, 300, 600, 1800, 3600)
        .register();

    /**
     * Worker task waiting in the queue
     * From task creation, until dequeue by a given thread
     */
    public static final Histogram WORKER_TASKS_IDLE_DURATION_IN_QUEUE = Histogram.build()
        .name(VitamMetricsNames.VITAM_PROCESSING_WORKER_TASK_IDLE_DURATION_IN_QUEUE_SECONDS)
        .help(
            "Worker tasks waiting time since task creation until task dequeue from the queue. Task contains one or collection of elements to send to workers"
        )
        .labelNames("worker_family", "workflow", "step_name")
        .buckets(
            .005,
            .01,
            .025,
            .05,
            .075,
            .1,
            .25,
            .5,
            .75,
            1,
            2.5,
            5,
            7.5,
            10,
            30,
            60,
            120,
            180,
            300,
            600,
            1800,
            3600
        )
        .register();

    /**
     * ProcessWorkflow step execution duration form ProcessEngine point of view
     */
    public static final Histogram PROCESS_WORKFLOW_STEP_EXECUTION_DURATION_HISTOGRAM = Histogram.build()
        .name(VitamMetricsNames.VITAM_PROCESSING_WORKFLOW_STEP_EXECUTION_DURATION_SECONDS)
        .help("ProcessWorkflow step execution duration. From call of distributor until receiving the response")
        .labelNames("workflow", "step_name")
        .buckets(.1, .25, .5, .75, 1, 2.5, 5, 7.5, 10, 30, 60, 120, 180, 300, 600, 1800, 3600)
        .register();
}
