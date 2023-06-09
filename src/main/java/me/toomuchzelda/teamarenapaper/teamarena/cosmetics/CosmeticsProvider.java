package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface CosmeticsProvider {

	@Nullable
	CosmeticsProvider getParent();

	boolean hasCosmeticItem(CosmeticType type, NamespacedKey key);

	default boolean checkCosmeticItem(CosmeticType type, NamespacedKey key) {
		CosmeticsProvider cosmetics = this;
		do {
			if (cosmetics.hasCosmeticItem(type, key))
				return true;
			cosmetics = cosmetics.getParent();
		} while (cosmetics != null);
		return false;
	}

	Set<NamespacedKey> getCosmeticItems(CosmeticType type);

	/**
	 * Gets all owned cosmetic items, including those from parent {@code CosmeticsProvider}s.
	 * @param type The cosmetic type
	 * @return A set of all owned cosmetic items
	 */
	default Set<NamespacedKey> getAllCosmeticItems(CosmeticType type) {
		CosmeticsProvider cosmetics = this;
		Set<NamespacedKey> owned = new HashSet<>();
		do {
			owned.addAll(cosmetics.getCosmeticItems(type));
			cosmetics = cosmetics.getParent();
		} while (cosmetics != null);
		return Set.of(owned.toArray(new NamespacedKey[0]));
	}

	/**
	 * Gets a player's selected cosmetic.
	 * There are three possible return values:
	 * <ul>
	 *     <li>
	 *         {@code null}, which indicates that the player has not selected
	 *         a cosmetic. Callers should fallback to a parent {@code CosmeticsProvider},
	 *         or choose an appropriate default value.
	 *     </li>
	 *     <li>
	 *         An empty {@code Collection}, which indicates that the player has disabled
	 *         this {@code CosmticType}.
	 *     </li>
	 *     <li>
	 *         A non-empty {@code Collection}.
	 *     </li>
	 * </ul>
	 * @param type The cosmetic type
	 * @return A {@code Collection}, or {@code null}
	 */
	@Nullable
	Collection<NamespacedKey> getSelectedCosmetic(CosmeticType type);

	/**
	 * Sets a player's selected cosmetic.
	 * @see CosmeticsProvider#getSelectedCosmetic(CosmeticType)
	 * @param type The cosmetic type
	 * @param key A {@code Collection}, or {@code null}
	 */
	void setSelectedCosmetic(@NotNull CosmeticType type, @Nullable Collection<NamespacedKey> key);

	CosmeticsProvider DEFAULT = new CosmeticsProvider() {
		@Override
		public @Nullable CosmeticsProvider getParent() {
			return null;
		}

		@Override
		public boolean hasCosmeticItem(CosmeticType type, NamespacedKey key) {
			var item = CosmeticsManager.getCosmetic(type, key);
			return item != null && item.isDefault;
		}

		@Override
		public Set<NamespacedKey> getCosmeticItems(CosmeticType type) {
			return CosmeticsManager.getDefaultCosmetics(type);
		}

		@Override
		public @Nullable Collection<NamespacedKey> getSelectedCosmetic(CosmeticType type) {
			return null;
		}

		@Override
		public void setSelectedCosmetic(@NotNull CosmeticType type, @Nullable Collection<NamespacedKey> key) {

		}
	};
	CosmeticsProvider ALL = new CosmeticsProvider() {
		@Override
		public @Nullable CosmeticsProvider getParent() {
			return null;
		}

		@Override
		public boolean hasCosmeticItem(CosmeticType type, NamespacedKey key) {
			return true;
		}

		@Override
		public Set<NamespacedKey> getCosmeticItems(CosmeticType type) {
			return CosmeticsManager.getLoadedCosmetics(type);
		}

		@Override
		public @Nullable Collection<NamespacedKey> getSelectedCosmetic(CosmeticType type) {
			return null;
		}

		@Override
		public void setSelectedCosmetic(@NotNull CosmeticType type, @Nullable Collection<NamespacedKey> key) {

		}
	};

}
