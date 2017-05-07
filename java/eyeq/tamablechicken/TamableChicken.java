package eyeq.tamablechicken;

import eyeq.util.client.renderer.ResourceLocationFactory;
import eyeq.util.client.resource.ULanguageCreator;
import eyeq.util.client.resource.lang.LanguageResourceManager;
import eyeq.util.common.registry.UEntityRegistry;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import eyeq.tamablechicken.entity.passive.EntityChickenSheared;
import eyeq.tamablechicken.entity.passive.EntityChickenTamed;
import eyeq.tamablechicken.client.renderer.entity.RenderChickenTamed;
import eyeq.tamablechicken.event.TamableChickenEventHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;

import static eyeq.tamablechicken.TamableChicken.MOD_ID;

@Mod(modid = MOD_ID, version = "1.0", dependencies = "after:eyeq_util")
public class TamableChicken {
    public static final String MOD_ID = "eyeq_tamablechicken";

    @Mod.Instance(MOD_ID)
    public static TamableChicken instance;

    private static final ResourceLocationFactory resource = new ResourceLocationFactory(MOD_ID);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new TamableChickenEventHandler());
        registerEntities();
        if(event.getSide().isServer()) {
            return;
        }
        registerEntityRenderings();
        createFiles();
    }

    public static void registerEntities() {
        EntityList.EntityEggInfo egg = EntityList.ENTITY_EGGS.get(new ResourceLocation("chicken"));

        UEntityRegistry.registerModEntity(resource, EntityChickenSheared.class, "ShearedChicken", 0, instance, egg);
        UEntityRegistry.registerModEntity(resource, EntityChickenTamed.class, "TamedChicken", 1, instance, 0xC5AA95, egg.secondaryColor);
    }

    @SideOnly(Side.CLIENT)
    public static void registerEntityRenderings() {
        RenderingRegistry.registerEntityRenderingHandler(EntityChickenSheared.class, RenderChickenTamed::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityChickenTamed.class, RenderChickenTamed::new);
    }

    public static void createFiles() {
        File project = new File("../1.11.2-TamableChicken");

        LanguageResourceManager language = new LanguageResourceManager();

        language.register(LanguageResourceManager.EN_US, EntityChickenSheared.class, "Chicken");
        language.register(LanguageResourceManager.JA_JP, EntityChickenSheared.class, "ニワトリ");
        language.register(LanguageResourceManager.EN_US, EntityChickenTamed.class, "Chicken");
        language.register(LanguageResourceManager.JA_JP, EntityChickenTamed.class, "ニワトリ");

        ULanguageCreator.createLanguage(project, MOD_ID, language);
    }
}
