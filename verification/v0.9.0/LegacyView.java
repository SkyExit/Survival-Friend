package verification;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.event.inventory.InventoryType;

/** Compile this adapter against Spigot 1.19.4 only; loaded only on the old API. */
public final class LegacyView extends InventoryView {
    private final Player player;
    private final Inventory inventory;
    public LegacyView(Player player, Inventory inventory) { this.player = player; this.inventory = inventory; }
    public Inventory getTopInventory() { return inventory; }
    public Inventory getBottomInventory() { return inventory; }
    public Player getPlayer() { return player; }
    public InventoryType getType() { return InventoryType.CHEST; }
    public String getTitle() { return "MenuSF"; }
    public String getOriginalTitle() { return "MenuSF"; }
    public void setTitle(String title) { }
}
