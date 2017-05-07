package eyeq.tamablechicken.entity.ai;

import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.IShearable;

public class EntityAIEatHeldItemSeeds extends EntityAIBase {
    protected EntityAnimal entity;

    public EntityAIEatHeldItemSeeds(EntityAnimal entity) {
        this.entity = entity;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        for(ItemStack itemStack : this.entity.getHeldEquipment()) {
            if(itemStack.getItem() instanceof ItemSeeds) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean continueExecuting() {
        return false;
    }

    @Override
    public void updateTask() {
        if(this.entity.getHealth() >= this.entity.getMaxHealth()) {
            return;
        }
        ItemStack eatItemStack = null;
        for(ItemStack itemStack : this.entity.getHeldEquipment()) {
            if(itemStack.getItem() instanceof ItemSeeds) {
                eatItemStack = itemStack;
                break;
            }
        }
        if(eatItemStack == null) {
            return;
        }
        if(this.entity.isChild() || (this.entity instanceof IShearable && !((IShearable) this.entity).isShearable(null, entity.world, entity.getPosition()))) {
            entity.setGrowingAge((int) (entity.getGrowingAge() * 0.9F)); // 0.995F
        }
        if(!this.entity.isChild() && !this.entity.isInLove()) {
            if(this.entity instanceof IEntityOwnable && ((IEntityOwnable) entity).getOwner() instanceof EntityPlayer) {
                entity.setInLove(((EntityPlayer) ((IEntityOwnable) entity).getOwner()));
            } else {
                entity.setInLove(null);
            }
        }
        this.entity.heal(1.0F);
        eatItemStack.shrink(1);
    }
}
