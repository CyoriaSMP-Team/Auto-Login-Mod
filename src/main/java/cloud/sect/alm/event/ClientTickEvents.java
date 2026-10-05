package cloud.sect.alm.event;

import cloud.sect.alm.keybinding.LoginKeybinding;

public class ClientTickEvents {
    public static void register() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                LoginKeybinding::checkKeybindings
        );
    }
}
