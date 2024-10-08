package com.github.lunatrius.schematica.command;

import com.github.lunatrius.core.util.FileUtils;
import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.handler.DownloadHandler;
import com.github.lunatrius.schematica.network.transfer.SchematicTransfer;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.util.FileFilterSchematic;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentTranslation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
public class CommandSchematicaDownload extends CommandSchematicaBase {
    private static final FileFilterSchematic FILE_FILTER_SCHEMATIC = new FileFilterSchematic(false);

    @Override
    public String getCommandName() {
        return Names.Command.Download.NAME;
    }

    @Override
    public String getCommandUsage(final ICommandSender sender) {
        return Names.Command.Download.Message.USAGE;
    }

    @Override
    public List<String> addTabCompletionOptions(final ICommandSender sender, final String[] args, final BlockPos pos) {
        if (!(sender instanceof EntityPlayer)) {
            return Collections.emptyList();
        }

        final File directory = Schematica.proxy.getPlayerSchematicDirectory((EntityPlayer) sender, true);
        final File[] files = directory.listFiles(FILE_FILTER_SCHEMATIC);

        if (files != null) {
            final List<String> filenames = new ArrayList<String>();

            for (final File file : files) {
                filenames.add(file.getName());
            }

            return getListOfStringsMatchingLastWord(args, filenames);
        }

        return Collections.emptyList();
    }

    @Override
    public void processCommand(final ICommandSender sender, final String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException(Names.Command.Download.Message.PLAYERS_ONLY);
        }

        final String filename = String.join(" ", args);
        final EntityPlayerMP player = (EntityPlayerMP) sender;
        final File directory = Schematica.proxy.getPlayerSchematicDirectory(player, true);
        try {
            if (!FileUtils.contains(directory, filename)) {
                Reference.logger.error("{} has tried to download the file {}", player.getName(), filename);
                throw new CommandException(Names.Command.Download.Message.DOWNLOAD_FAILED);
            }
        }
        catch (IOException ex) {
            Reference.logger.error("An unknown error occurred when {} tried to download the file {}", player.getName(), filename, ex);
            throw new CommandException(Names.Command.Download.Message.DOWNLOAD_FAILED);
        }

        final ISchematic schematic = SchematicFormat.readFromFile(directory, filename);

        if (schematic != null) {
            DownloadHandler.INSTANCE.transferMap.put(player, new SchematicTransfer(schematic, filename));
            sender.addChatMessage(new ChatComponentTranslation(Names.Command.Download.Message.DOWNLOAD_STARTED, filename));
        } else {
            throw new CommandException(Names.Command.Download.Message.DOWNLOAD_FAILED);
        }
    }
}
