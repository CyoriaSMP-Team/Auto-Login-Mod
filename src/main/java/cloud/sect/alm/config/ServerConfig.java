package cloud.sect.alm.config;

import cloud.sect.alm.util.EncryptionUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;

public class ServerConfig {
    private static final File CONFIG_FILE = new File("config/alm-servers.json");
    private static final File DEVICE_KEY_FILE = new File("config/alm-device.key");
    private static final String LEGACY_FALLBACK_KEY = "AutoLoginModSecretKey2026";
    private static String MASTER_KEY = LEGACY_FALLBACK_KEY;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Map<ServerIP, ServerEntry>
    private static Map<String, ServerEntry> servers = new HashMap<>();
    private static String globalSalt = "";
    
    // Global settings
    private static String globalPassword = "";
    private static boolean globalRegisterMode = false;
    private static String globalPasswordSalt = "";
    private static boolean autoTypeEnabled = true;
    private static boolean smartModeEnabled = true;
    private static int minDelay = 800;
    private static int maxDelay = 2000;
    private static java.util.List<String> customTriggers = new java.util.ArrayList<>();
    
    // Security
    private static String masterPasswordHash = "";
    private static String masterPasswordSalt = "";
    private static String rememberedMasterKey = "";
    private static String rememberedMasterKeySalt = "";
    private static boolean rememberUnlockOnDevice = true;
    private static boolean isUnlocked = false;
    private static boolean sessionLoggedIn = false;
    private static boolean authAttemptPending = false;
    private static long lastAuthAttemptTime = 0;
    private static final long AUTH_ATTEMPT_COOLDOWN = 5000;

    public static boolean isSessionLoggedIn() {
        return sessionLoggedIn;
    }

    public static synchronized void setSessionLoggedIn(boolean loggedIn) {
        sessionLoggedIn = loggedIn;
        if (loggedIn) authAttemptPending = false;
    }

    public static synchronized void resetSession() {
        sessionLoggedIn = false;
        authAttemptPending = false;
        lastAuthAttemptTime = 0;
    }

    public static synchronized boolean beginAuthAttempt() {
        if (sessionLoggedIn || authAttemptPending) return false;
        long now = System.currentTimeMillis();
        if (now - lastAuthAttemptTime < AUTH_ATTEMPT_COOLDOWN) return false;
        authAttemptPending = true;
        lastAuthAttemptTime = now;
        return true;
    }

    public static synchronized void finishAuthAttempt() {
        authAttemptPending = false;
    }

    
    public static class ServerEntry {
        public String ip;
        public String encryptedCommand;
        public boolean isRegisterCommand;
        public boolean autoRegister;
        public String salt;
        public boolean enabled = true;
        
