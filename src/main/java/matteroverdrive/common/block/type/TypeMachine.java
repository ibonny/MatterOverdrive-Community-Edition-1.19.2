package matteroverdrive.common.block.type;

import java.util.Arrays;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum TypeMachine {

	solar_panel(Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D), true), matter_decomposer(true);

	// DUNSEW
	public final VoxelShape[] shapes = new VoxelShape[6];
	public final boolean hasCustomAABB;
	
	public final boolean isRedstoneConnected;

	private TypeMachine(boolean isRedstoneConnected) {
		hasCustomAABB = false;
		this.isRedstoneConnected = isRedstoneConnected;
	}

	private TypeMachine(VoxelShape allDirs, boolean isRedstoneConnected) {
		hasCustomAABB = true;
		Arrays.fill(shapes, allDirs);
		this.isRedstoneConnected = isRedstoneConnected;
	}

	public VoxelShape getShape(Direction dir) {
		return shapes[dir.ordinal()];
	}

}
