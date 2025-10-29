/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.fpetrola.oozx.fuse.peripherals.t;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;

public class DownloadAndUnzip {

  private static final Path TMP_DIR = Paths.get("/tmp");

  public static void main(String[] args) {
    new DownloadAndUnzip().unzip("https://zxinfo.dk/media/zxdb/sinclair/entries/0030743/BigBrother.z80.zip");
  }

  public Path unzip(String zipUrl) {
    String outputDirName = "zxinfo_extracted";

    try {
//      File tempFile = File.createTempFile("zxinfo", "tmp");
      Path extractDir = TMP_DIR.resolve(outputDirName);
      Files.createDirectories(extractDir);

      Path path = downloadAndUnzip(zipUrl, extractDir);
      System.out.println("Descomprimido en: " + extractDir.toAbsolutePath());
      return path;

    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public static Path downloadAndUnzip(String zipUrl, Path extractTo) throws IOException {
    List<Path> result= new ArrayList<>();
// 1. Descargar el ZIP en memoria (o a disco si es muy grande)
    byte[] zipData = downloadFile(new URL(zipUrl));

    // 2. Descomprimir en memoria
    try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
         ZipInputStream zis = new ZipInputStream(bais)) {

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        Path filePath = extractTo.resolve(entry.getName()).normalize();
        result.add(filePath);
        // Seguridad: evitar path traversal (../)
        if (!filePath.startsWith(extractTo)) {
          throw new IOException("Entrada ZIP maliciosa: " + entry.getName());
        }

        if (entry.isDirectory()) {
          Files.createDirectories(filePath);
        } else {
          Files.createDirectories(filePath.getParent());
          try (OutputStream out = Files.newOutputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = zis.read(buffer)) > 0) {
              out.write(buffer, 0, len);
            }
          }
        }
        zis.closeEntry();
      }
    }

    return result.get(0);
  }

  private static byte[] downloadFile(URL url) throws IOException {
    try (InputStream in = url.openStream();
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
      return baos.toByteArray();
    }
  }
}