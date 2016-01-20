package com.twcable.grabbit

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

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

/**
 * Exception representing a fault in retrieving a non-existent job from a job explorer.
 * See {@link ClientJobStatus} for usage.
 */
@CompileStatic
@InheritConstructors
class NonExistentJobException extends RuntimeException {}
