package com.aearost.aranarthcore.voice;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A direct, encrypted TCP link between Survival and SMP that carries live /vc audio and channel state.
*/
public class VoiceLink {

    private static final byte TYPE_STATE = 2;
    private static final byte TYPE_AUDIO = 3;
    private static final byte TYPE_HANDOFF = 4;

    private static final int MAX_FRAME_SIZE = 65536;
    private static final int MAX_HANDSHAKE_FRAME_SIZE = 64;
    private static final int NONCE_SIZE = 32;
    private static final int QUEUE_CAPACITY = 4096;
    private static final int RECONNECT_DELAY_MS = 5000;
    private static final int HANDSHAKE_TIMEOUT_MS = 5000;
    private static final int MAX_CONCURRENT_HANDSHAKES = 2;

    private static VoiceLink instance;

    private final boolean isHostMode;
    private final String host;
    private final int port;
    private final String allowedIp;
    private final byte[] masterKey;
    private final SecureRandom random = new SecureRandom();
    private final LinkedBlockingQueue<byte[]> outQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final Semaphore handshakeSlots = new Semaphore(MAX_CONCURRENT_HANDSHAKES);
    private final ScheduledExecutorService deadlines = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "AranarthCore-VoiceLink-Deadlines");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean isRunning = true;
    private volatile Session session;
    private ServerSocket serverSocket;

    /**
     * An authenticated connection with its own keys and frame counters for each direction.
     */
    private static final class Session {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;
        private final SecretKeySpec sendKey;
        private final SecretKeySpec receiveKey;
        private long sendCounter = 0;
        private long receiveCounter = 0;

        private Session(Socket socket, DataInputStream in, DataOutputStream out, SecretKeySpec sendKey, SecretKeySpec receiveKey) {
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.sendKey = sendKey;
            this.receiveKey = receiveKey;
        }
    }

    private VoiceLink(boolean isHostMode, String host, int port, String allowedIp, String secret) throws Exception {
        this.isHostMode = isHostMode;
        this.host = host;
        this.port = port;
        this.allowedIp = allowedIp;
        this.masterKey = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
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
                    config.getInt("voice-chat.link.port", 24460),
                    config.getString("voice-chat.link.allowed-ip", ""), secret);
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
            instance.deadlines.shutdownNow();
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
        return instance != null && instance.session != null;
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
     * Host mode: accepts connections from the other server. Only a few handshakes may run at once, so a
     * flood of connections cannot exhaust the server's threads or memory.
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
                if (!isAllowedAddress(accepted.getInetAddress()) || !handshakeSlots.tryAcquire()) {
                    accepted.close();
                    continue;
                }
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
     * Provides whether a connection may come from the address, using voice-chat.link.allowed-ip when it is set.
     */
    private boolean isAllowedAddress(InetAddress address) {
        if (allowedIp == null || allowedIp.isBlank()) {
            return true;
        }
        try {
            return InetAddress.getByName(allowedIp.trim()).equals(address);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Authenticates an incoming connection before it replaces the current connection.
     */
    private void handleIncoming(Socket accepted) {
        AtomicBoolean isHandshakeDone = new AtomicBoolean(false);
        ScheduledFuture<?> deadline = scheduleHandshakeDeadline(accepted, isHandshakeDone);
        Session newSession;
        try {
            accepted.setTcpNoDelay(true);
            accepted.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            DataInputStream in = new DataInputStream(new BufferedInputStream(accepted.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(accepted.getOutputStream()));

            // 1. The client's nonce
            byte[] clientNonce = readHandshakeFrame(in);
            if (clientNonce.length != NONCE_SIZE) {
                throw new IOException("Invalid client nonce");
            }
            // 2. This server's nonce, and proof that this server knows the secret
            byte[] hostNonce = newNonce();
            writeHandshakeFrame(out, hostNonce);
            writeHandshakeFrame(out, hmac(masterKey, "host", clientNonce, hostNonce));
            out.flush();
            // 3. The client's proof that it knows the secret
            byte[] clientProof = readHandshakeFrame(in);
            if (!MessageDigest.isEqual(clientProof, hmac(masterKey, "client", clientNonce, hostNonce))) {
                throw new IOException("Invalid client proof");
            }

            newSession = new Session(accepted, in, out,
                    deriveKey("host-to-client", clientNonce, hostNonce), deriveKey("client-to-host", clientNonce, hostNonce));
            isHandshakeDone.set(true);
            deadline.cancel(false);
            accepted.setSoTimeout(0);
        } catch (Exception e) {
            // Wrong secret, a port scanner, a replayed handshake, or a dropped connection
            deadline.cancel(false);
            closeQuietly(accepted);
            return;
        } finally {
            handshakeSlots.release();
        }
        installConnection(newSession);
        readLoop(newSession);
    }

    /**
     * Closes the socket if its handshake has not finished within the time limit, however slowly the
     * other side sends data.
     */
    private ScheduledFuture<?> scheduleHandshakeDeadline(Socket socket, AtomicBoolean isHandshakeDone) {
        return deadlines.schedule(() -> {
            if (!isHandshakeDone.get()) {
                closeQuietly(socket);
            }
        }, HANDSHAKE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Connects to the host server and reconnects whenever the link drops.
     */
    @SuppressWarnings("BusyWait") // Deliberate 5 second reconnect delay, not polling
    private void connectLoop() {
        boolean hasLoggedFailure = false;
        while (isRunning) {
            Socket connecting = new Socket();
            AtomicBoolean isHandshakeDone = new AtomicBoolean(false);
            ScheduledFuture<?> deadline = null;
            try {
                connecting.setTcpNoDelay(true);
                connecting.connect(new InetSocketAddress(host, port), HANDSHAKE_TIMEOUT_MS);
                deadline = scheduleHandshakeDeadline(connecting, isHandshakeDone);
                connecting.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
                DataInputStream in = new DataInputStream(new BufferedInputStream(connecting.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(connecting.getOutputStream()));

                // 1. This server's nonce
                byte[] clientNonce = newNonce();
                writeHandshakeFrame(out, clientNonce);
                out.flush();
                // 2. The host's nonce and its proof that it knows the secret
                byte[] hostNonce = readHandshakeFrame(in);
                byte[] hostProof = readHandshakeFrame(in);
                if (hostNonce.length != NONCE_SIZE
                        || !MessageDigest.isEqual(hostProof, hmac(masterKey, "host", clientNonce, hostNonce))) {
                    throw new IOException("The voice link host could not prove it knows the secret");
                }
                // 3. This server's proof
                writeHandshakeFrame(out, hmac(masterKey, "client", clientNonce, hostNonce));
                out.flush();

                Session newSession = new Session(connecting, in, out,
                        deriveKey("client-to-host", clientNonce, hostNonce), deriveKey("host-to-client", clientNonce, hostNonce));
                isHandshakeDone.set(true);
                deadline.cancel(false);
                connecting.setSoTimeout(0);
                installConnection(newSession);
                hasLoggedFailure = false;
                readLoop(newSession);
            } catch (Exception e) {
                if (deadline != null) {
                    deadline.cancel(false);
                }
                if (!hasLoggedFailure && isRunning) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Voice] Could not connect the voice link to "
                            + host + ":" + port + " (" + e.getMessage() + "), retrying every " + (RECONNECT_DELAY_MS / 1000) + " seconds");
                    hasLoggedFailure = true;
                }
                closeQuietly(connecting);
            }
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private synchronized void installConnection(Session newSession) {
        closeConnection();
        outQueue.clear();
        session = newSession;
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Voice] Voice link connected to " + newSession.socket.getRemoteSocketAddress());
        VoiceChatManager.onLinkConnected();
    }

    private synchronized void closeConnection(Session expected) {
        if (session == expected) {
            closeConnection();
        } else {
            closeQuietly(expected.socket);
        }
    }

    private synchronized void closeConnection() {
        Session current = session;
        if (current != null) {
            closeQuietly(current.socket);
            session = null;
            outQueue.clear();
            if (isRunning) {
                Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Voice] Voice link disconnected");
            }
            VoiceChatManager.onLinkDisconnected();
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void readLoop(Session current) {
        try {
            while (isRunning && session == current) {
                byte[] frame = readFrame(current);
                if (frame == null) {
                    break;
                }
                handleFrame(frame);
            }
        } catch (Exception e) {
            // Connection dropped, or a frame failed to decrypt (wrong key, replayed or reordered)
        }
        closeConnection(current);
    }

    private void writeLoop() {
        while (isRunning) {
            try {
                byte[] plain = outQueue.take();
                Session current = session;
                if (current == null) {
                    continue;
                }
                try {
                    writeFrame(current, plain);
                    if (outQueue.isEmpty()) {
                        current.out.flush();
                    }
                } catch (Exception e) {
                    closeConnection(current);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private byte[] newNonce() {
        byte[] nonce = new byte[NONCE_SIZE];
        random.nextBytes(nonce);
        return nonce;
    }

    private static byte[] hmac(byte[] key, String label, byte[] clientNonce, byte[] hostNonce) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        mac.update(label.getBytes(StandardCharsets.UTF_8));
        mac.update(clientNonce);
        mac.update(hostNonce);
        return mac.doFinal();
    }

    /**
     * Derives the AES key for one direction of this connection from the secret and both nonces.
     */
    private SecretKeySpec deriveKey(String direction, byte[] clientNonce, byte[] hostNonce) throws Exception {
        return new SecretKeySpec(hmac(masterKey, "key:" + direction, clientNonce, hostNonce), "AES");
    }

    private static void writeHandshakeFrame(DataOutputStream stream, byte[] data) throws IOException {
        stream.writeInt(data.length);
        stream.write(data);
    }

    /**
     * Reads a small unencrypted handshake frame, checking its length before allocating anything.
     */
    private static byte[] readHandshakeFrame(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > MAX_HANDSHAKE_FRAME_SIZE) {
            throw new IOException("Invalid voice link handshake frame length " + length);
        }
        byte[] data = new byte[length];
        in.readFully(data);
        return data;
    }

    /**
     * Builds the GCM IV for a frame from its counter, which never repeats within a key.
     */
    private static GCMParameterSpec ivFor(long counter) {
        byte[] iv = new byte[12];
        for (int i = 0; i < 8; i++) {
            iv[4 + i] = (byte) (counter >>> (56 - 8 * i));
        }
        return new GCMParameterSpec(128, iv);
    }

    /**
     * Encrypts and writes a frame: [length][ciphertext]. The IV is the session's send counter.
     */
    private static void writeFrame(Session current, byte[] plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, current.sendKey, ivFor(current.sendCounter++));
        byte[] encrypted = cipher.doFinal(plain);
        current.out.writeInt(encrypted.length);
        current.out.write(encrypted);
    }

    /**
     * Reads and decrypts a frame using the expected receive counter, so a replayed, reordered or
     * reflected frame fails to decrypt.
     * @return The decrypted frame, or null if the stream ended.
     */
    private static byte[] readFrame(Session current) throws Exception {
        int length;
        try {
            length = current.in.readInt();
        } catch (EOFException e) {
            return null;
        }
        if (length <= 16 || length > MAX_FRAME_SIZE) {
            throw new IOException("Invalid voice link frame length " + length);
        }
        byte[] data = new byte[length];
        current.in.readFully(data);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, current.receiveKey, ivFor(current.receiveCounter++));
        byte[] plain = cipher.doFinal(data);
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
                // Unknown frame types are ignored
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
        if (session == null) {
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
        if (session == null || opus.length == 0 || opus.length > 65535) {
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
        if (session == null) {
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
