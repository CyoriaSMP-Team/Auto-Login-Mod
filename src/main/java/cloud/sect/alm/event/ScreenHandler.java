package cloud.sect.alm.event;

import cloud.sect.alm.config.ServerConfig;
import cloud.sect.alm.util.AuthDetector;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public class ScreenHandler {
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (ServerConfig.isLocked()
                    || !ServerConfig.isSmartModeEnabled()
                    || !ServerConfig.isAutoTypeEnabled()
                    || ServerConfig.isSessionLoggedIn()) return;

            String title = screen.getTitle().getString();
            AuthDetector.AuthType type = AuthDetector.detect(title);
            
            if (type == AuthDetector.AuthType.LOGIN || type == AuthDetector.AuthType.REGISTER) {
                boolean isRegister = (type == AuthDetector.AuthType.REGISTER);
                if (ServerConfig.beginAuthAttempt()) handleScreenAuth(client, isRegister);
            }
        });
    }

    private static void handleScreenAuth(Minecraft client, boolean isRegister) {
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

        final String finalPassword = password;
        final int delay = ServerConfig.getRandomDelay();
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                client.execute(() -> {
                    try {
                        if (client.player != null) {
                            String cmd = isRegister ? "register " + finalPassword + " " + finalPassword : "login " + finalPassword;
                            client.player.connection.sendCommand(cmd);
                            client.player.sendSystemMessage(
                                Component.literal("§6[Auto-Login] §aDetected GUI and sent §e/" + (isRegister ? "register" : "login") + " §8(Delay: " + delay + "ms)")
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
