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

import org.springframework.batch.core.repository.dao.ExecutionContextDao
import org.springframework.batch.item.ExecutionContext;


/**
 * Modified DAO Interface for persisting and retrieving {@link ExecutionContext}
 * @see ExecutionContextDao for more details
 */
interface GrabbitExecutionContextDao extends ExecutionContextDao {

    /**
     * Returns job execution context paths by comparing "executionId" property on "executionContext/job/<id>" with
     * "executionId" property on JobExecutions for the @param jobExecutionResourcePaths
     */
    public Collection<String> getJobExecutionContextPaths(Collection<String> jobExecutionResourcePaths)

    /**
     * Returns step execution context paths by comparing "executionId" property on "executionContext/job/<id>" with
     * "id" property on StepExecutions for the @param stepExecutionResourcePaths
     */
    public Collection<String> getStepExecutionContextPaths(Collection<String> stepExecutionResourcePaths)
}
