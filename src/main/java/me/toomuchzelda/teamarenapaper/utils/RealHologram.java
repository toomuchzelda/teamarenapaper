package me.toomuchzelda.teamarenapaper.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Objects;

/**
 * A hologram that's a real armor stand(s) that exists in a world, not packets only
 * More convenient than PacketHologram as it manages trackers and such itself
 */
public class RealHologram {

	public static final double NEXT_LINE_VERTICAL_OFFSET = 0.33;

    private final ArrayList<HologramLine> lines;
    private Location baseLoc;
	private Alignment alignment;

    public RealHologram(Location location, Alignment alignment, Component... text) {
        lines = new ArrayList<>();
        baseLoc = location.clone();

		for (Component component : text) {
			lines.add(new HologramLine(component, baseLoc));
		}

		//move into correct positions here
		setVerticalAlignment(alignment);
    }

	public void setVerticalAlignment(Alignment alignment) {
		int i = 0;
		for(HologramLine line : lines) {
			Location newLoc = getLocationForIter(baseLoc, alignment, i);
			line.moveTo(newLoc);
			i++;
		}

		this.alignment = alignment;
	}

	private static Location getLocationForIter(Location loc, Alignment alignment, int i) {
		double offset;
		if(alignment == Alignment.TOP)
			offset = -NEXT_LINE_VERTICAL_OFFSET;
		else
			offset = NEXT_LINE_VERTICAL_OFFSET;

		return loc.clone().add(0, i * offset, 0);
	}

	public void moveTo(Location location) {
		if(!this.baseLoc.equals(location)) {
			int i = 0;

			for (HologramLine line : lines) {
				line.moveTo(getLocationForIter(location, this.alignment, i++));
			}
			this.baseLoc = location;
		}
	}

    public void setText(Component... newText) {
        int max = Math.max(newText.length, lines.size());

        for (int i = 0; i < max; i++) {
            //replace the HologramLine text
            if (i < newText.length && i < lines.size()) {
                lines.get(i).setText(newText[i]);
            }
            //more existing lines than we now want, so remove existing line
            else if (i >= newText.length && i < lines.size()) {
                lines.get(i).kill();
                //don't remove just yet to not interrupt the for loop?
                lines.set(i, null);
            }
            //we need to add more lines
            else if (i < newText.length) {
                lines.add(new HologramLine(newText[i], getLocationForIter(baseLoc, this.alignment, i)));
            }
        }

        //clean up
        lines.removeIf(Objects::isNull);
    }

    public void remove() {
        ListIterator<HologramLine> iter = lines.listIterator();
        while (iter.hasNext()) {
            iter.next().bukkitStand.remove();
            iter.remove();
        }
    }

    public int getAge() {
        return lines.get(0).bukkitStand.getTicksLived();
    }

    private static class HologramLine {
        public ArmorStand bukkitStand;

        public HologramLine(Component text, Location location) {
            bukkitStand = location.getWorld().spawn(location, ArmorStand.class, stand -> {
                stand.setMarker(true);
                stand.setVisible(false);
                stand.setCustomNameVisible(true);
                stand.customName(text);
                stand.setCanTick(false);
            });
        }

        public void setText(Component text) {
            bukkitStand.customName(text);
        }

		public void moveTo(Location location) {
			bukkitStand.teleport(location);
		}

        public void kill() {
            bukkitStand.remove();
        }
    }

	public enum Alignment {
		TOP, BOTTOM
	}
}
