package block.economy;

import net.fabricmc.api.ModInitializer;

// Import commands
import block.economy.command.ModCommands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockEconomy implements ModInitializer {
	public static final String MOD_ID = "block-economy";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Register commands
		ModCommands.registerCommands();
	}
}