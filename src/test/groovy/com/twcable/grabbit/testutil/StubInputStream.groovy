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
package com.twcable.grabbit.testutil

import javax.annotation.Nonnull
import javax.servlet.ServletInputStream

class StubInputStream extends ServletInputStream {


    private final int byte_length
    private final byte[] bytes
    private int byte_index = 0

    private StubInputStream(@Nonnull final String data) {
        bytes = data as byte[]
        byte_length = bytes.length
    }

    private StubInputStream(@Nonnull final File fromFile) {
        bytes = fromFile.readBytes()
        byte_length = bytes.length
    }

    static InputStream inputStream(@Nonnull final String data) {
        return new StubInputStream(data)
    }

    static InputStream inputStream(@Nonnull final File fromFile) {
        return new StubInputStream(fromFile)
    }

    @Override
    int read() throws IOException {
        if (byte_index <= byte_length - 1) {
            final thisByte = bytes[byte_index] as int
            byte_index++
            return thisByte
        }
        return -1
    }
}
