package block.economy.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.*;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static block.economy.BlockEconomy.LOGGER;

public class ModCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Sell Items Command
            dispatcher.register(CommandManager.literal("sell")
                    .executes(context -> {
                        ServerCommandSource src = context.getSource();
                        // If person using command is player
                        if (src.getEntity() instanceof ServerPlayerEntity player) {
                            ServerWorld world = player.getServerWorld();

                            // Make a villager
                            VillagerEntity newVillager = new VillagerEntity(
                                    net.minecraft.entity.EntityType.VILLAGER,
                                    world
                            );

                            // Position it where we want, make it invisible
                            Vec3d pos = player.getPos();
                            newVillager.setPosition(pos.x, pos.y, pos.z);
                            newVillager.setSilent(true);
                            newVillager.setNoGravity(true);
                            newVillager.setAiDisabled(true);
                            newVillager.setInvulnerable(true);
                            newVillager.addStatusEffect(new StatusEffectInstance(
                                    StatusEffects.INVISIBILITY,
                                    StatusEffectInstance.INFINITE,
                                    0,
                                    false,
                                    false,
                                    false
                            ));

                            // Give it type info so it's a valid merchant
                            newVillager.setVillagerData(newVillager.getVillagerData()
                                    .withType(Registries.VILLAGER_TYPE.getOrThrow(VillagerType.SNOW))
                                    .withProfession(Registries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.ARMORER))
                                    .withLevel(5));

                            // Spawn it in the world
                            world.spawnEntity(newVillager);

                            // Custom Trades
                            TradeOfferList offers = new TradeOfferList();

                            List<TradedItem> forEmerald = new ArrayList<>();
                            forEmerald.add(new TradedItem(Items.PAPER, 12));
                            forEmerald.add(new TradedItem(Items.CARROT, 7));
                            forEmerald.add(new TradedItem(Items.WHEAT_SEEDS, 32));
                            forEmerald.add(new TradedItem(Items.WHEAT, 5));
                            forEmerald.add(new TradedItem(Items.WHITE_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.POTATO, 5));
                            forEmerald.add(new TradedItem(Items.PUMPKIN, 1));
                            forEmerald.add(new TradedItem(Items.BLACK_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.RED_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.BROWN_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.ORANGE_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.MAGENTA_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.LIGHT_BLUE_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.YELLOW_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.LIME_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.PINK_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.GRAY_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.LIGHT_GRAY_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.CYAN_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.PURPLE_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.BLUE_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.GREEN_WOOL, 4));

                            for (TradedItem tradedItem : forEmerald) {
                                offers.add(new TradeOffer(
                                        tradedItem,
                                        new ItemStack(Items.EMERALD, 1),
                                        99999,
                                        0,
                                        0.05f
                                ));
                            }

                            // Sync villager trade info
                            newVillager.setOffers(offers);
                            newVillager.setCustomer(player);

                            // Open a screen handler for the villager on the merchant side
                            OptionalInt optionalInt = player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                                    ((syncId, playerInventory, p) -> new MerchantCleanupScreen(syncId, playerInventory, newVillager, (ServerWorld) player.getServerWorld())),
                                    Text.translatable("Sell")
                            ));
                            // If screen opened, use player to send to merchant as well
                            if (optionalInt.isPresent()) {
                                TradeOfferList merchantOffers = newVillager.getOffers();
                                if (!merchantOffers.isEmpty()) {
                                    player.sendTradeOffers(optionalInt.getAsInt(), merchantOffers, 5, newVillager.getExperience(), newVillager.isLeveledMerchant(), newVillager.canRefreshTrades());
                                }
                            }
                            return 1;
                        } else {
                            src.sendError(Text.literal("This command can only be used by players"));
                            return 0;
                        }
                    })
            );
            LOGGER.info("Finished registering commands for block-economy!");
        });
    }
}
