/*
 * Copyright (C) 2020 The zfoo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.zfoo.protocol.serializer.go;

import com.zfoo.protocol.generate.GenerateOperation;
import com.zfoo.protocol.generate.GenerateProtocolFile;
import com.zfoo.protocol.generate.GenerateProtocolNote;
import com.zfoo.protocol.registration.IProtocolRegistration;
import com.zfoo.protocol.registration.ProtocolRegistration;
import com.zfoo.protocol.registration.anno.Compatible;
import com.zfoo.protocol.registration.field.IFieldRegistration;
import com.zfoo.protocol.serializer.CodeLanguage;
import com.zfoo.protocol.serializer.reflect.*;
import com.zfoo.protocol.util.ClassUtils;
import com.zfoo.protocol.util.FileUtils;
import com.zfoo.protocol.util.IOUtils;
import com.zfoo.protocol.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.zfoo.protocol.util.FileUtils.LS;
import static com.zfoo.protocol.util.StringUtils.TAB;

/**
 * @author godotg
 * @version 3.0
 */
public abstract class GenerateGoUtils {

    private static String protocolOutputRootPath = "goProtocol/";

    private static Map<ISerializer, IGoSerializer> goSerializerMap;

    public static IGoSerializer goSerializer(ISerializer serializer) {
        return goSerializerMap.get(serializer);
    }

    public static void init(GenerateOperation generateOperation) {
        protocolOutputRootPath = FileUtils.joinPath(generateOperation.getProtocolPath(), protocolOutputRootPath);

        FileUtils.deleteFile(new File(protocolOutputRootPath));
        FileUtils.createDirectory(protocolOutputRootPath);

        goSerializerMap = new HashMap<>();
        goSerializerMap.put(BooleanSerializer.INSTANCE, new GoBooleanSerializer());
        goSerializerMap.put(ByteSerializer.INSTANCE, new GoByteSerializer());
        goSerializerMap.put(ShortSerializer.INSTANCE, new GoShortSerializer());
        goSerializerMap.put(IntSerializer.INSTANCE, new GoIntSerializer());
        goSerializerMap.put(LongSerializer.INSTANCE, new GoLongSerializer());
        goSerializerMap.put(FloatSerializer.INSTANCE, new GoFloatSerializer());
        goSerializerMap.put(DoubleSerializer.INSTANCE, new GoDoubleSerializer());
        goSerializerMap.put(CharSerializer.INSTANCE, new GoCharSerializer());
        goSerializerMap.put(StringSerializer.INSTANCE, new GoStringSerializer());
        goSerializerMap.put(ArraySerializer.INSTANCE, new GoArraySerializer());
        goSerializerMap.put(ListSerializer.INSTANCE, new GoListSerializer());
        goSerializerMap.put(SetSerializer.INSTANCE, new GoSetSerializer());
        goSerializerMap.put(MapSerializer.INSTANCE, new GoMapSerializer());
        goSerializerMap.put(ObjectProtocolSerializer.INSTANCE, new GoObjectProtocolSerializer());
    }

    public static void clear() {
        goSerializerMap = null;
        protocolOutputRootPath = null;
    }

    /**
     * 生成协议依赖的工具类
     */
    public static void createProtocolManager(List<IProtocolRegistration> protocolList) throws IOException {
        var list = List.of("go/ByteBuffer.go");

        for (var fileName : list) {
            var fileInputStream = ClassUtils.getFileFromClassPath(fileName);
            var createFile = new File(StringUtils.format("{}/{}", protocolOutputRootPath, StringUtils.substringAfterFirst(fileName, "go/")));
            FileUtils.writeInputStreamToFile(createFile, fileInputStream);
        }

        var initProtocolBuilder = new StringBuilder();
        protocolList.stream()
                .filter(it -> Objects.nonNull(it))
                .forEach(it -> initProtocolBuilder.append(TAB).append(StringUtils.format("Protocols[{}] = new({})", it.protocolId(), it.protocolConstructor().getDeclaringClass().getSimpleName())).append(LS));

        var protocolManagerTemplate = StringUtils.bytesToString(IOUtils.toByteArray(ClassUtils.getFileFromClassPath("go/ProtocolManagerTemplate.go")));
        protocolManagerTemplate = StringUtils.format(protocolManagerTemplate, initProtocolBuilder.toString().trim());
        FileUtils.writeStringToFile(new File(StringUtils.format("{}/{}", protocolOutputRootPath, "ProtocolManager.go")), protocolManagerTemplate, true);
    }

