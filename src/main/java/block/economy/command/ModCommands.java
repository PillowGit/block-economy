package block.economy.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.world.World;

import java.util.Optional;

import static block.economy.BlockEconomy.LOGGER; // Import your mod's logger

public class ModCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Ender Chest Command
            dispatcher.register(CommandManager.literal("e-chest")
                    .executes(context -> {
                        ServerCommandSource src = context.getSource();
                        if (src.getEntity() instanceof ServerPlayerEntity player) {
                            player.openHandledScreen(
                                    new SimpleNamedScreenHandlerFactory((syncid, inv, p) -> {
                                        return GenericContainerScreenHandler.createGeneric9x3(syncid, inv, player.getEnderChestInventory());
                                    }, Text.translatable("container.enderchest"))
                            );
                            return 1;
                        } else {
                            src.sendError(Text.literal("This command can only be used by players"));
                            return 0;
                        }
                    })
            );
            LOGGER.info("Registered /e-chest command.");

            // Buy menu command
            dispatcher.register(CommandManager.literal("building-blocks")
                    .executes(context -> {
                        ServerCommandSource src = context.getSource();
                        if (src.getEntity() instanceof ServerPlayerEntity player) {

                            // Get players currently world (dimension)
                            World world = player.getEntityWorld();
                            LOGGER.info(world.toString());

                            // Make a fake villager for them to trade with
                            VillagerEntity dummy = new VillagerEntity(EntityType.VILLAGER, world);
                            TradeOfferList offerList = new TradeOfferList();
                            LOGGER.info(dummy.toString());

                            // Add each offer
                            offerList.add(
                                    new TradeOffer(
                                            new TradedItem(Items.COBBLESTONE, 8),
                                            Optional.of(new TradedItem(Items.COAL, 1)),
                                            new ItemStack(Items.STONE, 8),
                                            999,
                                            0,
                                            0.5f
                                    )
                            );

                            // Give the dummy villager the trades
                            dummy.setOffers(offerList);

                            // Open the trade screen for the player
                            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                                    (syncid, inv, p) -> new MerchantScreenHandler(syncid, inv, dummy),
                                    Text.literal("Buy Building Blocks")
                            ));

                            // TODO make it fucking work >:(

                            return 1;
                        } else {
                            return 0;
                        }
                    })
            );
        });
    }
}