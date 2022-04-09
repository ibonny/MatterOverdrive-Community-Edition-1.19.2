package matteroverdrive.core.screen;

import com.mojang.blaze3d.vertex.PoseStack;

import matteroverdrive.References;
import matteroverdrive.core.inventory.GenericInventory;
import matteroverdrive.core.screen.component.utils.IGuiComponent;
import matteroverdrive.core.utils.UtilsRendering;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GenericVanillaScreen<T extends GenericInventory> extends GenericScreen<T> {

	protected ResourceLocation vanillaBg = new ResourceLocation(References.ID + ":textures/gui/base/base_vanilla.png");

	public GenericVanillaScreen(T pMenu, Inventory pPlayerInventory, Component pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
	}

	@Override
	protected void renderBg(PoseStack stack, float partialTick, int x, int y) {
		UtilsRendering.bindTexture(vanillaBg);
		int guiWidth = (width - imageWidth) / 2;
		int guiHeight = (height - imageHeight) / 2;
		blit(stack, guiWidth, guiHeight, 0, 248, imageWidth, 4);
		blit(stack, guiWidth, guiHeight + 4, 0, 0, imageWidth, imageHeight - 8);
		blit(stack, guiWidth, guiHeight + imageHeight - 4, 0, 252, imageWidth, 4);
		int xAxis = x - guiWidth;
		int yAxis = y - guiHeight;
		for (IGuiComponent component : components) {
			component.renderBackground(stack, xAxis, yAxis, guiWidth, guiHeight);
		}
	}

}