        public ServerEntry(String ip, String encryptedCommand, boolean isRegisterCommand, boolean autoRegister, String salt) {
            this.ip = ip;
            this.encryptedCommand = encryptedCommand;
            this.isRegisterCommand = isRegisterCommand;
            this.autoRegister = autoRegister;
            this.salt = salt;
        }
    }
    
    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                createDefaultConfig();
                return;
            }
            
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root.has("salt")) globalSalt = root.get("salt").getAsString();
                if (root.has("servers")) servers = GSON.fromJson(root.get("servers"), new TypeToken<Map<String, ServerEntry>>(){}.getType());
                if (root.has("customTriggers")) customTriggers = GSON.fromJson(root.get("customTriggers"), new TypeToken<java.util.List<String>>(){}.getType());
                if (root.has("globalPassword")) globalPassword = root.get("globalPassword").getAsString();
                if (root.has("globalRegisterMode")) globalRegisterMode = root.get("globalRegisterMode").getAsBoolean();
                if (root.has("globalPasswordSalt")) globalPasswordSalt = root.get("globalPasswordSalt").getAsString();
                if (root.has("autoTypeEnabled")) autoTypeEnabled = root.get("autoTypeEnabled").getAsBoolean();
                if (root.has("smartModeEnabled")) smartModeEnabled = root.get("smartModeEnabled").getAsBoolean();
                if (root.has("minDelay")) minDelay = root.get("minDelay").getAsInt();
                if (root.has("maxDelay")) maxDelay = root.get("maxDelay").getAsInt();
                if (root.has("masterPasswordHash")) masterPasswordHash = root.get("masterPasswordHash").getAsString();
                if (root.has("masterPasswordSalt")) masterPasswordSalt = root.get("masterPasswordSalt").getAsString();
                if (root.has("rememberedMasterKey")) rememberedMasterKey = root.get("rememberedMasterKey").getAsString();
                if (root.has("rememberedMasterKeySalt")) rememberedMasterKeySalt = root.get("rememberedMasterKeySalt").getAsString();
                if (root.has("rememberUnlockOnDevice")) rememberUnlockOnDevice = root.get("rememberUnlockOnDevice").getAsBoolean();
            }
            initializeUnlockState();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void save() {
        try {
            if (globalSalt.isEmpty()) globalSalt = EncryptionUtil.generateSalt();
            
            JsonObject root = new JsonObject();
            root.addProperty("salt", globalSalt);
            root.add("servers", GSON.toJsonTree(servers));
            root.add("customTriggers", GSON.toJsonTree(customTriggers));
            root.addProperty("globalPassword", globalPassword);
            root.addProperty("globalRegisterMode", globalRegisterMode);
            root.addProperty("globalPasswordSalt", globalPasswordSalt);
            root.addProperty("autoTypeEnabled", autoTypeEnabled);
            root.addProperty("smartModeEnabled", smartModeEnabled);
            root.addProperty("minDelay", minDelay);
            root.addProperty("maxDelay", maxDelay);
            root.addProperty("masterPasswordHash", masterPasswordHash);
            root.addProperty("masterPasswordSalt", masterPasswordSalt);
            root.addProperty("rememberedMasterKey", rememberedMasterKey);
            root.addProperty("rememberedMasterKeySalt", rememberedMasterKeySalt);
            root.addProperty("rememberUnlockOnDevice", rememberUnlockOnDevice);
            
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static java.util.List<String> getCustomTriggers() {
        return customTriggers;
    }

    public static void addCustomTrigger(String trigger) {
        if (!customTriggers.contains(trigger.toLowerCase())) {
            customTriggers.add(trigger.toLowerCase());
            save();
        }
    }

    public static void removeCustomTrigger(String trigger) {
        customTriggers.remove(trigger.toLowerCase());
        save();
    }

    private static long lastActivityTime = 0;
    private static final long AUTO_LOCK_TIMEOUT = 5 * 60 * 1000; // 5 minutes
    
    public static boolean setMasterPassword(String password) {
        try {
            masterPasswordSalt = EncryptionUtil.generateSalt();
            masterPasswordHash = EncryptionUtil.hashPassword(password, masterPasswordSalt);
            MASTER_KEY = password;
            isUnlocked = true;
            updateActivity();
            rememberCurrentMasterKey();
            save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean unlock(String password) {
        try {
            String hash = EncryptionUtil.hashPassword(password, masterPasswordSalt);
            if (hash.equals(masterPasswordHash)) {
                MASTER_KEY = password;
                isUnlocked = true;
                updateActivity();
                rememberCurrentMasterKey();
                save();
                return true;
            }
        } catch (Exception e) {}
        return false;
    }

    public static void updateActivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    public static boolean isLocked() {
        if (!hasMasterPassword()) return false;
        if (!isUnlocked) return true;
        if (!rememberUnlockOnDevice && System.currentTimeMillis() - lastActivityTime > AUTO_LOCK_TIMEOUT) {
            lock();
            return true;
        }
        return false;
    }

    public static boolean hasMasterPassword() {
        return masterPasswordHash != null && !masterPasswordHash.isEmpty();
    }

    public static void lock() {
        MASTER_KEY = LEGACY_FALLBACK_KEY;
        isUnlocked = false;
    }

    public static boolean isRememberUnlockOnDevice() {
        return rememberUnlockOnDevice;
    }

    public static void setRememberUnlockOnDevice(boolean remember) {
        rememberUnlockOnDevice = remember;
        if (!remember) {
            rememberedMasterKey = "";
            rememberedMasterKeySalt = "";
        } else if (isUnlocked && hasMasterPassword()) {
            rememberCurrentMasterKey();
        }
        save();
    }

    private static void initializeUnlockState() {
        if (!hasMasterPassword()) {
            MASTER_KEY = LEGACY_FALLBACK_KEY;
            isUnlocked = true;
            updateActivity();
            return;
        }

        isUnlocked = false;
        if (!rememberUnlockOnDevice || rememberedMasterKey.isEmpty() || rememberedMasterKeySalt.isEmpty()) {
            return;
        }

        try {
            String password = EncryptionUtil.decrypt(
                    rememberedMasterKey,
                    getDeviceKey(),
                    rememberedMasterKeySalt
            );
            String hash = EncryptionUtil.hashPassword(password, masterPasswordSalt);
            if (hash.equals(masterPasswordHash)) {
                MASTER_KEY = password;
                isUnlocked = true;
                updateActivity();
            }
        } catch (Exception ignored) {
            // Device key changed or remembered data is stale; require one manual unlock.
        }
    }

    private static void rememberCurrentMasterKey() {
        if (!rememberUnlockOnDevice || !hasMasterPassword() || !isUnlocked) return;
        try {
            rememberedMasterKeySalt = EncryptionUtil.generateSalt();
            rememberedMasterKey = EncryptionUtil.encrypt(
                    MASTER_KEY,
                    getDeviceKey(),
                    rememberedMasterKeySalt
            );
        } catch (Exception e) {
            rememberedMasterKey = "";
            rememberedMasterKeySalt = "";
        }
    }

    private static String getDeviceKey() throws Exception {
        CONFIG_FILE.getParentFile().mkdirs();
        if (DEVICE_KEY_FILE.exists()) {
            return Files.readString(DEVICE_KEY_FILE.toPath(), StandardCharsets.UTF_8).trim();
        }

        String key = EncryptionUtil.generateSalt() + EncryptionUtil.generateSalt();
        Files.writeString(
                DEVICE_KEY_FILE.toPath(),
                key,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
        try {
            Files.setPosixFilePermissions(
                    DEVICE_KEY_FILE.toPath(),
                    PosixFilePermissions.fromString("rw-------")
            );
        } catch (UnsupportedOperationException ignored) {
            // Windows and some filesystems do not expose POSIX permissions.
        }
        return key;
    }

    public static int getRandomDelay() {
        int min = Math.max(0, Math.min(minDelay, maxDelay));
        int max = Math.max(min, Math.max(minDelay, maxDelay));
        if (min == max) return min;

        double mean = (min + max) / 2.0;
        double sigma = Math.max(1.0, (max - min) / 6.0);
        double value = new java.util.Random().nextGaussian() * sigma + mean;
        return (int) Math.max(min, Math.min(max, Math.round(value)));
    }

    public static boolean isSmartModeEnabled() { return smartModeEnabled; }
    public static void setSmartModeEnabled(boolean enabled) { smartModeEnabled = enabled; save(); }
    public static int getMinDelay() { return minDelay; }
    public static void setMinDelay(int delay) { minDelay = delay; save(); }
    public static int getMaxDelay() { return maxDelay; }
    public static void setMaxDelay(int delay) { maxDelay = delay; save(); }
    
    private static void createDefaultConfig() throws Exception {
        globalSalt = EncryptionUtil.generateSalt();
        servers = new HashMap<>();
        MASTER_KEY = LEGACY_FALLBACK_KEY;
        isUnlocked = true;
        updateActivity();
        save();
    }
    
    // Add or update server
    public static void setServer(String ip, String password, boolean isRegisterCommand, boolean autoRegister) {
        try {
            String salt = EncryptionUtil.generateSalt();
            String encrypted = EncryptionUtil.encrypt(password, MASTER_KEY, salt);
            servers.put(ip, new ServerEntry(ip, encrypted, isRegisterCommand, autoRegister, salt));
            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Get server config
    public static ServerEntry getServer(String ip) {
        return servers.get(ip);
    }
    
    // Get decrypted command
    public static String getDecryptedCommand(ServerEntry entry) {
        if (entry == null) return "";
        try {
            return EncryptionUtil.decrypt(entry.encryptedCommand, MASTER_KEY, entry.salt);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    // Remove server
    public static void removeServer(String ip) {
        servers.remove(ip);
        save();
    }
    
    // Get all servers
    public static Map<String, ServerEntry> getAllServers() {
        return new HashMap<>(servers);
    }
    
    // Check if has auto-register enabled
    public static boolean hasAutoRegister(String ip) {
        ServerEntry entry = servers.get(ip);
        return entry != null && entry.autoRegister;
    }
    
    // Set global register mode (apply to all servers)
    public static void setGlobalRegisterMode(boolean enabled) {
        globalRegisterMode = enabled;
        save();
    }
    
    // Get global register mode
    public static boolean isGlobalRegisterMode() {
        return globalRegisterMode;
    }
    
    // Set global password (apply to all servers)
    public static void setGlobalPassword(String password) {
        try {
            if (globalPasswordSalt.isEmpty()) {
                globalPasswordSalt = EncryptionUtil.generateSalt();
            }
            globalPassword = EncryptionUtil.encrypt(password, MASTER_KEY, globalPasswordSalt);
            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Get decrypted global password
    public static String getGlobalPassword() {
        if (globalPassword.isEmpty()) return "";
        try {
            return EncryptionUtil.decrypt(globalPassword, MASTER_KEY, globalPasswordSalt);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    // Check if has global password set
    public static boolean hasGlobalPassword() {
        return !globalPassword.isEmpty();
    }
    
    // Get auto-type enabled status
    public static boolean isAutoTypeEnabled() {
        return autoTypeEnabled;
    }
    
    // Set auto-type enabled status
    public static void setAutoTypeEnabled(boolean enabled) {
        autoTypeEnabled = enabled;
        save();
    }
}
