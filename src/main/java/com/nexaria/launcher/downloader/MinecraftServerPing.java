package com.nexaria.launcher.downloader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexaria.launcher.model.ServerStatusInfo;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Minimal Minecraft server status ping (1.7+ protocol)
 */
public class MinecraftServerPing {

    private static void writeVarInt(DataOutputStream out, int value) throws Exception {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static int readVarInt(DataInputStream in) throws Exception {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    private static void writeString(DataOutputStream out, String s) throws Exception {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    public static ServerStatusInfo ping(String host, int port, String name) throws Exception {
        long start = System.nanoTime();
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(3000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Handshake packet
            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(handshakeBytes);
            handshake.writeByte(0x00); // packet id = 0
            writeVarInt(handshake, 763); // protocol version (1.20.1)
            writeString(handshake, host);
            handshake.writeShort(port);
            writeVarInt(handshake, 1); // next state: status
            handshake.flush();

            byte[] hs = handshakeBytes.toByteArray();
            writeVarInt(out, hs.length);
            out.write(hs);

            // Status request packet
            out.writeByte(0x01); // length of following (id only)
            out.writeByte(0x00); // packet id 0

            // Read response
            readVarInt(in); // packet length
            int packetId = readVarInt(in);
            if (packetId != 0x00)
                throw new RuntimeException("Invalid packet id");
            int jsonLen = readVarInt(in);
            byte[] jsonBytes = new byte[Math.max(jsonLen, 0)];
            in.readFully(jsonBytes);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            long pingMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject players = root.getAsJsonObject("players");
            int online = players != null && players.has("online") ? players.get("online").getAsInt() : -1;
            int max = players != null && players.has("max") ? players.get("max").getAsInt() : -1;

            ImageIcon icon = null;
            if (root.has("favicon")) {
                String fav = root.get("favicon").getAsString();
                if (fav.startsWith("data:image")) {
                    int comma = fav.indexOf(",");
                    if (comma > 0) {
                        String b64 = fav.substring(comma + 1);
                        byte[] png = Base64.getDecoder().decode(b64);
                        icon = new ImageIcon(png);
                    }
                }
            }

            String description = "";
            if (root.has("description")) {
                if (root.get("description").isJsonPrimitive()) {
                    description = root.get("description").getAsString();
                } else {
                    description = root.get("description").getAsJsonObject().get("text").getAsString();
                }
            }

            java.util.List<String> playerList = new java.util.ArrayList<>();
            if (players != null && players.has("sample")) {
                com.google.gson.JsonArray sample = players.getAsJsonArray("sample");
                for (com.google.gson.JsonElement e : sample) {
                    JsonObject p = e.getAsJsonObject();
                    if (p.has("name")) {
                        playerList.add(p.get("name").getAsString());
                    }
                }
            }

            ServerStatusInfo info = new ServerStatusInfo();
            info.name = name != null ? name : host;
            info.online = true;
            info.playersOnline = online;
            info.playersMax = max;
            info.pingMs = pingMs;
            info.favicon = icon;
            info.description = description;
            info.playerList = playerList;
            return info;
        } catch (Exception e) {
            ServerStatusInfo info = new ServerStatusInfo();
            info.name = name != null ? name : host;
            info.online = false;
            info.playersOnline = -1;
            info.playersMax = -1;
            info.pingMs = -1;
            info.favicon = null;
            info.description = "Serveur inaccessible";
            info.playerList = new java.util.ArrayList<>();
            return info;
        }
    }
}
