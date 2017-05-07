package eyeq.tamablechicken.entity.passive;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Optional;
import eyeq.util.entity.EntityUtils;
import eyeq.util.entity.IEntityRideablePlayer;
import eyeq.util.entity.player.EntityPlayerUtils;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import eyeq.util.entity.IUEntityOwnable;

public class EntityChickenTamed extends EntityChicken implements IShearable, IUEntityOwnable, IEntityRideablePlayer {
    protected static final DataParameter<Optional<UUID>> OWNER_ID = EntityDataManager.createKey(EntityChickenTamed.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    private static final DataParameter<Boolean> SHEARED = EntityDataManager.createKey(EntityChickenTamed.class, DataSerializers.BOOLEAN);

    private EntityPlayer ridingPlayer;
    private float timeGriding;
    private boolean isGriding;

    public EntityChickenTamed(World world) {
        super(world);
        this.setHealth(4F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(OWNER_ID, Optional.absent());
        this.dataManager.register(SHEARED, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if(this.getOwnerId() == null) {
            compound.setString("OwnerUUID", "");
        } else {
            compound.setString("OwnerUUID", this.getOwnerId().toString());
        }
        compound.setBoolean("Sheared", this.isSheared());
    }
    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if(compound.hasKey("OwnerUUID", 8)) {
            this.setOwnerId(UUID.fromString(compound.getString("OwnerUUID")));
        }
        this.setSheared(compound.getBoolean("Sheared"));
    }

    @Override
    public UUID getOwnerId() {
        return (UUID) ((Optional) this.dataManager.get(OWNER_ID)).orNull();
    }

    @Override
    public void setOwnerId(UUID ownerId) {
        this.dataManager.set(OWNER_ID, Optional.fromNullable(ownerId));
    }

    @Override
    public EntityLivingBase getOwner() {
        try {
            UUID uuid = this.getOwnerId();
            return uuid == null ? null : this.world.getPlayerEntityByUUID(uuid);
        } catch(IllegalArgumentException var2) {
            return null;
        }
    }

    public boolean isSheared() {
        return this.dataManager.get(SHEARED);
    }

    public void setSheared(boolean isSheared) {
        this.dataManager.set(SHEARED, isSheared);
    }

    @Override
    public boolean isShearable(ItemStack itemStack, IBlockAccess world, BlockPos pos) {
        return !this.isChild() && !isSheared();
    }

    @Override
    public List<ItemStack> onSheared(ItemStack itemStack, IBlockAccess world, BlockPos pos, int fortune) {
        this.setSheared(true);
        this.setGrowingAge(36000);
        int n = 2 + rand.nextInt(3);
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            list.add(new ItemStack(Items.FEATHER));
        }
        this.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 0.8F, 1.1F);
        return list;
    }

    @Override
    public EntityItem entityDropItem(ItemStack itemStack, float offsetY) {
        if(offsetY == 0.0F) {
            if((this.isChild() || this.isSheared()) && itemStack.getItem() == Items.FEATHER) {
                return null;
            }
        }
        return super.entityDropItem(itemStack, offsetY);
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(health);
        this.timeUntilNextEgg = this.rand.nextInt(6000) + 6000;
        if(health > 4F) {
            this.timeUntilNextEgg *= 16F / (5F * health - 4F);
        }
    }

    @Override
    public double getYOffset() {
        if(ridingPlayer != null) {
            return super.getYOffset() + (this.isChild() ? 0.4F : 0.2F);
        }
        return super.getYOffset();
    }

    @Override
    public EntityChickenTamed createChild(EntityAgeable entity) {
        return new EntityChickenTamed(this.world);
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if(passenger instanceof EntityChicken) {
            passenger.setPosition(this.posX, this.posY + this.height * 0.5F + passenger.getYOffset(), this.posZ);
            ((EntityChicken) passenger).renderYawOffset = this.renderYawOffset;
        } else {
            super.updatePassenger(passenger);
        }
    }

    @Override
    public void onUpdate() {
        if(ridingPlayer != null) {
            if(ridingPlayer.isDead) {
                EntityPlayerUtils.PASSENGER.remove(ridingPlayer);
                ridingPlayer = null;
            } else {
                if(!world.isRemote) {
                    if(isRiding()) {
                        dismountRidingEntity();
                    }
                }
                EntityUtils.setRidingEntity(this, ridingPlayer);
            }
        }
        if(ridingPlayer == null) {
            super.onUpdate();
            return;
        }
        // updateRidden
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        super.onUpdate();
        this.prevOnGroundSpeedFactor = this.onGroundSpeedFactor;
        this.onGroundSpeedFactor = 0.0F;
        this.fallDistance = 0.0F;
        this.setPosition(ridingPlayer.posX, ridingPlayer.posY + ridingPlayer.getMountedYOffset() + this.getYOffset(), ridingPlayer.posZ);
        EntityUtils.setRidingEntity(this, null);
    }

