package EEssentials.mixins;

import EEssentials.EEssentials;
import EEssentials.util.Location;
import EEssentials.storage.PlayerStorage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class BackTeleportationMixin {

    @Inject(method = "teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FFZ)Z", at = @At("HEAD"))
    public void savePreviousLocationBeforeTeleport(ServerWorld targetWorld, double x, double y, double z, Set<?> set, float yaw, float pitch, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        // Cast to PlayerEntity to access isSpectator()
        if (!((PlayerEntity) player).isSpectator()) {
            PlayerStorage storage = EEssentials.storage.getPlayerStorage(player);
            storage.setPreviousLocation(new Location(player.getWorld(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
            storage.save();
        }
    }
}

