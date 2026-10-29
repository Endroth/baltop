package com.servidor.baltop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Baltop extends JavaPlugin {

    private static final int POR_PAGINA = 10;
    private static final long ATUALIZAR_TICKS = 20L * 60; // atualiza o ranking a cada 60s

    private Economy eco;
    private volatile List<Map.Entry<String, Double>> ranking = new ArrayList<>();

    @Override
    public void onEnable() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("Nenhum plugin de economia encontrado no Vault! Desativando.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        eco = rsp.getProvider();

        // Roda na thread principal (seguro para o Vault) e guarda em cache
        Bukkit.getScheduler().runTaskTimer(this, this::atualizar, 20L, ATUALIZAR_TICKS);
    }

    private void atualizar() {
        List<Map.Entry<String, Double>> novo = new ArrayList<>();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getName() == null) continue;
            if (!p.hasPlayedBefore() && !p.isOnline()) continue;
            double saldo = eco.getBalance(p);
            novo.add(Map.entry(p.getName(), saldo));
        }
        novo.sort(Comparator.comparingDouble((Map.Entry<String, Double> e) -> e.getValue()).reversed());
        ranking = novo;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("baltop")) return false;

        if (!sender.hasPermission("baltop.use")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão.");
            return true;
        }

        // Atualiza na hora se o cache ainda estiver vazio
        if (ranking.isEmpty()) atualizar();
        List<Map.Entry<String, Double>> lista = ranking;

        int paginas = Math.max(1, (int) Math.ceil(lista.size() / (double) POR_PAGINA));
        int pagina = 1;
        if (args.length > 0) {
            try {
                pagina = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Use: /baltop [página]");
                return true;
            }
        }
        pagina = Math.max(1, Math.min(pagina, paginas));

        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Top Dinheiro "
                + ChatColor.GRAY + "(" + pagina + "/" + paginas + ") " + ChatColor.GOLD + "" + ChatColor.BOLD + "===");

        int inicio = (pagina - 1) * POR_PAGINA;
        int fim = Math.min(inicio + POR_PAGINA, lista.size());
        for (int i = inicio; i < fim; i++) {
            Map.Entry<String, Double> e = lista.get(i);
            ChatColor cor = i == 0 ? ChatColor.GOLD : i == 1 ? ChatColor.YELLOW : i == 2 ? ChatColor.WHITE : ChatColor.GRAY;
            sender.sendMessage(cor + "#" + (i + 1) + " " + ChatColor.GREEN + e.getKey()
                    + ChatColor.DARK_GRAY + " - " + ChatColor.AQUA + eco.format(e.getValue()));
        }

        if (pagina < paginas) {
            sender.sendMessage(ChatColor.GRAY + "Use /baltop " + (pagina + 1) + " para a próxima página.");
        }
        return true;
    }
}
