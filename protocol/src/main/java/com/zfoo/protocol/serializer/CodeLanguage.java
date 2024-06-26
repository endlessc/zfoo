/*
 * Copyright (C) 2020 The zfoo Authors
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.zfoo.protocol.serializer;

/**
 * @author godotg
 */
public enum CodeLanguage {

    /**
     * Javassist字节码增强
     */
    Enhance(1),

    Cpp(1 << 1),

    Go(1 << 2),

    JavaScript(1 << 3),

    ES(1 << 4),

    TypeScript(1 << 5),

    Lua(1 << 10),

    CSharp(1 << 11),

    GdScript(1 << 12),

    Python(1 << 13),

    Protobuf(1 << 30);

    public final int id;

    private CodeLanguage(int id) {
        this.id = id;
    }

}
