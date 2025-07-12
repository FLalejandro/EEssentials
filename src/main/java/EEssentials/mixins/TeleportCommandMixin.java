package EEssentials.mixins;

import EEssentials.EEssentials;
import EEssentials.util.Location;
import EEssentials.storage.PlayerStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import java.util.Set;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {

    @Inject(method = "teleport", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FFZ)Z"))
    private static void savePreviousLocationBeforeTeleport(ServerCommandSource source, Entity target, ServerWorld world, double x, double y, double z, Set<PositionFlag> movementFlags, float yaw, float pitch, @Coerce Object facingLocation, CallbackInfo ci) {
        if (target instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) target;

            // Save the current position before the teleportation starts
            PlayerStorage storage = EEssentials.storage.getPlayerStorage(player);
            storage.setPreviousLocation(new Location(player.getWorld(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()));
            storage.save();
        }
    }

}
