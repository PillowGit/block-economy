package block.economy.command;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.ComponentType;
import net.minecraft.data.DataOutput;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.*;
import net.minecraft.registry.Registries;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

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

            // Buy Lights Command
            dispatcher.register(CommandManager.literal("buy-lights")
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

                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GLOWSTONE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SHROOMLIGHT, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.OCHRE_FROGLIGHT, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.VERDANT_FROGLIGHT, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PEARLESCENT_FROGLIGHT, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.END_ROD, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.TORCH, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SOUL_TORCH, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LANTERN, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SOUL_LANTERN, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SEA_LANTERN, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.REDSTONE_LAMP, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.CAMPFIRE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SOUL_CAMPFIRE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));

                            // Sync villager trade info
                            newVillager.setOffers(offers);
                            newVillager.setCustomer(player);

                            // Open a screen handler for the villager on the merchant side
                            OptionalInt optionalInt = player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                                    ((syncId, playerInventory, p) -> new MerchantCleanupScreen(syncId, playerInventory, newVillager, (ServerWorld) player.getServerWorld())),
                                    Text.translatable("Light Blocks")
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

            // Buy Blocks Command
            dispatcher.register(CommandManager.literal("buy-blocks")
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

                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 4),
                                    new ItemStack(Items.STONECUTTER, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SLIME_BLOCK, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.HONEY_BLOCK, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.QUARTZ, 6),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GLASS, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ICE, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PACKED_ICE, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLUE_ICE, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.COBBLESTONE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.STONE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.COBBLED_DEEPSLATE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.DEEPSLATE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.DIORITE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ANDESITE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GRANITE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.CALCITE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.TUFF, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLACKSTONE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SANDSTONE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.RED_SANDSTONE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PRISMARINE, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.NETHERRACK, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BRICKS, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MUD_BRICKS, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.RESIN_BRICKS, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PRISMARINE_BRICKS, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.NETHER_BRICKS, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.END_STONE_BRICKS, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PURPUR_BLOCK, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MOSS_BLOCK, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MOSS_CARPET, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MUD, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.WHITE_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ORANGE_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MAGENTA_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_BLUE_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.YELLOW_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIME_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PINK_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GRAY_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_GRAY_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.CYAN_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PURPLE_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLUE_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BROWN_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GREEN_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.RED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLACK_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));

                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.WHITE_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ORANGE_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MAGENTA_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_BLUE_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.YELLOW_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIME_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PINK_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GRAY_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_GRAY_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.CYAN_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PURPLE_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLUE_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BROWN_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GREEN_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.RED_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLACK_GLAZED_TERRACOTTA, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.HONEYCOMB, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.COPPER_BLOCK, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.EXPOSED_COPPER, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.WEATHERED_COPPER, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.OXIDIZED_COPPER, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.OAK_SAPLING, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SPRUCE_SAPLING, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BIRCH_SAPLING, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.JUNGLE_SAPLING, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ACACIA_SAPLING, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.DARK_OAK_SAPLING, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MANGROVE_PROPAGULE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.CHERRY_SAPLING, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PALE_OAK_SAPLING, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BASALT, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SMOOTH_BASALT, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BAMBOO_BLOCK, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));

                            // Sync villager trade info
                            newVillager.setOffers(offers);
                            newVillager.setCustomer(player);

                            // Open a screen handler for the villager on the merchant side
                            OptionalInt optionalInt = player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                                    ((syncId, playerInventory, p) -> new MerchantCleanupScreen(syncId, playerInventory, newVillager, (ServerWorld) player.getServerWorld())),
                                    Text.translatable("Building Blocks")
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

            // Buy Misc Command
            dispatcher.register(CommandManager.literal("buy-misc")
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

                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 32),
                                    new ItemStack(Items.DEBUG_STICK, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SCAFFOLDING, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD_BLOCK, 5),
                                    new ItemStack(Items.VILLAGER_SPAWN_EGG, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.FIREWORK_ROCKET, 16),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 16),
                                    new ItemStack(Items.WOLF_ARMOR, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD_BLOCK, 5),
                                    new ItemStack(Items.TOTEM_OF_UNDYING, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.REDSTONE, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.STRING, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.VINE, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ITEM_FRAME, 2),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.FIREFLY_BUSH, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.NAME_TAG, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));

                            // Sync villager trade info
                            newVillager.setOffers(offers);
                            newVillager.setCustomer(player);

                            // Open a screen handler for the villager on the merchant side
                            OptionalInt optionalInt = player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                                    ((syncId, playerInventory, p) -> new MerchantCleanupScreen(syncId, playerInventory, newVillager, (ServerWorld) player.getServerWorld())),
                                    Text.translatable("Other Things")
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
