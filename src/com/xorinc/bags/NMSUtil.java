package com.xorinc.bags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.minecraft.server.v1_7_R1.ItemStack;
import net.minecraft.server.v1_7_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_7_R1.NBTTagCompound;
import net.minecraft.server.v1_7_R1.NBTTagList;


public class NMSUtil {
	
	private static final char SECTION = '¤';
	private static final char[] hexArray = "0123456789abcdef".toCharArray();
	private static Pattern dataLore = Pattern.compile("(¤[0-9a-f]){1,16000}");
	
	public static void storeItems(org.bukkit.inventory.ItemStack bag, List<org.bukkit.inventory.ItemStack> inventory){
		
		ItemMeta i = bag.getItemMeta();
		
		if(!i.hasLore())
			return;
		
		List<String> lores = i.getLore();
		
		String bagTag = null;
		
		for(String lore : lores){
			
			if(BagsOfHolding.LORETAG.matcher(lore).matches()){
				bagTag = lore;
				break;
			}
			
		}
				
		if(bagTag == null)
			return;
		
		NBTTagList items = new NBTTagList();
		
		for(org.bukkit.inventory.ItemStack item : inventory){
			
			NBTTagCompound tag = new NBTTagCompound();
			
			if(item != null)			
				CraftItemStack.asNMSCopy(item).save(tag);
			
			items.add(tag);
		}
		
		NBTTagCompound inv = new NBTTagCompound();
		
		inv.set("inv", items);
		
		byte[] data = NBTCompressedStreamTools.a(inv);
		
		List<String> newLore = toColorCodes(data);
		
		newLore.add(0, bagTag);
				
		i.setLore(newLore);
		
		bag.setItemMeta(i);
		
	}
	
	public static List<org.bukkit.inventory.ItemStack> unstoreItems(org.bukkit.inventory.ItemStack bag){
		
		ItemMeta i = bag.getItemMeta();
		
		if(!i.hasLore())
			return null;
		
		List<String> lores = i.getLore();
				
		boolean hasMatched = false;
		List<String> raw = new ArrayList<String>();
		
		for(String lore : lores){
						
			if(dataLore.matcher(lore).matches()){
				
				raw.add(lore);
				hasMatched = true;
			}
			else{
				if(hasMatched)
					break;
			}
			
		}
		
		if(raw.isEmpty())
			return Collections.emptyList();
		
		byte[] data = fromColorCodes(raw);
		
		NBTTagCompound invCompound = NBTCompressedStreamTools.a(data);
		NBTTagList inv = (NBTTagList) invCompound.get("inv");
		
		List<org.bukkit.inventory.ItemStack> items = new ArrayList<org.bukkit.inventory.ItemStack>();
		
		for(int j = 0; j < inv.size(); j++){
			
			NBTTagCompound itemTag = inv.get(j);
			
			if(itemTag.isEmpty())
				items.add(null);
			else{
				ItemStack item = ItemStack.createStack(itemTag);
								
				items.add(CraftItemStack.asCraftMirror(item));
			}
		}
		
		return items;
	}
	
	public static List<String> toColorCodes(byte[] data){
		
		String raw = "";
		
		for ( int j = 0; j < data.length; j++ ) {
			
	        int v = data[j] & 0xFF;
	        
	        raw += "" + SECTION + hexArray[v >>> 4] + SECTION + hexArray[v & 0x0F];
	    }
		
		int sections = raw.length() / 32000;
		
		List<String> clean = new ArrayList<String>(); 
		
		for(int j = 0; j < sections; j++){
			
			clean.add(raw.substring(j * 32000, j * 32000 + 32000));
			
		}
		
		if(raw.length() % 32000 != 0)
			clean.add(raw.substring(sections * 32000));
		
		return clean;
	}
	
	public static byte[] fromColorCodes(List<String> lores){
		
		String raw = "";
		
		for(String lore : lores)
			raw += lore;
		
		raw = raw.replace("" + SECTION, "");
		
		byte[] data = new byte[raw.length()/2];
		
		for(int i = 0; i < raw.length(); i += 2){
			
			data[i/2] = (byte) Integer.parseInt(raw.substring(i, i + 2), 16);
			
		}
		
		return data;		
	}
	
}
