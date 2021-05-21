package me.hatena.project.safeblock.commands;

import me.hatena.project.safeblock.UserManager;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CheckCommand extends BaseCommand {

    public final static String SUBPERMISSION = "chk";
    public final static String[] ALIAS = {"check"};

    private static final String start = "start";
    private static final String stop = "stop";

    public CheckCommand() {
        super(ALIAS, "조사를 시작합니다.", SUBPERMISSION);

        Map<String, String> arg = new HashMap<String, String>() {{
            put(start, start);
            put(stop, stop);
        }};

        elementList = new ArrayList<>();
        elementList.add(GenericArguments.choices(Text.of("arg"), arg));
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        String uuid = getCommandSourceUuid(src);
        Optional<String> optArg = args.<String>getOne("arg");
        if (!optArg.isPresent()) {
            src.sendMessage(Text.of("매게변수는 start 혹은 stop 입니다."));
            return CommandResult.empty();
        }
        String arg = optArg.get();
        UserManager um = UserManager.getInstance();
        if (arg.equals(start)) {
            if (um.isChecking(uuid)) {
                src.sendMessage(Text.of("이미 조사모드입니다."));
                return CommandResult.empty();
            }

            um.checkStart(uuid);
            src.sendMessage(Text.of("조사모드를 시작합니다."));
            return CommandResult.success();
        } else if (arg.equals(stop)) {
            if (!um.isChecking(uuid)) {
                src.sendMessage(Text.of("조사모드가 아닙니다."));
                return CommandResult.empty();
            }

            um.checkStop(uuid);
            src.sendMessage(Text.of("조사모드를 종료합니다."));
            return CommandResult.success();
        } else {
            return CommandResult.empty();
        }
    }
}