    public static void createGoProtocolFile(ProtocolRegistration registration) throws IOException {
        GenerateProtocolFile.index.set(0);

        var protocolId = registration.protocolId();
        var registrationConstructor = registration.getConstructor();
        var protocolClazzName = registrationConstructor.getDeclaringClass().getSimpleName();

        var protocolTemplate = StringUtils.bytesToString(IOUtils.toByteArray(ClassUtils.getFileFromClassPath("go/ProtocolTemplate.go")));

        var classNote = GenerateProtocolNote.classNote(protocolId, CodeLanguage.Go);
        var fieldDefinition = fieldDefinition(registration);
        var writeObject = writeObject(registration);
        var readObject = readObject(registration);
        protocolTemplate = StringUtils.format(protocolTemplate, classNote, protocolClazzName, fieldDefinition.trim()
                , protocolClazzName, protocolId, protocolClazzName, protocolClazzName
                , writeObject.trim()
                , protocolClazzName, protocolClazzName, readObject.trim());

        var protocolOutputPath = StringUtils.format("{}/{}.go"
                , protocolOutputRootPath
                , protocolClazzName);
        FileUtils.writeStringToFile(new File(protocolOutputPath), protocolTemplate, true);
    }

    private static String fieldDefinition(ProtocolRegistration registration) {
        var protocolId = registration.protocolId();
        var fields = registration.getFields();
        var fieldRegistrations = registration.getFieldRegistrations();

        var csBuilder = new StringBuilder();
        // 协议的属性生成
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            IFieldRegistration fieldRegistration = fieldRegistrations[i];
            var fieldName = field.getName();
            var fieldType = goSerializer(fieldRegistration.serializer()).fieldType(field, fieldRegistration);

            var propertyFullName = StringUtils.format("{} {}", fieldName, fieldType);
            // 生成注释
            var filedNote = GenerateProtocolNote.fieldNote(protocolId, fieldName, CodeLanguage.Go);
            if (StringUtils.isNotBlank(filedNote)) {
                csBuilder.append(TAB).append(filedNote).append(LS);
            }
            csBuilder.append(TAB).append(propertyFullName).append(LS);
        }
        return csBuilder.toString();
    }

    private static String writeObject(ProtocolRegistration registration) {
        var fields = registration.getFields();
        var fieldRegistrations = registration.getFieldRegistrations();
        var csBuilder = new StringBuilder();
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            var fieldRegistration = fieldRegistrations[i];
            goSerializer(fieldRegistration.serializer()).writeObject(csBuilder, "message." + field.getName(), 1, field, fieldRegistration);
        }
        return csBuilder.toString();
    }


    private static String readObject(ProtocolRegistration registration) {
        var fields = registration.getFields();
        var fieldRegistrations = registration.getFieldRegistrations();
        var csBuilder = new StringBuilder();
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            var fieldRegistration = fieldRegistrations[i];
            if (field.isAnnotationPresent(Compatible.class)) {
                csBuilder.append(TAB).append("if !buffer.IsReadable()").append(LS);
                csBuilder.append(TAB).append("{").append(LS);
                csBuilder.append(TAB + TAB).append("return packet").append(LS);
                csBuilder.append(TAB).append("}").append(LS);
            }
            var readObject = goSerializer(fieldRegistration.serializer()).readObject(csBuilder, 1, field, fieldRegistration);
            csBuilder.append(TAB).append(StringUtils.format("packet.{} = {}", field.getName(), readObject)).append(LS);
        }
        return csBuilder.toString();
    }

    public static String toGoClassName(String typeName) {
        typeName = typeName.replaceAll("java.util.|java.lang.", StringUtils.EMPTY);
        typeName = typeName.replaceAll("com\\.[a-zA-Z0-9_.]*\\.", StringUtils.EMPTY);

        // CSharp不适用基础类型的泛型，会影响性能
        switch (typeName) {
            case "boolean":
            case "Boolean":
                typeName = "bool";
                return typeName;
            case "byte":
            case "Byte":
                typeName = "int8";
                return typeName;
            case "short":
            case "Short":
                typeName = "int16";
                return typeName;
            case "int":
            case "Integer":
                typeName = "int";
                return typeName;
            case "long":
            case "Long":
                typeName = "int64";
                return typeName;
            case "Float":
                typeName = "float32";
                return typeName;
            case "Double":
                typeName = "float64";
                return typeName;
            case "Character":
                typeName = "string";
                return typeName;
            default:
        }

        return typeName;
    }
}
