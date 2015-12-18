package net.silentchaos512.gems.client.particle;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class EntityFXChaosCharge extends EntityFX {

  public static final int MAX_AGE = 10;
  public static final float MAX_SCALE = 0.5f;

  public EntityFXChaosCharge(World world, double posX, double posY, double posZ, double motionX,
      double motionY, double motionZ) {

    this(world, posX, posY, posZ, motionX, motionY, motionZ, MAX_SCALE, MAX_AGE, 1f, 1f, 1f);
  }

  public EntityFXChaosCharge(World world, double posX, double posY, double posZ, double motionX,
      double motionY, double motionZ, float scale, int maxAge, float red, float green, float blue) {

    super(world, posX, posY, posZ, 0, 0, 0);
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
    this.particleGravity = 0.05f;
    this.particleAlpha = 1.0f;
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
