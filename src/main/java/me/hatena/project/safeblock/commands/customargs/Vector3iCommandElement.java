package me.hatena.project.safeblock.commands.customargs;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import java.util.Collections;
import java.util.List;

public class Vector3iCommandElement extends CommandElement {

    public Vector3iCommandElement(Text key) {
        super(key);
    }

    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {

        String xInput = args.next();
        int x = parseInt(xInput, args);

        String yInput = args.next();
        int y = parseInt(yInput, args);

        String zInput = args.next();
        int z = parseInt(zInput, args);

        return new Vector3i(x, y, z);
    }

    private int parseInt(String input, CommandArgs args) throws ArgumentParseException {
        try {
            return Integer.parseInt(input);
        } catch(NumberFormatException e) {
            throw args.createError(Text.of("'" + input + "' is not a valid number!"));
        }
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return Collections.emptyList();
    }

    @Override
    public Text getUsage(CommandSource src) {
        return Text.of("<x> <y> <z>");
    }
}
