package matteroverdrive.common.tile;

import matteroverdrive.SoundRegister;
import matteroverdrive.common.block.type.TypeMachine;
import matteroverdrive.common.inventory.InventoryMicrowave;
import matteroverdrive.core.capability.types.energy.CapabilityEnergyStorage;
import matteroverdrive.core.capability.types.item.CapabilityInventory;
import matteroverdrive.core.sound.SoundBarrierMethods;
import matteroverdrive.core.tile.types.GenericSoundTile;
import matteroverdrive.core.utils.UtilsItem;
import matteroverdrive.core.utils.UtilsTile;
import matteroverdrive.registry.TileRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class TileMicrowave extends GenericSoundTile {

	public static final int SLOT_COUNT = 7;

	public static final int OPERATING_TIME = 50;
	private static final int USAGE_PER_TICK = 30;
	private static final int ENERGY_STORAGE = 512000;
	private static final int DEFAULT_SPEED = 1;

	private double currProgress = 0;

	public double clientProgress;

	private SmokingRecipe cachedRecipe;

	public TileMicrowave(BlockPos pos, BlockState state) {
		super(TileRegistry.TILE_MICROWAVE.get(), pos, state);
		
		setSpeed(DEFAULT_SPEED);
		setPowerUsage(USAGE_PER_TICK);
		
		defaultSpeed = DEFAULT_SPEED;
		defaultPowerStorage = ENERGY_STORAGE;
		defaultPowerUsage = USAGE_PER_TICK;
		
		addInventoryCap(new CapabilityInventory(SLOT_COUNT, true, true).setInputs(1).setOutputs(1).setEnergySlots(1)
				.setUpgrades(4).setOwner(this)
				.setDefaultDirections(state, new Direction[] { Direction.UP, Direction.NORTH },
						new Direction[] { Direction.DOWN })
				.setValidator(machineValidator()).setValidUpgrades(InventoryMicrowave.UPGRADES));
		addEnergyStorageCap(new CapabilityEnergyStorage(ENERGY_STORAGE, true, false).setOwner(this)
				.setDefaultDirections(state, new Direction[] { Direction.WEST, Direction.EAST }, null));
		setMenuProvider(new SimpleMenuProvider(
				(id, inv, play) -> new InventoryMicrowave(id, play.getInventory(), getInventoryCap(), getCoordsData()),
				getContainerName(TypeMachine.MICROWAVE.id())));
		setHasMenuData();
		setTickable();
	}

	@Override
	public void tickServer() {
		boolean currState = getLevel().getBlockState(getBlockPos()).getValue(BlockStateProperties.LIT);
		if (currState && !isRunning) {
			UtilsTile.updateLit(this, Boolean.FALSE);
		} else if (!currState && isRunning) {
			UtilsTile.updateLit(this, Boolean.TRUE);
		}
		if (!canRun()) {
			isRunning = false;
			currProgress = 0;
			return;
		}
		UtilsTile.drainElectricSlot(this);
		CapabilityInventory inv = getInventoryCap();
		ItemStack input = inv.getInputs().get(0);
		if (input.isEmpty()) {
			isRunning = false;
			currProgress = 0;
			return;
		}

		boolean matched = false;
		if (cachedRecipe == null) {
			Level world = getLevel();
			for (SmokingRecipe recipe : world.getRecipeManager().getAllRecipesFor(RecipeType.SMOKING)) {
				if (recipe.getIngredients().get(0).test(input)) {
					cachedRecipe = recipe;
					matched = true;
				}
			}
		} else {
			matched = cachedRecipe.getIngredients().get(0).test(input);
		}
		if (!matched) {
			isRunning = false;
			currProgress = 0;
			return;
		}

		CapabilityEnergyStorage energy = getEnergyStorageCap();
		if (energy.getEnergyStored() < getCurrentPowerUsage()) {
			isRunning = false;
			return;
		}

		ItemStack output = inv.getOutputs().get(0);
		ItemStack result = cachedRecipe.getResultItem();

		if (!(output.isEmpty() || (UtilsItem.compareItems(output.getItem(), result.getItem())
				&& (output.getCount() + result.getCount() <= result.getMaxStackSize())))) {
			isRunning = false;
			return;
		}

		if (energy.getEnergyStored() >= getCurrentPowerUsage()
				&& (output.isEmpty() || (UtilsItem.compareItems(output.getItem(), result.getItem())
						&& (output.getCount() + result.getCount() <= result.getMaxStackSize())))) {

		}

		isRunning = true;
		currProgress += getCurrentSpeed();
		energy.removeEnergy((int) getCurrentPowerUsage());
		if (currProgress >= OPERATING_TIME) {
			currProgress = 0;
			if (output.isEmpty()) {
				inv.setStackInSlot(1, result.copy());
			} else {
				output.grow(result.getCount());
			}
			input.shrink(1);
		}
		setChanged();

	}

	@Override
	public void tickClient() {
		if (shouldPlaySound() && !clientSoundPlaying) {
			clientSoundPlaying = true;
			SoundBarrierMethods.playTileSound(SoundRegister.SOUND_MACHINE.get(), this, 1.0F, 1.0F, true);
		}
	}

	public void getMenuData(CompoundTag tag) {
		tag.putDouble("progress", currProgress);
	}

	@Override
	public void readMenuData(CompoundTag tag) {
		clientProgress = tag.getDouble("progress");
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);

		CompoundTag additional = new CompoundTag();
		additional.putDouble("progress", currProgress);
		additional.putDouble("speed", getCurrentSpeed());
		additional.putDouble("usage", getCurrentPowerUsage());
		additional.putBoolean("muffled", isMuffled());

		tag.put("additional", additional);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);

		CompoundTag additional = tag.getCompound("additional");
		currProgress = additional.getDouble("progress");
		setSpeed(additional.getDouble("speed"));
		setPowerUsage(additional.getDouble("usage"));
		setMuffled(additional.getBoolean("muffled"));
	}

	@Override
	public double getCurrentPowerStorage() {
		return getEnergyStorageCap().getMaxEnergyStored();
	}

	@Override
	public void setPowerStorage(double storage) {
		getEnergyStorageCap().updateMaxEnergyStorage((int) storage);
	}

	@Override
	public double getProcessingTime() {
		return OPERATING_TIME;
	}

}
