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

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.springframework.batch.core.JobExecution

/**
 * GrabbitJobExecution is a simple extension of {@Link JobExecution}.
 *
 * <p>
 * It simply provides a new property "transactionID." This transactionID allows us to associate a group
 * of job executions together by their common run transaction (configuration run). For example, if I
 * give Grabbit a configuration with two paths, two job executions will be started, and they could both then
 * be grouped/referenced by their common transactionID.
 * </p>
 *
 * @see {@link JcrGrabbitJobExecutionDao} for a good handle on how we
 * serve up this JobExecution during the Spring Batch lifecycle.
 */
@CompileStatic
@InheritConstructors
class GrabbitJobExecution extends JobExecution {
    long transactionID
}
