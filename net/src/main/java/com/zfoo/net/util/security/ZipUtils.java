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

package com.zfoo.net.util.security;

import com.zfoo.protocol.util.FileUtils;
import com.zfoo.protocol.util.IOUtils;

import java.io.*;
import java.util.zip.*;

/**
 * @author godotg
 */
public abstract class ZipUtils {

    private static final int BUFFER_SIZE = IOUtils.ONE_BYTE;

    // compression level (0-9)，只能是0-9
    private static final int COMPRESS_LEVEL = 5;

    public static byte[] zip(byte[] bytes) {
        // deflate  [dɪ'fleɪt]  v.抽出空气; 缩小
        Deflater deflater = new Deflater(COMPRESS_LEVEL);
        deflater.setInput(bytes);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
        byte[] buffer = new byte[BUFFER_SIZE];

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            baos.write(buffer, 0, count);
        }
        deflater.end();

        IOUtils.closeIO(baos);

        return baos.toByteArray();
    }

    public static byte[] unZip(byte[] bytes) {
        Inflater inflater = new Inflater();
        inflater.setInput(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                baos.write(buffer, 0, count);
            }
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeIO(baos);
        }
        return baos.toByteArray();
    }

    public static void unzip(String zipFilePath, String destDirectory) {
        FileUtils.createDirectory(destDirectory);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(zipFilePath);
            var zipIn = new ZipInputStream(fileInputStream);
            var entry = zipIn.getNextEntry();
            // 遍历ZIP文件中的所有条目
            while (entry != null) {
                var filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // 如果条目是文件，则解压该文件
                    FileUtils.writeInputStreamToFile(new File(filePath), zipIn);
                } else {
                    // 如果条目是目录，则创建目录
                    FileUtils.createDirectory(filePath);
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
        } catch (Exception e) {
            IOUtils.closeIO(fileInputStream);
        }
    }

}
