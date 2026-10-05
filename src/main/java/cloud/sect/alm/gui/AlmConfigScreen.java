package cloud.sect.alm.gui;

import cloud.sect.alm.config.ServerConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class AlmConfigScreen extends Screen {
    private final Screen parent;
    
    // Input Fields
    private EditBox globalPassField;
    private EditBox serverIpField;
    private EditBox serverPassField;
    private EditBox triggerField;
    
    private String statusMessage = "";
    private int messageTimer = 0;
    private boolean forceSecurityScreen = false;

    public AlmConfigScreen(Screen parent) {
        super(Component.literal("§6§lAuto-Login Mod §r§7- §fAdvanced Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (ServerConfig.isLocked() || forceSecurityScreen) {
            initSecurityScreen(centerX, centerY);
        } else {
            initAdvancedUI(centerX, centerY);
        }
    }

    private void applyPasswordMask(EditBox box) {
        try {
            // Try Minecraft 26.1.2+ style (addFormatter with internal interface)
            try {
                Class<?> formatterClass = Class.forName("net.minecraft.client.gui.components.EditBox$TextFormatter");
                Method addFormatter = box.getClass().getMethod("addFormatter", formatterClass);
                
                Object proxy = Proxy.newProxyInstance(
                    formatterClass.getClassLoader(),
                    new Class[]{formatterClass},
                    (p, method, args) -> {
                        if (method.getName().equals("format") || method.getName().equals("method_20911")) {
                            String text = (String) args[0];
                            return FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY);
                        }
                        return null;
                    }
                );
                addFormatter.invoke(box, proxy);
                return;
            } catch (Exception ignored) {}

            // Try Minecraft 1.21.1 style (setFormatter with BiFunction)
            try {
                Method setFormatter = box.getClass().getMethod("setFormatter", BiFunction.class);
                setFormatter.invoke(box, (BiFunction<String, Integer, FormattedCharSequence>) (text, offset) -> 
                    FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY)
                );
            } catch (Exception ignored) {}
        } catch (Throwable t) {
            // Fallback: If reflection fails, we still allow the mod to work without masking
        }
    }

    private void initSecurityScreen(int centerX, int centerY) {
        this.serverPassField = new EditBox(this.font, centerX - 100, centerY - 10, 200, 20, Component.literal("Master Password"));
        applyPasswordMask(this.serverPassField);
        this.addRenderableWidget(this.serverPassField);

        String btnText = ServerConfig.hasMasterPassword() ? "§aUnlock Console" : "§6Set Master Password";
        this.addRenderableWidget(Button.builder(Component.literal(btnText), button -> {
            String pass = this.serverPassField.getValue();
            if (pass.isEmpty()) return;
            if (ServerConfig.hasMasterPassword()) {
                if (ServerConfig.unlock(pass)) {
                    forceSecurityScreen = false;
                    this.init();
                } else showStatus("§cInvalid Master Password!", 60);
            } else {
                if (ServerConfig.setMasterPassword(pass)) {
                    forceSecurityScreen = false;
                    this.init();
                }
            }
        }).bounds(centerX - 100, centerY + 20, 200, 20).build());
    }

    private void initAdvancedUI(int centerX, int centerY) {
        int leftX = centerX - 145;
        int rightX = centerX + 5;
        int startY = 40;

        // --- Column 1: Global Settings & Triggers ---
        
        // Global Password
        this.globalPassField = new EditBox(this.font, leftX, startY + 15, 110, 20, Component.literal("Global Pass"));
        this.globalPassField.setValue(ServerConfig.getGlobalPassword());
        applyPasswordMask(this.globalPassField);
        this.addRenderableWidget(this.globalPassField);
        
        this.addRenderableWidget(Button.builder(Component.literal("§aSave"), b -> {
            ServerConfig.setGlobalPassword(globalPassField.getValue());
            showStatus("§aGlobal Password Saved!", 40);
        }).bounds(leftX + 115, startY + 15, 25, 20).build());

        // Toggles
        String modeText = ServerConfig.isGlobalRegisterMode() ? "§eREGISTER" : "§bLOGIN";
        this.addRenderableWidget(Button.builder(Component.literal("Mode: " + modeText), b -> {
            ServerConfig.setGlobalRegisterMode(!ServerConfig.isGlobalRegisterMode());
            this.init();
        }).bounds(leftX, startY + 40, 140, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Auto-Type: " + (ServerConfig.isAutoTypeEnabled() ? "§aON" : "§cOFF")), b -> {
            ServerConfig.setAutoTypeEnabled(!ServerConfig.isAutoTypeEnabled());
            this.init();
        }).bounds(leftX, startY + 65, 140, 20).build());

        // Triggers Management
        this.triggerField = new EditBox(this.font, leftX, startY + 105, 110, 20, Component.literal("New Trigger"));
        this.triggerField.setHint(Component.literal("§8Add Trigger..."));
        this.addRenderableWidget(this.triggerField);

        this.addRenderableWidget(Button.builder(Component.literal("§e+"), b -> {
            if (!triggerField.getValue().isEmpty()) {
                String val = triggerField.getValue();
                ServerConfig.addCustomTrigger(val);
                showStatus("§aAdded Trigger: §e" + val, 60);
                triggerField.setValue("");
                this.init();
            }
        }).bounds(leftX + 115, startY + 105, 25, 20).build());

        List<String> triggers = ServerConfig.getCustomTriggers();
        for (int i = 0; i < Math.min(triggers.size(), 5); i++) {
            String t = triggers.get(i);
            this.addRenderableWidget(Button.builder(Component.literal("§c✕ §7" + t), b -> {
                ServerConfig.removeCustomTrigger(t);
                showStatus("§eRemoved Trigger: " + t, 60);
                this.init();
            }).bounds(leftX, startY + 130 + (i * 22), 140, 20).build());
        }

        // --- Column 2: Server Nodes ---

        // Add Server
        this.serverIpField = new EditBox(this.font, rightX, startY + 15, 140, 20, Component.literal("Server IP"));
        this.serverIpField.setHint(Component.literal("§8Server IP..."));
        this.addRenderableWidget(this.serverIpField);

        this.serverPassField = new EditBox(this.font, rightX, startY + 40, 140, 20, Component.literal("Server Pass"));
        this.serverPassField.setHint(Component.literal("§8Server Password..."));
        applyPasswordMask(this.serverPassField);
        this.addRenderableWidget(this.serverPassField);

        this.addRenderableWidget(Button.builder(Component.literal("§e[+] Add Server Node"), b -> {
            if (!serverIpField.getValue().isEmpty() && !serverPassField.getValue().isEmpty()) {
                ServerConfig.setServer(serverIpField.getValue(), serverPassField.getValue(), false, false);
                showStatus("§aAdded Server: §e" + serverIpField.getValue(), 60);
                serverIpField.setValue("");
                serverPassField.setValue("");
                this.init();
            }
        }).bounds(rightX, startY + 65, 140, 20).build());

        // Manage Servers List
        Map<String, ServerConfig.ServerEntry> servers = ServerConfig.getAllServers();
        List<String> ipList = new ArrayList<>(servers.keySet());
        for (int i = 0; i < Math.min(ipList.size(), 6); i++) {
            String ip = ipList.get(i);
            this.addRenderableWidget(Button.builder(Component.literal("§c✕ §f" + ip), b -> {
                ServerConfig.removeServer(ip);
                showStatus("§eRemoved Node: " + ip, 60);
                this.init();
            }).bounds(rightX, startY + 105 + (i * 22), 140, 20).build());
        }

        // --- Bottom Controls ---
        String securityText;
        if (!ServerConfig.hasMasterPassword()) {
            securityText = "§eSet Master Password";
        } else {
            securityText = "§bRemember Unlock: " + (ServerConfig.isRememberUnlockOnDevice() ? "§aON" : "§cOFF");
        }

        this.addRenderableWidget(Button.builder(Component.literal(securityText), b -> {
            if (!ServerConfig.hasMasterPassword()) {
                forceSecurityScreen = true;
                this.init();
            } else {
                ServerConfig.setRememberUnlockOnDevice(!ServerConfig.isRememberUnlockOnDevice());
                showStatus(
                    ServerConfig.isRememberUnlockOnDevice()
                        ? "§aThis device will unlock automatically."
                        : "§eRemembered unlock disabled.",
                    60
                );
                this.init();
            }
        }).bounds(centerX - 145, this.height - 35, 140, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c✖ Close & Save"), b -> this.onClose())
            .bounds(centerX + 5, this.height - 35, 140, 20).build());
    }

    private void showStatus(String msg, int ticks) {
        this.statusMessage = msg;
        this.messageTimer = ticks;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        ServerConfig.updateActivity();
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        
        int centerX = this.width / 2;
        context.drawCenteredString(this.font, this.title, centerX, 15, 0xFFAA00);
        
        if (!ServerConfig.isLocked() && !forceSecurityScreen) {
            int leftX = centerX - 145;
            int rightX = centerX + 5;
            int startY = 40;
            
            context.drawString(this.font, "§6§lSYSTEM CONFIG", leftX, startY, 0xFFFFFF);
            context.drawString(this.font, "§7Global Password", leftX, startY + 5, 0xAAAAAA);
            
            context.drawString(this.font, "§d§lDETECTION TRIGGERS", leftX, startY + 95, 0xFFFFFF);
            
            context.drawString(this.font, "§b§lSERVER NODES", rightX, startY, 0xFFFFFF);
            context.drawString(this.font, "§7Add Per-Server Config", rightX, startY + 5, 0xAAAAAA);
            
            context.drawString(this.font, "§fStored Nodes:", rightX, startY + 95, 0xFFFFFF);
        }

        if (messageTimer > 0) {
            context.drawCenteredString(this.font, statusMessage, centerX, this.height - 55, 0xFFFFFF);
            messageTimer--;
        }
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0xCC000000); 
        context.fill(this.width / 2 - 155, 10, this.width / 2 + 155, this.height - 10, 0x22FFFFFF); 
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }
}
