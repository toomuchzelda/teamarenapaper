package me.toomuchzelda.teamarenapaper.utils;

import net.kyori.adventure.text.Component;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * A hologram that's a real armor stand that exists in a world, not packets only
 * easier than refactoring Hologram.java lel
 */
public class RealHologram {

    //public static final HashMap<Integer, RealHologram> allHolograms = new HashMap<>();

    public ArrayList<HologramLine> lines;
    public Location baseLoc;

    public RealHologram(Location location, Component... text) {
        lines = new ArrayList<>();
        baseLoc = location.clone();

        for(int i = 0; i < text.length; i++) {
            Location lineLoc = location.clone();
            double decreaseHeight = (double) i * -0.33;
            lineLoc.setY(lineLoc.getY() + decreaseHeight);

            lines.add(new HologramLine(text[i], lineLoc));
        }
    }

    public void setText(Component... newText) {
        int max = Math.max(newText.length, lines.size());

        for(int i = 0; i < max; i++) {
            //replace the HologramLine text
            if(i < newText.length && i < lines.size()) {
                lines.get(i).setText(newText[i]);
            }
            //more existing lines than we now want, so remove existing line
            else if(i >= newText.length && i < lines.size()) {
                lines.get(i).kill();
                //don't remove just yet to not interrupt the for loop?
                lines.set(i, null);
            }
            //we need to add more lines
            else if(i >= lines.size() && i < newText.length){
                lines.add(new HologramLine(newText[i], baseLoc.clone().add(0, i * -0.33, 0)));
            }
        }

        //clean up
        Iterator<HologramLine> iter = lines.iterator();
        while(iter.hasNext()) {
            if(iter.next() == null)
                iter.remove();
        }
    }

    public void remove() {
        ListIterator<HologramLine> iter = lines.listIterator();
        while(iter.hasNext()) {
            iter.next().bukkitStand.remove();
            iter.remove();
        }
    }
    
    public int getAge() {
        return lines.get(0).bukkitStand.getTicksLived();
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
            nmsWorld.addFreshEntity(nmsStand, CreatureSpawnEvent.SpawnReason.CUSTOM);
        }

        public void setText(Component text) {
            bukkitStand.customName(text);
        }

        public void kill() {
            bukkitStand.remove();
        }
    }
}
