package me.hatena.project.safeblock.commands;

import lombok.Getter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.List;

public abstract class BaseCommand implements CommandExecutor {

    /**
     * Command base permission string
     */
    public final static String PERMISSION_BASE = "hatena.safeblock";

    @Getter
    protected String[] alias;

    @Getter
    protected Text description;

    protected List<CommandElement> elementList;

    /**
     * Full permission string
     */
    @Getter
    protected String permission;

    /**
     * Make full permission string
     *
     * @param subPermission sub permission string
     * @return full permission string
     */
    private String makePermissionString(String subPermission) {
        StringBuilder sb = new StringBuilder();
        sb.append(BaseCommand.PERMISSION_BASE).append('.').append(subPermission);
        return sb.toString();
    }

    /**
     * CTor
     *
     * @param alias         trigger string array
     * @param description   description
     * @param subPermission sub permission
     */
    protected BaseCommand(String[] alias, String description, String subPermission) {
        this.alias = alias;
        this.description = TextSerializers.FORMATTING_CODE.deserialize(description);
        this.permission = makePermissionString(subPermission);
    }

    /**
     * CTor
     *
     * @param alias         trigger string
     * @param description   description
     * @param subPermission sub permission
     */
    protected BaseCommand(String alias, String description, String subPermission) {
        String[] a = new String[1];
        a[0] = alias;

        this.alias = a;
        this.description = TextSerializers.FORMATTING_CODE.deserialize(description);
        this.permission = makePermissionString(subPermission);
    }

    /**
     * Get source uuid from {@link CommandSource}
     *
     * @param src {@link CommandSpec}
     * @return source uuid
     */
    protected String getCommandSourceUuid(CommandSource src) {
        if (src instanceof Player) {
            Player admin = (Player) src;
            return admin.getUniqueId().toString();
        } else {
            return null;
        }
    }

    /**
     * Build self
     *
     * @return {@link CommandSpec}
     */
    public CommandSpec buildSelf() {
        CommandSpec.Builder builder = CommandSpec.builder()
                .description(description)
                .permission(permission);

        if (this.elementList != null && this.elementList.size() > 0) {
            builder.arguments(this.elementList.toArray(new CommandElement[this.elementList.size()]));
        }

        return builder.executor(this).build();
    }
}