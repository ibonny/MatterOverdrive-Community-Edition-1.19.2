package matteroverdrive.client;

import matteroverdrive.core.capability.MatterOverdriveCapabilities;
import matteroverdrive.core.matter.MatterRegister;
import matteroverdrive.core.utils.UtilsText;
import matteroverdrive.core.utils.UtilsMatter;
import matteroverdrive.core.utils.UtilsNbt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientEventHandler {

	@SubscribeEvent
	public static void matterTooltipApplier(ItemTooltipEvent event) {
		if (Screen.hasShiftDown()) {
			event.getToolTip().add(getMatterTooltip(event.getItemStack()));
		}
	}

	private static Component getMatterTooltip(ItemStack stack) {
		if (UtilsMatter.isRawDust(stack)) {
			double val = UtilsNbt.readMatterVal(stack);
			return val > 0
					? UtilsText.tooltip("potmatterval",
							new TextComponent(UtilsText.formatMatterValue(val)).withStyle(ChatFormatting.LIGHT_PURPLE))
							.withStyle(ChatFormatting.BLUE)
					: UtilsText.tooltip("potmatterval", UtilsText.tooltip("nomatter").withStyle(ChatFormatting.RED))
							.withStyle(ChatFormatting.BLUE);
		} else if (UtilsMatter.isRefinedDust(stack)) {
			double val = UtilsNbt.readMatterVal(stack);
			return val > 0
					? UtilsText
							.tooltip("matterval",
									new TextComponent(UtilsText.formatMatterValue(val)).withStyle(ChatFormatting.GOLD))
							.withStyle(ChatFormatting.BLUE)
					: UtilsText.tooltip("matterval", UtilsText.tooltip("nomatter").withStyle(ChatFormatting.RED))
							.withStyle(ChatFormatting.BLUE);
		} else {
			Double val = MatterRegister.INSTANCE.getClientMatterValue(stack);
			return val == null
					? UtilsText.tooltip("matterval", UtilsText.tooltip("nomatter").withStyle(ChatFormatting.RED))
							.withStyle(ChatFormatting.BLUE)
					: UtilsText
							.tooltip("matterval",
									new TextComponent(UtilsText.formatMatterValue(val)).withStyle(ChatFormatting.GOLD))
							.withStyle(ChatFormatting.BLUE);
		}

	}

	@SubscribeEvent
	public static void handlerTransporterArrival(MovementInputUpdateEvent event) {
		Entity entity = event.getEntity();
		entity.getCapability(MatterOverdriveCapabilities.ENTITY_DATA).ifPresent(h -> {
			if(h.getTransporterTimer() > 0) {
				//event.setCanceled(true);
			}
		});
	}

}
