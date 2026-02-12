/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.mi.generator;

import io.ballerina.mi.model.Connector;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

/**
 * Copies connector resources (icons, JARs, mediator classes) from the classpath/filesystem
 * to the generated connector output directory.
 * <p>
 * Extracted from ConnectorSerializer to encapsulate all resource copying logic.
 *
 * @since 0.6.0
 */
public final class ResourceCopier {

    private ResourceCopier() {
        // Utility class — no instantiation
    }

    /**
     * Copy resources from the JAR file to the destination directory.
     *
     * @param classLoader   Class loader to load resources
     * @param destination   Destination directory
     * @param jarPath       Path to the JAR file
     * @param org           Organization name
     * @param module        Module name
     * @param moduleVersion Module version
     * @throws IOException        If an I/O error occurs
     * @throws URISyntaxException If the URI is invalid
     */
    public static void copyResources(ClassLoader classLoader, Path destination, URI jarPath, String org,
                                     String module, String moduleVersion)
            throws IOException, URISyntaxException {
        URI uri = URI.create("jar:" + jarPath.toString());
        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            copyResources(classLoader, fs, destination, Connector.LIB_PATH, ".jar");
            copyIcons(classLoader, fs, destination);
        }
    }

    /**
     * Copies mediator classes from mi-native dependency.
     */
    static void copyMediatorClasses(ClassLoader classLoader, FileSystem fs, Path destination, String org,
                                    String module, String moduleVersion)
            throws IOException {
        List<Path> paths = Files.walk(fs.getPath("io/ballerina/stdlib/mi"))
                .filter(f -> f.toString().endsWith(".class"))
                .toList();

        for (Path path : paths) {
            Path relativePath = fs.getPath("io/ballerina/stdlib/mi").relativize(path);
            Path outputPath = destination.resolve(relativePath.toString());
            Files.createDirectories(outputPath.getParent());
            InputStream inputStream = getFileFromResourceAsStream(classLoader, path.toString());
            if (path.getFileName().toString().contains("ModuleInfo.class")) {
                // Reserved for future constant updates
            } else {
                Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
            inputStream.close();
        }
    }

    /**
     * Copies connector icons to the destination folder.
     * Supports:
     * 1. Single icon file from bala package (icon.png in docs folder) - used for both small and large
     * 2. Directory with two PNG files - separates into small and large by file size
     * 3. Falls back to default icons from resources if no valid icons found
     */
    static void copyIcons(ClassLoader classLoader, FileSystem fs, Path destination) throws IOException {
        Connector connector = Connector.getConnector();
        if (connector.getIconPath() == null) {
            copyResources(classLoader, fs, destination, Connector.ICON_FOLDER, ".png");
            return;
        }

        Path iconPath = Paths.get(connector.getIconPath());
        if (!iconPath.isAbsolute()) {
            iconPath = destination.getParent().resolve(connector.getIconPath()).normalize();
        }

        if (!Files.exists(iconPath)) {
            copyResources(classLoader, fs, destination, Connector.ICON_FOLDER, ".png");
            return;
        }

        if (Files.isRegularFile(iconPath)) {
            copySingleIconAsBoth(iconPath, destination);
            return;
        }

        List<Path> paths = Files.walk(iconPath)
                .filter(f -> f.toString().endsWith(".png"))
                .toList();

        if (paths.size() == 1) {
            copySingleIconAsBoth(paths.get(0), destination);
        } else if (paths.size() == 2) {
            copyIconsBySize(destination, paths);
        } else {
            copyResources(classLoader, fs, destination, Connector.ICON_FOLDER, ".png");
        }
    }

    /**
     * Copies a single icon file as both small and large icons.
     */
    private static void copySingleIconAsBoth(Path singleIconPath, Path destination) throws IOException {
        Path smallOutputPath = destination.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeOutputPath = destination.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);
        Files.createDirectories(smallOutputPath.getParent());

        copyIconToDestination(singleIconPath, smallOutputPath);
        copyIconToDestination(singleIconPath, largeOutputPath);
    }

    /**
     * Separates two PNG files into small and large icons by comparing file sizes.
     */
    private static void copyIconsBySize(Path destination, List<Path> paths) throws IOException {
        Path smallOutputPath = destination.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeOutputPath = destination.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);
        Path smallIconPath;
        Path largeIconPath;
        Files.createDirectories(smallOutputPath.getParent());
        if (Files.size(paths.get(0)) > Files.size(paths.get(1))) {
            smallIconPath = paths.get(1);
            largeIconPath = paths.get(0);
        } else {
            smallIconPath = paths.get(0);
            largeIconPath = paths.get(1);
        }

        copyIconToDestination(smallIconPath, smallOutputPath);
        copyIconToDestination(largeIconPath, largeOutputPath);
    }

    /**
     * Copies a single icon PNG file to the given destination path.
     */
    private static void copyIconToDestination(Path iconPath, Path destination) throws IOException {
        try (InputStream inputStream = Files.newInputStream(iconPath)) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Copies resources from a JAR filesystem matching the given file extension.
     */
    private static void copyResources(ClassLoader classLoader, FileSystem fs, Path destination,
                                      String resourceFolder, String fileExtension) throws IOException {
        List<Path> paths = Files.walk(fs.getPath(resourceFolder))
                .filter(f -> f.toString().contains(fileExtension))
                .toList();
        for (Path path : paths) {
            copyResource(classLoader, path, destination);
        }
    }

    /**
     * Copies a single resource file from classpath to the destination.
     */
    private static void copyResource(ClassLoader classLoader, Path path, Path destination) throws IOException {
        Path outputPath = destination.resolve(path.toString());
        Files.createDirectories(outputPath.getParent());
        InputStream inputStream = getFileFromResourceAsStream(classLoader, path.toString());
        Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Loads a file from the classpath as an InputStream.
     *
     * @throws IllegalArgumentException if the file is not found
     */
    static InputStream getFileFromResourceAsStream(ClassLoader classLoader, String fileName) {
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found " + fileName);
        } else {
            return inputStream;
        }
    }
}
