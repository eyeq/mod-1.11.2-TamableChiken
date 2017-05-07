package eyeq.tamablechicken.event;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import com.google.common.collect.Sets;
import eyeq.util.entity.EntityUtils;
import eyeq.util.entity.ai.UEntityAIDumpHeldItem;
import eyeq.util.world.WorldTimeUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import eyeq.tamablechicken.entity.passive.EntityChickenSheared;
import eyeq.tamablechicken.entity.passive.EntityChickenTamed;
import eyeq.tamablechicken.entity.ai.EntityAIEatHeldItemSeeds;
import eyeq.tamablechicken.entity.ai.EntityAIIncubator;
import eyeq.util.entity.IUEntityOwnable;
import eyeq.util.entity.ai.UEntityAIMoveToEntityItem;

public class TamableChickenEventHandler {
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if(event.getWorld().isRemote) {
            return;
        }
        if(event.getEntity() instanceof EntityChicken) {
            EntityChicken entity = (EntityChicken) event.getEntity();
            entity.tasks.addTask(1, new EntityAIIncubator(entity));
            entity.tasks.addTask(1, new EntityAIEatHeldItemSeeds(entity));
            entity.tasks.addTask(2, new UEntityAIMoveToEntityItem(entity, 1.0, 16.0F, Items.WHEAT_SEEDS));
            entity.tasks.addTask(8, new UEntityAIDumpHeldItem(entity, Sets.newHashSet(Items.WHEAT_SEEDS, Items.EGG)));
            entity.setDropChance(EntityEquipmentSlot.MAINHAND, 2.0F);
            entity.setDropChance(EntityEquipmentSlot.OFFHAND, 2.0F);
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.getEntityWorld();
        if(world.isRemote || !(entity instanceof EntityChicken)) {
            return;
        }
        if(entity.isDead) {
            return;
        }
        for(EntityItem entityItem : world.getEntitiesWithinAABB(EntityItem.class, entity.getEntityBoundingBox().expand(1.0, 0.0, 1.0))) {
            ItemStack itemStack = entityItem.getEntityItem();
            if(!entityItem.isDead && !entityItem.cannotPickup()) {
                if(itemStack.getItem() instanceof ItemSeeds) {
                    ItemStack handItemStack = entity.getHeldItemMainhand();
                    if(handItemStack.getCount() < 1) {
                        entity.setHeldItem(EnumHand.MAIN_HAND, itemStack);
                    } else if(handItemStack.isItemEqual(itemStack)) {
                        handItemStack.grow(itemStack.getCount());
                    }
                    entity.onItemPickup(entityItem, itemStack.getCount());
                    entityItem.setDead();
                    break;
                }
            }
        }
        if(entity instanceof IShearable && ((IShearable) entity).isShearable(null, world, entity.getPosition())) {
            if(entity.getRNG().nextInt(WorldTimeUtils.getSeason(world) == WorldTimeUtils.Season.AUTUMN ? 6000 : 18000) == 0) {
                entity.dropItem(Items.FEATHER, 1);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        EntityPlayer player = event.getEntityPlayer();
        Entity oldEntity = event.getTarget();
        World world = oldEntity.getEntityWorld();
        if(!(oldEntity instanceof EntityChicken)) {
            return;
        }
        EnumHand hand = event.getHand();
        ItemStack itemStack = player.getHeldItem(hand);
        Item item = itemStack.getItem();
        if(item instanceof ItemShears) {
            if(world.isRemote) {
                return;
            }
            if(oldEntity.getClass() != EntityChicken.class) {
                return;
            }
            if(((EntityChicken) oldEntity).isChild()) {
                return;
            }
            oldEntity.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 0.8F, 1.1F);
            Random rand = player.getRNG();
            int n = 2 + rand.nextInt(3);
            for(int i = 0; i < n; ++i) {
                EntityItem entityitem = oldEntity.dropItem(Items.FEATHER, 1);
                entityitem.motionY += rand.nextFloat() * 0.05F;
                entityitem.motionX += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
                entityitem.motionZ += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
            }
            itemStack.damageItem(1, ((EntityChicken) oldEntity));

            EntityChickenSheared newEntity = new EntityChickenSheared(world);
            try {
                EntityUtils.copyDataFromOld(newEntity, oldEntity);
            } catch(InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
            newEntity.setGrowingAge(36000);

            world.removeEntity(oldEntity);
            world.onEntityRemoved(oldEntity);
            oldEntity.isDead = true;

            world.spawnEntity(newEntity);
            return;
        }
        if(!(item instanceof ItemSeeds)) {
            return;
        }
        boolean isShearedClass = oldEntity.getClass() == EntityChickenSheared.class;
        if(oldEntity.getClass() == EntityChicken.class || isShearedClass) {
            event.setCanceled(true);
            if(world.isRemote) {
                return;
            }
            EntityChickenTamed newEntity = new EntityChickenTamed(world);
            try {
                EntityUtils.copyDataFromOld(newEntity, oldEntity);
            } catch(InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
            newEntity.setOwnerId(player.getUniqueID());
            newEntity.setSheared(isShearedClass);

            world.removeEntity(oldEntity);
            world.onEntityRemoved(oldEntity);
            oldEntity.isDead = true;

            world.spawnEntity(newEntity);
            return;
        }
        ItemStack handItemStack = ((EntityChicken) oldEntity).getHeldItemMainhand();
        if(handItemStack.getCount() < 1) {
            ((EntityChicken) oldEntity).setHeldItem(EnumHand.MAIN_HAND, new ItemStack(itemStack.getItem(), 1, itemStack.getMetadata()));
        } else if(itemStack.isItemEqual(handItemStack)) {
            handItemStack.grow(1);
        } else {
            return;
        }
        event.setCanceled(true);
        Random rand = ((EntityChicken) oldEntity).getRNG();
        double dx = rand.nextGaussian() * 0.02;
        double dy = rand.nextGaussian() * 0.02;
        double dz = rand.nextGaussian() * 0.02;
        world.spawnParticle(EnumParticleTypes.NOTE, oldEntity.posX, oldEntity.posY + oldEntity.height + 0.1, oldEntity.posZ, dx, dy, dz);
        if(!((EntityChicken) oldEntity).isChild() && !((EntityChicken) oldEntity).isInLove()) {
            ((EntityChicken) oldEntity).setInLove(player);
        }
        if(world.isRemote) {
            return;
        }
        // 寝取り
        if(oldEntity instanceof IUEntityOwnable) {
            ((IUEntityOwnable) oldEntity).setOwnerId(player.getUniqueID());
        }
        if(!player.isCreative()) {
            itemStack.shrink(1);
        }
    }
}
