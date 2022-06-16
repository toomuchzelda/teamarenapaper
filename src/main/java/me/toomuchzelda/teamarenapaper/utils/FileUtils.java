package me.toomuchzelda.teamarenapaper.utils;

import me.toomuchzelda.teamarenapaper.Main;

import java.io.*;

public class FileUtils
{
	//from kayz1:
	// https://stackoverflow.com/questions/6214703/copy-entire-directory-contents-to-another-directory

	public static void copyFolder(File source, File destination)
	{
		if (source.isDirectory())
		{
			if (!destination.exists())
			{
				destination.mkdirs();
			}

			String files[] = source.list();

			for (String file : files)
			{
				File srcFile = new File(source, file);
				File destFile = new File(destination, file);

				copyFolder(srcFile, destFile);
			}
		}
		else
		{
			InputStream in = null;
			OutputStream out = null;

			try
			{
				in = new FileInputStream(source);
				out = new FileOutputStream(destination);

				byte[] buffer = new byte[1024];

				int length;
				while ((length = in.read(buffer)) > 0)
				{
					out.write(buffer, 0, length);
				}
			}
			catch (Exception e)
			{
				try
				{
					in.close();
				}
				catch (IOException e1)
				{
					e1.printStackTrace();
				}

				try
				{
					out.close();
				}
				catch (IOException e1)
				{
					e1.printStackTrace();
				}
			}
		}
	}

	public static void delete(File file) {
		deleteRec(file);

		Main.logger().info("Deleted file " + file.getAbsolutePath());
	}

	private static void deleteRec(File file) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				deleteRec(f);
			}
		}

		if(!file.delete()) {
			Main.logger().severe("Failed to delete " + file.getAbsolutePath());
		}
	}
}
