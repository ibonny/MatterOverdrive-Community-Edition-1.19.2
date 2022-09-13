package matteroverdrive.common.item.utils;

import java.util.List;

import matteroverdrive.core.utils.UtilsText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class OverdriveItem extends Item {

	private final boolean hasShiftTip;
	
	public OverdriveItem(Properties pProperties, boolean hasShiftTip) {
		super(pProperties);
		this.hasShiftTip = hasShiftTip;
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> tooltips, TooltipFlag flag) {
		appendPreSuperTooltip(stack, world, tooltips, flag);
		super.appendHoverText(stack, world, tooltips, flag);
		appendPostSuperTooltip(stack, world, tooltips, flag);
		if(hasShiftTip) {
			if(Screen.hasShiftDown()) {
				tooltips.add(UtilsText.itemTooltip(this).withStyle(ChatFormatting.DARK_GRAY));
			} else {
				tooltips.add(UtilsText.tooltip("hasshifttip", UtilsText.tooltip("shiftkey").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.DARK_GRAY));
			}
		}
		
	}
	
	protected void appendPreSuperTooltip(ItemStack stack, Level world, List<Component> tooltips, TooltipFlag flag) {
		
	}
	
	protected void appendPostSuperTooltip(ItemStack stack, Level world, List<Component> tooltips, TooltipFlag flag) {
		
	}

}