    @Override
    public void onLivingUpdate() {
        this.onGridingUpdate();
        float maxTimeGriding = this.getHealth() * 2;
        if(this.timeGriding < maxTimeGriding) {
            this.timeGriding += 0.3F;
            if(this.timeGriding > maxTimeGriding) {
                this.timeGriding = maxTimeGriding;
            }
        } else {
            this.isGriding = true;
        }
        if(!this.world.isRemote) {
            if(!this.isChild() && this.timeUntilNextEgg <= 1.0F) {
                this.dropItem(Items.EGG, 1);
                this.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
                if(this.getHealth() > 4.0F) {
                    this.attackEntityFrom(DamageSource.causeMobDamage(this), 1.0F);
                }
            }
            if(this.isSheared() && this.getGrowingAge() == 0)
                this.setSheared(false);
        }
        super.onLivingUpdate();
        if(this.isSheared()) {
            if(!this.onGround && this.motionY < 0.0) {
                this.motionY *= 1.67;
            }
        }
    }

    protected void onGridingUpdate() {
        if(this.isChild() || this.isSheared()) {
            return;
        }
        Entity ridingEntity = ridingPlayer;
        if(ridingEntity == null) {
            ridingEntity = this.getRidingEntity();
            if(ridingEntity == null) {
                this.isGriding = false;
                return;
            }
        }
        // getLowestRidingEntity
        while(true) {
            Entity temp = null;
            if(ridingEntity instanceof IEntityRideablePlayer) {
                temp = ((IEntityRideablePlayer) ridingEntity).getRidingPlayer();
            }
            if(temp == null) {
                temp = ridingEntity.getRidingEntity();
            }
            if(temp == null) {
                break;
            }
            ridingEntity = temp;
        }
        this.onGround = ridingEntity.onGround;
        if(!this.onGround && ridingEntity.motionY < 0.0 && isGriding) {
            ridingEntity.motionY *= 0.78;
            this.timeGriding += ridingEntity.motionY * 3 - 0.3;
            if(this.timeGriding <= 0.0F) {
                isGriding = false;
            }
        }
        this.rotationYaw = ridingEntity.rotationYaw;
        ridingEntity.fallDistance = 1.0F;
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        if(!this.isSheared()) {
            return;
        }
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

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack itemStack = player.getHeldItem(hand);
        if(itemStack.isEmpty() && hand == EnumHand.MAIN_HAND) {
            if(ridingPlayer == player) {
                EntityPlayerUtils.PASSENGER.remove(player);
                ridingPlayer = null;
                return true;
            }
            Entity riddenEntity;
            if(EntityPlayerUtils.PASSENGER.containsKey(player)) {
                riddenEntity = EntityPlayerUtils.PASSENGER.get(player);
                if(riddenEntity.isDead) {
                    EntityPlayerUtils.PASSENGER.remove(player);
                    riddenEntity = player;
                } else {
                    while(!riddenEntity.getPassengers().isEmpty()) {
                        riddenEntity = riddenEntity.getPassengers().get(0);
                    }
                }
            } else {
                riddenEntity = player;
            }
            if(riddenEntity instanceof EntityPlayer) {
                EntityPlayerUtils.PASSENGER.put(((EntityPlayer) riddenEntity), this);
                ridingPlayer = (EntityPlayer) riddenEntity;
            } else {
                if(!world.isRemote) {
                    this.startRiding(riddenEntity);
                }
            }
            if(!world.isRemote) {
                for(EnumHand enumHand : EnumHand.values()) {
                    ItemStack heldItem = this.getHeldItem(enumHand);
                    if(!heldItem.isEmpty()) {
                        this.entityDropItem(heldItem, 0.0F);
                        this.setHeldItem(enumHand, ItemStack.EMPTY);
                    }
                }
            }
            return true;
        }
        Item item = itemStack.getItem();
        if(item == Items.FEATHER) {
            if(!world.isRemote) {
                player.startRiding(this);
            }
            return true;
        }
        if(item instanceof ItemEgg) {
            if(!world.isRemote) {
                boolean isFull = true;
                for(EnumHand enumHand : EnumHand.values()) {
                    if(this.getHeldItem(enumHand).isEmpty()) {
                        isFull = false;
                        this.setHeldItem(enumHand, new ItemStack(item, 1, itemStack.getMetadata()));
                        break;
                    }
                }
                if(isFull) {
                    this.entityDropItem(this.getHeldItemMainhand(), 0.0F);
                    this.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(item, 1, itemStack.getMetadata()));
                }
                itemStack.shrink(1);
            }
            return true;
        }
        for(EnumHand enumHand : EnumHand.values()) {
            ItemStack heldItem = this.getHeldItem(enumHand);
            if(heldItem.getItem() instanceof ItemEgg) {
                if(!world.isRemote) {
                    this.entityDropItem(heldItem, 0.0F);
                }
                this.setHeldItem(enumHand, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    @Override
    public EntityPlayer getRidingPlayer() {
        return ridingPlayer;
    }
}
