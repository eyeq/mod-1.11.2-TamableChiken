package eyeq.tamablechicken.entity.ai;

import java.util.Random;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

public class EntityAIIncubator extends EntityAIBase {
    protected EntityLiving entity;

    public EntityAIIncubator(EntityLiving entity) {
        this.entity = entity;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        for(ItemStack itemStack : this.entity.getHeldEquipment()) {
            if(itemStack.getItem() instanceof ItemEgg) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateTask() {
        this.entity.getNavigator().clearPathEntity();
        World world = this.entity.world;
        if(world.isRemote || this.entity.isChild()) {
            return;
        }
        Random rand = this.entity.getRNG();
        if(this.entity.getRNG().nextInt(8000) != 0) {
            return;
        }
        ItemStack eggItemStack = null;
        for(ItemStack itemStack : this.entity.getHeldEquipment()) {
            if(itemStack.getItem() == Items.EGG) {
                eggItemStack = itemStack;
            }
        }
        if(eggItemStack == null) {
            return;
        }
        int n = rand.nextInt(32) == 0 ? 2 : 1;
        for(int i = 0; i < n; i++) {
            EntityChicken baby = new EntityChicken(world);
            baby.setGrowingAge(-24000);
            baby.setLocationAndAngles(this.entity.posX, this.entity.posY, this.entity.posZ, this.entity.rotationYaw, 0.0F);
            world.spawnEntity(baby);
        }
        for(int j = 0; j < 8; ++j) {
            world.spawnParticle(EnumParticleTypes.ITEM_CRACK, this.entity.posX, this.entity.posY, this.entity.posZ,
                    (rand.nextFloat() - 0.5) * 0.08, (rand.nextFloat() - 0.5) * 0.08, (rand.nextFloat() - 0.5) * 0.08, Item.getIdFromItem(Items.EGG));
        }
        eggItemStack.shrink(1);
    }
}
