package me.wega.invisibilitypotionvanish;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class InvisibilityPotionVanish extends JavaPlugin implements Listener {
    private final Set<UUID> hiddenPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.registerPacketListener();
    }

    @Override
    public void onDisable() {
        for (final UUID uuid : new HashSet<>(hiddenPlayers)) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null)
                showPlayerToEveryone(player);
        }

        hiddenPlayers.clear();

        ProtocolLibrary.getProtocolManager()
                .removePacketListeners(this);
    }

    private void registerPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(
                        this,
                        ListenerPriority.NORMAL,
                        PacketType.Play.Server.ENTITY_EFFECT,
                        PacketType.Play.Server.REMOVE_ENTITY_EFFECT
                ) {
                    @Override
                    public void onPacketSending(final PacketEvent event) {
                        final Player target = event.getPlayer();

                        if (event.getPacketType() == PacketType.Play.Server.ENTITY_EFFECT) {
                            System.out.println("ENTITY EFFECT = " + event.getPacket().getBytes().read(0));
                            final byte effectId = event.getPacket()
                                    .getBytes()
                                    .read(0);

                            if (effectId != PotionEffectType.INVISIBILITY.getId()) return;

                            hidePlayerFromEveryone(target);
                        }

                        if (event.getPacketType() == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
                            System.out.println("REMOVE ENTITY EFFECT = " + event.getPacket().getIntegers().read(1));
                            final int effectId = event.getPacket()
                                    .getIntegers()
                                    .read(1);

                            if (effectId != PotionEffectType.INVISIBILITY.getId()) return;

                            showPlayerToEveryone(target);
                        }
                    }
                }
        );
    }

    private void hidePlayerFromEveryone(Player target) {
        hiddenPlayers.add(target.getUniqueId());

        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) {
                System.out.println("HIDE PLAYER = " + target.getName() + " FROM " + viewer.getName());
                viewer.hidePlayer(target);
            }
        }
    }

    private void showPlayerToEveryone(Player target) {
        hiddenPlayers.remove(target.getUniqueId());

        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(target)) {
                System.out.println("SHOW PLAYER = " + target.getName() + " TO " + viewer.getName());
                viewer.showPlayer(target);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        System.out.println("PLAYER JOIN = " + event.getPlayer().getName());
        System.out.println("HIDDEN PLAYERs = " + hiddenPlayers);
        for (final UUID hiddenPlayerId : hiddenPlayers) {
            final Player hiddenPlayer = Bukkit.getPlayer(hiddenPlayerId);
            if (hiddenPlayer != null)
                event.getPlayer().hidePlayer(hiddenPlayer);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        hiddenPlayers.remove(event.getPlayer().getUniqueId());
    }
}