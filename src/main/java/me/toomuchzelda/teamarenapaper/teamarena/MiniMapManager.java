package me.toomuchzelda.teamarenapaper.teamarena;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MaterialColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.map.CraftMapView;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class MiniMapManager {
    public final MapView view;
    final int mapWidth;
    final int centerX, centerZ;
	final int scale;

    private record CursorProvider(BiPredicate<Player, PlayerInfo> displayCondition,
                                  BiFunction<Player, PlayerInfo, @NotNull CursorInfo> infoSupplier) { }

    private record ComplexCursorProvider(BiPredicate<Player, PlayerInfo> displayCondition,
                                         @Nullable BiFunction<Player, PlayerInfo, boolean @NotNull []> complexCondition,
                                         BiFunction<Player, PlayerInfo, @NotNull CursorInfo @NotNull[]> infoSupplier) { }

    public record CursorInfo(Location location, boolean directional, MapCursor.Type icon, @Nullable Component caption) {
        public CursorInfo(Location location, boolean directional, MapCursor.Type icon) {
            this(location, directional, icon, null);
        }

        MapCursor toMapCursor(GameRenderer renderer) {
            return new MapCursor(renderer.convertX(location), renderer.convertZ(location),
                    directional ? renderer.convertYaw(location) : (byte) 8, icon, true, caption);
        }
    }


    // cursors
    final List<CursorProvider> cursors = new ArrayList<>();
    final List<ComplexCursorProvider> complexCursors = new ArrayList<>();

	// default cursors
	private static final BiPredicate<Player, PlayerInfo> ALWAYS_SHOW_CURSOR = (ignored1, ignored2) -> true;

	// For the map item
	private static final List<Component> ITEM_LORE = List.of(
		Component.text("Right click to view teammates/players kits", TextUtils.RIGHT_CLICK_TO)
	);

	// canvas access
    @FunctionalInterface
    public interface CanvasOperation {
        void render(Player player, PlayerInfo info, MapCanvas canvas, GameRenderer renderer);
    }

    final List<CanvasOperation> canvasOperations = new ArrayList<>();

	private static MapView sharedView;
    public MiniMapManager(TeamArena game) {
		if (sharedView == null) {
			sharedView = Bukkit.createMap(Bukkit.getWorlds().get(0));
		}
        // create map view and adjust center and scale
        view = sharedView;
        BoundingBox box = game.border;
        centerX = Location.locToBlock(box.getCenterX());
        centerZ = Location.locToBlock(box.getCenterZ());
        view.setCenterX(centerX);
        view.setCenterZ(centerZ);
        int side = Math.max((int) Math.ceil(box.getWidthX()), (int) Math.ceil(box.getWidthZ()));
        int log = 32 - Integer.numberOfLeadingZeros(side - 1); // ceil(log2(side))
		if (log < 7) { // map cannot be shorter than 128 blocks
			Main.logger().info("[Minimap] Map was too short in length (" + side + " blocks), extended to 128 blocks");
			log = 7;
		}
        mapWidth = 1 << log;
        scale = MathUtils.clamp(0, 4,  log - 7);
        //noinspection deprecation,ConstantConditions
        view.setScale(MapView.Scale.valueOf((byte) scale));
        view.setTrackingPosition(false);

		Main.logger().info("[MiniMap] Center: (%d, %d), width: %d, scale: %d".formatted(centerX, centerZ, mapWidth, scale));

		// let's hope the map file is removed
		File mainWorldFile = new File(Bukkit.getWorldContainer(), Bukkit.getWorlds().get(0).getName());
		File mapDataFile = new File(mainWorldFile, "data" + File.separator + "map_" + view.getId() + ".dat");
		mapDataFile.deleteOnExit();

        // our renderers
		view.getRenderers().forEach(view::removeRenderer);
		if (CommandDebug.disableMiniMapInCaseSomethingTerribleHappens) // don't add our renderers when disabled
			return;
		view.addRenderer(new GameMapRenderer(scale, mapWidth, centerX, centerZ));
        view.addRenderer(new GameRenderer());
    }

	private static final VarHandle renderCacheVarHandle;
	private static final VarHandle canvasesVarHandle;

	static {
		try {
			var lookup = MethodHandles.privateLookupIn(CraftMapView.class, MethodHandles.lookup());
			renderCacheVarHandle = lookup.findVarHandle(CraftMapView.class, "renderCache", Map.class);

			canvasesVarHandle = MethodHandles.privateLookupIn(CraftMapView.class, MethodHandles.lookup())
				.findVarHandle(CraftMapView.class, "canvases", Map.class);
		} catch (Exception ex) {
			throw new AssertionError("Couldn't find CraftMapView.renderCache", ex);
		}
	}

    public void onGameEnd() {
    }

    public void onGameCleanup() {
		cursors.clear();
		complexCursors.clear();
		canvasOperations.clear();
		// properly release the renderers, or get nasty memory leaks
		view.getRenderers().forEach(view::removeRenderer);
		// bukkit moment
		((Map<?, ?>) renderCacheVarHandle.get(view)).clear();
	}

	public void onPlayerCleanup(Player player) {
		// Defer because the impl. re-adds it right after this
		Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
			((Map<?, ?>) renderCacheVarHandle.get(view)).remove(player);

			for (MapRenderer renderer : view.getRenderers()) {
				Map canvases = (Map) canvasesVarHandle.get(view);
				Map renderMap = (Map) canvases.get(renderer);
				if (renderMap != null)
					renderMap.remove(player);
			}
		}, 0L);
	}

    @NotNull
    public ItemStack getMapItem() {
        return getMapItem(null);
    }

    @NotNull
    public ItemStack getMapItem(@Nullable TeamArenaTeam team) {
        return ItemBuilder.of(Material.FILLED_MAP)
				.displayName(Component.text("Game map", team != null ? team.getRGBTextColor() : NamedTextColor.WHITE))
                .meta(MapMeta.class, mapMeta -> {
                    mapMeta.setMapView(view);
                    if (team != null) {
                        mapMeta.setColor(team.getColour());
                    }
                })
				.lore(ITEM_LORE)
				.hideAll()
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

	public static final class GameMapRenderer extends MapRenderer {
		public final int scale;
		public final int mapWidth;
		public final int centerX;
		public final int centerZ;
		GameMapRenderer(int scale, int mapWidth, int centerX, int centerZ) {
			super(false);
			this.scale = scale;
			this.mapWidth = mapWidth;
			this.centerX = centerX;
			this.centerZ = centerZ;
		}

		private BlockState getCorrectStateForFluidBlock(Level world, BlockState state, BlockPos pos) {
			FluidState fluid = state.getFluidState();

			return !fluid.isEmpty() && !state.isFaceSturdy(world, pos, Direction.UP) ? fluid.createLegacyBlock() : state;
		}

		public boolean hasDrawn = false;

		// adapted from MapItem#update
		@Override
		public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
			if (hasDrawn) {
				return;
			}
			hasDrawn = true;

			var box = Main.getGame().border;
			var bukkitWorld = Main.getGame().getWorld();
			int minY = Math.max((int) box.getMinY(), bukkitWorld.getMinHeight());
			int maxY = Math.min((int) box.getMaxY(), bukkitWorld.getMaxHeight());

			var world = ((CraftWorld) bukkitWorld).getHandle();
			int blocksPerPixel = 1 << scale;

			int minX = (int) Math.floor(box.getMinX());
			int maxX = (int) Math.ceil(box.getMaxX());
			int minZ = (int) Math.floor(box.getMinZ());
			int maxZ = (int) Math.ceil(box.getMaxZ());

			Multiset<MaterialColor> multiset = LinkedHashMultiset.create();

			for (int mapX = 0; mapX < 128; ++mapX) {
				double d0 = 0.0D;

				for (int mapZ = 0; mapZ < 128; ++mapZ) {
					int blockX = (centerX / blocksPerPixel + mapX - 64) * blocksPerPixel;
					int blockZ = (centerZ / blocksPerPixel + mapZ - 64) * blocksPerPixel;

					// check if within map borders
					// render every block outside of map borders
					boolean isOutsideBorder = blockX < minX || blockX + blocksPerPixel > maxX ||
							blockZ < minZ || blockZ + blocksPerPixel > maxZ;

					multiset.clear();
					var chunk = (LevelChunk) world.getChunk(blockX >> 4, blockZ >> 4); // will load unloaded chunks
					if (chunk.isEmpty()) {
						continue;
					}
					ChunkPos chunkPos = chunk.getPos();
					int chunkBlockX = blockX & 15;
					int chunkBlockZ = blockZ & 15;
					int k3 = 0;
					double d1 = 0.0D;

					BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
					BlockPos.MutableBlockPos pos1 = new BlockPos.MutableBlockPos();

					for (int i = 0; i < blocksPerPixel; ++i) {
						for (int j = 0; j < blocksPerPixel; ++j) {
//							if (isOutsideBorder) {
//								multiset.add(MaterialColor.NONE);
//								continue;
//							}


							int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, i + chunkBlockX, j + chunkBlockZ) + 1;
							int blockY = Math.min(surface, maxY);
							BlockState blockState;

							if (blockY > minY + 1) {
								do {
									--blockY;
									pos.set(chunkPos.getMinBlockX() + i + chunkBlockX, blockY, chunkPos.getMinBlockZ() + j + chunkBlockZ);
									blockState = chunk.getBlockState(pos);
								} while (blockState.getMapColor(world, pos) == MaterialColor.NONE && blockY > minY);

								if (blockY > minY && !blockState.getFluidState().isEmpty()) {
									int l4 = blockY - 1;

									pos1.set(pos);

									BlockState iblockdata1;

									do {
										pos1.setY(l4--);
										iblockdata1 = chunk.getBlockState(pos1);
										++k3;
									} while (l4 > minY && !iblockdata1.getFluidState().isEmpty());

									blockState = this.getCorrectStateForFluidBlock(world, blockState, pos);
								}
							} else {
								blockState = Blocks.AIR.defaultBlockState();
							}

							d1 += (double) blockY / (double) (blocksPerPixel * blocksPerPixel);
							multiset.add(blockState.getMapColor(world, pos));
						}
					}

					k3 /= blocksPerPixel * blocksPerPixel;
					MaterialColor materialColor = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MaterialColor.NONE);
					double d2;
					MaterialColor.Brightness brightness;

					if (isOutsideBorder) {
						brightness = MaterialColor.Brightness.LOWEST; // dark pixels to indicate that it is outside the playable area
					} else {
						if (materialColor == MaterialColor.WATER) {
							d2 = (double) k3 * 0.1D + (double) (mapX + mapZ & 1) * 0.2D;
							if (d2 < 0.5D) {
								brightness = MaterialColor.Brightness.HIGH;
							} else if (d2 > 0.9D) {
								brightness = MaterialColor.Brightness.LOW;
							} else {
								brightness = MaterialColor.Brightness.NORMAL;
							}
						} else {
							d2 = (d1 - d0) * 4.0D / (double) (blocksPerPixel + 4) + ((double) (mapX + mapZ & 1) - 0.5D) * 0.4D;
							if (d2 > 0.6D) {
								brightness = MaterialColor.Brightness.HIGH;
							} else if (d2 < -0.6D) {
								brightness = MaterialColor.Brightness.LOW;
							} else {
								brightness = MaterialColor.Brightness.NORMAL;
							}
						}
					}

					d0 = d1;
					canvas.setPixel(mapX, mapZ, materialColor.getPackedId(brightness));
				}
			}
		}
	}

    public final class GameRenderer extends MapRenderer {
        GameRenderer() {
            super(true);
        }

        @Override
        public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
			if (Main.getGame() == null)
				return;

			MapCursorCollection mapCursors = new MapCursorCollection();
			PlayerInfo playerInfo = Main.getPlayerInfo(player);
			if (playerInfo == null)
				return;

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
            return (byte) Math.round((x - centerX) * 128 / (mapWidth / 2d));
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
            return (byte) Math.round((z - centerZ) * 128 / (mapWidth / 2d));
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
            return (byte) Math.floorMod(Math.round(Math.floorMod(yaw, 360) * 16 / 360d), 16);
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
