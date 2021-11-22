package me.toomuchzelda.teamarenapaper.core;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLib;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.Collection;

public class TeamsPacket
{
	public static final int ADD = 0;
	public static final int REMOVE = 1;
	public static final int CHANGE = 2;
	public static final int JOIN = 3;
	public static final int LEAVE = 4;
	
	private PacketContainer packet;
	
	public TeamsPacket() {
		packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
	}
	
	public void setName(String name) {
		packet.getStrings().write(0, name);
	}
	
	public void setAction(int action) {
		packet.getIntegers().write(0,action);
	}
	
	public void setMembers(Collection<String> members) {
		packet.getModifier().write(2, members);
	}
	
	public void setMembers(String[] members) {
		setMembers(Arrays.asList(members));
	}
	
	public void setDisplayName(Component displayName) {
	
	}
}
