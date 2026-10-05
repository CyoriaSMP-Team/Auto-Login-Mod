package cloud.sect.alm.event;

import cloud.sect.alm.config.ServerConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PlayerJoinHandler {
    private static final String PREFIX = "§6[Auto-Login] §r";
    
    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerConfig.resetSession();
            client.execute(() -> {
                if (client.player == null) return;
                
                String serverIP = getCurrentServerIP(client);
                ServerConfig.ServerEntry entry = ServerConfig.getServer(serverIP);
                
                // Greeting and Status Check
                if (ServerConfig.isLocked()) {
                    client.player.sendSystemMessage(
                        Component.literal(PREFIX + "§c§lDatabase Locked! §7Press §eF9 §7to enter Master Password.")
                    );
                    return;
                }

                if (entry != null) {
                    String type = entry.isRegisterCommand ? "§e/register" : "§e/login";
                    client.player.sendSystemMessage(
                        Component.literal(PREFIX + "§7Active config: " + type + " §8(Per-Server)")
                    );
                } else if (ServerConfig.hasGlobalPassword()) {
                    String type = ServerConfig.isGlobalRegisterMode() ? "§e/register" : "§e/login";
                    client.player.sendSystemMessage(
                        Component.literal(PREFIX + "§7Active config: " + type + " §8(Global)")
                    );
                } else {
                    client.player.sendSystemMessage(
                        Component.literal(PREFIX + "§c§lNot Configured! §7Use §e/alm gui §7to set a password.")
                    );
                    return;
                }
                
                // Auto login is prompt-driven by default. This avoids racing the auth plugin
                // and prevents JOIN + chat/GUI detectors from sending duplicate commands.
                if (!ServerConfig.isAutoTypeEnabled()) {
                    client.player.sendSystemMessage(
                        Component.literal(PREFIX + "§8Auto-login is disabled. Press §fF9 §8to open settings.")
                    );
                    return;
                }

                if (ServerConfig.isSmartModeEnabled()) {
                    client.player.sendSystemMessage(
                        Component.literal(PREFIX + "§aReady. §7Waiting for the server login prompt...")
                    );
                    return;
                }
                
                // Fallback mode for servers that never send a detectable auth prompt.
                String password;
                boolean isRegisterCommand;
                
                if (entry != null) {
                    password = ServerConfig.getDecryptedCommand(entry);
                    isRegisterCommand = entry.isRegisterCommand;
                } else {
                    password = ServerConfig.getGlobalPassword();
                    isRegisterCommand = ServerConfig.isGlobalRegisterMode();
                }
                
                if (password.isEmpty()) return;
                
                if (!ServerConfig.beginAuthAttempt()) return;

                final int delay = ServerConfig.getRandomDelay();
                client.player.sendSystemMessage(
                    Component.literal(PREFIX + "§aAuto-typing login in §e" + delay + "ms§a...")
                );
                
                final String finalPassword = password;
                final boolean finalIsRegister = isRegisterCommand;

                new Thread(() -> {
                    try {
                        Thread.sleep(delay);
                        client.execute(() -> {
                            try {
                                if (client.player != null) {
                                    String cmd = finalIsRegister ? "register " + finalPassword + " " + finalPassword : "login " + finalPassword;
                                    client.player.connection.sendCommand(cmd);
                                    client.player.sendSystemMessage(
                                        Component.literal(PREFIX + "§aFallback auto-sent §e/" + (finalIsRegister ? "register" : "login"))
                                    );
                                }
                            } finally {
                                ServerConfig.finishAuthAttempt();
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        ServerConfig.finishAuthAttempt();
                    }
                }).start();
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerConfig.resetSession();
            ServerConfig.updateActivity();
        });
    }
    
    private static String getCurrentServerIP(Minecraft client) {
        if (client.getCurrentServer() != null) {
            return client.getCurrentServer().ip;
        }
        return "singleplayer";
    }
}
