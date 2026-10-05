package cloud.sect.alm.command;

import cloud.sect.alm.AutoLoginModClient;
import cloud.sect.alm.config.ServerConfig;
import cloud.sect.alm.gui.AlmConfigScreen;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class AlmCommand {
    private static final String PREFIX = "§6[Auto-Login] §r";

    private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, com.mojang.brigadier.arguments.ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var root = literal("alm")
                // Open GUI
                .executes(context -> {
                    AutoLoginModClient.LOGGER.info("Opening AlmConfigScreen via command...");
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().setScreen(new AlmConfigScreen(null));
                    });
                    return 1;
                })
                .then(literal("gui")
                    .executes(context -> {
                        AutoLoginModClient.LOGGER.info("Opening AlmConfigScreen via /alm gui...");
                        Minecraft.getInstance().execute(() -> {
                            Minecraft.getInstance().setScreen(new AlmConfigScreen(null));
                        });
                        return 1;
                    })
                )
                // Add new server
                .then(literal("add")
                    .then(argument("ip", StringArgumentType.string())
                        .then(argument("password", StringArgumentType.greedyString())
                            .executes(context -> {
                                String ip = StringArgumentType.getString(context, "ip");
                                String password = StringArgumentType.getString(context, "password");
                                ServerConfig.setServer(ip, password, false, false);
                                context.getSource().sendFeedback(
                                    Component.literal(PREFIX + "§aAdded server: §e" + ip + " §awith §6/login")
                                );
                                return 1;
                            })
                        )
                    )
                )
                // List servers and global settings
                .then(literal("list")
                    .executes(context -> {
                        String globalMode = ServerConfig.isGlobalRegisterMode() ? "§e/register" : "§e/login";
                        String hasGlobal = ServerConfig.hasGlobalPassword() ? "§a[Set]" : "§c[Not Set]";
                        context.getSource().sendFeedback(Component.literal(PREFIX + "§fGlobal: " + globalMode + " " + hasGlobal));
                        
                        if (!ServerConfig.getAllServers().isEmpty()) {
                            context.getSource().sendFeedback(Component.literal(PREFIX + "§fPer-Server Settings:"));
                            ServerConfig.getAllServers().forEach((ip, entry) -> {
                                String type = entry.isRegisterCommand ? "§e[Reg]" : "§e[Login]";
                                context.getSource().sendFeedback(
                                    Component.literal("  " + type + " §7" + ip)
                                );
                            });
                        }
                        return 1;
                    })
                )
                // Remove server
                .then(literal("remove")
                    .then(argument("ip", StringArgumentType.greedyString())
                        .executes(context -> {
                            String ip = StringArgumentType.getString(context, "ip");
                            ServerConfig.removeServer(ip);
                            context.getSource().sendFeedback(
                                Component.literal(PREFIX + "§cRemoved server: §7" + ip)
                            );
                            return 1;
                        })
                    )
                )
                // Set global login mode
                .then(literal("set")
                    .then(argument("password", StringArgumentType.greedyString())
                        .executes(context -> {
                            String password = StringArgumentType.getString(context, "password");
                            ServerConfig.setGlobalPassword(password);
                            ServerConfig.setGlobalRegisterMode(false);
                            context.getSource().sendFeedback(
                                Component.literal(PREFIX + "§aSet global §6/login §apassword for all servers")
                            );
                            return 1;
                        })
                    )
                )
                // Trigger Management
                .then(literal("trigger")
                    .then(literal("add")
                        .then(argument("word", StringArgumentType.string())
                            .executes(context -> {
                                String word = StringArgumentType.getString(context, "word");
                                ServerConfig.addCustomTrigger(word);
                                context.getSource().sendFeedback(
                                    Component.literal(PREFIX + "§aAdded custom trigger: §e" + word)
                                );
                                return 1;
                            })
                        )
                    )
                    .then(literal("remove")
                        .then(argument("word", StringArgumentType.string())
                            .executes(context -> {
                                String word = StringArgumentType.getString(context, "word");
                                ServerConfig.removeCustomTrigger(word);
                                context.getSource().sendFeedback(
                                    Component.literal(PREFIX + "§cRemoved custom trigger: §7" + word)
                                );
                                return 1;
                            })
                        )
                    )
                    .then(literal("list")
                        .executes(context -> {
                            context.getSource().sendFeedback(Component.literal(PREFIX + "§fCustom Triggers:"));
                            ServerConfig.getCustomTriggers().forEach(t -> {
                                context.getSource().sendFeedback(Component.literal("  §e- " + t));
                            });
                            return 1;
                        })
                    )
                )
                // Help
                .then(literal("help")
                    .executes(context -> {
                        context.getSource().sendFeedback(Component.literal("§6§l=== Auto-Login Mod Help ==="));
                        context.getSource().sendFeedback(Component.literal("§e/alm gui §7- Open configuration screen"));
                        context.getSource().sendFeedback(Component.literal("§e/alm set <pass> §7- Global /login"));
                        context.getSource().sendFeedback(Component.literal("§e/alm add <ip> <pass> §7- Per-server /login"));
                        context.getSource().sendFeedback(Component.literal("§e/alm trigger <add|remove|list> §7- Manage triggers"));
                        context.getSource().sendFeedback(Component.literal("§e/alm list §7- Show all settings"));
                        context.getSource().sendFeedback(Component.literal("§6Hotkeys: §fF9 §7(Open Settings)"));
                        context.getSource().sendFeedback(Component.literal("§7Master Password is optional; remembered unlock keeps Auto Login automatic."));
                        return 1;
                    })
                );
            
            dispatcher.register(root);
            
            // Register /autologinmod as an alias
            var alias = literal("autologinmod")
                .redirect(dispatcher.register(root));
            dispatcher.register(alias);
        });
    }
}
