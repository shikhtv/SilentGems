package net.silentchaos512.gems.client.particle;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.silentchaos512.gems.SilentGems;
import net.silentchaos512.gems.core.proxy.ClientProxy;

public class EntityParticleFXChaosTransfer extends EntityFX {

  public static final int MAX_AGE = 20;
  public static final int MAX_SCALE = 6;

  public EntityParticleFXChaosTransfer(World world, double posX, double posY, double posZ) {

    this(world, posX, posY, posZ, 0.0001, 0.0001, 0.0001, 1.0f, 10, 0.0f, 0.2f, 0.8f);
  }

  public EntityParticleFXChaosTransfer(World world, double posX, double posY, double posZ,
      double motionX, double motionY, double motionZ) {

    this(world, posX, posY, posZ, motionX, motionY, motionZ, MAX_SCALE, MAX_AGE, 0.0f, 0.3f, 0.8f);
  }

  public EntityParticleFXChaosTransfer(World world, double posX, double posY, double posZ,
      double motionX, double motionY, double motionZ, float scale, int maxAge, float red,
      float green, float blue) {

    super(world, posX, posY, posZ, 0.0, 0.0, 0.0);
    this.motionX = motionX;
    this.motionY = motionY;
    this.motionZ = motionZ;
    this.particleTextureIndexX = 4;
    this.particleTextureIndexY = 2;
    this.particleRed = red;
    this.particleGreen = green;
    this.particleBlue = blue;
    this.particleScale = scale;
    this.particleMaxAge = maxAge;
    this.noClip = true;
    this.particleGravity = 0.0f;
    this.particleAlpha = 0.2f;
  }

  @Override
  public void onUpdate() {

    if (this.particleAge++ >= this.particleMaxAge - 1) {
      this.setDead();
    }

    this.prevPosX = this.posX;
    this.prevPosY = this.posY;
    this.prevPosZ = this.posZ;

    this.moveEntity(this.motionX, this.motionY, this.motionZ);

    this.particleScale -= MAX_SCALE / (MAX_AGE * 1.5f);
    this.particleAlpha += 0.8f * 1f / MAX_AGE;

    // Spawn trail particles
    if (SilentGems.proxy.getParticleSettings() == 0) {
      double mx = worldObj.rand.nextGaussian() * 0.025;
      double my = worldObj.rand.nextGaussian() * 0.025;
      double mz = worldObj.rand.nextGaussian() * 0.025;
      SilentGems.proxy.spawnParticles(ClientProxy.FX_CHAOS_TRAIL, worldObj, posX, posY, posZ, mx,
          my, mz);
    }
  }

  @Override
  public void func_180434_a(WorldRenderer worldRenderer, Entity entity, float posX, float posY,
      float posZ, float par5, float par6, float par7) {

    float uMin = (float) this.particleTextureIndexX / 16.0F;
    float uMax = uMin + .25f;
    float vMin = (float) this.particleTextureIndexY / 16.0F;
    float vMax = vMin + .25f;
    float f10 = 0.1F * this.particleScale;

    if (this.particleIcon != null) {
      uMin = this.particleIcon.getMinU();
      uMax = this.particleIcon.getMaxU();
      vMin = this.particleIcon.getMinV();
      vMax = this.particleIcon.getMaxV();
    }

    float f11 = (float) (this.prevPosX + (this.posX - this.prevPosX) * (double) posX - interpPosX);
    float f12 = (float) (this.prevPosY + (this.posY - this.prevPosY) * (double) posX - interpPosY);
    float f13 = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * (double) posX - interpPosZ);
    worldRenderer.setColorRGBA_F(this.particleRed, this.particleGreen, this.particleBlue,
        this.particleAlpha);
    worldRenderer.addVertexWithUV(f11 - posY * f10 - par6 * f10, f12 - posZ * f10,
        f13 - par5 * f10 - par7 * f10, uMax, vMax);
    worldRenderer.addVertexWithUV(f11 - posY * f10 + par6 * f10, f12 + posZ * f10,
        f13 - par5 * f10 + par7 * f10, uMax, vMin);
    worldRenderer.addVertexWithUV(f11 + posY * f10 + par6 * f10, f12 + posZ * f10,
        f13 + par5 * f10 + par7 * f10, uMin, vMin);
    worldRenderer.addVertexWithUV(f11 + posY * f10 - par6 * f10, f12 - posZ * f10,
        f13 + par5 * f10 - par7 * f10, uMin, vMax);
  }
}
