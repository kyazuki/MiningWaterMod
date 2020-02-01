package com.github.kyazuki.miningwatermod;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MiningWaterMod.MODID)
@Mod.EventBusSubscriber
public class MiningWaterMod {
  public static final String MODID = "miningwatermod";
  public static final Logger LOGGER = LogManager.getLogger(MODID);
  public static Fluid fluid = Fluids.EMPTY;

  public MiningWaterMod() {
    LOGGER.debug("MiningWaterMod Loaded.");
  }

  @SubscribeEvent
  public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.player.world.isRemote()) {
      if (fluid != Fluids.EMPTY) {
        fluid = Fluids.EMPTY;
        event.player.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
      }
    } else {
      BlockPos lookingBlockPos = LookingWaterPos();
      if (lookingBlockPos != null) {
        BlockState lookingBlockState = event.player.getEntityWorld().getBlockState(lookingBlockPos);
        if (heldSilkTouch(event.player)) {
          if (event.player.isSwingInProgress) {
            event.player.isSwingInProgress = false;
            event.player.swingProgress = 0.0f;
            event.player.swingProgressInt = 0;
            if (lookingBlockState.getBlock() instanceof IBucketPickupHandler) {
              fluid = ((IBucketPickupHandler) lookingBlockState.getBlock()).pickupFluid(event.player.world, lookingBlockPos, lookingBlockState);
              if (fluid != Fluids.EMPTY) {
                event.player.getHeldItemMainhand().damageItem(1, event.player, (heldplayer) -> {
                  heldplayer.sendBreakAnimation(Hand.MAIN_HAND);
                });
                event.player.addExhaustion(0.005F);
                Block.spawnAsEntity(event.player.world, lookingBlockPos, new ItemStack(ModEventSubscriber.WATER));
              }
            }
          }
        }
      }
    }
  }

  @SubscribeEvent
  public static void onBreakBlock(PlayerEvent.BreakSpeed event) {
    if (LookingWaterPos() != null) {
      if (heldSilkTouch(event.getPlayer())) {
        event.setCanceled(true);
      }
    }
  }

  public static BlockPos LookingWaterPos() {
    Minecraft minecraft = Minecraft.getInstance();
    Entity viewpoint = minecraft.getRenderViewEntity();
    if (viewpoint != null) {
      Vec3d eyePosition = viewpoint.getEyePosition(0);
      Vec3d lookVector = viewpoint.getLook(0);
      float reach = minecraft.playerController.getBlockReachDistance();
      Vec3d traceEnd = eyePosition.add(lookVector.x * reach, lookVector.y * reach, lookVector.z * reach);
      RayTraceContext context = new RayTraceContext(eyePosition, traceEnd, RayTraceContext.BlockMode.OUTLINE, RayTraceContext.FluidMode.SOURCE_ONLY, viewpoint);
      BlockPos lookingBlock = new BlockPos(viewpoint.getEntityWorld().rayTraceBlocks(context).getPos());
      if (viewpoint.getEntityWorld().getBlockState(lookingBlock).getBlock() == Blocks.WATER)
        return lookingBlock;
    }
    return null;
  }

  public static boolean heldSilkTouch(PlayerEntity player) {
    if (player.getHeldItemMainhand().isEnchanted()) {
      for (int i = 0; i < player.getHeldItemMainhand().getEnchantmentTagList().size(); i++) {
        if (player.getHeldItemMainhand().getEnchantmentTagList().getCompound(i).getString("id").equals("minecraft:silk_touch"))
          return true;
      }
    }
    return false;
  }
}
