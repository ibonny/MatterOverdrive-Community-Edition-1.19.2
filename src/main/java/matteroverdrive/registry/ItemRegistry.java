package matteroverdrive.registry;

import java.util.function.Function;

import matteroverdrive.References;
import matteroverdrive.common.item.ItemPatternDrive;
import matteroverdrive.common.item.ItemUpgrade;
import matteroverdrive.common.item.ItemUpgrade.UpgradeType;
import matteroverdrive.common.item.tools.ItemMatterContainer;
import matteroverdrive.common.item.tools.ItemMatterContainer.ContainerType;
import matteroverdrive.common.item.tools.ItemTransporterFlashdrive;
import matteroverdrive.common.item.tools.electric.ItemBattery;
import matteroverdrive.common.item.tools.electric.ItemBattery.BatteryType;
import matteroverdrive.common.item.tools.electric.ItemEnergyWeapon;
import matteroverdrive.common.item.tools.electric.ItemMatterScanner;
import matteroverdrive.common.item.type.TypeIsolinearCircuit;
import matteroverdrive.common.item.utils.OverdriveItem;
import matteroverdrive.common.tile.matter_network.matter_replicator.TileMatterReplicator;
import matteroverdrive.core.registers.BulkRegister;
import matteroverdrive.core.registers.IBulkRegistryObject;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Rarity;

public class ItemRegistry {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, References.ID);

	/* ITEMS */

	public static final RegistryObject<Item> ITEM_RAW_MATTER_DUST = ITEMS.register("raw_matter_dust",
			() -> new OverdriveItem(new Item.Properties().tab(References.MAIN), true));
	public static final RegistryObject<Item> ITEM_MATTER_DUST = ITEMS.register("matter_dust",
			() -> new OverdriveItem(new Item.Properties().tab(References.MAIN), true));
	public static final RegistryObject<Item> ITEM_BASE_UPGRADE = ITEMS.register("upgrade_base",
			() -> new OverdriveItem(new Item.Properties().tab(References.MAIN).stacksTo(16), false));
	public static final BulkRegister<Item> ITEM_UPGRADES = bulkItem(
			upgrade -> ITEMS.register(((UpgradeType) upgrade).id(), () -> new ItemUpgrade((UpgradeType) upgrade)),
			UpgradeType.values());
	public static final RegistryObject<Item> ITEM_ION_SNIPER = ITEMS.register("ion_sniper",
			() -> new ItemEnergyWeapon(new Item.Properties().tab(References.MAIN).rarity(Rarity.UNCOMMON), true, 10000, true,
					true, 1000));
	public static final RegistryObject<Item> ITEM_PHASER_RIFLE = ITEMS.register("phaser_rifle",
			() -> new ItemEnergyWeapon(new Item.Properties().tab(References.MAIN).rarity(Rarity.UNCOMMON), true, 10000, true,
					true, 1000));
	public static final RegistryObject<Item> ITEM_PHASER = ITEMS.register("phaser",
			() -> new ItemEnergyWeapon(new Item.Properties().tab(References.MAIN).rarity(Rarity.UNCOMMON), true, 10000, true,
					true, 1000));
	public static final RegistryObject<Item> ITEM_PLASMA_SHOTGUN = ITEMS.register("plasma_shotgun",
			() -> new ItemEnergyWeapon(new Item.Properties().tab(References.MAIN).rarity(Rarity.UNCOMMON), true, 10000, true,
					true, 1000));
	public static final RegistryObject<Item> ITEM_OMNI_TOOL = ITEMS.register("omni_tool",
			() -> new ItemEnergyWeapon(new Item.Properties().tab(References.MAIN).rarity(Rarity.UNCOMMON), true, 10000, true,
					true, 1000));
	public static final BulkRegister<Item> ITEM_BATTERIES = bulkItem(
			battery -> ITEMS.register(((BatteryType) battery).id(), () -> new ItemBattery((BatteryType) battery)),
			BatteryType.values());
	public static final BulkRegister<Item> ITEM_MATTER_CONTAINERS = bulkItem(
			container -> ITEMS.register(container.id(), () -> new ItemMatterContainer((ContainerType) container)),
			ContainerType.values());
	public static final BulkRegister<Item> ITEM_ISOLINEAR_CIRCUITS = bulkItem(
			circuit -> ITEMS.register(((TypeIsolinearCircuit) circuit).id(),
					() -> new Item(new Item.Properties().tab(References.MAIN))),
			TypeIsolinearCircuit.values());
	public static final RegistryObject<Item> ITEM_TRANSPORTER_FLASHDRIVE = ITEMS.register("transporter_flashdrive",
			() -> new ItemTransporterFlashdrive());
	public static final RegistryObject<Item> ITEM_PATTERN_DRIVE = ITEMS.register("pattern_drive",
			() -> new ItemPatternDrive());
	public static final RegistryObject<Item> ITEM_MATTER_SCANNER = ITEMS.register("matter_scanner",
			() -> new ItemMatterScanner());

	public static final RegistryObject<Item> ITEM_TRITANIUM_PLATE = ITEMS.register("tritanium_plate",
			() -> new Item(new Item.Properties().tab(References.MAIN)));
	public static final RegistryObject<Item> ITEM_LEAD_PLATE = ITEMS.register("lead_plate",
			() -> new Item(new Item.Properties().tab(References.MAIN).stacksTo(TileMatterReplicator.NEEDED_PLATES)));

	private static BulkRegister<Item> bulkItem(Function<IBulkRegistryObject, RegistryObject<Item>> factory,
			IBulkRegistryObject[] bulkValues) {
		return new BulkRegister<>(factory, bulkValues);
	}

}
