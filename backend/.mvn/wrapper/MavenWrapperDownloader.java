
/*
 * Copyright 2007-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.net.*;
import java.io.*;
import java.nio.channels.*;
import java.util.Properties;

public class MavenWrapperDownloader {

    private static final String WRAPPER_VERSION = "3.2.0";
    private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("MVNW_VERBOSE"));

    public static void main(String args[]) {
        File baseDirectory = new File(args[0]);
        System.out.println("- Downloader started");
        File wrapperJar = new File(baseDirectory, ".mvn/wrapper/maven-wrapper.jar");
        if (!wrapperJar.exists()) {
            downloadFileFromURL(
                "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/" + WRAPPER_VERSION + "/maven-wrapper-distribution-" + WRAPPER_VERSION + "-bin.zip",
                wrapperJar
            );
        }
        System.exit(0);
    }

    private static void downloadFileFromURL(String urlString, File destination) {
        try {
            URL url = new URL(urlString);
            if (VERBOSE) {
                System.out.println("- Downloading from: " + url);
            }
            try (ReadableByteChannel rbc = Channels.newChannel(url.openStream())) {
                try (FileOutputStream fos = new FileOutputStream(destination)) {
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }
            }
            if (VERBOSE) {
                System.out.println("- Downloaded to: " + destination);
            }
        } catch (Exception e) {
            System.err.println("Error downloading maven-wrapper.jar: " + e.getMessage());
            System.exit(1);
        }
    }
}
