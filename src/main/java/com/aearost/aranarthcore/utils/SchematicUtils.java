package com.aearost.aranarthcore.utils;

import com.sk89q.worldedit.WorldEdit;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Handles the WorldEdit schematic files used by /ac schemtransfer.
 */
public class SchematicUtils {

    /**
     * The largest schematic that can be transferred, so the encoded copy fits in a single MySQL row.
     */
    public static final int MAX_TRANSFER_BYTES = 8 * 1024 * 1024;

    private static final List<String> EXTENSIONS = List.of(".schem", ".schematic");

    /**
     * Provides WorldEdit's schematics folder, or null if WorldEdit is not enabled on this server.
     *
     * @return The schematics folder.
     */
    public static Path getSchematicsFolder() {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
            return null;
        }
        WorldEdit worldEdit = WorldEdit.getInstance();
        return worldEdit.getWorkingDirectoryPath(worldEdit.getConfiguration().saveDir).toAbsolutePath().normalize();
    }

    /**
     * Resolves a path inside the schematics folder, refusing anything that would leave it.
     *
     * @param folder   The schematics folder.
     * @param fileName The schematic's path relative to the folder.
     * @return The resolved path, or null if it is outside the folder.
     */
    public static Path resolveInside(Path folder, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        Path resolved = folder.resolve(fileName).toAbsolutePath().normalize();
        if (!resolved.startsWith(folder) || resolved.equals(folder)) {
            return null;
        }
        return resolved;
    }

    /**
     * Finds the schematic file matching the name, with or without its extension.
     *
     * @param folder The schematics folder.
     * @param name   The name entered by the player.
     * @return The schematic file, or null if there is none.
     */
    public static Path findSchematic(Path folder, String name) {
        List<String> candidates = new ArrayList<>();
        if (hasSchematicExtension(name)) {
            candidates.add(name);
        } else {
            for (String extension : EXTENSIONS) {
                candidates.add(name + extension);
            }
        }
        for (String candidate : candidates) {
            Path path = resolveInside(folder, candidate);
            if (path != null && Files.isRegularFile(path)) {
                return path;
            }
        }
        return null;
    }

    /**
     * Writes a received schematic into the folder, replacing the existing file only when asked to.
     *
     * @param target   The file to write.
     * @param data     The schematic's contents.
     * @param override Whether an existing file may be replaced.
     * @return False if the file already exists and override is false.
     * @throws IOException If the file could not be written.
     */
    public static boolean writeSchematic(Path target, byte[] data, boolean override) throws IOException {
        if (Files.exists(target) && !override) {
            return false;
        }
        Files.createDirectories(target.getParent());
        // Written beside the target first so a failed write never leaves a broken schematic behind
        Path temp = target.resolveSibling(target.getFileName() + ".transfer");
        Files.write(temp, data);
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    /**
     * Provides the names of the schematics in the folder for tab completion, including those in sub-folders.
     *
     * @return The schematic names, without their extension.
     */
    public static List<String> getSchematicNames() {
        Path folder = getSchematicsFolder();
        if (folder == null || !Files.isDirectory(folder)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(folder, 3)) {
            paths.filter(Files::isRegularFile)
                    .map(path -> folder.relativize(path).toString().replace('\\', '/'))
                    .filter(SchematicUtils::hasSchematicExtension)
                    .map(SchematicUtils::stripExtension)
                    .forEach(names::add);
        } catch (IOException ignored) {
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Removes the schematic extension from the name, if it has one.
     *
     * @param name The schematic's name.
     * @return The name without its extension.
     */
    public static String stripExtension(String name) {
        for (String extension : EXTENSIONS) {
            if (name.toLowerCase().endsWith(extension)) {
                return name.substring(0, name.length() - extension.length());
            }
        }
        return name;
    }

    private static boolean hasSchematicExtension(String name) {
        String lower = name.toLowerCase();
        return EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
