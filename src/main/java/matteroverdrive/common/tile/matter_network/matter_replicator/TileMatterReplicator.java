package matteroverdrive.common.tile.matter_network.matter_replicator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.mojang.math.Vector3f;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.client.particle.replicator.ParticleOptionReplicator;
import matteroverdrive.common.block.type.TypeMachine;
import matteroverdrive.common.inventory.InventoryMatterReplicator;
import matteroverdrive.common.item.ItemPatternDrive;
import matteroverdrive.common.item.ItemUpgrade;
import matteroverdrive.common.network.NetworkMatter;
import matteroverdrive.common.tile.matter_network.TileMatterNetworkCable;
import matteroverdrive.common.tile.matter_network.matter_replicator.utils.QueuedReplication;
import matteroverdrive.common.tile.matter_network.matter_replicator.utils.SoundHandlerReplicator;
import matteroverdrive.core.capability.MatterOverdriveCapabilities;
import matteroverdrive.core.capability.types.energy.CapabilityEnergyStorage;
import matteroverdrive.core.capability.types.item.CapabilityInventory;
import matteroverdrive.core.capability.types.item_pattern.ICapabilityItemPatternStorage;
import matteroverdrive.core.capability.types.matter.CapabilityMatterStorage;
import matteroverdrive.core.matter.MatterRegister;
import matteroverdrive.core.network.utils.IMatterNetworkMember;
import matteroverdrive.core.property.Property;
import matteroverdrive.core.property.PropertyTypes;
import matteroverdrive.core.tile.types.GenericSoundTile;
import matteroverdrive.core.utils.UtilsCapability;
import matteroverdrive.core.utils.UtilsDirection;
import matteroverdrive.core.utils.UtilsItem;
import matteroverdrive.core.utils.UtilsMath;
import matteroverdrive.core.utils.UtilsNbt;
import matteroverdrive.core.utils.UtilsTile;
import matteroverdrive.registry.ItemRegistry;
import matteroverdrive.registry.TileRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.TriPredicate;

public class TileMatterReplicator extends GenericSoundTile implements IMatterNetworkMember {

	public static final int SLOT_COUNT = 10;
	
	private static final int USAGE_PER_TICK = 80;
	private static final float FAILURE_CHANCE = 0.005F;
	private static final int MATTER_STORAGE = 1024;
	private static final int ENERGY_STORAGE = 512000;
	private static final int DEFAULT_SPEED = 1;
	private static final double MATTER_MULTIPLIER = 92;
	public static final int SOUND_TICKS = 92;
	public static final int NEEDED_PLATES = 5;
	
	private boolean isPowered = false;
	private List<QueuedReplication> orders = new ArrayList<>();
	private double currRecipeValue = 0;
	private double currProgress = 0;
	private QueuedReplication currentOrder = null;
	private boolean usingFused = false;
	
	//Render data
	public boolean clientPowered;
	public QueuedReplication clientCurrentOrder;
	public double clientRecipeValue;
	private SoundHandlerReplicator soundHandler;
	
	//Menu data
	public List<QueuedReplication> clientOrders;
	public double clientProgress;
	public ItemStack outputItem = ItemStack.EMPTY;
	
	public final Property<CompoundTag> capInventoryProp;
	public final Property<CompoundTag> capEnergyStorageProp;
	public final Property<CompoundTag> capMatterStorageProp;
	
