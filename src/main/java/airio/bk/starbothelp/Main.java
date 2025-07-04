package airio.bk.starbothelp;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class Main extends Plugin {

    private String url;
    private int interval;

    @Override
    public void onEnable() {
        loadConfig();

        getLogger().info("BanKicker 插件已启动，每 " + interval + " 秒检查一次封禁玩家");

        ProxyServer.getInstance().getScheduler().schedule(this, this::fetchAndKick,
                0, interval, TimeUnit.SECONDS);
    }

    private void loadConfig() {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();

            File configFile = new File(dataFolder, "config.yml");
            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    if (in != null) {
                        try (OutputStream out = new FileOutputStream(configFile)) {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = in.read(buffer)) > 0) {
                                out.write(buffer, 0, len);
                            }
                        }
                    }
                }
            }

            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            this.url = config.getString("url", "http://192.168.31.11:35760/bannedplayer/?token=41c520");
            this.interval = config.getInt("interval", 15);

        } catch (IOException e) {
            getLogger().warning("加载配置文件失败: " + e.getMessage());
            this.url = "http://192.168.31.11:35760/bannedplayer/?token=41c520";
            this.interval = 15;
        }
    }

    private void fetchAndKick() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int status = conn.getResponseCode();
            if (status != 200) {
                getLogger().warning("请求失败，状态码: " + status);
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            String content = response.toString().trim();
            if (content.isEmpty()) {
                // 返回内容为空白，不做处理
                return;
            }

            String[] ids = content.split(";");
            for (String id : ids) {
                id = id.trim();
                if (!id.isEmpty()) {
                    getLogger().info("踢出玩家: " + id);
                    ProxyServer.getInstance().getPluginManager().dispatchCommand(
                            ProxyServer.getInstance().getConsole(), "kick " + id);
                }
            }

        } catch (Exception e) {
            getLogger().warning("请求或处理错误: " + e.getMessage());
        }
    }
}
