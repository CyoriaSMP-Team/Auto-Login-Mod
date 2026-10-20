package cloud.sect.alm.keybinding;

import cloud.sect.alm.gui.AlmConfigScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class LoginKeybinding {
    private static KeyMapping configKeyMapping;

    public static void register() {
        try {
            configKeyMapping = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("alm.key.config", InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_F9, KeyMapping.Category.MISC)
            );
            cloud.sect.alm.AutoLoginModClient.LOGGER.info("F9 Keybinding registered.");
        } catch (Exception e) {
            cloud.sect.alm.AutoLoginModClient.LOGGER.error("Keybinding registration failed!", e);
        }
    }

    public static void checkKeybindings(Minecraft client) {
        try {
            if (configKeyMapping != null && configKeyMapping.consumeClick()) {
                client.setScreen(new AlmConfigScreen(null));
            }
        } catch (Throwable ignored) {}
    }
}
