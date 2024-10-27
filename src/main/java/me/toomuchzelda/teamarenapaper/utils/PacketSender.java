package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public interface PacketSender {
	void enqueue(Player player, PacketContainer packet);
	void enqueue(Player player, PacketContainer... packets);
	void enqueue(Player player, Collection<PacketContainer> packets);

	void broadcast(PacketContainer packet);
	void broadcast(PacketContainer... packets);
	void broadcast(Collection<PacketContainer> packets);

	void clear();
	void flush();

	PacketSender.Immediate IMMEDIATE_INSTANCE = new Immediate();

	static PacketSender getDefault(int listSize) {
		return new Cached(listSize);
	}

	static PacketSender.Immediate getImmediateInstance() {
		return PacketSender.IMMEDIATE_INSTANCE;
	}

	class Immediate implements PacketSender {
		@Override
		public void enqueue(Player player, PacketContainer packet) {
			PlayerUtils.sendPacket(player, packet);
		}

		@Override
		public void enqueue(Player player, PacketContainer... packets) {
			PlayerUtils.sendPacket(player, packets);
		}

		@Override
		public void enqueue(Player player, Collection<PacketContainer> packets) {
			PlayerUtils.sendPacket(player, packets);
		}

		@Override
		public void broadcast(PacketContainer packet) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				PlayerUtils.sendPacket(player, packet);
			}
		}

		@Override
		public void broadcast(PacketContainer... packets) {
			PlayerUtils.sendPacket(Bukkit.getOnlinePlayers(), packets);
		}

		@Override
		public void broadcast(Collection<PacketContainer> packets) {
			// TODO not use toArray()
			PlayerUtils.sendPacket(Bukkit.getOnlinePlayers(), packets.toArray(new PacketContainer[0]));
		}

		@Override
		public void clear() {}
		@Override
		public void flush() {}
	}

	class Cached implements PacketSender {
		private final HashMap<Player, ArrayList<PacketContainer>> cache;
		private final int newListSize;

		public Cached() {
			this(10);
		}

		public Cached(int listSize) {
			this(Bukkit.getOnlinePlayers().size() * 2, listSize);
		}

		public Cached(int mapSize, int listSize) {
			this.cache = HashMap.newHashMap(mapSize);
			this.newListSize = listSize;
		}

		private ArrayList<PacketContainer> getList(Player p, int sz) {
			return cache.computeIfAbsent(p, ignored -> new ArrayList<>(Math.max(this.newListSize, sz)));
		}

		@Override
		public void enqueue(Player player, PacketContainer packet) {
			getList(player, this.newListSize).add(packet);
		}

		@Override
		public void enqueue(Player player, PacketContainer... packets) {
			ArrayList<PacketContainer> list = getList(player, packets.length + (packets.length / 2));
			Collections.addAll(list, packets);
		}

		@Override
		public void enqueue(Player player, Collection<PacketContainer> packets) {
			getList(player, packets.size() + (packets.size() / 2)).addAll(packets);
		}

		// TODO fancy optimise broadcasts
		@Override
		public void broadcast(PacketContainer packet) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				enqueue(player, packet);
			}
		}

		@Override
		public void broadcast(PacketContainer... packets) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				enqueue(player, packets);
			}
		}

		@Override
		public void broadcast(Collection<PacketContainer> packets) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				enqueue(player, packets);
			}
		}

		@Override
		public void clear() {
			this.cache.clear();
		}

		@Override
		public void flush() {
			for (var entry : this.cache.entrySet()) {
				ArrayList<PacketContainer> packets = entry.getValue();
				Player player = entry.getKey();
				if (packets.size() > 1) {
					PacketContainer bundle = PlayerUtils.createBundle(packets);
					PlayerUtils.sendPacket(player, bundle);
				}
				else {
					PlayerUtils.sendPacket(player, packets.getFirst());
				}
			}

			this.clear();
		}
	}
}
