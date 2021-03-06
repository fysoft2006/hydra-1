/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.hydra.job.alert.types;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.addthis.basis.util.LessStrings;

import com.addthis.codec.annotations.Time;
import com.addthis.hydra.job.Job;
import com.addthis.hydra.job.alert.AbstractJobAlert;
import com.addthis.hydra.job.alert.JobAlertUtil;
import com.addthis.meshy.MeshyClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This {@link AbstractJobAlert JobAlert} <span class="hydra-summary">alerts on simple threshold for tree jobs</span>.
 *
 * @user-reference
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapCanaryJobAlert extends AbstractJobAlert {

    /**
     * Query path. For example, 'root/ymd/{{now-1}}:+count'.
     * The query should have exactly one '+' (generally, +count)
     * and return a single numeric quantity per task.
     */
    @JsonProperty public final String canaryPath;

    /**
     * Alert if computed value is below the threshold.
     */
    @JsonProperty public final int canaryConfigThreshold;

    public MapCanaryJobAlert(@Nullable @JsonProperty("alertId") String alertId,
                             @JsonProperty("description") String description,
                             @Time(TimeUnit.MINUTES) @JsonProperty("delay") long delay,
                             @JsonProperty("email") String email,
                             @JsonProperty(value = "jobIds", required = true) List<String> jobIds,
                             @JsonProperty("suppressChanges") boolean suppressChanges,
                             @JsonProperty("canaryPath") String canaryPath,
                             @JsonProperty("canaryConfigThreshold") int canaryConfigThreshold,
                             @JsonProperty("lastAlertTime") long lastAlertTime,
                             @JsonProperty("activeJobs") Map<String, String> activeJobs,
                             @JsonProperty("activeTriggerTimes") Map<String, Long> activeTriggerTimes) {
        super(alertId, description, delay, email, jobIds, suppressChanges,
              lastAlertTime, activeJobs, activeTriggerTimes);
        this.canaryPath = canaryPath;
        this.canaryConfigThreshold = canaryConfigThreshold;
    }

    @JsonIgnore
    @Override protected String getTypeStringInternal() {
        return "Map canary";
    }

    @Nullable @Override
    protected String testAlertActiveForJob(@Nullable MeshyClient meshClient, Job job, String previousErrorMessage) {
        try {
            long queryVal = JobAlertUtil.getQueryCount(job.getId(), canaryPath);
            consecutiveCanaryExceptionCount.set(0);
            if (queryVal < canaryConfigThreshold) {
                return "query value: " + queryVal + " < " + canaryConfigThreshold;
            }
        } catch (Exception ex) {
            return handleCanaryException(ex, previousErrorMessage);
        }
        return null;
    }

    @Override public String isValid() {
        if (LessStrings.isEmpty(canaryPath)) {
            return "Canary path is empty";
        } else if (canaryConfigThreshold <= 0) {
            return "Canary config is not a positive integer";
        } else {
            return null;
        }
    }

}
