package me.toomuchzelda.teamarenapaper.utils;

import me.toomuchzelda.teamarenapaper.Main;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

public class FileUtils {

	public static void init() {

	}

	private static final Set<String> IGNORED_DIRECTORIES = Set.of("poi");
	private static final Set<String> IGNORED_FILES = Set.of("uid.dat");
	public static void copyFolder(File source, File destination) {
		Path sourcePath = source.toPath();
		Path destPath = destination.toPath();
		try {
			Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
						throws IOException {
					if (IGNORED_DIRECTORIES.contains(dir.getFileName().toString())) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					Files.createDirectories(destPath.resolve(sourcePath.relativize(dir)));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
						throws IOException {
					if (!IGNORED_FILES.contains(file.getFileName().toString()))
						Files.copy(file, destPath.resolve(sourcePath.relativize(file)),
								StandardCopyOption.REPLACE_EXISTING);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException ex) {
			Main.logger().severe("Failed to copy \"" + source.getAbsolutePath() + "\" to \"" + destination.getAbsolutePath() + "\"");
			ex.printStackTrace();
		}
	}

	// anonymous classes be like
	private static final SimpleFileVisitor<Path> FILE_DELETE_VISITOR = new SimpleFileVisitor<>() {
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			Files.delete(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			Files.delete(dir);
			return FileVisitResult.CONTINUE;
		}
	};
	public static void delete(File file) {
		Path directory = file.toPath();

		try {
			if (Files.isDirectory(directory)) {
				Files.walkFileTree(directory, FILE_DELETE_VISITOR);
			} else {
				Files.delete(directory);
			}
			Main.logger().info("Deleted file " + file.getAbsolutePath());
		} catch (IOException ex) {
			Main.logger().warning("Failed to delete " + file.getAbsolutePath());
			ex.printStackTrace();
		}
	}

	public record FileInfo(String fileName, @NotNull String fileExtension) {}
	public static FileInfo getFileExtension(String fileName) {
		int idx = fileName.lastIndexOf('.');
		if (idx == -1)
			return new FileInfo(fileName, "");
		return new FileInfo(fileName.substring(0, idx), fileName.substring(idx + 1));
	}
}
