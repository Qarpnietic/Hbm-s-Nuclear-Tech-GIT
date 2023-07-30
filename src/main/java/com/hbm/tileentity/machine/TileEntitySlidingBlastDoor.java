package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IAnimatedDoor;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.main.MainRegistry;
import com.hbm.packet.AuxGaugePacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.TEDoorAnimationPacket;
import com.hbm.sound.AudioWrapper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntitySlidingBlastDoor extends TileEntityLockableBase implements ITickable, IAnimatedDoor {

	//0: closed, 1: open, 2: closing, 3: opening
	public byte state = 0;
	public byte texture = 0;
	public long sysTime;
	private int timer = 0;
	public boolean shouldUseBB = true;
	public boolean redstoned = false;
	public boolean keypadLocked = false;
	
	private AudioWrapper audio;

	@Override
	public void update() {
		if(!world.isRemote) {
			if(state < 2) {
				timer = 0;
			} else {
				timer ++;
				if(state == 2){
					if(timer == 2){
						placeDummy(-2);
						placeDummy(2);
					} else if(timer == 6){
						placeDummy(-1);
						placeDummy(1);
					} else if(timer == 12){
						placeDummy(0);
					} if(timer > 24){
						state = 0;
					}
				} else if(state == 3){
					if(timer == 12){
						removeDummy(0);
					} else if(timer == 16){
						removeDummy(-1);
						removeDummy(1);
					} else if(timer == 20){
						removeDummy(-2);
						removeDummy(2);
					} else if(timer > 24){
						state = 1;
					}
				}
			}
			PacketDispatcher.wrapper.sendToAllAround(new TEDoorAnimationPacket(pos, state, texture), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 200));
			PacketDispatcher.wrapper.sendToAllAround(new AuxGaugePacket(pos, shouldUseBB == true ? 1 : 0, 0), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 200));
		}
	}
	
	public boolean tryToggle(EntityPlayer player){
		if(this.state == 0) {
			if(!world.isRemote && canAccess(player)) {
				this.state = 3;
			}
			return true;
		} else if(this.state == 1) {
			if(!world.isRemote && canAccess(player)) {
				this.state = 2;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean canAccess(EntityPlayer player) {
		if(keypadLocked && player != null)
			return false;
		
		if(!this.isLocked()) {
			return true;
		} else {
			ItemStack stack = player.getHeldItemMainhand();
			
			if(stack.getItem() instanceof ItemKeyPin && ItemKeyPin.getPins(stack) == this.lock) {
	        	world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.lockOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
				return true;
			}
			
			if(stack.getItem() == ModItems.key_red) {
	        	world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.lockOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
				return true;
			}
			
			return this.tryPick(player);
		}
	}

	private void placeDummy(int offset){
		ForgeDirection dir = ForgeDirection.getOrientation(getBlockMetadata() - BlockDummyable.offset);
		BlockPos placePos = null;
		switch(dir){
		case SOUTH:
			placePos = pos.add(offset, 0, 0);
			break;
		case NORTH:
			placePos = pos.add(-offset, 0, 0);
			break;
		case EAST:
			placePos = pos.add(0, 0, offset);
			break;
		case WEST:
			placePos = pos.add(0, 0, -offset);
			break;
		default:
			return;
		}
		if(offset == 0){
			shouldUseBB = true;
		} else {
			((BlockDummyable)getBlockType()).removeExtra(world, placePos.getX(), placePos.getY(), placePos.getZ());
		}
		((BlockDummyable)getBlockType()).removeExtra(world, placePos.getX(), placePos.getY()+1, placePos.getZ());
		((BlockDummyable)getBlockType()).removeExtra(world, placePos.getX(), placePos.getY()+2, placePos.getZ());
		((BlockDummyable)getBlockType()).removeExtra(world, placePos.getX(), placePos.getY()+3, placePos.getZ());
	}

	private void removeDummy(int offset){
		ForgeDirection dir = ForgeDirection.getOrientation(getBlockMetadata() - BlockDummyable.offset);
		BlockPos placePos = null;
		switch(dir){
		case SOUTH:
			placePos = pos.add(offset, 0, 0);
			break;
		case NORTH:
			placePos = pos.add(-offset, 0, 0);
			break;
		case EAST:
			placePos = pos.add(0, 0, offset);
			break;
		case WEST:
			placePos = pos.add(0, 0, -offset);
			break;
		default:
			return;
		}
		BlockDummyable.safeRem = true;
		if(offset == 0){
			shouldUseBB = false;
		} else {
			((BlockDummyable)getBlockType()).makeExtra(world, placePos.getX(), placePos.getY(), placePos.getZ());
		}
		((BlockDummyable)getBlockType()).makeExtra(world, placePos.getX(), placePos.getY()+1, placePos.getZ());
		((BlockDummyable)getBlockType()).makeExtra(world, placePos.getX(), placePos.getY()+2, placePos.getZ());
		((BlockDummyable)getBlockType()).makeExtra(world, placePos.getX(), placePos.getY()+3, placePos.getZ());
		BlockDummyable.safeRem = false;
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return 65536D;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		state = compound.getByte("state");
		sysTime = compound.getLong("sysTime");
		timer = compound.getInteger("timer");
		redstoned = compound.getBoolean("redstoned");
		keypadLocked = compound.getBoolean("keypadLocked");
		shouldUseBB = compound.getBoolean("shouldUseBB");
		texture = compound.getByte("texture");
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setByte("state", state);
		compound.setLong("sysTime", sysTime);
		compound.setInteger("timer", timer);
		compound.setBoolean("redstoned", redstoned);
		compound.setBoolean("keypadLocked", keypadLocked);
		compound.setBoolean("shouldUseBB", shouldUseBB);
		compound.setByte("texture", texture);
		return super.writeToNBT(compound);
	}

	@Override
	public void onChunkUnload(){
		if(audio != null) {
			audio.stopSound();
			audio = null;
    	}
	}
	
	@Override
	public void invalidate(){
		if(audio != null) {
			audio.stopSound();
			audio = null;
    	}
		super.invalidate();
	}
	
	@Override
	public void handleNewState(byte state) {
		if(this.state != state){
			if(this.state == 0 && state == 3){
				if(audio == null){
					audio = MainRegistry.proxy.getLoopedSoundStartStop(world, HBMSoundHandler.qe_sliding_opening, null, HBMSoundHandler.qe_sliding_opened, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 2, 1);
					audio.startSound();
				}
			}
			if(this.state == 1 && state == 2){
				if(audio == null){
					audio = MainRegistry.proxy.getLoopedSoundStartStop(world, HBMSoundHandler.qe_sliding_opening, null, HBMSoundHandler.qe_sliding_shut, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 2, 1);
					audio.startSound();
				}
			}
			if((this.state == 3 && state == 1) || (this.state == 2 && state == 0)){
				if(audio != null){
					audio.stopSound();
					audio = null;
				}
			}
			if(this.state < 2 && state >= 2){
				sysTime = System.currentTimeMillis();
			}
			this.state = state;
		}
	}

	@Override
	public void open() {
		if(state == 0)
			toggle();
	}

	@Override
	public void close() {
		if(state == 1)
			toggle();
	}

	@Override
	public DoorState getState() {
		return DoorState.values()[state];
	}

	@Override
	public void toggle() {
		tryToggle(null);
	}
	
	@Override
	public void setTextureState(byte tex){
		this.texture = tex;
	}
	
	@Override
	public boolean setTexture(String tex) {
		if(tex.equals("sliding_blast_door")){
			this.texture = 0;
			return true;
		} else if(tex.equals("sliding_blast_door_variant1")){
			this.texture = 1;
			return true;
		} else if(tex.equals("sliding_blast_door_variant2")){
			this.texture = 2;
			return true;
		}
		return false;
	}
}
