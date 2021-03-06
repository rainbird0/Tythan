package co.lotc.core.command;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;

import co.lotc.core.CoreLog;
import co.lotc.core.agnostic.Sender;
import co.lotc.core.command.RanCommand.CmdParserException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AgnosticExecutor{
	private final ArcheCommand rootCommand;
	
	public boolean onCommand(Sender sender, String label, String[] args) {
		List<String> listArgs = new ArrayList<>();
		Collections.addAll(listArgs, args);
		runCommand(sender, rootCommand, label, listArgs);
		return true;
	}
	
	public List<String> onTabComplete(Sender sender, String alias, String[] args) {
		try {
			List<String> listArgs = new ArrayList<>(Arrays.asList(args));
			return getCompletions(sender, rootCommand, listArgs);
		} catch(ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return Lists.newArrayList();
		}
	}
	
	private List<String> getCompletions(Sender sender, ArcheCommand command, List<String> args) {
		ArcheCommand subCommand = wantsSubCommand(command, args);
		if(subCommand != null && subCommand.hasPermission(sender)) {
			args.remove(0);
			return getCompletions(sender, subCommand, args);
		} else {
			List<String> options;
			if(args.isEmpty()) return Lists.newArrayList();
			int index = args.size() - 1;
			String last = args.get(index).toLowerCase();
			if(args.size() == 1) options = subCompletions(sender, command, last);
			else options = new ArrayList<>();
			
			if(index < command.getArgs().size())
				command.getArgs()
				.get(index)
				.getCompleter()
				.suggest(sender, args.get(index))
				.forEach(sugg->options.add(sugg.getLiteral()));
			
			return options.stream().filter(s->s.toLowerCase().startsWith(last)).collect(Collectors.toList());
		}
	}

	private void runCommand(Sender sender, ArcheCommand command, String usedAlias, List<String> args) {
		ArcheCommand subCommand = wantsSubCommand(command, args);
		CoreLog.debug("catching alias " + usedAlias + ". SubCommand found: " + subCommand);
		if(!args.isEmpty()) CoreLog.debug("These are its arguments: " + StringUtils.join(args, ", "));
		if(subCommand != null) {
			runSubCommand(sender, subCommand, usedAlias, args);
		} else if (!command.hasPermission(sender)) {
			sender.sendMessage(RanCommand.ERROR_PREFIX + "You do not have permission to use this");
		} else {
			RanCommand c = new RanCommand(command, usedAlias, sender);
			
			try{
				c.parseAll(args);
			} catch(Exception e) {
				c.handleException(e);
				return;
			}
			
			HelpCommand help = command.getHelp();
			if(help != null && c.hasFlag("h")) {
				help.runHelp(c, c.getFlag("h"));
			} else {
				executeCommand(command, c);
			}
		}
	}
	
	private void runSubCommand(Sender sender, ArcheCommand subCommand, String usedAlias, List<String> args) {
		if(subCommand.isInvokeOverload()) {
			runCommand(sender, subCommand, usedAlias, args);
		} else {
			String usedSubcommandAlias = args.remove(0).toLowerCase();
			String newAlias = usedAlias + ' ' + usedSubcommandAlias;
			runCommand(sender, subCommand, newAlias, args);
		}
	}
	
	private void executeCommand(ArcheCommand command, RanCommand c) {
		try {
			command.execute(c); //TODO will this respect permissions even with invoke()??
		} catch(CmdParserException e) {
			c.handleException(e);
		}
	}
	
	private List<String> subCompletions(Sender sender, ArcheCommand cmd, String argZero){
		List<String> result = new ArrayList<>();
		String lower = argZero.toLowerCase();
		cmd.getSubCommands().stream()
			.filter(s->s.hasPermission(sender))
			.map(s->s.getBestAlias(lower))
			.filter(Objects::nonNull)
			.forEach(result::add);

		return result;
	}
	
	private ArcheCommand wantsSubCommand(ArcheCommand cmd, List<String> args) {
		
		List<ArcheCommand> matches = new ArrayList<>();

		//Check if one of the overloads matches better based on arg count
		cmd.getSubCommands().stream()
		  .filter(ArcheCommand::isInvokeOverload)
		  .filter(ac->ac.fitsArgSize(args.size()))
		  .forEach(matches::add);
		
		if(matches.size() > 1) throw new IllegalStateException("Invoke overload ambiguity: Same arg size took multiple args!!");
		else if(matches.size() == 1) return matches.get(0);
			
		//Otherwise, Check for literals that match a subcommand
		if(args.isEmpty()) return null;
		String subArg = args.get(0).toLowerCase();
		
		cmd.getSubCommands().stream()
			.filter(s->!s.getMainCommand().isEmpty())
		  .filter(s->s.isAlias(subArg))
		  .forEach(matches::add);
		if(matches.isEmpty()) return null;
		else if(matches.size() == 1) return matches.get(0);
		
		int s = args.size() - 1;
		for(ArcheCommand match : matches) {
			if(match.fitsArgSize(s)) return match;
		}
		
		//Fallback, no subcommand wants the amount of given arguments
		return null;
	}


	
}
