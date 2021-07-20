package com.plusls.llsmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.plusls.llsmanager.LlsManager;
import com.plusls.llsmanager.data.LlsPlayer;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class LlsLoginCommand {

    private static LlsManager llsManager;

    public static void register(LlsManager llsManager) {
        llsManager.commandManager.register(createBrigadierCommand(llsManager));
    }

    private static BrigadierCommand createBrigadierCommand(LlsManager llsManager) {
        LlsLoginCommand.llsManager = llsManager;
        LiteralCommandNode<CommandSource> llsLoginNode = LiteralArgumentBuilder
                .<CommandSource>literal("lls_login").requires(
                        commandSource -> commandSource instanceof Player player &&
                                llsManager.players.get(player.getRemoteAddress()).status != LlsPlayer.Status.LOGGED_IN
                )
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("password", StringArgumentType.string())
                        .executes(LlsLoginCommand::llsLogin)).build();
        return new BrigadierCommand(llsLoginNode);
    }

    private static int llsLogin(CommandContext<CommandSource> context) {
        Player player = (Player) context.getSource();
        LlsPlayer llsPlayer = llsManager.players.get(player.getRemoteAddress());
        String password = context.getArgument("password", String.class);
        if (!BCrypt.checkpw(password, llsPlayer.getPassword())) {
            context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_login.password_error", NamedTextColor.RED));
            return 0;
        }
        password = BCrypt.hashpw(password, BCrypt.gensalt());
        llsPlayer.status = LlsPlayer.Status.LOGGED_IN;
        llsPlayer.setPassword(password);
        context.getSource().sendMessage(Component.translatable("lls-manager.command.lls_login.success", NamedTextColor.GREEN));
        Optional<RegisteredServer> registeredServerOptional = llsManager.server.getServer(llsPlayer.getLastServerName());
        registeredServerOptional.ifPresent(registeredServer -> player.createConnectionRequest(registeredServer).connect());
        return 1;
    }
}