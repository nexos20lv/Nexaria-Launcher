package com.nexaria.launcher.rpc;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

public class DiscordPresenter {
    private static final Logger logger = LoggerFactory.getLogger(DiscordPresenter.class);
    // TODO: Replace with your actual Discord Application ID
    private static final long APPLICATION_ID = 1459940548009001304L;
    private static IPCClient client;
    private static boolean running = false;
    private static OffsetDateTime startTimestamp;

    public static void start() {
        if (running)
            return;

        logger.info("[RPC] Initialisation Discord RPC (Socket Mode)...");

        new Thread(() -> {
            try {
                client = new IPCClient(APPLICATION_ID);
                client.setListener(new IPCListener() {
                    @Override
                    public void onReady(IPCClient client) {
                        logger.info("[RPC] Pret: Connecte a Discord !");
                        running = true;
                        startTimestamp = OffsetDateTime.now();
                        update("Chargement des systèmes", "Initialisation du noyau...");
                    }
                });
                client.connect();
            } catch (NoDiscordClientException e) {
                logger.warn("Discord n'est pas lance: {}", e.getMessage());
            } catch (Exception e) {
                logger.warn("Erreur d'initialisation RPC: {}", e.getMessage());
            }
        }, "Discord-RPC-Connector").start();
    }

    public static void update(String state, String details) {
        if (!running || client == null)
            return;

        try {
            RichPresence.Builder builder = new RichPresence.Builder();
            builder.setState(state)
                    .setDetails(details)
                    .setStartTimestamp(startTimestamp)
                    .setLargeImage("logo", "Nexaria Launcher")
                    .setButton1Text("Rejoindre le Discord")
                    .setButton1Url("https://discord.gg/wcT77w5k") // Lien temporaire ou générique
                                                                  // si non fourni
                    .setButton2Text("Site Web")
                    .setButton2Url("https://eclozionmc.ovh");

            client.sendRichPresence(builder.build());
            logger.info("[RPC] Update: {} - {}", state, details);
        } catch (Exception e) {
            logger.warn("Echec update RPC: {}", e.getMessage());
        }
    }

    public static void stop() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
        running = false;
        logger.info("[RPC] Arret.");
    }
}
