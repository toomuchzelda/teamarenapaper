package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.Main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Graffiti {

	public final BufferedImage image;
	public Graffiti(File file) {
		try {
			BufferedImage image = ImageIO.read(file);

			if (image.getWidth() != 128 || image.getHeight() != 128) {
				Main.logger().warning("File " + file.getName() + " is not 128*128, resizing automatically");
				image = resizeImage(image);
			}
			this.image = image;
		} catch (IOException ex) {
			throw new RuntimeException("Failed to read image " + file.getPath(), ex);
		}
	}

	private static BufferedImage resizeImage(BufferedImage original) {
		Image scaled = original.getScaledInstance(128, 128, Image.SCALE_FAST);
		BufferedImage ret = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);

		var graphics = ret.createGraphics();
		graphics.drawImage(scaled, 0, 0, null);
		graphics.dispose();

		return ret;
	}

}