	public TileMatterReplicator(BlockPos pos, BlockState state) {
		super(TileRegistry.TILE_MATTER_REPLICATOR.get(), pos, state);
		
		setSpeed(DEFAULT_SPEED);
		setFailure(FAILURE_CHANCE);
		setPowerUsage(USAGE_PER_TICK);
		
		defaultSpeed = DEFAULT_SPEED;
		defaultFailureChance = FAILURE_CHANCE;
		defaultMatterStorage = MATTER_STORAGE;
		defaultPowerStorage = ENERGY_STORAGE;
		defaultPowerUsage = USAGE_PER_TICK;
		
		capInventoryProp = this.getPropertyManager().addTrackedProperty(PropertyTypes.NBT.create(() -> getInventoryCap().serializeNBT(),
				tag -> getInventoryCap().deserializeNBT(tag)));
		capMatterStorageProp = this.getPropertyManager().addTrackedProperty(PropertyTypes.NBT.create(() -> getMatterStorageCap().serializeNBT(),
				tag -> getMatterStorageCap().deserializeNBT(tag)));
		capEnergyStorageProp = this.getPropertyManager().addTrackedProperty(PropertyTypes.NBT.create(() -> getEnergyStorageCap().serializeNBT(),
				tag -> getEnergyStorageCap().deserializeNBT(tag)));
		
		addInventoryCap(new CapabilityInventory(SLOT_COUNT, true, true).setInputs(2).setOutputs(2).setEnergySlots(1)
				.setMatterSlots(1).setUpgrades(4).setOwner(this).setValidUpgrades(InventoryMatterReplicator.UPGRADES)
				.setValidator(getValidator()).setPropertyManager(capInventoryProp));
		addEnergyStorageCap(new CapabilityEnergyStorage(ENERGY_STORAGE, true, false).setOwner(this).setPropertyManager(capEnergyStorageProp));
		addMatterStorageCap(new CapabilityMatterStorage(MATTER_STORAGE, true, false).setOwner(this).setPropertyManager(capMatterStorageProp));
		setMenuProvider(
				new SimpleMenuProvider(
						(id, inv, play) -> new InventoryMatterReplicator(id, play.getInventory(), getInventoryCap(), getCoordsData()),
						getContainerName(TypeMachine.MATTER_REPLICATOR.id())));
		setTickable();
		setHasMenuData();
		setHasRenderData();
	}
	
