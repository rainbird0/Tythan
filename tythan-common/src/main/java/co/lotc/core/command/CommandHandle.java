package co.lotc.core.command;

import co.lotc.core.agnostic.Sender;
import net.md_5.bungee.api.chat.BaseComponent;

public interface CommandHandle {
	
	/**
	 * @return whoever issued the command, which may not be the Player/Persona target
	 */
	Sender getSender();
	
	/**
	 * Send a formatted message to the command issuer
	 * @param message The message in String.format / printf compliant style
	 * @param format the formatting parameters
	 */
	void msg(String message, Object... format);
	
	void msg(Object message);
	
	/**
	 * Send a Json-formatted message to the command issuer (Json features only work on players)
	 * @param message The message in a bungee BaseComponent format
	 */
	void msg(BaseComponent message);
	
	/**
	 * Send something to the command issuer
	 * @param message an unformatted string message
	 */
	void msgRaw(String message);
	
	/**
	 * Terminate the command sequence execution immediately in a way that the command engine understands
	 * @param err an error message that will be formatted and provided to the command sender
	 */
	void error(String err);
	
	/**
	 * Shorthand method to simplify the command flow. Require something is true or terminate with an error message otherwise.
	 * @param condition something that you require to be true at this point in the command sugar
	 * @param error the error message the commandSender will see if the condition fails
	 */
	void validate(boolean condition, String error);
	 
	/**
	 * @param flagName The Flag Name to check
	 * @return if the Flag exists or not
	 */
	boolean hasFlag(String flagName);
	
	/**
	 * @param flagName the Flag Name to check
	 * @return The argument sender provided with the flag, or some default
	 */
	<T> T getFlag(String flagName);
}
