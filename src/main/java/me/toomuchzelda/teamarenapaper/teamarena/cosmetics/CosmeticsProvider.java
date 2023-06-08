package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

public interface CosmeticsProvider {

	@Nullable
	CosmeticsProvider getParent();

	boolean hasCosmeticItem(CosmeticType type, NamespacedKey key);

	Set<NamespacedKey> getCosmeticItems(CosmeticType type);

	@Nullable
	Collection<NamespacedKey> getSelectedCosmetic(CosmeticType type);

	void setSelectedCosmetic(@NotNull CosmeticType type, @Nullable Set<NamespacedKey> key);

	static CosmeticsProvider DEFAULT = new CosmeticsProvider() {
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
		public void setSelectedCosmetic(@NotNull CosmeticType type, @Nullable Set<NamespacedKey> key) {

		}
	};

}
