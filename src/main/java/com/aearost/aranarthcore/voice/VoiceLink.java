package com.aearost.aranarthcore.voice;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A direct, encrypted TCP link between Survival and SMP that carries live /vc audio and channel state.
 */
public class VoiceLink {

    private static final byte TYPE_HELLO = 1;
    private static final byte TYPE_STATE = 2;
    private static final byte TYPE_AUDIO = 3;
    private static final byte TYPE_HANDOFF = 4;

    private static final int MAX_FRAME_SIZE = 65536;
    private static final int QUEUE_CAPACITY = 4096;
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final int HANDSHAKE_TIMEOUT_MS = 5000;

    private static VoiceLink instance;

    private final boolean isHostMode;
    private final String host;
    private final int port;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();
    private final LinkedBlockingQueue<byte[]> outQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private volatile boolean isRunning = true;
    private volatile Socket socket;
    private volatile DataOutputStream out;
    private ServerSocket serverSocket;

    private VoiceLink(boolean isHostMode, String host, int port, String secret) throws Exception {
        this.isHostMode = isHostMode;
        this.host = host;
        this.port = port;
        byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Starts the link if it is enabled in the config.
     */
    public static void start() {
        FileConfiguration config = AranarthCore.getInstance().getConfig();
        if (!config.getBoolean("voice-chat.link.enabled", false)) {
            return;
        }
        String secret = config.getString("voice-chat.link.secret", "");
        if (secret.length() < 16) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Voice] The voice link secret must be at least 16 characters, so the link was not started");
            return;
        }
        try {
            instance = new VoiceLink(config.getBoolean("voice-chat.link.host-mode", false),
                    config.getString("voice-chat.link.host", ""),
                    config.getInt("voice-chat.link.port", 24460), secret);
            instance.startThreads();
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Voice] Failed to start the voice link: " + e.getMessage());
            instance = null;
        }
    }

    /**
     * Stops the link and closes all sockets.
     */
    public static void shutdown() {
        if (instance != null) {
            instance.isRunning = false;
            instance.closeConnection();
            if (instance.serverSocket != null) {
                try {
                    instance.serverSocket.close();
                } catch (IOException ignored) {
                }
            }
            instance = null;
        }
    }

    public static VoiceLink getInstance() {
        return instance;
    }

    /**
     * Determines whether the link to the other server is currently connected.
     */
    public static boolean isConnected() {
        return instance != null && instance.out != null;
    }

    private void startThreads() {
        Thread connector = new Thread(isHostMode ? this::acceptLoop : this::connectLoop, "AranarthCore-VoiceLink");
        connector.setDaemon(true);
        connector.start();

        Thread writer = new Thread(this::writeLoop, "AranarthCore-VoiceLink-Writer");
        writer.setDaemon(true);
        writer.start();
    }

    /**
     * Host mode: accepts connections from the other server, one at a time.
     */
    private void acceptLoop() {
        try {
            serverSocket = new ServerSocket(port);
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Voice] Voice link listening on port " + port);
        } catch (IOException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Voice] Could not open voice link port " + port + ": " + e.getMessage());
            return;
        }
        while (isRunning) {
            try {
                Socket accepted = serverSocket.accept();
                Thread handler = new Thread(() -> handleIncoming(accepted), "AranarthCore-VoiceLink-Reader");
                handler.setDaemon(true);
                handler.start();
            } catch (IOException e) {
                if (isRunning) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Voice] Voice link accept failed: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Verifies the first frame of an incoming connection before it replaces the current connection.
     */
    private void handleIncoming(Socket accepted) {
        try {
            accepted.setTcpNoDelay(true);
            accepted.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            DataInputStream in = new DataInputStream(new BufferedInputStream(accepted.getInputStream()));
            byte[] hello = readFrame(in);
            if (hello == null || hello[0] != TYPE_HELLO) {
                accepted.close();
                return;
            }
            accepted.setSoTimeout(0);
            installConnection(accepted);
            readLoop(accepted, in);
        } catch (Exception e) {
            // Wrong secret, a port scanner, or a dropped connection
            try {
                accepted.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Connects to the host server and reconnects whenever the link drops.
     */
    @SuppressWarnings("BusyWait") // Deliberate 5 second reconnect delay, not polling
    private void connectLoop() {
        boolean hasLoggedFailure = false;
        while (isRunning) {
            Socket connecting = new Socket();
            try {
                connecting.setTcpNoDelay(true);
                connecting.connect(new InetSocketAddress(host, port), HANDSHAKE_TIMEOUT_MS);
                DataOutputStream helloOut = new DataOutputStream(new BufferedOutputStream(connecting.getOutputStream()));
                writeFrame(helloOut, new byte[]{TYPE_HELLO});
                helloOut.flush();
                installConnection(connecting);
                hasLoggedFailure = false;
                readLoop(connecting, new DataInputStream(new BufferedInputStream(connecting.getInputStream())));
            } catch (Exception e) {
                if (!hasLoggedFailure && isRunning) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Voice] Could not connect the voice link to "
                            + host + ":" + port + " (" + e.getMessage() + "), retrying every " + (RECONNECT_DELAY_MS / 1000) + " seconds");
                    hasLoggedFailure = true;
                }
                try {
                    connecting.close();
                } catch (IOException ignored) {
                }
            }
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private synchronized void installConnection(Socket newSocket) throws IOException {
        closeConnection();
        outQueue.clear();
        socket = newSocket;
        out = new DataOutputStream(new BufferedOutputStream(newSocket.getOutputStream()));
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Voice] Voice link connected to " + newSocket.getRemoteSocketAddress());
        VoiceChatManager.onLinkConnected();
    }

    private synchronized void closeConnection(Socket expected) {
        if (socket == expected) {
            closeConnection();
        }
    }

    private synchronized void closeConnection() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
            out = null;
            outQueue.clear();
            if (isRunning) {
                Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Voice] Voice link disconnected");
            }
            VoiceChatManager.onLinkDisconnected();
        }
    }

    private void readLoop(Socket current, DataInputStream in) {
        try {
            while (isRunning && socket == current) {
                byte[] frame = readFrame(in);
                if (frame == null) {
                    break;
                }
                handleFrame(frame);
            }
        } catch (Exception e) {
            // Connection dropped or a frame failed to decrypt
        }
        closeConnection(current);
    }

    private void writeLoop() {
        while (isRunning) {
            try {
                byte[] plain = outQueue.take();
                DataOutputStream currentOut = out;
                if (currentOut == null) {
                    continue;
                }
                try {
                    writeFrame(currentOut, plain);
                    if (outQueue.isEmpty()) {
                        currentOut.flush();
                    }
                } catch (Exception e) {
                    closeConnection();
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Encrypts and writes a frame: [length][12-byte IV][ciphertext].
     */
    private void writeFrame(DataOutputStream stream, byte[] plain) throws Exception {
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(plain);
        stream.writeInt(iv.length + encrypted.length);
        stream.write(iv);
        stream.write(encrypted);
    }

    /**
     * Reads and decrypts a frame.
     * @return The decrypted frame, or null if the stream ended.
     */
    private byte[] readFrame(DataInputStream in) throws Exception {
        int length;
        try {
            length = in.readInt();
        } catch (EOFException e) {
            return null;
        }
        if (length <= 12 || length > MAX_FRAME_SIZE) {
            throw new IOException("Invalid voice link frame length " + length);
        }
        byte[] data = new byte[length];
        in.readFully(data);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, data, 0, 12));
        byte[] plain = cipher.doFinal(data, 12, length - 12);
        if (plain.length == 0) {
            throw new IOException("Empty voice link frame");
        }
        return plain;
    }

    private void handleFrame(byte[] frame) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame, 1, frame.length - 1));
        switch (frame[0]) {
            case TYPE_STATE -> {
                UUID uuid = readUuid(in);
                boolean announce = in.readBoolean();
                byte channelOrdinal = in.readByte();
                if (channelOrdinal < 0 || channelOrdinal >= VoiceChannel.values().length) {
                    VoiceChatManager.onRemoteState(uuid, null, announce);
                    return;
                }
                String nickname = in.readUTF();
                UUID dominionId = in.readBoolean() ? readUuid(in) : null;
                int allowedCount = in.readInt();
                Set<UUID> allowed = new HashSet<>();
                for (int i = 0; i < allowedCount; i++) {
                    allowed.add(readUuid(in));
                }
                VoiceMember member = new VoiceMember(uuid, nickname, VoiceChannel.values()[channelOrdinal],
                        dominionId, Set.copyOf(allowed), null, 0, 0, 0);
                VoiceChatManager.onRemoteState(uuid, member, announce);
            }
            case TYPE_AUDIO -> {
                UUID uuid = readUuid(in);
                byte[] opus = new byte[in.readUnsignedShort()];
                in.readFully(opus);
                VoiceChatManager.onRemoteAudio(uuid, opus);
            }
            case TYPE_HANDOFF -> {
                UUID uuid = readUuid(in);
                VoiceChatManager.onRemoteHandoff(uuid, VoiceChannel.values()[in.readByte()]);
            }
            default -> {
                // HELLO frames after the handshake and unknown types are ignored
            }
        }
    }

    /**
     * Sends a player's channel state to the other server.
     *
     * @param uuid     The player's UUID.
     * @param member   The player's snapshot, or null if they are no longer in a channel.
     * @param announce Whether the other server should send join/leave messages and jingles.
     */
    public void sendState(UUID uuid, VoiceMember member, boolean announce) {
        if (out == null) {
            return;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(bytes);
            data.writeByte(TYPE_STATE);
            writeUuid(data, uuid);
            data.writeBoolean(announce);
            if (member == null) {
                data.writeByte(-1);
            } else {
                data.writeByte(member.channel().ordinal());
                data.writeUTF(member.nickname());
                data.writeBoolean(member.dominionId() != null);
                if (member.dominionId() != null) {
                    writeUuid(data, member.dominionId());
                }
                data.writeInt(member.allowedDominions().size());
                for (UUID allowed : member.allowedDominions()) {
                    writeUuid(data, allowed);
                }
            }
            outQueue.offer(bytes.toByteArray());
        } catch (IOException ignored) {
        }
    }

    /**
     * Sends an Opus-encoded audio frame spoken by a player to the other server.
     */
    public void sendAudio(UUID uuid, byte[] opus) {
        if (out == null || opus.length == 0 || opus.length > 65535) {
            return;
        }
        byte[] plain = new byte[1 + 16 + 2 + opus.length];
        plain[0] = TYPE_AUDIO;
        putUuid(plain, 1, uuid);
        plain[17] = (byte) (opus.length >> 8);
        plain[18] = (byte) opus.length;
        System.arraycopy(opus, 0, plain, 19, opus.length);
        outQueue.offer(plain);
    }

    /**
     * Tells the other server that a player is transferring to it, so they rejoin their channel without any announcements.
     */
    public void sendHandoff(UUID uuid, VoiceChannel channel) {
        if (out == null) {
            return;
        }
        byte[] plain = new byte[1 + 16 + 1];
        plain[0] = TYPE_HANDOFF;
        putUuid(plain, 1, uuid);
        plain[17] = (byte) channel.ordinal();
        outQueue.offer(plain);
    }

    private static void writeUuid(DataOutputStream data, UUID uuid) throws IOException {
        data.writeLong(uuid.getMostSignificantBits());
        data.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    private static void putUuid(byte[] target, int offset, UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            target[offset + i] = (byte) (most >>> (56 - 8 * i));
            target[offset + 8 + i] = (byte) (least >>> (56 - 8 * i));
        }
    }
}
