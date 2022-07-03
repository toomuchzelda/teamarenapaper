package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.kingofthehill.KingOfTheHill;
import me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy.SearchAndDestroy;
import org.jetbrains.annotations.NotNull;

public enum GameType {
    KOTH(KingOfTheHill.class),
	CTF(CaptureTheFlag.class),
	SND(SearchAndDestroy.class);

	@NotNull
	public final Class<? extends TeamArena> gameClazz;
	GameType(@NotNull Class<? extends TeamArena> gameClazz) {
		this.gameClazz = gameClazz;
	}
}
