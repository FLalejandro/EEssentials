package EEssentials.mixins;

import EEssentials.EEssentials;
import EEssentials.util.Location;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "loadPlayerData(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/ErrorReporter;)Ljava/util/Optional;",
            at = @At("RETURN"), cancellable = true)
    public void onPlayerJoin(ServerPlayerEntity player, CallbackInfoReturnable<Optional<NbtCompound>> cir) {
        // Prevent player data loading if the mod isn't fully initialized
        if (!EEssentials.isModFullyInitialized()) {
            EEssentials.LOGGER.info("Preventing player data loading for " + player.getGameProfile().getName() + " - mod not fully initialized");
            cir.setReturnValue(Optional.empty());
            return;
        }

        Optional<NbtCompound> playerDataOptional = cir.getReturnValue();

        // Handle the player data being present or absent
        NbtCompound playerData = playerDataOptional.orElse(null);

        // Check if the player is joining for the first time by looking for the "joinedBefore" tag
        boolean isFirstJoin = playerData == null || !playerData.contains("joinedBefore");

        // If it's the player's first join, teleport them to the spawn
        if (isFirstJoin) {
            Location spawnLocation = EEssentials.storage.locationManager.serverSpawn;
            if (spawnLocation != null) {
                // Teleport the player to the spawn location
                spawnLocation.teleport(player);

                // Create a new NBT compound if one doesn't exist and add the "joinedBefore" flag
                if (playerData == null) {
                    playerData = new NbtCompound();
                }
                playerData.putBoolean("joinedBefore", true);

                // Wrap the modified NBT in Optional and set it as the new return value
                cir.setReturnValue(Optional.of(playerData));
            }
        }
    }
}