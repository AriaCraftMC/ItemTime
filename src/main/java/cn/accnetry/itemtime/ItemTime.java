// Decompiled with: FernFlower
// Class Version: 7
package cn.accnetry.itemtime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemTime extends JavaPlugin implements Listener {
    private String lore1;
    private String lore2;
    private boolean armor;
    private boolean hand;
    private List heldChecks;
    private final String timeFormat = "yyyy年MM月dd日 HH:mm:ss";

    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        this.lore1 = this.getConfig().getString("Lore1").replace("&", "§");
        this.lore2 = this.getConfig().getString("Lore2").replace("&", "§");
        int tick = this.getConfig().getInt("Check.Tick");
        this.armor = this.getConfig().getBoolean("Check.Armor");
        this.hand = this.getConfig().getBoolean("Check.Hand");
        int handTick = this.getConfig().getInt("Check.HandTick");
        if (tick > 0) {
            this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                public void run() {
                    Object o = Bukkit.getOnlinePlayers();
                    if (o != null) {
                        for(Object ob : (Collection)o) {
                            if (ob instanceof Player) {
                                Player player = (Player)ob;
                                ItemTime.this.check(player);
                            }
                        }
                    }

                }
            }, (long) tick, (long) tick);
        }

        if (handTick > 0) {
            this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                public void run() {
                    Object o = Bukkit.getOnlinePlayers();
                    if (o != null) {
                        for(Object ob : (Collection)o) {
                            if (ob instanceof Player) {
                                Player player = (Player)ob;
                                ItemTime.this.checkHand(player);
                            }
                        }
                    }

                }
            }, (long) handTick, (long) handTick);
        }

    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        if (this.armor) {
            if (e.getEntity() instanceof Player) {
                this.checkArmor((Player)e.getEntity());
            }
        }
    }

    @EventHandler
    public void held(PlayerItemHeldEvent e) {
        if (this.hand) {
            if (!this.heldChecks.contains(e.getPlayer().getName())) {
                this.heldChecks.add(e.getPlayer().getName());
                final String name = e.getPlayer().getName();
                this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                    public void run() {
                        ItemTime.this.heldChecks.remove(name);
                        Player player = Bukkit.getPlayerExact(name);
                        if (player != null) {
                            ItemTime.this.checkHand(player);
                        }

                    }
                }, 20L);
            }
        }
    }

    public void check(Player player) {
        ItemStack[] items = player.getInventory().getContents();
        boolean outTime = false;
        boolean newTime = false;

        for(int x = 0; x < items.length; ++x) {
            ItemStack item = items[x];
            if (item != null && item.getType() != Material.AIR) {
                long time = this.getItemTime(item);
                if (time > 0L && System.currentTimeMillis() > time) {
                    items[x] = new ItemStack(Material.AIR);
                    outTime = true;
                }
            }

            if (x == player.getInventory().getHeldItemSlot() && !player.isOp()) {
                ItemStack newItem = this.updateItem(item);
                if (newItem != null) {
                    items[x] = newItem;
                    newTime = true;
                }
            }
        }

        if (outTime || newTime) {
            player.getInventory().setContents(items);
        }

        items = player.getInventory().getArmorContents();

        for(int x = 0; x < items.length; ++x) {
            ItemStack item = items[x];
            if (item != null && item.getType() != Material.AIR) {
                long time = this.getItemTime(item);
                if (time > 0L && System.currentTimeMillis() > time) {
                    items[x] = new ItemStack(Material.AIR);
                    outTime = true;
                }

                if (!player.isOp()) {
                    ItemStack newItem = this.updateItem(item);
                    if (newItem != null) {
                        items[x] = newItem;
                        newTime = true;
                    }
                }
            }
        }

        if (outTime || newTime) {
            player.getInventory().setArmorContents(items);
        }

        if (outTime || newTime) {
            player.updateInventory();
        }

        if (outTime) {
            String msg = this.getConfig().getString("OutItem").replace("&", "§");
            player.sendMessage(msg);
        }

        if (newTime) {
            String msg = this.getConfig().getString("NewItem").replace("&", "§");
            player.sendMessage(msg);
        }

    }

    public void checkHand(Player player) {
        ItemStack item = player.getItemInHand().clone();
        if (item != null && item.getType() != Material.AIR) {
            long time = this.getItemTime(item);
            if (time > 0L && System.currentTimeMillis() > time) {
                player.setItemInHand(new ItemStack(Material.AIR));
                player.updateInventory();
                String msg = this.getConfig().getString("OutItem").replace("&", "§");
                player.sendMessage(msg);
                return;
            }

            if (time <= 0L && !player.isOp()) {
                ItemStack newItem = this.updateItem(item);
                if (newItem != null) {
                    String msg = this.getConfig().getString("NewItem").replace("&", "§");
                    player.setItemInHand(newItem);
                    player.updateInventory();
                    player.sendMessage(msg);
                    return;
                }
            }
        }

    }

    public void checkArmor(Player player) {
        PlayerInventory i = player.getInventory();
        ItemStack[] items = i.getArmorContents();
        boolean outTime = false;
        boolean newTime = false;

        for(int x = 0; x < items.length; ++x) {
            ItemStack item = items[x];
            if (item != null && item.getType() != Material.AIR) {
                long time = this.getItemTime(item);
                if (time > 0L && System.currentTimeMillis() > time) {
                    items[x] = new ItemStack(Material.AIR);
                    outTime = true;
                }

                if (!player.isOp()) {
                    ItemStack newItem = this.updateItem(item);
                    if (newItem != null) {
                        items[x] = newItem;
                        newTime = true;
                    }
                }
            }
        }

        if (outTime || newTime) {
            player.getInventory().setArmorContents(items);
            if (outTime) {
                String msg = this.getConfig().getString("OutItem").replace("&", "§");
                player.sendMessage(msg);
            }

            if (newTime) {
                String msg = this.getConfig().getString("NewItem").replace("&", "§");
                player.sendMessage(msg);
            }

            player.updateInventory();
        }

    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 0 && !args[0].equalsIgnoreCase("help")) {
            if (args[0].equalsIgnoreCase("set")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§a该指令只能由玩家执行");
                    return true;
                } else {
                    Player player = (Player)sender;
                    ItemStack item = player.getItemInHand();
                    if (item != null && item.getType() != Material.AIR) {
                        if (args.length < 2) {
                            sender.sendMessage("§a参数长度不正确");
                            return true;
                        } else {
                            String time = args[1];
                            if (!time.contains("d") && !time.contains("h") && !time.contains("m") && !time.contains("s")) {
                                sender.sendMessage("§a时间无单位");
                                return true;
                            } else {
                                long t = this.getTimeFromCommand(time);
                                if (t <= 0L) {
                                    sender.sendMessage("§a时间错误");
                                    return true;
                                } else if (this.getItemTime(item) > 0L) {
                                    sender.sendMessage("§a该物品已有有效期");
                                    return true;
                                } else {
                                    ItemStack i = this.updateItem(item.clone());
                                    if (i != null) {
                                        sender.sendMessage("§a该物品已有有效期");
                                        return true;
                                    } else {
                                        ItemMeta meta = item.getItemMeta();
                                        List lore = meta.getLore();
                                        if (lore == null) {
                                            lore = new ArrayList();
                                        }

                                        t += System.currentTimeMillis();
                                        SimpleDateFormat sdf = new SimpleDateFormat(this.timeFormat);
                                        String timeString = sdf.format(new Date(t));
                                        lore.add(this.lore1.replace("%time%", timeString + "§b§e§f"));
                                        meta.setLore(lore);
                                        item.setItemMeta(meta);
                                        player.setItemInHand(item);
                                        player.updateInventory();
                                        sender.sendMessage("§a成功设置手上的物品有效期至§6" + timeString);
                                        return true;
                                    }
                                }
                            }
                        }
                    } else {
                        sender.sendMessage("§a手上的物品不能为空");
                        return true;
                    }
                }
            } else if (args[0].equalsIgnoreCase("add")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§a该指令只能由玩家执行");
                    return true;
                } else {
                    Player player = (Player)sender;
                    ItemStack item = player.getItemInHand();
                    if (item != null && item.getType() != Material.AIR) {
                        if (args.length < 2) {
                            sender.sendMessage("§a参数长度不正确");
                            return true;
                        } else {
                            String time = args[1];
                            if (!time.contains("d") && !time.contains("h") && !time.contains("m") && !time.contains("s")) {
                                sender.sendMessage("§a时间无单位");
                                return true;
                            } else {
                                long t = this.getTimeFromCommand(time);
                                if (t <= 0L) {
                                    sender.sendMessage("§a时间错误");
                                    return true;
                                } else if (this.getItemTime(item) > 0L) {
                                    sender.sendMessage("§a该物品已有有效期");
                                    return true;
                                } else {
                                    ItemStack i = this.updateItem(item.clone());
                                    if (i != null) {
                                        sender.sendMessage("§a该物品已有有效期");
                                        return true;
                                    } else {
                                        String timeString = this.toTimeString(t);
                                        ItemMeta meta = item.getItemMeta();
                                        List lore = meta.getLore();
                                        if (lore == null) {
                                            lore = new ArrayList();
                                        }

                                        lore.add(this.lore2.replace("%time%", timeString + "§a§e§f"));
                                        meta.setLore(lore);
                                        item.setItemMeta(meta);
                                        player.setItemInHand(item);
                                        player.updateInventory();
                                        sender.sendMessage("§a成功设置手上的物品有效期为§6" + timeString + "§a,该物品将在非op玩家手持后解锁");
                                        return true;
                                    }
                                }
                            }
                        }
                    } else {
                        sender.sendMessage("§a手上的物品不能为空");
                        return true;
                    }
                }
            } else {
                sender.sendMessage("§a未知参数");
                return true;
            }
        } else {
            sender.sendMessage("§a/itemtime set <有效期>  为手上的物品设置有效期");
            sender.sendMessage("§6有效期格式: XdXhXmXs");
            sender.sendMessage("§6d=天,h=小时,m=分钟,s=秒");
            sender.sendMessage("§6例如: 1d=1天,1h5s=1小时5秒");
            sender.sendMessage("§a/itemtime add <有效期>  为手上的物品设置有效期,该有效期将在非op玩家手持后解锁");
            return true;
        }
    }

    public long getItemTime(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
            SimpleDateFormat sdf = new SimpleDateFormat(this.timeFormat);
            String matches = ".*[0-9]{4}年[0-9]{1,2}月[0-9]{1,2}日 [0-9]{2}:[0-9]{2}:[0-9]{2}.*";
            String get = "[0-9]{4}年[0-9]{1,2}月[0-9]{1,2}日 [0-9]{2}:[0-9]{2}:[0-9]{2}";

            for(String a : item.getItemMeta().getLore()) {
                if (a.matches(matches)) {
                    Pattern p = Pattern.compile(get);
                    Matcher m = p.matcher(a);
                    if (m.find()) {
                        try {
                            String t = m.group();
                            return sdf.parse(t).getTime();
                        } catch (Exception var10) {
                            var10.printStackTrace();
                        }
                    }
                }
            }

            return 0L;
        } else {
            return 0L;
        }
    }

    public String toTimeString(long time) {
        long d = time / 86400000L;
        long h = time % 86400000L / 3600000L;
        long m = time % 3600000L / 60000L;
        long s = time % 60000L / 1000L;
        String timeString = "";
        if (d > 0L) {
            timeString = d + "天";
        }

        if (h > 0L) {
            timeString = timeString + h + "小时";
        }

        if (m > 0L) {
            timeString = timeString + m + "分钟";
        }

        if (s > 0L) {
            timeString = timeString + s + "秒";
        }

        return timeString;
    }

    public long getTimeFromCommand(String a) {
        String dMatches = "[0-9]+d";
        String hMatches = "[0-9]+h";
        String mMatches = "[0-9]+m";
        String sMatches = "[0-9]+s";
        long d = 0L;
        long h = 0L;
        long m = 0L;
        long s = 0L;
        Pattern p = Pattern.compile(dMatches);
        Matcher ma = p.matcher(a);
        if (ma.find()) {
            d = this.getTime(ma.group());
        }

        p = Pattern.compile(hMatches);
        ma = p.matcher(a);
        if (ma.find()) {
            h = this.getTime(ma.group());
        }

        p = Pattern.compile(mMatches);
        ma = p.matcher(a);
        if (ma.find()) {
            m = this.getTime(ma.group());
        }

        p = Pattern.compile(sMatches);
        ma = p.matcher(a);
        if (ma.find()) {
            s = this.getTime(ma.group());
        }

        long time = d * 86400000L;
        time += h * 3600000L;
        time += m * 60000L;
        return time + s * 1000L;
    }

    public ItemStack updateItem(ItemStack item) {
        String dMatches = "[0-9]+天";
        String hMatches = "[0-9]+小时";
        String mMatches = "[0-9]+分钟";
        String sMatches = "[0-9]+秒";
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
            int index = -1;
            long d = 0L;
            long h = 0L;
            long m = 0L;
            long s = 0L;

            for(int x = 0; x < item.getItemMeta().getLore().size(); ++x) {
                String a = (String)item.getItemMeta().getLore().get(x);
                if (a.endsWith("§a§e§f")) {
                    Pattern p = Pattern.compile(dMatches);
                    Matcher ma = p.matcher(a);
                    if (ma.find()) {
                        d = this.getTime(ma.group());
                    }

                    p = Pattern.compile(hMatches);
                    ma = p.matcher(a);
                    if (ma.find()) {
                        h = this.getTime(ma.group());
                    }

                    p = Pattern.compile(mMatches);
                    ma = p.matcher(a);
                    if (ma.find()) {
                        m = this.getTime(ma.group());
                    }

                    p = Pattern.compile(sMatches);
                    ma = p.matcher(a);
                    if (ma.find()) {
                        s = this.getTime(ma.group());
                    }

                    if (d != 0L || h != 0L || m != 0L || s != 0L) {
                        index = x;
                        break;
                    }
                }
            }

            if (index == -1) {
                return null;
            } else {
                List lore = item.getItemMeta().getLore();
                long time = d * 86400000L;
                time += h * 3600000L;
                time += m * 60000L;
                time += s * 1000L;
                SimpleDateFormat sdf = new SimpleDateFormat(this.timeFormat);
                time += System.currentTimeMillis();
                String a = sdf.format(new Date(time));
                String b = this.lore1.replace("%time%", a);
                lore.set(index, b);
                ItemMeta meta = item.getItemMeta();
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
            }
        } else {
            return null;
        }
    }

    public long getTime(String a) {
        Pattern p = Pattern.compile("[0-9]+");
        Matcher m = p.matcher(a);
        return m.find() ? Long.parseLong(m.group()) : 0L;
    }
}
