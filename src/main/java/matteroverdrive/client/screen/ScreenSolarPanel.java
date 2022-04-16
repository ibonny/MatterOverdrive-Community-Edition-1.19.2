package matteroverdrive.client.screen;

import matteroverdrive.common.inventory.InventorySolarPanel;
import matteroverdrive.core.screen.GenericScreen;
import matteroverdrive.core.screen.component.ScreenComponentCharge;
import matteroverdrive.core.screen.component.ScreenComponentHotbarBar;
import matteroverdrive.core.screen.component.ScreenComponentIndicator;
import matteroverdrive.core.screen.component.button.ButtonGeneric;
import matteroverdrive.core.screen.component.button.ButtonGeneric.ButtonType;
import matteroverdrive.core.screen.component.button.ButtonMenuBar;
import matteroverdrive.core.screen.component.button.ButtonMenuOption;
import matteroverdrive.core.screen.component.button.ButtonMenuOption.MenuButtonType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;

public class ScreenSolarPanel extends GenericScreen<InventorySolarPanel> {

	private static boolean EXTENDED = false;
	
	private ButtonGeneric close;
	private ButtonMenuBar menu;
	private ButtonMenuOption home;
	private ButtonMenuOption settings;
	private ButtonMenuOption upgrades;
	
	private int screenNumber = 0;

	private static final int BETWEEN_MENUS = 26;
	private static final int FIRST_HEIGHT = 40;

	public ScreenSolarPanel(InventorySolarPanel menu, Inventory playerinventory, Component title) {
		super(menu, playerinventory, title);
		components.add(new ScreenComponentCharge(() -> 0.3, this, 81, 35, new int[]{0}));
		components.add(new ScreenComponentIndicator(() -> true, this, -31, 159, new int[]{0, 1, 2}));
		components.add(new ScreenComponentHotbarBar(this, 3, 143, new int[]{0, 1, 2}));
	}

	@Override
	protected void init() {
		super.init();
		int guiWidth = (width - imageWidth) / 2;
		int guiHeight = (height - imageHeight) / 2;
		close = new ButtonGeneric(guiWidth + 170, guiHeight + 6, ButtonType.CLOSE_SCREEN, button -> onClose());
		menu = new ButtonMenuBar(guiWidth + 175, guiHeight + 33, EXTENDED, button -> {
			toggleBarOpen();
			home.visible = !home.visible;
			settings.visible = !settings.visible;
			upgrades.visible = !upgrades.visible;
		}, (button, stack, mouseX, mouseY) -> {
			ButtonMenuBar bar = (ButtonMenuBar) button;
			if (bar.isExtended) {
				displayTooltip(stack, new TranslatableComponent("tooltip.matteroverdrive.closemenu"), mouseX, mouseY);
			} else {
				displayTooltip(stack, new TranslatableComponent("tooltip.matteroverdrive.openmenu"), mouseX, mouseY);
			}
		});
		home = new ButtonMenuOption(guiWidth + 180, guiHeight + FIRST_HEIGHT, this, button -> {
			updateScreen(0);
			settings.isActivated = false;
			upgrades.isActivated = false;
		}, MenuButtonType.HOME, menu, true);
		settings = new ButtonMenuOption(guiWidth + 180, guiHeight + FIRST_HEIGHT + BETWEEN_MENUS, this, button -> {
			updateScreen(1);
			home.isActivated = false;
			upgrades.isActivated = false;
		}, MenuButtonType.SETTINGS, menu, false);
		upgrades = new ButtonMenuOption(guiWidth + 180, guiHeight + FIRST_HEIGHT + BETWEEN_MENUS * 2, this, button -> {
			updateScreen(2);
			home.isActivated = false;
			settings.isActivated = false;
		}, MenuButtonType.UPGRADES, menu, false);
		addRenderableWidget(close);
		addRenderableWidget(menu);
		addRenderableWidget(home);
		addRenderableWidget(settings);
		addRenderableWidget(upgrades);
	}
	
	private void toggleBarOpen() {
		EXTENDED = !EXTENDED;
	}
	
	private void updateScreen(int screenNumber) {
		this.screenNumber = screenNumber;
		updateSlotActivity(this.screenNumber);
	}

	@Override
	public int getScreenNumber() {
		return screenNumber;
	}

}
