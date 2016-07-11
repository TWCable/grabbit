/*
 * Copyright 2015 Time Warner Cable, Inc.
 *
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

package com.twcable.grabbit.spring.batch.repository

import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.repository.dao.JobExecutionDao

/**
 * Modified DAO Interface for persisting and retrieving {@link JobExecution}
 * @see JobExecutionDao for more details
 */
interface GrabbitJobExecutionDao extends JobExecutionDao{

    /**
     * Returns job execution paths for given BatchStatuses
     */
    public Collection<String> getJobExecutions(Collection<BatchStatus> batchStatuses)

    /**
     * Returns job execution paths which ended @param hours ago from "Now"
     */
    public Collection<String> getJobExecutions(int hours, Collection<String> jobExecutionPaths)

}
