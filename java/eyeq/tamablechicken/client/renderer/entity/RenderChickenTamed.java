package eyeq.tamablechicken.client.renderer.entity;

import eyeq.util.client.renderer.EntityRenderResourceLocation;
import eyeq.util.entity.IEntityRideablePlayer;
import net.minecraft.client.model.ModelChicken;
import net.minecraft.client.renderer.entity.RenderChicken;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.IShearable;

import static eyeq.tamablechicken.TamableChicken.MOD_ID;

public class RenderChickenTamed extends RenderChicken {
    protected static final ResourceLocation texturesChick = new EntityRenderResourceLocation(MOD_ID, "chicken/chick");
    protected static final ResourceLocation texturesSheared = new EntityRenderResourceLocation(MOD_ID, "chicken/chicken_sheared");

    public RenderChickenTamed(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityChicken entity, double x, double y, double z, float rotationYaw, float partialTicks) {
        ModelChicken model = (ModelChicken) this.mainModel;
        boolean isIncubator = false;
        for(ItemStack itemStack : entity.getHeldEquipment()) {
            if(itemStack.getItem() instanceof ItemEgg) {
                isIncubator = true;
                y -= entity.isChild() ? 0.15 : 0.3;
                break;
            }
        }
        if(isIncubator || entity.getRidingEntity() != null || (entity instanceof IEntityRideablePlayer && ((IEntityRideablePlayer) entity).getRidingPlayer() != null)) {
            model.rightLeg.showModel = false;
            model.leftLeg.showModel = false;
        } else {
            model.rightLeg.showModel = true;
            model.leftLeg.showModel = true;
        }
        super.doRender(entity, x, y, z, rotationYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityChicken entity) {
        if(entity.isChild()) {
            return texturesChick;
        }
        if(!((IShearable) entity).isShearable(null, entity.world, entity.getPosition())) {
            return texturesSheared;
        }
        return super.getEntityTexture(entity);
    }
}
