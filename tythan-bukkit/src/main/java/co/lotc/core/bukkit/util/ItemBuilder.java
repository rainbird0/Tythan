package co.lotc.core.bukkit.util;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.ItemTagType;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.Value;
import lombok.var;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.persistence.PersistentDataType;

@Accessors(fluent=true)
@FieldDefaults(level=AccessLevel.PRIVATE)
public class ItemBuilder {
	final Material material;
	@Setter int durability = 0;
	@Setter int amount = 1;
	@Setter String name;
	@Setter boolean unbreakable = false;
	
	final List<String> lore = new ArrayList<>();
	final List<ItemFlag> flags = new ArrayList<>();
	
	final Map<Enchantment, Integer> enchants = new HashMap<>();
	final Multimap<Attribute, AttributeModifier> attributes = MultimapBuilder.hashKeys().arrayListValues().build();
	
	ItemMeta meta;
	
	public ItemBuilder(Material material) {
		this.material = material;
		meta = Bukkit.getItemFactory().getItemMeta(material);
	}
	
	public ItemBuilder unbreakable() {
		return unbreakable(true);
	}
	
	public ItemBuilder nameColor(org.bukkit.ChatColor color) {
		return nameColor(color.asBungee());
	}
	
	public ItemBuilder nameColor(ChatColor color) {
		name = color.name();
		return this;
	}
	
	public ItemBuilder lore(String... lore) {
		for(var l : lore) this.lore.add(l);
		return this;
	}
	
	public ItemBuilder loreColor(org.bukkit.ChatColor color) {
		return loreColor(color.asBungee());
	}
	
	public ItemBuilder loreColor(ChatColor color) {
		lore.replaceAll(s->color.toString() + s);
		return this;
	}
	
	public ItemBuilder flag(ItemFlag... flags) {
		Collections.addAll(this.flags, flags);
		return this;
	}
	
	public ItemBuilder enchantment(Enchantment enchantment) {
		return enchantment(enchantment,1);
	}
	
	public ItemBuilder enchantment(Enchantment enchantment, int level) {
		enchants.put(enchantment, level);
		return this;
	}
	
	public ItemBuilder attribute(Attribute attribute, AttributeModifier modifier) {
		attributes.put(attribute, modifier);
		return this;
	}
	
	public ItemBuilder tag(String key, String value) {
		tag(ItemUtil.getNamespacedKey(key), PersistentDataType.STRING, value);
		return this;
	}
	
	public <T,Z> ItemBuilder tag(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
		meta.getPersistentDataContainer().set(key, type, value);
		return this;
	}
	
	public ItemStack build() {
		ItemStack result = new ItemStack(material, amount);
		enchants.forEach(result::addEnchantment);
		
		if(durability > 0 && meta instanceof Damageable)
			((Damageable)meta).setDamage(durability);
		if(name != null) meta.setDisplayName(name);
		if(!lore.isEmpty()) meta.setLore(lore);
		flags.forEach(meta::addItemFlags);
		meta.setUnbreakable(unbreakable);
		meta.setAttributeModifiers(attributes);
		result.setItemMeta(meta);
		
		return result;
	}
	
	@Value
	@Accessors(fluent=false)
	private static class Taggable<T,Z> {
		NamespacedKey key;
		PersistentDataType<T, Z> type;
		Z value;
	}
	
}
