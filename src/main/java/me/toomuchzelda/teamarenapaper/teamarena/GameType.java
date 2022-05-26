package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.kingofthehill.KingOfTheHill;
import org.jetbrains.annotations.NotNull;

public enum GameType {
    KOTH(KingOfTheHill.class),
	CTF(CaptureTheFlag.class);
//	SND;

	@NotNull
	public final Class<? extends TeamArena> gameClazz;
	GameType(@NotNull Class<? extends TeamArena> gameClazz) {
		this.gameClazz = gameClazz;
	}
}
