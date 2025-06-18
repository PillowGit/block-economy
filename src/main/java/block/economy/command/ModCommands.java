package block.economy.command;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.ComponentType;
import net.minecraft.data.DataOutput;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryEntryLookup;
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
import java.util.Optional;
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
                                    EntityType.VILLAGER,
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
                            forEmerald.add(new TradedItem(Items.BEETROOT, 5));
                            forEmerald.add(new TradedItem(Items.WHEAT_SEEDS, 32));
                            forEmerald.add(new TradedItem(Items.WHEAT, 3));
                            forEmerald.add(new TradedItem(Items.WHITE_WOOL, 4));
                            forEmerald.add(new TradedItem(Items.POTATO, 5));
                            forEmerald.add(new TradedItem(Items.PUMPKIN, 1));
                            forEmerald.add(new TradedItem(Items.MELON_SLICE, 6));
                            forEmerald.add(new TradedItem(Items.BLACK_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.RED_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.BROWN_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.ORANGE_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.MAGENTA_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.LIGHT_BLUE_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.YELLOW_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.LIME_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.PINK_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.GRAY_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.LIGHT_GRAY_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.CYAN_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.PURPLE_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.BLUE_WOOL, 2));
                            forEmerald.add(new TradedItem(Items.GREEN_WOOL, 2));

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
                                    EntityType.VILLAGER,
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
                                    new ItemStack(Items.SEA_PICKLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GLOW_LICHEN, 4),
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

            // Buy Colored Blocks Command
            dispatcher.register(CommandManager.literal("buy-colored-blocks")
                    .executes(context -> {
                        ServerCommandSource src = context.getSource();
                        // If person using command is player
                        if (src.getEntity() instanceof ServerPlayerEntity player) {
                            ServerWorld world = player.getServerWorld();

                            // Make a villager
                            VillagerEntity newVillager = new VillagerEntity(
                                    EntityType.VILLAGER,
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
                                    new ItemStack(Items.WHITE_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ORANGE_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MAGENTA_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_BLUE_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.YELLOW_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIME_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PINK_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GRAY_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_GRAY_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.CYAN_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PURPLE_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLUE_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BROWN_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GREEN_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.RED_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLACK_CONCRETE_POWDER, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.WHITE_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ORANGE_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MAGENTA_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_BLUE_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.YELLOW_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIME_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PINK_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GRAY_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_GRAY_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.CYAN_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PURPLE_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLUE_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BROWN_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GREEN_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.RED_TERRACOTTA, 8),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLACK_TERRACOTTA, 8),
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
                                    new ItemStack(Items.CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.WHITE_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ORANGE_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MAGENTA_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_BLUE_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.YELLOW_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIME_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PINK_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GRAY_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_GRAY_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.CYAN_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PURPLE_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLUE_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BROWN_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GREEN_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.RED_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLACK_CANDLE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.WHITE_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ORANGE_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MAGENTA_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_BLUE_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.YELLOW_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIME_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PINK_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GRAY_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.LIGHT_GRAY_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.CYAN_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PURPLE_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLUE_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BROWN_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GREEN_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.RED_DYE, 4),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLACK_DYE, 4),
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
                                    Text.translatable("Multi-Colored Blocks")
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
                                    EntityType.VILLAGER,
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
                                    new ItemStack(Items.QUARTZ, 36),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.QUARTZ_BLOCK, 6),
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
                                    new ItemStack(Items.GOLD_BLOCK, 4),
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
                                    EntityType.VILLAGER,
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
                                    new TradedItem(Items.EMERALD_BLOCK, 5),
                                    Optional.of(new TradedItem(Items.DIAMOND, 5)),
                                    new ItemStack(Items.TRIDENT, 1),
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
                                    new TradedItem(Items.EMERALD, 8),
                                    new ItemStack(Items.SADDLE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 8),
                                    new ItemStack(Items.SPONGE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.WET_SPONGE, 1),
                                    new ItemStack(Items.SPONGE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD_BLOCK, 10),
                                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1),
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

            // Buy Variants Command
            dispatcher.register(CommandManager.literal("buy-variants")
                    .executes(context -> {
                        ServerCommandSource src = context.getSource();
                        // If person using command is player
                        if (src.getEntity() instanceof ServerPlayerEntity player) {
                            ServerWorld world = player.getServerWorld();

                            // Make a villager
                            VillagerEntity newVillager = new VillagerEntity(
                                    EntityType.VILLAGER,
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
                                    new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ANGLER_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ARCHER_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.ARMS_UP_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BLADE_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BREWER_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.BURN_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.DANGER_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.EXPLORER_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.FLOW_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.FRIEND_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.GUSTER_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.HEART_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.HEARTBREAK_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.HOWL_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MINER_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.MOURNER_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PLENTY_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PRIZE_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.PRIZE_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SCRAPE_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SHEAF_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SHELTER_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SKULL_POTTERY_SHERD, 1),
                                    99999,
                                    0,
                                    0.05f
                            ));
                            offers.add(new TradeOffer(
                                    new TradedItem(Items.EMERALD, 1),
                                    new ItemStack(Items.SNORT_POTTERY_SHERD, 1),
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
                                    Text.translatable("Items w Many Variants")
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

            // Buy Custom Items Command
            dispatcher.register(CommandManager.literal("buy-custom-items")
                    .executes(context -> {
                        ServerCommandSource src = context.getSource();
                        // If person using command is player
                        if (src.getEntity() instanceof ServerPlayerEntity player) {
                            ServerWorld world = player.getServerWorld();
                            String playername = player.getGameProfile().getName();

                            // Get server instance
                            CommandManager commandManager = src.getServer().getCommandManager();
                            String cmd = String.format("execute at %s run summon villager ~ ~ ~ {VillagerData:{profession:weaponsmith,level:5,type:snow},Health:1.0,Invulnerable:1,PersistenceRequired:1,Silent:1,NoAI:1,Offers:{Recipes:[{buy:{id:diamond_block,count:3},buyB:{id:obsidian,count:2},sell:{id:diamond_pickaxe,count:1,components:{unbreakable:{},attribute_modifiers:[{type:'block_interaction_range',amount:1.5,operation:'add_value',id:'example:custom_range'}],enchantments:{'efficiency':10,'unbreaking':10,'fortune':5},rarity:'epic',lore:[{text:'A pickaxe that never breaks'}]}},rewardExp:0b,maxUses:9999999},{buy:{id:diamond_block,count:3},buyB:{id:obsidian,count:2},sell:{id:diamond_pickaxe,count:1,components:{unbreakable:{},attribute_modifiers:[{type:'block_interaction_range',amount:1.5,operation:'add_value',id:'example:custom_range'}],enchantments:{'efficiency':10,'unbreaking':10,'silk_touch':10},rarity:'epic',lore:[{text:'A pickaxe that never breaks'}]}},rewardExp:0b,maxUses:9999999},{buy:{id:diamond_block,count:1},buyB:{id:obsidian,count:2},sell:{id:diamond_shovel,count:1,components:{unbreakable:{},attribute_modifiers:[{type:'block_interaction_range',amount:1.5,operation:'add_value',id:'example:custom_range'}],enchantments:{'efficiency':10,'unbreaking':10,'silk_touch':10},rarity:'epic',lore:[{text:'A shovel that never breaks'}]}},rewardExp:0b,maxUses:9999999},{buy:{id:elytra,count:1},buyB:{id:dragon_egg,count:1},sell:{id:elytra,count:1,components:{unbreakable:{},attribute_modifiers:[{type:'gravity',amount:-0.08,operation:'add_value',id:'example:custom_range'}],enchantments:{'unbreaking':10,'protection':10},rarity:'epic',lore:[{text:'Wings that last forever and'},{text:'help you jump higher'}]}},rewardExp:0b,maxUses:9999999},{buy:{id:diamond_boots,count:1},buyB:{id:end_crystal,count:1},sell:{id:diamond_boots,count:1,components:{unbreakable:{},attribute_modifiers:[{type:'sneaking_speed',amount:0.3,operation:'add_value',id:'example:custom_range'},{type:'movement_efficiency',amount:1,operation:'add_value',id:'example:custom_range'},{type:'movement_speed',amount:0.05,operation:'add_value',id:'example:custom_range'},{type:'safe_fall_distance',amount:7,operation:'add_value',id:'example:custom_range'},{type:'fall_damage_multiplier',amount:-0.6,operation:'add_value',id:'example:custom_range'},{type:'step_height',amount:0.4,operation:'add_value',id:'example:custom_range'},{type:'armor',amount:5,operation:'add_value',id:'example:custom_range'},{type:'armor_toughness',amount:5,operation:'add_value',id:'example:custom_range'},{type:'knockback_resistance',amount:1,operation:'add_value',id:'example:custom_range'}],enchantments:{'unbreaking':10,'protection':5,'feather_falling':5,'depth_strider':3,'soul_speed':3,'blast_protection':5,'fire_protection':5,'projectile_protection':5},rarity:'epic',lore:[{text:'Amazing boots that let you run fast,'},{text:'step up blocks, sneak faster, and'},{text:'stop fall damage better'}]}},rewardExp:0b,maxUses:9999999},{buy:{id:diamond_leggings,count:1},buyB:{id:end_crystal,count:1},sell:{id:diamond_leggings,count:1,components:{unbreakable:{},attribute_modifiers:[{type:'tempt_range',amount:-7,operation:'add_value',id:'example:custom_range'},{type:'sweeping_damage_ratio',amount:1,operation:'add_value',id:'example:custom_range'},{type:'armor',amount:8,operation:'add_value',id:'example:custom_range'},{type:'armor_toughness',amount:5,operation:'add_value',id:'example:custom_range'},{type:'knockback_resistance',amount:1,operation:'add_value',id:'example:custom_range'}],enchantments:{'unbreaking':10,'protection':5,'blast_protection':5,'fire_protection':5,'projectile_protection':5},rarity:'epic',lore:[{text:'Pants that make you extra sneaky'},{text:'(mobs have to be closer to notice you)'}]}},rewardExp:0b,maxUses:9999999},{buy:{id:diamond_chestplate,count:1},buyB:{id:end_crystal,count:1},sell:{id:diamond_chestplate,count:1,components:{unbreakable:{},attribute_modifiers:[{type:'max_health',amount:10,operation:'add_value',id:'example:custom_range'},{type:'armor',amount:10,operation:'add_value',id:'example:custom_range'},{type:'armor_toughness',amount:5,operation:'add_value',id:'example:custom_range'},{type:'knockback_resistance',amount:1,operation:'add_value',id:'example:custom_range'}],enchantments:{'unbreaking':10,'protection':5,'blast_protection':5,'fire_protection':5,'projectile_protection':5},rarity:'epic',lore:[{text:'Chestplate that gives you more health'}]}},rewardExp:0b,maxUses:9999999},{buy:{id:diamond_helmet,count:1},buyB:{id:end_crystal,count:1},sell:{id:diamond_helmet,count:1,components:{unbreakable:{},attribute_modifiers:[{type:'submerged_mining_speed',amount:0.8,operation:'add_value',id:'example:custom_range'},{type:'entity_interaction_range',amount:0.5,operation:'add_value',id:'example:custom_range'},{type:'submerged_mining_speed',amount:0.8,operation:'add_value',id:'example:custom_range'},{type:'armor',amount:5,operation:'add_value',id:'example:custom_range'},{type:'armor_toughness',amount:5,operation:'add_value',id:'example:custom_range'},{type:'knockback_resistance',amount:1,operation:'add_value',id:'example:custom_range'}],enchantments:{'unbreaking':10,'protection':5,'blast_protection':5,'fire_protection':5,'respiration':10,'projectile_protection':5},rarity:'epic',lore:[{text:'Helmet that lets you mine underwater like it\\'s nothing'}]}},rewardExp:0b,maxUses:9999999}]}}", playername);
                            commandManager.executeWithPrefix(player.getCommandSource(), cmd);

                            src.sendFeedback(() -> Text.literal("Now trade with it :)"), false);
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