	@Override
	public void tickServer() {
		UtilsTile.drainElectricSlot(this);
		UtilsTile.drainMatterSlot(this);
		removeCompletedOrders();
		boolean currState = getLevel().getBlockState(getBlockPos()).getValue(BlockStateProperties.LIT);
		if(!canRun()) {
			isRunning = false;
			isPowered = false;
			currProgress = 0;
			if (currState && !isRunning) {
				UtilsTile.updateLit(this, Boolean.FALSE);
			}
			return;
		}
		
		CapabilityEnergyStorage energy = getEnergyStorageCap();
		if(energy.getEnergyStored() < getCurrentPowerUsage()) {
			isRunning = false;
			isPowered = false;
			currProgress = 0;
			if (currState && !isRunning) {
				UtilsTile.updateLit(this, Boolean.FALSE);
			}
			return;
		}
		isPowered = true;
		
		CapabilityInventory inv = getInventoryCap();
		ItemStack drive = inv.getStackInSlot(0);
		if(drive.isEmpty()) {
			usingFused = false;
		} else {
			if(!usingFused && orders.size() > 0) {
				orders.clear();
			}
			if(orders.isEmpty()) {
				LazyOptional<ICapabilityItemPatternStorage> lazy = drive.getCapability(MatterOverdriveCapabilities.STORED_PATTERNS);
				if(lazy.isPresent()) {
					orders.add(new QueuedReplication(((ICapabilityItemPatternStorage)(lazy.cast().resolve().get())).getStoredPatterns()[0], 1));
				}
			} 
			usingFused = true;
		}
		
		if(orders.size() <= 0) {
			isRunning = false;
			currProgress = 0;
			if (currState && !isRunning) {
				UtilsTile.updateLit(this, Boolean.FALSE);
			}
			return;
		}
		currentOrder = orders.get(0);
		
		ItemStack stack = new ItemStack(currentOrder.getItem());
		double value = MatterRegister.INSTANCE.getServerMatterValue(stack);
		if(value <= 0.0 || currentOrder == null || currentOrder.getPercentage() <= 0) {
			currentOrder.cancel();
			isRunning = false;
			currProgress = 0;
			if (currState && !isRunning) {
				UtilsTile.updateLit(this, Boolean.FALSE);
			}
			return;
		}
		currRecipeValue = value;
		
		List<ItemStack> outputs = inv.getOutputs();
		ItemStack dust = outputs.get(1);
		boolean dustEmpty = dust.isEmpty();
		if(!dustEmpty && !(UtilsNbt.readMatterVal(dust) == value && dust.getCount() < dust.getMaxStackSize())) {
			isRunning = false;
			if (currState && !isRunning) {
				UtilsTile.updateLit(this, Boolean.FALSE);
			}
			return;
		}
		
		ItemStack output = outputs.get(0);
		boolean outputEmpty = output.isEmpty();
		if(!outputEmpty && !(ItemStack.isSame(stack, output) && output.getCount() < output.getMaxStackSize())) {
			isRunning = false;
			if (currState && !isRunning) {
				UtilsTile.updateLit(this, Boolean.FALSE);
			}
			return;
		}
		
		CapabilityMatterStorage matter = getMatterStorageCap();
		if(matter.getMatterStored() < currRecipeValue) {
			isRunning = false;
			if (currState && !isRunning) {
				UtilsTile.updateLit(this, Boolean.FALSE);
			}
			return;
		}
		
		isRunning = true;
		currProgress += getCurrentSpeed();
		
		energy.removeEnergy((int) getCurrentPowerUsage());
		int plateCount = inv.getStackInSlot(1).getCount();
		if(plateCount < NEEDED_PLATES) {
			int radius = NEEDED_PLATES - plateCount;
			List<LivingEntity> surroundEntities = getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(getBlockPos().offset(radius, radius, radius), getBlockPos().offset(-radius, -radius, -radius)));
			if(surroundEntities != null) {
				for(LivingEntity entity : surroundEntities) {
					entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, false, true, false));
				}
			}
		}
		
		if (!currState && isRunning) {
			UtilsTile.updateLit(this, Boolean.TRUE);
		}
		
		setChanged();
		
		
		
		if(currProgress < currRecipeValue * MATTER_MULTIPLIER) {
			return;
		}
		
		currProgress = 0;
		currentOrder.decRemaining();
		float progToFloat = (float) currentOrder.getPercentage() / 100.0F;
		
		if(roll() < getCurrentFailure() / progToFloat) {
			if(dustEmpty) {
				ItemStack newDust = new ItemStack(ItemRegistry.ITEM_RAW_MATTER_DUST.get());
				UtilsNbt.writeMatterVal(newDust, currRecipeValue);
				inv.setStackInSlot(3, newDust.copy());
			} else {
				dust.grow(1);
			}
			return;
		} 
		
		if(outputEmpty) {
			inv.setStackInSlot(2, stack.copy());
		} else {
			output.grow(1);
		}
		
		matter.removeMatter(currRecipeValue);
		currentOrder = null;
		currRecipeValue = 0;
		setChanged();

	}
	
	@Override
	public void tickClient() {
		if(soundHandler == null) {
			soundHandler = new SoundHandlerReplicator(this);
		}
		soundHandler.tick(getAdjustedTicks(), clientSoundPlaying);
		if(isRunning && clientCurrentOrder != null) {
			Level world = getLevel();
			BlockPos blockPos = getBlockPos();
			ItemEntity entity = new ItemEntity(world, blockPos.getX() + 0.5D, blockPos.getY() + 0.25, blockPos.getZ() + 0.5D, new ItemStack(clientCurrentOrder.getItem()));
			float progress = (float) clientProgress / (float) (getProcessingTime() == 0 ? 1 : getProcessingTime());
			Vector3f vec = new Vector3f((float) entity.getX(), (float) entity.getY(), (float) entity.getZ());
			double entityRadius = entity.getBbWidth();
			Random random = MatterOverdrive.RANDOM;
			double time = Math.min(progress, 1);
			float gravity = 0.1f;
			int count = 100;
			time = 1 - time;

			for (int i = 0; i < count; i++) {
				float speed = 0.05F; 
				float height = vec.y() + random.nextFloat() * entity.getBbHeight();

				Vector3f origin = new Vector3f(vec.x(), height, vec.z());
				Vector3f offset = UtilsMath.randomCirclePoint((float) entityRadius / 1.5F);
				Vector3f pos = new Vector3f(origin.x() + offset.x(), origin.y(), origin.z() + offset.z());
			
				world.addParticle(new ParticleOptionReplicator()
						.setGravity(gravity).setScale(0.01F).setAge(2), pos.x(), pos.y(), pos.z(), 0, speed, 0);
			}
		}
	}
	
	private void removeCompletedOrders() {
		Iterator<QueuedReplication> it = orders.iterator();
		int oldSize = orders.size();
		QueuedReplication queued;
		while(it.hasNext()) {
			queued = it.next();
			if(queued.isFinished()) {
				it.remove();
			}
		}
		if(oldSize > orders.size()) {
			setChanged();
		}
	}

	@Override
	public void getMenuData(CompoundTag tag) {
		int size = orders.size();
		tag.putInt("orderCount", size);
		QueuedReplication queued;
		for(int i = 0; i < size; i++) {
			queued = orders.get(i);
			queued.setOwnerLoc(getBlockPos());
			queued.setQueuePos(i);
			queued.writeToNbt(tag, "order" + i);
		}
		
	}
	
	@Override
	public void readMenuData(CompoundTag tag) {
		int orderSize = tag.getInt("orderCount");
		clientOrders = new ArrayList<>();
		for(int i = 0; i < orderSize; i++) {
			clientOrders.add(QueuedReplication.readFromNbt(tag.getCompound("order" + i)));
		}
		
	}
	
	@Override
	public void getRenderData(CompoundTag tag) {
		tag.putBoolean("powered", isPowered);
		if(orders.size() > 0) {
			orders.get(0).writeToNbt(tag, "order");
		}
		tag.putDouble("progress", currProgress);
		tag.putDouble("recipe", currRecipeValue);
		CapabilityInventory inv = getInventoryCap();
		CompoundTag item = new CompoundTag();
		inv.getStackInSlot(2).save(item);
		tag.put("item", item);
	}
	
	@Override
	public void readRenderData(CompoundTag tag) {
		clientPowered = tag.getBoolean("powered");
		if(tag.contains("order")) {
			clientCurrentOrder = QueuedReplication.readFromNbt(tag.getCompound("order"));
		} else {
			clientCurrentOrder = null;
		}
		clientRecipeValue = tag.getDouble("recipe");
		clientProgress = tag.getDouble("progress");
		outputItem = ItemStack.of(tag.getCompound("item"));
	}
	
	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		CompoundTag data = new CompoundTag();
		
		int size = orders.size();
		data.putInt("orderCount", size);
		for(int i = 0; i < size; i++) {
			orders.get(i).writeToNbt(data, "order" + i);
		}
		
		data.putDouble("progress", currProgress);
		data.putDouble("speed", getCurrentSpeed());
		data.putFloat("failure", getCurrentFailure());
		data.putDouble("usage", getCurrentPowerUsage());
		data.putBoolean("muffled", isMuffled());
		data.putBoolean("fused", usingFused);
		
		tag.put("data", data);
	}
	
	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		CompoundTag data = tag.getCompound("data");
		
		int orderSize = data.getInt("orderCount");
		orders = new ArrayList<>();
		for(int i = 0; i < orderSize; i++) {
			orders.add(QueuedReplication.readFromNbt(data.getCompound("order" + i)));
		}
		
		currProgress = data.getDouble("progress");
		setSpeed(data.getDouble("speed"));
		setFailure(data.getFloat("failure"));
		setPowerUsage(data.getDouble("usage"));
		setMuffled(data.getBoolean("muffled"));
		usingFused = data.getBoolean("fused");
	}

	@Override
	public double getCurrentMatterStorage() {
		return getMatterStorageCap().getMaxMatterStored();
	}

	@Override
	public double getCurrentPowerStorage() {
		return getEnergyStorageCap().getMaxEnergyStored();
	}

	@Override
	public void setMatterStorage(double storage) {
		getMatterStorageCap().updateMaxMatterStorage(storage);
	}

	@Override
	public void setPowerStorage(double storage) {
		getEnergyStorageCap().updateMaxEnergyStorage((int) storage);
	}

	@Override
	public double getProcessingTime() {
		return clientRecipeValue * MATTER_MULTIPLIER;
	}

	@Override
	public boolean canConnectToFace(Direction face) {
		Direction facing = getFacing();
		Direction back = Direction.NORTH;
		Direction relative = UtilsDirection.getRelativeSide(back, handleEastWest(facing));
		return relative == face;
	}
	
	@Override
	@Nullable
	public NetworkMatter getConnectedNetwork() {
		Direction back = UtilsDirection.getRelativeSide(Direction.NORTH, handleEastWest(getFacing()));
		BlockEntity entity = getLevel().getBlockEntity(getBlockPos().relative(back));
		if(entity != null && entity instanceof TileMatterNetworkCable cable) {
			return (NetworkMatter) cable.getNetwork(false);
		}
		return null;
	}
	
	@Override
	public boolean isPowered(boolean client) {
		return client ? clientPowered : isPowered;
	}
	
	public CompoundTag getNetworkData() {
		CompoundTag data = new CompoundTag();
		
		CapabilityInventory inv = getInventoryCap();
		data.put(inv.getSaveKey(), inv.serializeNBT());
		data.putBoolean("ispowered", isPowered);
		
		int size = orders.size();
		data.putInt("orderCount", size);
		QueuedReplication replication;
		for(int i = 0; i < size; i++) {
			replication = orders.get(i);
			replication.setOwnerLoc(getBlockPos());
			replication.setQueuePos(i);
			replication.writeToNbt(data, "order" + i);
		}
		data.putBoolean("fused", usingFused);
		
		return data;
	}
	
	public MatterReplicatorDataWrapper handleNetworkData(CompoundTag tag) {
		CapabilityInventory inv = new CapabilityInventory();
		inv.deserializeNBT(tag.getCompound(inv.getSaveKey()));
		boolean powered = tag.getBoolean("ispowered");
		
		int orderSize = tag.getInt("orderCount");
		List<QueuedReplication> orders = new ArrayList<>();
		for(int i = 0; i < orderSize; i++) {
			orders.add(QueuedReplication.readFromNbt(tag.getCompound("order" + i)));
		}
		return new MatterReplicatorDataWrapper(inv, powered, orders, tag.getBoolean("fused"));
	}
	
	private float roll() {
		return MatterOverdrive.RANDOM.nextFloat();
	}
	
	private int getAdjustedTicks() {
		return (int) Math.ceil(getProcessingTime() / (getCurrentSpeed() == 0 ? 1.0D : getCurrentSpeed()));
	}
	
	public int getCurrOrders() {
		return orders == null ? 0 : orders.size();
	}
	
	//Serverside
	public void queueOrder(QueuedReplication replication) {
		if(orders == null) {
			orders = new ArrayList<>();
		}
		orders.add(replication);
		setChanged();
	}
	
	public void cancelOrder(int index) {
		orders.get(index).cancel();
		currProgress = 0;
		currentOrder = null;
		setChanged();
	}
	
	public void setSoundPlaying() {
		clientSoundPlaying = true;
	}
	
	public boolean isFused() {
		return usingFused;
	}
	
	private static TriPredicate<Integer, ItemStack, CapabilityInventory> getValidator() {
		return (index, stack, cap) -> index == 0 && stack.getItem() instanceof ItemPatternDrive drive && drive.isFused(stack)
				|| index == 1 && UtilsItem.compareItems(stack.getItem(), ItemRegistry.ITEM_TRITANIUM_PLATE.get())
				|| index == 4 && UtilsCapability.hasEnergyCap(stack)
				|| index == 5 && UtilsCapability.hasMatterCap(stack)
				|| index > 5 && stack.getItem() instanceof ItemUpgrade;
	}
	
	public static class MatterReplicatorDataWrapper {
		
		private CapabilityInventory inv;
		private boolean isPowered;
		private List<QueuedReplication> orders;
		private boolean isFused;
		
		public MatterReplicatorDataWrapper(CapabilityInventory inv, boolean isPowered, @Nullable List<QueuedReplication> orders,
				boolean fused) {
			this.inv = inv;
			this.isPowered = isPowered;
			if(orders == null) {
				orders = new ArrayList<>();
			} 
			this.orders = orders;
			this.isFused = fused;
		}
		
		public CapabilityInventory getInventory() {
			return inv;
		}
		
		public boolean isPowered() {
			return isPowered;
		}
		
		public List<QueuedReplication> getOrders(){
			return orders;
		}
		
		public boolean isFused() {
			return isFused;
		}
		
	}
	
}
