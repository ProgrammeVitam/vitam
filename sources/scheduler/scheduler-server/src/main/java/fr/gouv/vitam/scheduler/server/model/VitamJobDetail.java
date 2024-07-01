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

package fr.gouv.vitam.scheduler.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.quartz.JobDetail;

import java.util.Map;

public class VitamJobDetail {

    public static final String KEY = "Key";
    public static final String DESCRIPTION = "Description";
    public static final String JOB_CLASS = "JobClass";
    public static final String JOB_DATA_MAP = "JobDataMap";
    public static final String DURABILITY = "Durability";
    public static final String SHOULD_RECOVER = "ShouldRecover";

    @JsonProperty(KEY)
    private String key;

    @JsonProperty(DESCRIPTION)
    private String description;

    @JsonProperty(JOB_CLASS)
    private String jobClass;

    @JsonProperty(JOB_DATA_MAP)
    private Map<String, Object> jobDataMap;

    @JsonProperty(DURABILITY)
    private boolean durability;

    @JsonProperty(SHOULD_RECOVER)
    private boolean shouldRecover;

    public VitamJobDetail(
        String key,
        String description,
        String jobClass,
        Map<String, Object> jobDataMap,
        boolean durability,
        boolean shouldRecover
    ) {
        this.key = key;
        this.description = description;
        this.jobClass = jobClass;
        this.jobDataMap = jobDataMap;
        this.durability = durability;
        this.shouldRecover = shouldRecover;
    }

    public VitamJobDetail(JobDetail jobDetail) {
        this(
            jobDetail.getKey().toString(),
            jobDetail.getDescription(),
            jobDetail.getJobClass().toString(),
            jobDetail.getJobDataMap().getWrappedMap(),
            jobDetail.isDurable(),
            jobDetail.requestsRecovery()
        );
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobClass() {
        return jobClass;
    }

    public void setJobClass(String jobClass) {
        this.jobClass = jobClass;
    }

    public Map<String, Object> getJobDataMap() {
        return jobDataMap;
    }

    public void setJobDataMap(Map<String, Object> jobDataMap) {
        this.jobDataMap = jobDataMap;
    }

    public boolean isDurability() {
        return durability;
    }

    public void setDurability(boolean durability) {
        this.durability = durability;
    }

    public boolean isShouldRecover() {
        return shouldRecover;
    }

    public void setShouldRecover(boolean shouldRecover) {
        this.shouldRecover = shouldRecover;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
