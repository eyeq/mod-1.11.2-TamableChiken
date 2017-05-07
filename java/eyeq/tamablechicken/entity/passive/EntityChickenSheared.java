package eyeq.tamablechicken.entity.passive;

import eyeq.util.entity.EntityUtils;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class EntityChickenSheared extends EntityChicken implements IShearable {
    public EntityChickenSheared(World world) {
        super(world);
        this.setGrowingAge(36000);
    }

    @Override
    public boolean isShearable(ItemStack itemStack, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack itemStack, IBlockAccess world, BlockPos pos, int fortune) {
        return new ArrayList<>();
    }

    @Override
    public EntityItem entityDropItem(ItemStack itemStack, float offsetY) {
        if(itemStack.getItem() == Items.FEATHER) {
            return null;
        }
        return super.entityDropItem(itemStack, offsetY);
    }

    @Override
    public void onLivingUpdate() {
        if(!this.world.isRemote && this.getGrowingAge() == 0) {
            EntityChicken newEntity = new EntityChicken(this.world);
            try {
                EntityUtils.copyDataFromOld(newEntity, this);
            } catch(InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
            this.world.removeEntity(this);
            this.world.onEntityRemoved(this);
            this.isDead = true;

            this.world.spawnEntity(newEntity);
        }
        super.onLivingUpdate();
        if(!this.onGround && this.motionY < 0.0) {
            this.motionY *= 1.67;
        }
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        float[] ret = net.minecraftforge.common.ForgeHooks.onLivingFall(this, distance, damageMultiplier);
        if(ret == null) {
            return;
        }
        distance = ret[0];
        damageMultiplier = ret[1];
        if(this.isBeingRidden()) {
            for(Entity entity : this.getPassengers()) {
                entity.fall(distance, damageMultiplier);
            }
        }
        PotionEffect potion = this.getActivePotionEffect(MobEffects.JUMP_BOOST);
        float buffer = potion == null ? 0.0F : potion.getAmplifier() + 1;
        int height = MathHelper.ceil((distance - 3.0F - buffer) * damageMultiplier);
        if(height <= 0) {
            return;
        }
        this.playSound(this.getFallSound(height), 1.0F, 1.0F);
        this.attackEntityFrom(DamageSource.FALL, (float) height);
        int x = MathHelper.floor(this.posX);
        int y = MathHelper.floor(this.posY - 0.20000000298023224);
        int z = MathHelper.floor(this.posZ);
        IBlockState state = this.world.getBlockState(new BlockPos(x, y, z));
        if(state.getMaterial() != Material.AIR) {
            SoundType soundtype = state.getBlock().getSoundType(state, world, new BlockPos(x, y, z), this);
            this.playSound(soundtype.getFallSound(), soundtype.getVolume() * 0.5F, soundtype.getPitch() * 0.75F);
        }
    }
}
