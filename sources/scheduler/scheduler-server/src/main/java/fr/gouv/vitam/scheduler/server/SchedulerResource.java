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

package fr.gouv.vitam.scheduler.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.scheduler.server.model.VitamJobDetail;
import org.apache.commons.lang.SerializationUtils;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Path("/scheduler/v1")
public class SchedulerResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchedulerResource.class);

    private static final String REG_EXP_SEPARATOR = "\\.";

    @GET
    @Path("/current-jobs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentJobs() throws SchedulerException {
        final List<JobExecutionContext> jobs =
            SchedulerListener.getInstance().getScheduler().getCurrentlyExecutingJobs();

        final List<VitamJobDetail> vitamJobs =
            jobs.stream().map(job -> new VitamJobDetail(job.getJobDetail())).collect(Collectors.toList());
        try {
            final List<JsonNode> jsonNodes = JsonHandler.toArrayList((ArrayNode) JsonHandler.toJsonNode(vitamJobs));
            return Response.ok(new RequestResponseOK<JsonNode>().addAllResults(jsonNodes)).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/jobs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobs() throws SchedulerException {
        final Scheduler scheduler = SchedulerListener.getInstance().getScheduler();
        final Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyGroup());
        final List<VitamJobDetail> vitamJobs = new ArrayList<>();
        for (JobKey jobKey : jobKeys) {
            final JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            vitamJobs.add(new VitamJobDetail(jobDetail));
        }
        try {
            final List<JsonNode> jsonNodes = JsonHandler.toArrayList((ArrayNode) JsonHandler.toJsonNode(vitamJobs));
            return Response.ok(new RequestResponseOK<JsonNode>().addAllResults(jsonNodes)).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.serverError().build();
        }
    }

    @PUT
    @Path("/pause/{group}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pauseScheduling(@PathParam("group") String group) throws SchedulerException {
        final Scheduler scheduler = SchedulerListener.getInstance().getScheduler();
        if (group.equals("ALL")) {
            scheduler.pauseAll();
        } else {
            scheduler.pauseJobs(GroupMatcher.groupEquals(group));
            scheduler.pauseTriggers(GroupMatcher.groupEquals(group));
        }
        return Response.accepted().build();
    }

    @PUT
    @Path("/resume/{group}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumeScheduling(@PathParam("group") String group) throws SchedulerException {
        final Scheduler scheduler = SchedulerListener.getInstance().getScheduler();
        if (group.equals("ALL")) {
            scheduler.resumeAll();
        } else {
            scheduler.resumeJobs(GroupMatcher.groupEquals(group));
            scheduler.resumeTriggers(GroupMatcher.groupEquals(group));
        }
        return Response.accepted().build();
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/job-state/{job-name}")
    public Response jobState(@PathParam("job-name") String jobName) throws SchedulerException {
        final Scheduler scheduler = SchedulerListener.getInstance().getScheduler();
        String[] jobKeyPath = jobName.split(REG_EXP_SEPARATOR);
        JobKey jobKey =
            (jobKeyPath.length > 1) ? JobKey.jobKey(jobKeyPath[1], jobKeyPath[0]) : JobKey.jobKey(jobKeyPath[0]);
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
        Optional<Trigger.TriggerState> reduce = triggers.stream().map(Trigger::getKey).map(e -> {
            try {
                return scheduler.getTriggerState(e);
            } catch (SchedulerException ex) {
                throw new RuntimeException(ex);
            }
        }).reduce(BinaryOperator.maxBy(Enum::compareTo));

        if (reduce.isPresent()) {
            return Response.ok(new RequestResponseOK<>().addResult(reduce.get())).build();
        } else {
            return Response.ok(new RequestResponseOK<>()).build();
        }
    }

    /*
     * for test only
     */
    @POST
    @Path("/schedule-job")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response scheduleJob(byte[] jobByte) throws SchedulerException {
        final Scheduler scheduler = SchedulerListener.getInstance().getScheduler();
        JobDetail job = (JobDetail) SerializationUtils.deserialize(jobByte);
        if (job.isDurable()) {
            scheduler.addJob(job, true);
        } else {
            scheduler.addJob(job, true, true);
        }
        return Response.accepted().build();
    }

    /*
     * for test only
     */
    @POST
    @Path("/trigger-job")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response triggerJob(byte[] triggerByte) throws SchedulerException {
        final Scheduler scheduler = SchedulerListener.getInstance().getScheduler();
        Trigger trigger = (Trigger) SerializationUtils.deserialize(triggerByte);
        scheduler.scheduleJob(trigger);
        return Response.accepted().build();
    }

    /*
     * for test only
     */
    @POST
    @Path("/trigger-job/{job-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response triggerJob(@PathParam("job-name") String jobName, JsonNode jobDataJson) throws SchedulerException {
        try {
            final Scheduler scheduler = SchedulerListener.getInstance().getScheduler();
            String[] jobKeyPath = jobName.split(REG_EXP_SEPARATOR);
            JobKey jobKey =
                (jobKeyPath.length > 1) ? JobKey.jobKey(jobKeyPath[1], jobKeyPath[0]) : JobKey.jobKey(jobKeyPath[0]);
            if (jobDataJson != null) {
                JobDataMap jobDataMap = JsonHandler.getFromJsonNode(jobDataJson, JobDataMap.class);
                scheduler.triggerJob(jobKey, jobDataMap);
            } else {
                scheduler.triggerJob(jobKey);
            }
            return Response.accepted().build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.serverError().build();
        }
    }
}
