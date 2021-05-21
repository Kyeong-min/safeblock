package me.hatena.project.safeblock.commands;

import com.flowpowered.math.vector.Vector3i;
import me.hatena.project.safeblock.Safeblock;
import me.hatena.project.safeblock.UserManager;
import me.hatena.project.safeblock.commands.customargs.Vector3iCommandElement;
import me.hatena.project.safeblock.database.entities.BLOCKLOG;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;

import java.util.*;

public class CheckLocationCommand extends BaseCommand {

    public final static String SUBPERMISSION = "chkloc";
    public final static String[] ALIAS = {"loc", "location"};

    public CheckLocationCommand() {
        super(ALIAS, "해당 위치를 조사합니다.", SUBPERMISSION);

        elementList = new ArrayList<>();
        elementList.add(new Vector3iCommandElement(Text.of("location")));
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            src.sendMessage(Text.of("Player only command."));
            return CommandResult.empty();
        }

        Player player = (Player)src;
        Optional<Vector3i> optVec = args.<Vector3i>getOne("location");
        if (!optVec.isPresent()) {
            src.sendMessage(Text.of("좌표 정보가 없습니다."));
            return CommandResult.empty();
        }
        Vector3i vec = optVec.get();
        Safeblock.getInstance().checkAndSendMessage(vec, player);

        return CommandResult.success();
    }
}
