package com.hbm.packet;

import java.util.Random;

import com.hbm.entity.missile.EntityBobmazon;
import com.hbm.handler.BobmazonOfferFactory;
import com.hbm.inventory.gui.GUIScreenBobmazon.Offer;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemCatalog;
import com.hbm.lib.ModDamageSource;

import io.netty.buffer.ByteBuf;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ItemBobmazonPacket implements IMessage {

	int offer;

	public ItemBobmazonPacket()
	{
		
	}

	public ItemBobmazonPacket(EntityPlayer player, Offer offer)
	{
		if(player.getHeldItemMainhand().getItem() == ModItems.bobmazon_materials)
			this.offer = BobmazonOfferFactory.materials.indexOf(offer);
		if(player.getHeldItemMainhand().getItem() == ModItems.bobmazon_machines)
			this.offer = BobmazonOfferFactory.machines.indexOf(offer);
		if(player.getHeldItemMainhand().getItem() == ModItems.bobmazon_weapons)
			this.offer = BobmazonOfferFactory.weapons.indexOf(offer);
		if(player.getHeldItemMainhand().getItem() == ModItems.bobmazon_tools)
			this.offer = BobmazonOfferFactory.tools.indexOf(offer);
		if(player.getHeldItemMainhand().getItem() == ModItems.bobmazon_hidden)
			this.offer = BobmazonOfferFactory.special.indexOf(offer);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		offer = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(offer);
	}

	public static class Handler implements IMessageHandler<ItemBobmazonPacket, IMessage> {
		
		@Override
		public IMessage onMessage(ItemBobmazonPacket m, MessageContext ctx) {
			
			EntityPlayerMP p = ctx.getServerHandler().player;
			
			p.getServer().addScheduledTask(() -> {
				World world = p.world;

				if(p.getHeldItemOffhand().getItem() instanceof ItemCatalog){
					p.sendMessage(new TextComponentTranslation("§3§l[Vault-tec]§r You must use your main hand!"));
					return;
				}
				
				Offer offer = null;
				if(p.getHeldItemMainhand().getItem() == ModItems.bobmazon_materials)
					offer = BobmazonOfferFactory.materials.get(m.offer);
				if(p.getHeldItemMainhand().getItem() == ModItems.bobmazon_machines)
					offer = BobmazonOfferFactory.machines.get(m.offer);
				if(p.getHeldItemMainhand().getItem() == ModItems.bobmazon_weapons)
					offer = BobmazonOfferFactory.weapons.get(m.offer);
				if(p.getHeldItemMainhand().getItem() == ModItems.bobmazon_tools)
					offer = BobmazonOfferFactory.tools.get(m.offer);
				if(p.getHeldItemMainhand().getItem() == ModItems.bobmazon_hidden)
					offer = BobmazonOfferFactory.special.get(m.offer);
				
				if(offer == null) {
					p.sendMessage(new TextComponentTranslation("§3§l[Vault-tec]§r There appears to be a mismatch between the offer you have requested and the offers that exist."));
					p.sendMessage(new TextComponentTranslation("§3§l[Vault-tec]§r Engaging fail-safe..."));
					p.attackEntityFrom(ModDamageSource.nuclearBlast, 1000);
					p.motionY = 2.0D;
					return;
				}
				
				ItemStack stack = offer.offer;
				
				Advancement req = offer.requirement.getAchievement();
				
				if(req != null && p.getAdvancements().getProgress(req).isDone() || p.capabilities.isCreativeMode) {
					
					if(countCaps(p) >= offer.cost || p.capabilities.isCreativeMode) {
						
						payCaps(p, offer.cost);
						p.inventoryContainer.detectAndSendChanges();
						
						Random rand = world.rand;
						EntityBobmazon bob = new EntityBobmazon(world);
						bob.posX = p.posX + rand.nextGaussian() * 10;
						bob.posY = 300;
						bob.posZ = p.posZ + rand.nextGaussian() * 10;
						bob.payload = stack;
						
						world.spawnEntity(bob);
					} else {
						p.sendMessage(new TextComponentTranslation("[LTG] Kill yourself!"));
					}
					
				} else {
					p.sendMessage(new TextComponentTranslation("§3§l[Vault-tec]§r Achievement requirement not met!"));
				}
			});
			
			
			return null;
		}
		
		private int countCaps(EntityPlayer player) {
			
			int count = 0;
			
			for(int i = 0; i < player.inventory.getSizeInventory(); i++) {
				
				ItemStack stack = player.inventory.getStackInSlot(i);
				
				if(stack != null) {
					
					Item item = stack.getItem();
					
					if(item == ModItems.cap_fritz ||
							item == ModItems.cap_korl ||
							item == ModItems.cap_nuka ||
							item == ModItems.cap_quantum ||
							item == ModItems.cap_rad ||
							item == ModItems.cap_sparkle ||
							item == ModItems.cap_star ||
							item == ModItems.cap_sunset)
						count += stack.getCount();
					
				}
			}
			
			return count;
		}
		
		private void payCaps(EntityPlayer player, int price) {
			
			if(price == 0)
				return;
			
			for(int i = 0; i < player.inventory.getSizeInventory(); i++) {
				
				ItemStack stack = player.inventory.getStackInSlot(i);
				
				if(stack != null) {
					
					Item item = stack.getItem();
					
					if(item == ModItems.cap_fritz ||
							item == ModItems.cap_korl ||
							item == ModItems.cap_nuka ||
							item == ModItems.cap_quantum ||
							item == ModItems.cap_rad ||
							item == ModItems.cap_sparkle ||
							item == ModItems.cap_star ||
							item == ModItems.cap_sunset) {
						
						int size = stack.getCount();
						for(int j = 0; j < size; j++) {
							
							player.inventory.decrStackSize(i, 1);
							price--;
							
							if(price == 0)
								return;
						}
					}
				}
			}
		}
	}
}