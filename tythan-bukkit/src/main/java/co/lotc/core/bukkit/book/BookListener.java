package co.lotc.core.bukkit.book;

import co.lotc.core.bukkit.TythanBukkit;
import co.lotc.core.bukkit.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BookListener implements Listener {

	private static Map<UUID, BookStream> bookStreamMap = new HashMap<>();
	private static Map<UUID, Integer> oldSlotMap = new HashMap<>();
	private static Map<UUID, ItemStack> oldItemMap = new HashMap<>();

	protected static void addStreamMap(Player p, BookStream stream) {
		bookStreamMap.put(p.getUniqueId(), stream);
	}
	protected static void addItemMap(Player p, int slot, ItemStack item) {
		UUID uuid = p.getUniqueId();
		oldSlotMap.put(uuid, slot);
		oldItemMap.put(uuid, item);
	}

	/**
	 * Return the item for the given player if it's on the stored list.
	 * @param player The player in question.
	 */
	protected static void returnItem(Player player) {
		UUID uuid = player.getUniqueId();
		if (oldItemMap.containsKey(uuid) && oldSlotMap.containsKey(uuid)) {
			player.getInventory().setItem(oldSlotMap.get(uuid), oldItemMap.get(uuid));
			oldSlotMap.remove(uuid);
			oldItemMap.remove(uuid);
		}
	}

	/**
	 * Plays when a book is closed (PlayerEditBookEvent) to remove the item
	 * BookStream from the list, if it's on it. Plays the BookStream's
	 * onBookClose method, and returns the item after 5 ticks.
	 */
	@EventHandler(ignoreCancelled=true)
	public void onBookClose(PlayerEditBookEvent e) {
		if (ItemUtil.hasCustomTag(e.getPreviousBookMeta(), BookStream.BOOK_TAG)) {

			// CANCEL
			if (e.getNewBookMeta().getTitle() != null && e.getNewBookMeta().getTitle().equalsIgnoreCase("cancel")) {
				new BukkitRunnable() {
					@Override
					public void run() {
						BookStream.getFor(e.getPlayer()).abort();
					}
				}.runTaskLaterAsynchronously(TythanBukkit.get(), 5);

			// SAVE AND RETURN
			} else {
				UUID uuid = e.getPlayer().getUniqueId();
				if (bookStreamMap.containsKey(uuid)) {
					BookStream stream = bookStreamMap.get(uuid);

					ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
					book.setItemMeta(e.getNewBookMeta());
					stream.setBookData(book);

					new BukkitRunnable() {
						@Override
						public void run() {
							returnItem(e.getPlayer());
						}
					}.runTaskLaterAsynchronously(TythanBukkit.get(), 5);

					e.setCancelled(true);
					bookStreamMap.remove(uuid);
					stream.onBookClose();
				}
			}
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if (checkIfPlayer(e.getPlayer())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerDrop(PlayerDropItemEvent e) {
		if (checkIfPlayer(e.getPlayer())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent e) {
		if (checkIfPlayer(e.getPlayer())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (checkIfPlayer(e.getWhoClicked())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		BookStream stream = BookStream.getFor(e.getPlayer());
		if (stream != null) {
			stream.abort();
		}
	}

	private boolean checkIfPlayer(HumanEntity p) {
		if (bookStreamMap.containsKey(p.getUniqueId())) {
			p.sendMessage("You have a book to edit!");
			return true;
		}
		return false;
	}
}