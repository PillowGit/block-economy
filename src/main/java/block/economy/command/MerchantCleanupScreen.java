package block.economy.command;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.server.world.ServerWorld;

public class MerchantCleanupScreen extends MerchantScreenHandler {
    private final ServerWorld world;
    private final VillagerEntity villager;

    public MerchantCleanupScreen(int syncId, PlayerInventory playerInventory, VillagerEntity villager, ServerWorld world) {
        super(syncId, playerInventory, villager);
        this.world = world;
        this.villager = villager;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        villager.setCustomer(null);
        villager.teleport(0, 1000, 0, false);
        villager.kill(this.world);
    }
}
