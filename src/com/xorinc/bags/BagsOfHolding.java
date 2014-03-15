package com.xorinc.bags;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;


public class BagsOfHolding extends JavaPlugin implements Listener{

	public static final Pattern LORETAG = Pattern.compile(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Bag Rows: (\\d)");
	
	private static final Map<Inventory, ItemStack> invs = new IdentityHashMap<Inventory, ItemStack>();
	
	private static final Map<Player, Integer> savingBag = Collections.synchronizedMap(new HashMap<Player, Integer>()); 
	
	@Override
	public void onEnable(){
		
		Bukkit.getPluginManager().registerEvents(this, this);
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerInteract(final PlayerInteractEvent event){
		
		if(event.getItem().getType() != Material.QUARTZ || (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) || !event.getItem().getItemMeta().hasLore())
			return;
		
		if(event.getItem().getAmount() != 1){
			event.getPlayer().sendMessage(ChatColor.RED + "Stacked bags of holding can't be opened.");
			return;
		}
		
		int size = 0;
		
		for(String lore : event.getItem().getItemMeta().getLore()){
			
			Matcher m = LORETAG.matcher(lore);
			
			if(m.matches()){
				size = Integer.parseInt(m.group(1));
				break;
			}
			
		}
		
		if(size == 0)
			return;
		
		final int finalSize = size;
				
		new BukkitRunnable() {

			@Override
			public void run() {

				final List<ItemStack> inv = NMSUtil.unstoreItems(event.getItem().clone());
				
				new BukkitRunnable() {

					@Override
					public void run() {

						Inventory inventory = Bukkit.createInventory(null, finalSize * 9, "Bag of Holding");
						
						inventory.setContents(inv.toArray(new ItemStack[inv.size()]));
								
						InventoryView view = event.getPlayer().openInventory(inventory);
								
						event.getPlayer().updateInventory();
						
						invs.put(view.getTopInventory(), event.getItem());
						
					}
					
				}.runTask(BagsOfHolding.this);
				
			}
			
		}.runTaskAsynchronously(this);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event){
				
		if(!invs.containsKey(event.getView().getTopInventory()) || event.getClickedInventory() != event.getView().getBottomInventory() || savingBag.containsKey(event.getWhoClicked()))
			return;
				
		int heldSlot = savingBag.containsKey(event.getWhoClicked()) ? savingBag.get(event.getWhoClicked()) : event.getInventory().getViewers().get(0).getInventory().getHeldItemSlot();
		
		if(event.getSlot() == heldSlot || event.getHotbarButton() == heldSlot){
			event.setCancelled(true);	
			return;
		}
		
		ItemStack clicked = event.getCurrentItem();
		
		if(clicked.getType() == Material.QUARTZ && clicked.getItemMeta().hasLore()){
			
			for(String lore : clicked.getItemMeta().getLore()){
				
				Matcher m = LORETAG.matcher(lore);
				
				if(m.matches()){
					event.setCancelled(true);
					return;
				}
				
			}
			
		}
		
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerDropItem(PlayerDropItemEvent event){
		
		if(savingBag.containsKey(event.getPlayer()))
			event.setCancelled(true);
		
	}
	
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerPickupItem(PlayerPickupItemEvent event){
		
		if(savingBag.containsKey(event.getPlayer()))
			event.setCancelled(true);
		
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onInventoryClose(final InventoryCloseEvent event){
		
		if(!invs.containsKey(event.getInventory()))
			return;
		
		final ItemStack bag = invs.get(event.getInventory());
				
		final PlayerInventory inv = event.getInventory().getViewers().get(0).getInventory();
		
		ItemStack i2 = inv.getItem(0);
				
		inv.setItemInHand(i2);
		inv.setItem(0, bag);
		
		final Player p = ((Player) event.getInventory().getViewers().get(0));
		
		p.updateInventory();
		
		savingBag.put(p, 0);
		
		new BukkitRunnable() {

			@Override
			public void run() {
							
				NMSUtil.storeItems(bag, Arrays.asList(event.getInventory().getContents()));
				
				new BukkitRunnable() {

					@Override
					public void run() {

						ItemStack i2 = inv.getItemInHand();
												
						inv.setItemInHand(bag);
						inv.setItem(0, i2);
												
						p.updateInventory();
						
						invs.remove(event.getInventory());
						
						savingBag.remove(p);
						
					}
					
				}.runTask(BagsOfHolding.this);
				
			}
			
		}.runTaskAsynchronously(this);
		
	}
	
}
