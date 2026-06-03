package com.aearost.aranarthcore.commands.general;

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.skills.SkillTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class CommandMctopCompleter implements TabCompleter {

    // All non-child primary skills plus "overall", loaded from mcMMO's enum at class-load time.
    private static final List<String> SKILLS;
    static {
        List<String> skills = new java.util.ArrayList<>();
        skills.add("overall");
        Arrays.stream(PrimarySkillType.values())
                .filter(skill -> !SkillTools.isChildSkill(skill))
                .map(PrimarySkillType::name)
                .forEach(skills::add);
        SKILLS = java.util.Collections.unmodifiableList(skills);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player)) {
			return List.of();
		}

        if (args.length == 1) {
            String prefix = args[0].toUpperCase();
            return SKILLS.stream().filter(s -> s.startsWith(prefix)).toList();
        }

        return List.of();
    }

}
