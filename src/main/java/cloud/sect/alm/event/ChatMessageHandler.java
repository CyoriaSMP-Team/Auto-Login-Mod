package cloud.sect.alm.event;

import cloud.sect.alm.config.ServerConfig;
import cloud.sect.alm.util.AuthDetector;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatMessageHandler {
    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;
            
            String text = message.getString();
            if (ServerConfig.isLocked()
                    || !ServerConfig.isSmartModeEnabled()
                    || !ServerConfig.isAutoTypeEnabled()) return;
            
            AuthDetector.AuthType type = AuthDetector.detect(text);
            
            if (type == AuthDetector.AuthType.SUCCESS) {
                ServerConfig.setSessionLoggedIn(true);
                return;
            }

            if (ServerConfig.isSessionLoggedIn()) return;
            
            if (type == AuthDetector.AuthType.REGISTER) {
                if (ServerConfig.beginAuthAttempt()) handleAutoResponse(client, true);
            } else if (type == AuthDetector.AuthType.LOGIN) {
                if (ServerConfig.beginAuthAttempt()) handleAutoResponse(client, false);
            }
        });
    }
    
    private static void handleAutoResponse(Minecraft client, boolean forceRegister) {
        String serverIP = getCurrentServerIP(client);
        ServerConfig.ServerEntry entry = ServerConfig.getServer(serverIP);
        
        String password;
        
        if (entry != null) {
            password = ServerConfig.getDecryptedCommand(entry);
        } else if (ServerConfig.hasGlobalPassword()) {
            password = ServerConfig.getGlobalPassword();
        } else {
            ServerConfig.finishAuthAttempt();
            return;
        }
        
        if (password == null || password.isEmpty()) {
            ServerConfig.finishAuthAttempt();
            return;
        }
        
        final boolean finalIsRegister = forceRegister;
        final String finalPassword = password;
        final int delay = ServerConfig.getRandomDelay();
        
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                client.execute(() -> {
                    try {
                        if (client.player != null) {
                            String cmd = finalIsRegister ? "register " + finalPassword + " " + finalPassword : "login " + finalPassword;
                            client.player.connection.sendCommand(cmd);
                            client.player.sendSystemMessage(
                                Component.literal("§6[Auto-Login] §aDetected and sent §e/" + (finalIsRegister ? "register" : "login") + " §8(Delay: " + delay + "ms)")
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
    }
    
    private static String getCurrentServerIP(Minecraft client) {
        if (client.getCurrentServer() != null) {
            return client.getCurrentServer().ip;
        }
        return "singleplayer";
    }
}
