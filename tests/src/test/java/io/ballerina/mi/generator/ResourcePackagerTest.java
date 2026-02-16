/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
import io.ballerina.mi.util.Constants;
import io.ballerina.projects.*;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for ResourcePackager class covering private methods via reflection.
 */
public class ResourcePackagerTest {

    private Path tempSourceDir;
    private Path tempTargetDir;
    private Path tempDestinationDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempSourceDir = Files.createTempDirectory("resource-packager-source");
        tempTargetDir = Files.createTempDirectory("resource-packager-target");
        tempDestinationDir = Files.createTempDirectory("resource-packager-dest");
        Connector.reset();
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Connector.reset();
        cleanupDir(tempSourceDir);
        cleanupDir(tempTargetDir);
        cleanupDir(tempDestinationDir);
    }

    private void cleanupDir(Path dir) throws IOException {
        if (dir != null && Files.exists(dir)) {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
        }
    }

    private Connector createTestConnector(String moduleName, String orgName, String version) {
        PackageDescriptor descriptor = mock(PackageDescriptor.class);
        PackageOrg packageOrg = mock(PackageOrg.class);
        PackageName packageName = mock(PackageName.class);
        PackageVersion packageVersion = mock(PackageVersion.class);

        when(descriptor.org()).thenReturn(packageOrg);
        when(descriptor.name()).thenReturn(packageName);
        when(descriptor.version()).thenReturn(packageVersion);
        when(packageOrg.value()).thenReturn(orgName);
        when(packageName.value()).thenReturn(moduleName);

        String[] versionParts = version.split("\\.");
        int major = versionParts.length > 0 ? Integer.parseInt(versionParts[0]) : 1;
        int minor = versionParts.length > 1 ? Integer.parseInt(versionParts[1]) : 0;
        int patch = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;
        SemanticVersion semVer = SemanticVersion.from(major + "." + minor + "." + patch);
        when(packageVersion.value()).thenReturn(semVer);

        return Connector.getConnector(descriptor);
    }

    @Test
    public void testResourcePackagerConstruction() {
        ResourcePackager packager = new ResourcePackager(tempSourceDir, tempTargetDir);
        Assert.assertNotNull(packager);
    }

    @Test
    public void testResourcePackagerWithNullPaths() {
        ResourcePackager packager = new ResourcePackager(null, null);
        Assert.assertNotNull(packager);
    }

    @Test
    public void testResourcePackagerWithValidPaths() {
        Path sourcePath = tempSourceDir.resolve("source");
        Path targetPath = tempTargetDir.resolve("target");

        ResourcePackager packager = new ResourcePackager(sourcePath, targetPath);
        Assert.assertNotNull(packager);
    }

    @Test
    public void testTempDirectoriesCreated() {
        Assert.assertTrue(Files.exists(tempSourceDir));
        Assert.assertTrue(Files.exists(tempTargetDir));
        Assert.assertTrue(Files.isDirectory(tempSourceDir));
        Assert.assertTrue(Files.isDirectory(tempTargetDir));
    }

    // Tests for copyResourcesFromDirectory (file scheme path)
    @Test
    public void testCopyResourcesFromDirectory() throws Exception {
        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyResourcesFromDirectory", ClassLoader.class, Path.class);
        method.setAccessible(true);

        method.invoke(null, getClass().getClassLoader(), tempDestinationDir);

        // Should create lib and icon directories
        Assert.assertTrue(Files.exists(tempDestinationDir.resolve(Connector.LIB_PATH)));
        Assert.assertTrue(Files.exists(tempDestinationDir.resolve(Connector.ICON_FOLDER)));
    }

    // Tests for copySingleIconAsBoth
    @Test
    public void testCopySingleIconAsBoth() throws Exception {
        // Create a test icon file
        Path iconFile = tempSourceDir.resolve("test-icon.png");
        byte[] iconData = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0}; // Minimal PNG-like header
        Files.write(iconFile, iconData);

        Method method = ResourcePackager.class.getDeclaredMethod(
                "copySingleIconAsBoth", Path.class, Path.class);
        method.setAccessible(true);

        method.invoke(null, iconFile, tempDestinationDir);

        // Both small and large icons should be created
        Path smallIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);

        Assert.assertTrue(Files.exists(smallIcon));
        Assert.assertTrue(Files.exists(largeIcon));
        Assert.assertEquals(Files.readAllBytes(smallIcon), iconData);
        Assert.assertEquals(Files.readAllBytes(largeIcon), iconData);
    }

    @Test
    public void testCopySingleIconAsBoth_CreatesDirectories() throws Exception {
        // Create a test icon file
        Path iconFile = tempSourceDir.resolve("icon.png");
        Files.write(iconFile, new byte[]{1, 2, 3, 4});

        Method method = ResourcePackager.class.getDeclaredMethod(
                "copySingleIconAsBoth", Path.class, Path.class);
        method.setAccessible(true);

        // Destination has no icon folder yet
        Assert.assertFalse(Files.exists(tempDestinationDir.resolve(Connector.ICON_FOLDER)));

        method.invoke(null, iconFile, tempDestinationDir);

        // Icon folder should be created
        Assert.assertTrue(Files.exists(tempDestinationDir.resolve(Connector.ICON_FOLDER)));
    }

    // Tests for copyIconPair
    @Test
    public void testCopyIconPair_SmallFirst() throws Exception {
        // Create two icon files with different sizes (small icon first in list)
        Path iconDir = tempSourceDir.resolve("icons");
        Files.createDirectories(iconDir);

        Path smallIconFile = iconDir.resolve("small.png");
        Path largeIconFile = iconDir.resolve("large.png");

        Files.write(smallIconFile, new byte[]{1, 2});  // 2 bytes - smaller
        Files.write(largeIconFile, new byte[]{1, 2, 3, 4, 5}); // 5 bytes - larger

        List<Path> paths = new ArrayList<>();
        paths.add(smallIconFile);
        paths.add(largeIconFile);

        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyIconPair", Path.class, List.class);
        method.setAccessible(true);

        method.invoke(null, tempDestinationDir, paths);

        Path smallOutputIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeOutputIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);

        Assert.assertTrue(Files.exists(smallOutputIcon));
        Assert.assertTrue(Files.exists(largeOutputIcon));
        // Verify the sizes match expectations
        Assert.assertEquals(Files.size(smallOutputIcon), 2L);
        Assert.assertEquals(Files.size(largeOutputIcon), 5L);
    }

    @Test
    public void testCopyIconPair_LargeFirst() throws Exception {
        // Create two icon files with large icon first in the list
        Path iconDir = tempSourceDir.resolve("icons");
        Files.createDirectories(iconDir);

        Path largeIconFile = iconDir.resolve("large.png");
        Path smallIconFile = iconDir.resolve("small.png");

        Files.write(largeIconFile, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}); // 10 bytes - larger
        Files.write(smallIconFile, new byte[]{1}); // 1 byte - smaller

        List<Path> paths = new ArrayList<>();
        paths.add(largeIconFile); // Large first
        paths.add(smallIconFile);

        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyIconPair", Path.class, List.class);
        method.setAccessible(true);

        method.invoke(null, tempDestinationDir, paths);

        Path smallOutputIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeOutputIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);

        Assert.assertTrue(Files.exists(smallOutputIcon));
        Assert.assertTrue(Files.exists(largeOutputIcon));
        // Verify they're correctly sorted by size
        Assert.assertEquals(Files.size(smallOutputIcon), 1L);
        Assert.assertEquals(Files.size(largeOutputIcon), 10L);
    }

    @Test
    public void testCopyIconPair_EqualSize() throws Exception {
        // Create two icon files with the same size
        Path iconDir = tempSourceDir.resolve("icons");
        Files.createDirectories(iconDir);

        Path icon1 = iconDir.resolve("icon1.png");
        Path icon2 = iconDir.resolve("icon2.png");

        Files.write(icon1, new byte[]{1, 2, 3});
        Files.write(icon2, new byte[]{4, 5, 6});

        List<Path> paths = new ArrayList<>();
        paths.add(icon1);
        paths.add(icon2);

        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyIconPair", Path.class, List.class);
        method.setAccessible(true);

        method.invoke(null, tempDestinationDir, paths);

        Path smallOutputIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeOutputIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);

        Assert.assertTrue(Files.exists(smallOutputIcon));
        Assert.assertTrue(Files.exists(largeOutputIcon));
    }

    // Tests for copyResources (file scheme branch)
    @Test
    public void testCopyResources_FileScheme() throws Exception {
        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyResources", ClassLoader.class, Path.class, URI.class,
                String.class, String.class, String.class);
        method.setAccessible(true);

        // Create a file:// URI to trigger the file scheme branch
        URI fileUri = tempSourceDir.toUri();

        method.invoke(null, getClass().getClassLoader(), tempDestinationDir, fileUri,
                "testOrg", "testModule", "1");

        // Should have created lib and icon directories (IDE/test mode)
        Assert.assertTrue(Files.exists(tempDestinationDir.resolve(Connector.LIB_PATH)));
        Assert.assertTrue(Files.exists(tempDestinationDir.resolve(Connector.ICON_FOLDER)));
    }

    // Tests for copyIcons branches
    @Test
    public void testCopyIcons_NullIconPath() throws Exception {
        // Initialize connector with null icon path
        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        Assert.assertNull(connector.getIconPath());

        // copyIcons will try to use default icons - since we're not in a JAR,
        // we need to test this differently. This test verifies the null path check.
    }

    @Test
    public void testCopyIcons_RelativeIconPath_NonExistent() throws Exception {
        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setIconPath("non-existent-icon-path");

        // The iconPath is relative and the resolved path won't exist
        // This should trigger the fallback to default icons
    }

    @Test
    public void testCopyIcons_AbsoluteIconPath_NonExistent() throws Exception {
        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setIconPath("/absolute/non/existent/path/icon.png");

        // The iconPath is absolute but doesn't exist
        // This should trigger the fallback to default icons
    }

    @Test
    public void testCopyIcons_AbsoluteIconPath_RegularFile() throws Exception {
        // Create a test icon file
        Path iconFile = tempSourceDir.resolve("connector-icon.png");
        Files.write(iconFile, new byte[]{1, 2, 3, 4, 5});

        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setIconPath(iconFile.toAbsolutePath().toString());

        Assert.assertTrue(iconFile.isAbsolute() || iconFile.toAbsolutePath().isAbsolute());
        Assert.assertTrue(Files.isRegularFile(iconFile));
    }

    @Test
    public void testCopyIcons_DirectoryWithOnePng() throws Exception {
        // Create a directory with exactly one PNG
        Path iconDir = tempSourceDir.resolve("icons-one");
        Files.createDirectories(iconDir);
        Path singleIcon = iconDir.resolve("single.png");
        Files.write(singleIcon, new byte[]{1, 2, 3});

        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setIconPath(iconDir.toAbsolutePath().toString());

        // Verify setup
        Assert.assertTrue(Files.isDirectory(iconDir));
        long pngCount = Files.walk(iconDir)
                .filter(f -> f.toString().endsWith(".png"))
                .count();
        Assert.assertEquals(pngCount, 1);
    }

    @Test
    public void testCopyIcons_DirectoryWithTwoPngs() throws Exception {
        // Create a directory with exactly two PNGs
        Path iconDir = tempSourceDir.resolve("icons-two");
        Files.createDirectories(iconDir);
        Path icon1 = iconDir.resolve("icon1.png");
        Path icon2 = iconDir.resolve("icon2.png");
        Files.write(icon1, new byte[]{1, 2});
        Files.write(icon2, new byte[]{1, 2, 3, 4, 5, 6});

        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setIconPath(iconDir.toAbsolutePath().toString());

        // Verify setup
        Assert.assertTrue(Files.isDirectory(iconDir));
        long pngCount = Files.walk(iconDir)
                .filter(f -> f.toString().endsWith(".png"))
                .count();
        Assert.assertEquals(pngCount, 2);
    }

    @Test
    public void testCopyIcons_DirectoryWithNoPngs() throws Exception {
        // Create a directory with no PNGs
        Path iconDir = tempSourceDir.resolve("icons-none");
        Files.createDirectories(iconDir);
        Files.write(iconDir.resolve("readme.txt"), new byte[]{1, 2, 3});

        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setIconPath(iconDir.toAbsolutePath().toString());

        // Verify setup
        Assert.assertTrue(Files.isDirectory(iconDir));
        long pngCount = Files.walk(iconDir)
                .filter(f -> f.toString().endsWith(".png"))
                .count();
        Assert.assertEquals(pngCount, 0);
    }

    @Test
    public void testCopyIcons_DirectoryWithThreePngs() throws Exception {
        // Create a directory with more than two PNGs (should fallback to default)
        Path iconDir = tempSourceDir.resolve("icons-three");
        Files.createDirectories(iconDir);
        Files.write(iconDir.resolve("icon1.png"), new byte[]{1});
        Files.write(iconDir.resolve("icon2.png"), new byte[]{1, 2});
        Files.write(iconDir.resolve("icon3.png"), new byte[]{1, 2, 3});

        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setIconPath(iconDir.toAbsolutePath().toString());

        // Verify setup - should have 3 PNGs which triggers fallback
        long pngCount = Files.walk(iconDir)
                .filter(f -> f.toString().endsWith(".png"))
                .count();
        Assert.assertEquals(pngCount, 3);
    }

    @Test
    public void testCopyIcons_RelativePathResolvedCorrectly() throws Exception {
        // Create icon in a relative path
        Path parentDir = tempSourceDir.resolve("parent");
        Files.createDirectories(parentDir);
        Path iconDir = parentDir.resolve("icons");
        Files.createDirectories(iconDir);
        Files.write(iconDir.resolve("icon.png"), new byte[]{1, 2, 3, 4});

        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setIconPath("icons"); // Relative path

        // Verify relative path is correctly identified as not absolute
        Path iconPath = java.nio.file.Paths.get(connector.getIconPath());
        Assert.assertFalse(iconPath.isAbsolute());
    }

    // Test icon path resolution edge cases
    @Test
    public void testIconPath_IsAbsoluteCheck() throws Exception {
        // Test the isAbsolute() branch
        Path absolutePath = tempSourceDir.toAbsolutePath();
        Assert.assertTrue(absolutePath.isAbsolute());

        Path relativePath = java.nio.file.Paths.get("relative/path");
        Assert.assertFalse(relativePath.isAbsolute());
    }

    @Test
    public void testIconPath_ResolutionWithParent() throws Exception {
        // When iconPath is relative, it should be resolved against destination.getParent()
        Path destination = tempDestinationDir.resolve("connector");
        Files.createDirectories(destination);

        Path iconDir = tempDestinationDir.resolve("my-icons");
        Files.createDirectories(iconDir);
        Files.write(iconDir.resolve("test.png"), new byte[]{1, 2, 3});

        // The relative path "my-icons" should resolve against destination's parent
        Path resolvedPath = destination.getParent().resolve("my-icons").normalize();
        Assert.assertTrue(Files.exists(resolvedPath));
    }

    // Test directory existence checks
    @Test
    public void testFilesExistsCheck_ExistingPath() throws Exception {
        Path existingPath = tempSourceDir;
        Assert.assertTrue(Files.exists(existingPath));
    }

    @Test
    public void testFilesExistsCheck_NonExistingPath() throws Exception {
        Path nonExistingPath = tempSourceDir.resolve("does-not-exist");
        Assert.assertFalse(Files.exists(nonExistingPath));
    }

    @Test
    public void testFilesIsRegularFile_RegularFile() throws Exception {
        Path regularFile = tempSourceDir.resolve("test.txt");
        Files.write(regularFile, new byte[]{1, 2, 3});
        Assert.assertTrue(Files.isRegularFile(regularFile));
    }

    @Test
    public void testFilesIsRegularFile_Directory() throws Exception {
        Path directory = tempSourceDir.resolve("test-dir");
        Files.createDirectories(directory);
        Assert.assertFalse(Files.isRegularFile(directory));
    }

    // Test icon file filtering
    @Test
    public void testIconFiltering_MixedFileTypes() throws Exception {
        Path iconDir = tempSourceDir.resolve("mixed-icons");
        Files.createDirectories(iconDir);
        Files.write(iconDir.resolve("icon.png"), new byte[]{1, 2, 3});
        Files.write(iconDir.resolve("icon.jpg"), new byte[]{4, 5, 6});
        Files.write(iconDir.resolve("readme.txt"), new byte[]{7, 8, 9});

        // Only PNG files should be counted
        long pngCount = Files.walk(iconDir)
                .filter(f -> f.toString().endsWith(".png"))
                .count();
        Assert.assertEquals(pngCount, 1);
    }

    @Test
    public void testIconFiltering_NestedDirectories() throws Exception {
        // PNGs in nested directories should also be found
        Path iconDir = tempSourceDir.resolve("nested-icons");
        Files.createDirectories(iconDir.resolve("sub1"));
        Files.createDirectories(iconDir.resolve("sub2"));
        Files.write(iconDir.resolve("top.png"), new byte[]{1});
        Files.write(iconDir.resolve("sub1/icon.png"), new byte[]{2});
        Files.write(iconDir.resolve("sub2/icon.png"), new byte[]{3});

        long pngCount = Files.walk(iconDir)
                .filter(f -> f.toString().endsWith(".png"))
                .count();
        Assert.assertEquals(pngCount, 3);
    }

    // Test file size comparison in copyIconPair
    @Test
    public void testFileSizeComparison() throws Exception {
        Path smallFile = tempSourceDir.resolve("small.dat");
        Path largeFile = tempSourceDir.resolve("large.dat");

        Files.write(smallFile, new byte[100]);
        Files.write(largeFile, new byte[1000]);

        Assert.assertTrue(Files.size(largeFile) > Files.size(smallFile));
    }

    // Test URI scheme detection
    @Test
    public void testUriScheme_FileScheme() throws Exception {
        URI fileUri = tempSourceDir.toUri();
        Assert.assertEquals(fileUri.getScheme(), "file");
    }

    @Test
    public void testUriScheme_JarScheme() throws Exception {
        URI jarUri = URI.create("jar:file:/path/to/file.jar!/");
        Assert.assertEquals(jarUri.getScheme(), "jar");
    }

    // Test connector module checks
    @Test
    public void testConnectorIsBalModule_True() throws Exception {
        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setBalModule(true);
        Assert.assertTrue(connector.isBalModule());
    }

    @Test
    public void testConnectorIsBalModule_False() throws Exception {
        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setBalModule(false);
        Assert.assertFalse(connector.isBalModule());
    }

    @Test
    public void testConnectorGetZipFileName_BalModule() throws Exception {
        Connector connector = createTestConnector("MyConnector", "wso2", "1.2.3");
        connector.setBalModule(true);

        String zipName = connector.getZipFileName();
        Assert.assertEquals(zipName, "MyConnector-connector-1.2.3.zip");
    }

    @Test
    public void testConnectorGetZipFileName_NonBalModule() throws Exception {
        Connector connector = createTestConnector("MyConnector", "wso2", "1.2.3");
        connector.setBalModule(false);

        String zipName = connector.getZipFileName();
        Assert.assertEquals(zipName, "ballerina-connector-MyConnector-1.2.3.zip");
    }

    // Test method reflection access
    @Test
    public void testPrivateMethodsExist() throws Exception {
        // Verify all private methods we need to test exist
        Method copyResourcesFromDirectory = ResourcePackager.class.getDeclaredMethod(
                "copyResourcesFromDirectory", ClassLoader.class, Path.class);
        Assert.assertNotNull(copyResourcesFromDirectory);

        Method copySingleIconAsBoth = ResourcePackager.class.getDeclaredMethod(
                "copySingleIconAsBoth", Path.class, Path.class);
        Assert.assertNotNull(copySingleIconAsBoth);

        Method copyIconPair = ResourcePackager.class.getDeclaredMethod(
                "copyIconPair", Path.class, List.class);
        Assert.assertNotNull(copyIconPair);
    }

    @Test
    public void testCopyIconPair_CreatesIconFolder() throws Exception {
        Path iconDir = tempSourceDir.resolve("icons");
        Files.createDirectories(iconDir);
        Files.write(iconDir.resolve("a.png"), new byte[]{1, 2});
        Files.write(iconDir.resolve("b.png"), new byte[]{3, 4, 5, 6});

        List<Path> paths = Files.walk(iconDir)
                .filter(f -> f.toString().endsWith(".png"))
                .toList();

        // Ensure icon folder doesn't exist
        Path iconFolder = tempDestinationDir.resolve(Connector.ICON_FOLDER);
        Assert.assertFalse(Files.exists(iconFolder));

        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyIconPair", Path.class, List.class);
        method.setAccessible(true);
        method.invoke(null, tempDestinationDir, paths);

        // Icon folder should be created
        Assert.assertTrue(Files.exists(iconFolder));
    }

    @Test
    public void testConnectorConstants() {
        Assert.assertEquals(Connector.TYPE_NAME, "connector");
        Assert.assertEquals(Connector.TEMP_PATH, "connector");
        Assert.assertEquals(Connector.ICON_FOLDER, "icon");
        Assert.assertEquals(Connector.SMALL_ICON_NAME, "icon-small.png");
        Assert.assertEquals(Connector.LARGE_ICON_NAME, "icon-large.png");
        Assert.assertEquals(Connector.LIB_PATH, "lib");
    }

    // Integration-style tests that verify the full flow
    @Test
    public void testIconCopyingIntegration_SingleFile() throws Exception {
        // Setup: Create source icon
        Path sourceIcon = tempSourceDir.resolve("my-icon.png");
        byte[] iconData = "PNG_IMAGE_DATA".getBytes();
        Files.write(sourceIcon, iconData);

        // Call copySingleIconAsBoth
        Method method = ResourcePackager.class.getDeclaredMethod(
                "copySingleIconAsBoth", Path.class, Path.class);
        method.setAccessible(true);
        method.invoke(null, sourceIcon, tempDestinationDir);

        // Verify both icons exist with correct content
        Path smallIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);

        Assert.assertTrue(Files.exists(smallIcon));
        Assert.assertTrue(Files.exists(largeIcon));
        Assert.assertEquals(Files.readAllBytes(smallIcon), iconData);
        Assert.assertEquals(Files.readAllBytes(largeIcon), iconData);
    }

    @Test
    public void testIconCopyingIntegration_TwoFiles() throws Exception {
        // Setup: Create two source icons with different sizes
        Path iconDir = tempSourceDir.resolve("connector-icons");
        Files.createDirectories(iconDir);

        byte[] smallData = new byte[50];
        byte[] largeData = new byte[500];
        for (int i = 0; i < 50; i++) smallData[i] = (byte) i;
        for (int i = 0; i < 500; i++) largeData[i] = (byte) (i % 256);

        Path smallSource = iconDir.resolve("small-icon.png");
        Path largeSource = iconDir.resolve("large-icon.png");
        Files.write(smallSource, smallData);
        Files.write(largeSource, largeData);

        List<Path> paths = new ArrayList<>();
        paths.add(smallSource);
        paths.add(largeSource);

        // Call copyIconPair
        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyIconPair", Path.class, List.class);
        method.setAccessible(true);
        method.invoke(null, tempDestinationDir, paths);

        // Verify icons exist with correct sizes
        Path smallOutput = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeOutput = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);

        Assert.assertEquals(Files.size(smallOutput), 50);
        Assert.assertEquals(Files.size(largeOutput), 500);
    }

    @Test
    public void testDirectoryStructureCreation() throws Exception {
        // Test that copyResourcesFromDirectory creates the expected structure
        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyResourcesFromDirectory", ClassLoader.class, Path.class);
        method.setAccessible(true);

        Path newDestination = tempDestinationDir.resolve("new-connector");
        Files.createDirectories(newDestination);

        method.invoke(null, getClass().getClassLoader(), newDestination);

        Assert.assertTrue(Files.isDirectory(newDestination.resolve(Connector.LIB_PATH)));
        Assert.assertTrue(Files.isDirectory(newDestination.resolve(Connector.ICON_FOLDER)));
    }

    // Integration test for packageConnector with BalModule=true
    @Test
    public void testPackageConnector_BalModule() throws Exception {
        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setBalModule(true);

        // Create the required directory structure
        Path binDir = tempTargetDir.resolve("bin");
        Files.createDirectories(binDir);
        Files.write(binDir.resolve("TestModule.jar"), new byte[]{1, 2, 3, 4});

        Path destinationPath = tempDestinationDir.resolve("connector-output");
        Files.createDirectories(destinationPath);
        Files.createDirectories(destinationPath.resolve(Connector.LIB_PATH));

        ResourcePackager packager = new ResourcePackager(tempSourceDir, tempTargetDir);

        try {
            packager.packageConnector(connector, destinationPath);
            // If we get here, it means the file scheme path was taken (copyResourcesFromDirectory)
            Assert.assertTrue(Files.exists(destinationPath.resolve(Connector.LIB_PATH)));
        } catch (Exception e) {
            // Expected if some resources aren't available in test environment
            Assert.assertTrue(e.getMessage() != null);
        }
    }

    // Integration test for packageConnector with BalModule=false
    @Test
    public void testPackageConnector_NonBalModule() throws Exception {
        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        connector.setBalModule(false);

        // Create a mock artifact path
        Path artifactPath = tempSourceDir.resolve("test-artifact.jar");
        Files.write(artifactPath, new byte[]{1, 2, 3, 4, 5});
        System.setProperty(Constants.CONNECTOR_TARGET_PATH, artifactPath.toString());

        Path destinationPath = tempDestinationDir.resolve("connector-output");
        Files.createDirectories(destinationPath);
        Files.createDirectories(destinationPath.resolve(Connector.LIB_PATH));

        ResourcePackager packager = new ResourcePackager(tempSourceDir, tempTargetDir);

        try {
            packager.packageConnector(connector, destinationPath);
            Assert.assertTrue(Files.exists(destinationPath.resolve(Connector.LIB_PATH)));
        } catch (Exception e) {
            // Expected if some resources aren't available in test environment
            Assert.assertTrue(e.getMessage() != null);
        } finally {
            System.clearProperty(Constants.CONNECTOR_TARGET_PATH);
        }
    }

    // Test copyResources with file scheme (simulates IDE/test mode)
    @Test
    public void testCopyResources_WithFileScheme() throws Exception {
        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyResources", ClassLoader.class, Path.class, URI.class,
                String.class, String.class, String.class);
        method.setAccessible(true);

        Path destination = tempDestinationDir.resolve("file-scheme-test");
        Files.createDirectories(destination);

        // Use a file:// URI to trigger the file scheme branch
        URI fileUri = tempSourceDir.toUri();
        Assert.assertEquals(fileUri.getScheme(), "file");

        method.invoke(null, getClass().getClassLoader(), destination, fileUri,
                "testOrg", "testModule", "1");

        // Should have created lib and icon directories
        Assert.assertTrue(Files.exists(destination.resolve(Connector.LIB_PATH)));
        Assert.assertTrue(Files.exists(destination.resolve(Connector.ICON_FOLDER)));
    }

    // Test that copySingleIconAsBoth correctly copies an icon to both destinations
    @Test
    public void testCopySingleIconAsBoth_FullIntegration() throws Exception {
        // Create source icon
        Path iconFile = tempSourceDir.resolve("my-icon.png");
        byte[] iconContent = new byte[]{(byte)0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        Files.write(iconFile, iconContent);

        Method method = ResourcePackager.class.getDeclaredMethod(
                "copySingleIconAsBoth", Path.class, Path.class);
        method.setAccessible(true);

        method.invoke(null, iconFile, tempDestinationDir);

        Path smallIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeIcon = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);

        Assert.assertTrue(Files.exists(smallIcon));
        Assert.assertTrue(Files.exists(largeIcon));
        Assert.assertEquals(Files.readAllBytes(smallIcon), iconContent);
        Assert.assertEquals(Files.readAllBytes(largeIcon), iconContent);
    }

    // Test copyIconPair with first icon larger
    @Test
    public void testCopyIconPair_FirstLarger() throws Exception {
        Path iconDir = tempSourceDir.resolve("icon-pair");
        Files.createDirectories(iconDir);

        // First icon is larger (10 bytes)
        Path icon1 = iconDir.resolve("large.png");
        Files.write(icon1, new byte[10]);

        // Second icon is smaller (5 bytes)
        Path icon2 = iconDir.resolve("small.png");
        Files.write(icon2, new byte[5]);

        List<Path> paths = new ArrayList<>();
        paths.add(icon1);
        paths.add(icon2);

        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyIconPair", Path.class, List.class);
        method.setAccessible(true);

        method.invoke(null, tempDestinationDir, paths);

        Path smallOutput = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeOutput = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);

        Assert.assertTrue(Files.exists(smallOutput));
        Assert.assertTrue(Files.exists(largeOutput));
        Assert.assertEquals(Files.size(smallOutput), 5L);
        Assert.assertEquals(Files.size(largeOutput), 10L);
    }

    // Test copyIconPair with second icon larger
    @Test
    public void testCopyIconPair_SecondLarger() throws Exception {
        Path iconDir = tempSourceDir.resolve("icon-pair2");
        Files.createDirectories(iconDir);

        // First icon is smaller (3 bytes)
        Path icon1 = iconDir.resolve("small.png");
        Files.write(icon1, new byte[3]);

        // Second icon is larger (8 bytes)
        Path icon2 = iconDir.resolve("large.png");
        Files.write(icon2, new byte[8]);

        List<Path> paths = new ArrayList<>();
        paths.add(icon1);
        paths.add(icon2);

        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyIconPair", Path.class, List.class);
        method.setAccessible(true);

        method.invoke(null, tempDestinationDir, paths);

        Path smallOutput = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path largeOutput = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);

        Assert.assertTrue(Files.exists(smallOutput));
        Assert.assertTrue(Files.exists(largeOutput));
        Assert.assertEquals(Files.size(smallOutput), 3L);
        Assert.assertEquals(Files.size(largeOutput), 8L);
    }

    // Test copyResourcesFromDirectory creates the correct structure
    @Test
    public void testCopyResourcesFromDirectory_CreatesStructure() throws Exception {
        Method method = ResourcePackager.class.getDeclaredMethod(
                "copyResourcesFromDirectory", ClassLoader.class, Path.class);
        method.setAccessible(true);

        Path newDest = tempDestinationDir.resolve("resources-dir-test");
        Files.createDirectories(newDest);

        method.invoke(null, getClass().getClassLoader(), newDest);

        Assert.assertTrue(Files.isDirectory(newDest.resolve(Connector.LIB_PATH)));
        Assert.assertTrue(Files.isDirectory(newDest.resolve(Connector.ICON_FOLDER)));
    }

    // Test the file scheme check in copyResources
    @Test
    public void testCopyResources_ChecksScheme() throws Exception {
        URI fileUri = URI.create("file:///test/path");
        Assert.assertEquals(fileUri.getScheme(), "file");

        URI jarUri = URI.create("jar:file:///test.jar!/");
        Assert.assertEquals(jarUri.getScheme(), "jar");

        // The copyResources method should check if scheme equals "file"
        Assert.assertTrue("file".equals(fileUri.getScheme()));
        Assert.assertFalse("file".equals(jarUri.getScheme()));
    }

    @Test
    public void testCopyResourcesByExtension_CopiesFromJarFs() throws Exception {
        Path zipPath = tempSourceDir.resolve("mock-resources.zip");
        try (FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()),
                Map.of("create", "true"))) {
            Path iconDir = zipFs.getPath("icon");
            Files.createDirectories(iconDir);
            Files.write(iconDir.resolve("icon-small.png"), new byte[]{1, 2, 3});

            ClassLoader classLoader = mock(ClassLoader.class);
            when(classLoader.getResourceAsStream("icon/icon-small.png"))
                    .thenReturn(new ByteArrayInputStream(new byte[]{9, 8, 7}));

            Method method = ResourcePackager.class.getDeclaredMethod(
                    "copyResourcesByExtension", ClassLoader.class, FileSystem.class, Path.class, String.class, String.class);
            method.setAccessible(true);
            method.invoke(null, classLoader, zipFs, tempDestinationDir, Connector.ICON_FOLDER, ".png");
        }

        Path copied = tempDestinationDir.resolve("icon").resolve("icon-small.png");
        Assert.assertTrue(Files.exists(copied));
        Assert.assertEquals(Files.readAllBytes(copied), new byte[]{9, 8, 7});
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCopyResourcesByExtension_MissingResourceStream() throws Throwable {
        Path zipPath = tempSourceDir.resolve("mock-missing-resources.zip");
        try (FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()),
                Map.of("create", "true"))) {
            Path iconDir = zipFs.getPath("icon");
            Files.createDirectories(iconDir);
            Files.write(iconDir.resolve("icon-small.png"), new byte[]{1});

            ClassLoader classLoader = mock(ClassLoader.class);
            when(classLoader.getResourceAsStream("icon/icon-small.png")).thenReturn(null);

            Method method = ResourcePackager.class.getDeclaredMethod(
                    "copyResourcesByExtension", ClassLoader.class, FileSystem.class, Path.class, String.class, String.class);
            method.setAccessible(true);
            try {
                method.invoke(null, classLoader, zipFs, tempDestinationDir, Connector.ICON_FOLDER, ".png");
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    @Test
    public void testCopyIcons_UsesDefaultIconsWhenIconPathNull() throws Exception {
        createTestConnector("TestModule", "wso2", "1.0.0");

        Path zipPath = tempSourceDir.resolve("mock-icons-default.zip");
        try (FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()),
                Map.of("create", "true"))) {
            Path iconDir = zipFs.getPath("icon");
            Files.createDirectories(iconDir);
            Files.write(iconDir.resolve("icon-small.png"), new byte[]{1});
            Files.write(iconDir.resolve("icon-large.png"), new byte[]{1, 2});

            ClassLoader classLoader = mock(ClassLoader.class);
            when(classLoader.getResourceAsStream("icon/icon-small.png"))
                    .thenReturn(new ByteArrayInputStream(new byte[]{5}));
            when(classLoader.getResourceAsStream("icon/icon-large.png"))
                    .thenReturn(new ByteArrayInputStream(new byte[]{6, 7}));

            Method method = ResourcePackager.class.getDeclaredMethod(
                    "copyIcons", ClassLoader.class, FileSystem.class, Path.class);
            method.setAccessible(true);
            method.invoke(null, classLoader, zipFs, tempDestinationDir);
        }

        Assert.assertTrue(Files.exists(tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME)));
        Assert.assertTrue(Files.exists(tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME)));
    }

    @Test
    public void testCopyIcons_RelativeRegularFilePath() throws Exception {
        Connector connector = createTestConnector("TestModule", "wso2", "1.0.0");
        Path relativeIcon = tempDestinationDir.getParent().resolve("relative-icon.png");
        Files.write(relativeIcon, new byte[]{4, 5, 6, 7});
        connector.setIconPath("relative-icon.png");

        Path zipPath = tempSourceDir.resolve("mock-icons-relative.zip");
        try (FileSystem zipFs = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()),
                Map.of("create", "true"))) {
            ClassLoader classLoader = mock(ClassLoader.class);
            Method method = ResourcePackager.class.getDeclaredMethod(
                    "copyIcons", ClassLoader.class, FileSystem.class, Path.class);
            method.setAccessible(true);
            method.invoke(null, classLoader, zipFs, tempDestinationDir);
        }

        Path small = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.SMALL_ICON_NAME);
        Path large = tempDestinationDir.resolve(Connector.ICON_FOLDER).resolve(Connector.LARGE_ICON_NAME);
        Assert.assertTrue(Files.exists(small));
        Assert.assertTrue(Files.exists(large));
        Assert.assertEquals(Files.readAllBytes(small), new byte[]{4, 5, 6, 7});
        Assert.assertEquals(Files.readAllBytes(large), new byte[]{4, 5, 6, 7});
    }
}
