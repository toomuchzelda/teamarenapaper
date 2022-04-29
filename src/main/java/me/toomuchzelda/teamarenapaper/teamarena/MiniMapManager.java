package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class MiniMapManager {
    final TeamArena game;
    final MapView view;
    final Renderer renderer;
    final int mapWidth;
    final int centerX, centerZ;
    final ItemStack stack;

    private record CursorProvider(BiPredicate<Player, PlayerInfo> displayCondition,
                                  BiFunction<Player, PlayerInfo, @NotNull CursorInfo> infoSupplier) { }

    private record ComplexCursorProvider(BiPredicate<Player, PlayerInfo> displayCondition,
                                         @Nullable BiFunction<Player, PlayerInfo, boolean @NotNull []> complexCondition,
                                         BiFunction<Player, PlayerInfo, @NotNull CursorInfo @NotNull[]> infoSupplier) { }

    @ParametersAreNonnullByDefault
    public record CursorInfo(Location location, boolean directional, MapCursor.Type icon, @Nullable Component caption) {
        public CursorInfo(Location location, boolean directional, MapCursor.Type icon) {
            this(location, directional, icon, null);
        }

        MapCursor toMapCursor(Renderer renderer) {
            return new MapCursor(renderer.convertX(location), renderer.convertZ(location),
                    directional ? renderer.convertYaw(location) : (byte) 8, icon, true, caption);
        }
    }


    // cursors
    final List<CursorProvider> cursors = new ArrayList<>();
    final List<ComplexCursorProvider> complexCursors = new ArrayList<>();

    // default cursors
    // only used as an example, since these cursors are rendered on top of other cursors
    private static final BiPredicate<Player, PlayerInfo> ALWAYS_SHOW_CURSOR = (ignored1, ignored2) -> true;
    private static final CursorProvider PLAYER_CURSOR = new CursorProvider(ALWAYS_SHOW_CURSOR,
            (player, ignored) -> new CursorInfo(player.getLocation(), true, MapCursor.Type.WHITE_POINTER));
    private static final ComplexCursorProvider TEAMMATES_CURSOR = new ComplexCursorProvider(ALWAYS_SHOW_CURSOR,
            null, // always show the location of every teammate
            (player, playerInfo) -> {
                if (playerInfo.team == Main.getGame().getSpectatorTeam()) {
                    // spectator view
                    return Main.getGame().getPlayers().stream()
                            .filter(other -> !other.isInvisible())
                            .map(other -> new CursorInfo(other.getLocation(), true, MapCursor.Type.GREEN_POINTER,
                                    Main.getPlayerInfo(other).team.colourWord(other.getName())))
                            .toArray(CursorInfo[]::new);
                } else {
                    // teammates view
                    return playerInfo.team.getPlayerMembers().stream()
                            .filter(teammate -> teammate != player) // don't show viewer
                            .map(teammate -> new CursorInfo(teammate.getLocation(), true, MapCursor.Type.BLUE_POINTER,
                                    player.isSneaking() ? teammate.name() : null)) // display teammate names if sneaking
                            .toArray(CursorInfo[]::new);
                }
            }
    );

    // canvas access
    @FunctionalInterface
    public interface CanvasOperation {
        void render(Player player, PlayerInfo info, MapCanvas canvas, Renderer renderer);
    }

    final List<CanvasOperation> canvasOperations = new ArrayList<>();

    public MiniMapManager(TeamArena game) {
        this.game = game;

        // create map view and adjust center and scale
        view = Bukkit.createMap(game.gameWorld);
        BoundingBox box = game.border;
        centerX = Location.locToBlock(box.getCenterX());
        centerZ = Location.locToBlock(box.getCenterZ());
        view.setCenterX(centerX);
        view.setCenterZ(centerZ);
        double side = Math.max(box.getWidthX(), box.getWidthZ());
        int log = (int) Math.ceil(Math.log(side) / Math.log(2));
        mapWidth = (int) Math.pow(2, log);
        int scale = MathUtils.clamp(0, 4,  log - 7);
        //noinspection deprecation,ConstantConditions
        view.setScale(MapView.Scale.valueOf((byte) scale));
        view.setTrackingPosition(false);

        // our renderer
        renderer = new Renderer();
        view.addRenderer(renderer);

        Main.logger().info("Minimap: Center: (" + centerX + ", " + centerZ + "), Scale: " + view.getScale().name() + ", Map width: " + mapWidth);

        stack = ItemBuilder.of(Material.FILLED_MAP).meta(MapMeta.class, mapMeta -> mapMeta.setMapView(view)).build();
    }

    public void cleanUp() {
        cursors.clear();
        complexCursors.clear();
        view.removeRenderer(renderer);
    }

    public void removeMapView() {
        // TODO
    }

    @NotNull
    public ItemStack getMapItem() {
        return getMapItem(null);
    }

    @NotNull
    public ItemStack getMapItem(TeamArenaTeam team) {
        return ItemBuilder.of(Material.FILLED_MAP)
                .meta(MapMeta.class, mapMeta -> {
                    mapMeta.setMapView(view);
                    if (team != null) {
                        mapMeta.displayName(team.getComponentName().decoration(TextDecoration.ITALIC, false));
                        mapMeta.setColor(team.getColour());
                    } else {
                        mapMeta.displayName(Component.text("Game map", NamedTextColor.WHITE)
                                .decoration(TextDecoration.ITALIC, false));
                    }
                })
                .build();
    }

    public boolean isMapItem(ItemStack stack) {
        return stack != null && stack.getItemMeta() instanceof MapMeta mapMeta && mapMeta.getMapView() == view;
    }

    public void registerCursor(@NotNull BiPredicate<Player, PlayerInfo> displayCondition,
                               @NotNull BiFunction<Player, PlayerInfo, @NotNull CursorInfo> infoSupplier) {
        cursors.add(new CursorProvider(displayCondition, infoSupplier));
    }

    public void registerCursor(@NotNull BiFunction<Player, PlayerInfo, @NotNull CursorInfo> infoSupplier) {
        registerCursor(ALWAYS_SHOW_CURSOR, infoSupplier);
    }

    /**
     *
     * @param displayCondition Whether to continue calculating cursors.
     * @param complexCondition A function to calculate the visibility status of each cursor.
     *                         If null, will always be visible when {@code displayCondition} is true.
     *                         If not null, the returned array must be the same size as the return value of {@code infoSupplier}
     * @param infoSupplier A function to calculate the properties of each cursor.
     */
    @ParametersAreNonnullByDefault
    public void registerCursors(BiPredicate<Player, PlayerInfo> displayCondition,
                                @Nullable BiFunction<Player, PlayerInfo, boolean @NotNull[]> complexCondition,
                                BiFunction<Player, PlayerInfo, @NotNull CursorInfo @NotNull[]> infoSupplier) {
        complexCursors.add(new ComplexCursorProvider(displayCondition, complexCondition, infoSupplier));
    }

    public void registerCanvasOperation(CanvasOperation operation) {
        canvasOperations.add(operation);
    }

    public boolean hasCanvasOperation(CanvasOperation operation) {
        return canvasOperations.contains(operation);
    }

    // Utility

    private static int floor(double num) {
        return NumberConversions.floor(num);
    }

    public final class Renderer extends MapRenderer {
        Renderer() {
            super(true);
        }

        @Override
        public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
            MapCursorCollection mapCursors = new MapCursorCollection();
            PlayerInfo playerInfo = Main.getPlayerInfo(player);

            for (CursorProvider provider : cursors) {
                // no need to add the cursor at all if it's not visible
                if (provider.displayCondition().test(player, playerInfo)) {
                    CursorInfo info = provider.infoSupplier().apply(player, playerInfo);
                    mapCursors.addCursor(info.toMapCursor(this));
                }
            }
            for (ComplexCursorProvider provider : complexCursors) {
                if (provider.displayCondition().test(player, playerInfo)) {
                    boolean[] arr = provider.complexCondition() != null ?
                            provider.complexCondition().apply(player, playerInfo) : null;
                    CursorInfo[] infos = provider.infoSupplier().apply(player, playerInfo);
                    // check indices
                    if (arr != null && arr.length != infos.length)
                        continue;

                    for (int i = 0; i < infos.length; i++) {
                        if (arr != null && !arr[i])
                            continue;

                        mapCursors.addCursor(infos[i].toMapCursor(this));
                    }

                }
            }
            // special player and teammates cursor
            renderSpecialCursors(player, playerInfo, mapCursors);
            canvas.setCursors(mapCursors);

            for (CanvasOperation operation : canvasOperations) {
                operation.render(player, playerInfo, canvas, this);
            }
        }



        // Utility methods

        /**
         * Maps world X coordinates to -128 to 127 on the map
         */
        public byte convertX(Location loc) {
            return convertX(loc.getBlockX());
        }

        /**
         * Maps world X coordinates to -128 to 127 on the map
         */
        public byte convertX(int x) {
            return (byte) ((x - centerX) * 128 / (mapWidth / 2));
        }

        /**
         * Maps world Z coordinates to -128 to 127 on the map
         */
        public byte convertZ(Location loc) {
            return convertZ(loc.getBlockZ());
        }

        /**
         * Maps world Z coordinates to -128 to 127 on the map
         */
        public byte convertZ(int z) {
            return (byte) ((z - centerZ) * 128 / (mapWidth / 2));
        }

        /**
         * Maps yaw to 0 to 15
         */
        public byte convertYaw(Location loc) {
            return convertYaw((int) loc.getYaw());
        }

        /**
         * Maps yaw to 0 to 15
         */
        public byte convertYaw(int yaw) {
            return (byte) (Math.floorMod(yaw, 360) * 16 / 360);
        }

        public static final byte TRANSPARENT = 0;
        public void drawLine(MapCanvas canvas, Vector from, Vector to, byte color) {
            int minX = (convertX(Math.min(from.getBlockX(), to.getBlockX())) + 128) / 2;
            int maxX = (convertX(Math.max(from.getBlockX(), to.getBlockX())) + 128) / 2;
            int minZ = (convertZ(Math.min(from.getBlockZ(), to.getBlockZ())) + 128) / 2;
            int maxZ = (convertZ(Math.max(from.getBlockZ(), to.getBlockZ())) + 128) / 2;
            double slope = (double) (maxZ - minZ) / (maxX - minX);
            for (int i = minX; i <= maxX; i++) {
                canvas.setPixel(i, (int) (minZ + (i - minX) * slope), color);
            }
        }

        public void drawRect(MapCanvas canvas, Vector from, Vector to, byte color, byte borderColor) {
            int minX = (convertX(Math.min(from.getBlockX(), to.getBlockX())) + 128) / 2;
            int maxX = (convertX(Math.max(from.getBlockX(), to.getBlockX())) + 128) / 2;
            int minZ = (convertZ(Math.min(from.getBlockZ(), to.getBlockZ())) + 128) / 2;
            int maxZ = (convertZ(Math.max(from.getBlockZ(), to.getBlockZ())) + 128) / 2;
            for (int i = minX; i <= maxX; i++) {
                for (int j = minZ; j <= maxZ; j++) {
                    byte actualColor = i == minX || i == maxX || j == minZ || j == maxZ ? borderColor : color;
                    if (actualColor == TRANSPARENT)
                        actualColor = canvas.getBasePixel(i, j);
                    canvas.setPixel(i, j, actualColor);
                }
            }
        }

        private void renderSpecialCursors(Player player, PlayerInfo playerInfo, MapCursorCollection collection) {
            CursorInfo[] teammateCursors;
            if (playerInfo.team == Main.getGame().getSpectatorTeam()) {
                // spectator view
                teammateCursors = Main.getGame().getPlayers().stream()
                        .filter(other -> !other.isInvisible())
                        .map(other -> new CursorInfo(other.getLocation(), true, MapCursor.Type.GREEN_POINTER,
                                Main.getPlayerInfo(other).team.colourWord(other.getName())))
                        .toArray(CursorInfo[]::new);
            } else {
                // teammates view
                teammateCursors = playerInfo.team.getPlayerMembers().stream()
                        .filter(teammate -> teammate != player) // don't show viewer
                        .map(teammate -> new CursorInfo(teammate.getLocation(), true, MapCursor.Type.BLUE_POINTER,
                                player.isSneaking() ? teammate.name() : null)) // display teammate names if sneaking
                        .toArray(CursorInfo[]::new);
            }
            for (CursorInfo cursor : teammateCursors) {
                collection.addCursor(cursor.toMapCursor(this));
            }
            // green pointer
            collection.addCursor(new CursorInfo(player.getLocation(), true, MapCursor.Type.WHITE_POINTER)
                    .toMapCursor(this));
        }
    }
}
