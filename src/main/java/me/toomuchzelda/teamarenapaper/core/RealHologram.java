package me.toomuchzelda.teamarenapaper.core;

import net.kyori.adventure.text.Component;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A hologram that's a real armor stand that exists in a world, not packets only
 * easier than refactoring Hologram.java lel
 */
public class RealHologram {

    //public static final HashMap<Integer, RealHologram> allHolograms = new HashMap<>();

    public LinkedList<HologramLine> lines;
    public Location baseLoc;

    public RealHologram(Location location, Component... text) {
        lines = new LinkedList<>();
        baseLoc = location.clone();

        for(int i = 0; i < text.length; i++) {
            Location lineLoc = location.clone();
            double decreaseHeight = (double) i * -0.33;
            lineLoc.setY(lineLoc.getY() + decreaseHeight);

            lines.addLast(new HologramLine(text[i], lineLoc));
        }
    }

    public void setText(Component... newText) {
        ListIterator<HologramLine> listIter = lines.listIterator();

        int cap = Math.max(newText.length, lines.size() + 1);

        while(listIter.previousIndex() <= cap) {
            HologramLine line = listIter.next();

            if(listIter.previousIndex() < newText.length) {
                line.setText(newText[listIter.previousIndex()]);
            }
            //delete unnecessary lines
            else if(listIter.previousIndex() >= newText.length) {
                listIter.remove();
            }
            //still more lines to add, expand the list
            else if(newText.length > listIter.previousIndex() && !listIter.hasNext()) {
                //keep adding to the end of the list here
                double decHeight;
                for(int i = listIter.previousIndex(); i < newText.length; i++) {
                    decHeight = i * -0.33;
                    listIter.add(new HologramLine(newText[listIter.previousIndex()], baseLoc.clone().add(0, decHeight, 0)));
                }
            }
        }
    }

    public void remove() {
        ListIterator<HologramLine> iter = lines.listIterator();
        while(iter.hasNext()) {
            iter.next().bukkitStand.remove();
            iter.remove();
        }
    }

    private static class HologramLine
    {
        public ArmorStand bukkitStand;

        public HologramLine(Component text, Location location) {
            //Can't create armorstand before adding to world, so use nms instead
            Level nmsWorld = ((CraftWorld) location.getWorld()).getHandle();
            net.minecraft.world.entity.decoration.ArmorStand nmsStand = new net.minecraft.world.entity.decoration.ArmorStand(
                    nmsWorld, location.getX(), location.getY(), location.getZ());

            /*nmsStand.setMarker(true);
            nmsStand.setInvisible(true);
            nmsStand.setCustomNameVisible(true);*/

            bukkitStand = (ArmorStand) nmsStand.getBukkitEntity();

            bukkitStand.setMarker(true);
            //bukkitStand.setInvisible(true);
            bukkitStand.setVisible(false);
            bukkitStand.setCustomNameVisible(true);
            bukkitStand.customName(text);
            bukkitStand.setCanTick(false);

            //spawn it
            nmsWorld.addEntity(nmsStand, CreatureSpawnEvent.SpawnReason.CUSTOM);
        }

        public void setText(Component text) {
            bukkitStand.customName(text);
        }
    }
}
