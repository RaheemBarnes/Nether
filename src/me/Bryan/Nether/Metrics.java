package me.Bryan.Nether;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitTask;

public class Metrics {
	
	 private final Plugin plugin;
	    @SuppressWarnings({ "unchecked", "rawtypes" })
		private final Set<Graph> graphs = Collections.synchronizedSet(new HashSet());
	    private final YamlConfiguration configuration;
	    private final File configurationFile;
	    private final String guid;
	    private final boolean debug;
	    private final Object optOutLock = new Object();
	    private volatile BukkitTask task = null;

	    public Metrics(Plugin plugin) throws IOException {
	        if (plugin == null) {
	            throw new IllegalArgumentException("Plugin cannot be null");
	        }
	        this.plugin = plugin;
	        this.configurationFile = this.getConfigFile();
	        this.configuration = YamlConfiguration.loadConfiguration((File)this.configurationFile);
	        this.configuration.addDefault("opt-out", (Object)false);
	        this.configuration.addDefault("guid", (Object)UUID.randomUUID().toString());
	        this.configuration.addDefault("debug", (Object)false);
	        if (this.configuration.get("guid", (Object)null) == null) {
	            this.configuration.options().header("http://mcstats.org").copyDefaults(true);
	            this.configuration.save(this.configurationFile);
	        }
	        this.guid = this.configuration.getString("guid");
	        this.debug = this.configuration.getBoolean("debug", false);
	    }

	    public Graph createGraph(String name) {
	        if (name == null) {
	            throw new IllegalArgumentException("Graph name cannot be null");
	        }
	        Graph graph = new Graph(name, null);
	        this.graphs.add(graph);
	        return graph;
	    }

	    public void addGraph(Graph graph) {
	        if (graph == null) {
	            throw new IllegalArgumentException("Graph cannot be null");
	        }
	        this.graphs.add(graph);
	    }

	    /*
	     * WARNING - Removed try catching itself - possible behaviour change.
	     */
	    public boolean start() {
	        Object object = this.optOutLock;
	        synchronized (object) {
	            block6 : {
	                block5 : {
	                    if (!this.isOptOut()) break block5;
	                    return false;
	                }
	                if (this.task == null) break block6;
	                return true;
	            }
	            this.task = this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin, new Runnable(){
	                private boolean firstPost;

	                /*
	                 * WARNING - Removed try catching itself - possible behaviour change.
	                 */
	                @Override
	                public void run() {
	                    block7 : {
	                        try {
	                            Object object = Metrics.this.optOutLock;
	                            synchronized (object) {
	                                if (Metrics.this.isOptOut() && Metrics.this.task != null) {
	                                    Metrics.this.task.cancel();
	                                    Metrics.access$2(Metrics.this, null);
	                                    for (Graph graph : Metrics.this.graphs) {
	                                        graph.onOptOut();
	                                    }
	                                }
	                            }
	                            Metrics.this.postPlugin(!this.firstPost);
	                            this.firstPost = false;
	                        }
	                        catch (IOException e) {
	                            if (!Metrics.this.debug) break block7;
	                            Bukkit.getLogger().log(Level.INFO, "[Metrics] " + e.getMessage());
	                        }
	                    }
	                }
	            }, 0, 18000);
	            return true;
	        }
	    }

	    /*
	     * WARNING - Removed try catching itself - possible behaviour change.
	     * Enabled aggressive block sorting
	     * Enabled unnecessary exception pruning
	     * Enabled aggressive exception aggregation
	     */
	    public boolean isOptOut() {
	        Object object = this.optOutLock;
	        synchronized (object) {
	            try {
	                this.configuration.load(this.getConfigFile());
	            }
	            catch (IOException ex) {
	                if (this.debug) {
	                    Bukkit.getLogger().log(Level.INFO, "[Metrics] " + ex.getMessage());
	                }
	                return true;
	            }
	            catch (InvalidConfigurationException ex) {
	                if (this.debug) {
	                    Bukkit.getLogger().log(Level.INFO, "[Metrics] " + ex.getMessage());
	                }
	                return true;
	            }
	            return this.configuration.getBoolean("opt-out", false);
	        }
	    }

	    /*
	     * WARNING - Removed try catching itself - possible behaviour change.
	     */
	    public void enable() throws IOException {
	        Object object = this.optOutLock;
	        synchronized (object) {
	            if (this.isOptOut()) {
	                this.configuration.set("opt-out", (Object)false);
	                this.configuration.save(this.configurationFile);
	            }
	            if (this.task == null) {
	                this.start();
	            }
	        }
	    }

	    /*
	     * WARNING - Removed try catching itself - possible behaviour change.
	     */
	    public void disable() throws IOException {
	        Object object = this.optOutLock;
	        synchronized (object) {
	            if (!this.isOptOut()) {
	                this.configuration.set("opt-out", (Object)true);
	                this.configuration.save(this.configurationFile);
	            }
	            if (this.task != null) {
	                this.task.cancel();
	                this.task = null;
	            }
	        }
	    }

	    public File getConfigFile() {
	        File pluginsFolder = this.plugin.getDataFolder().getParentFile();
	        return new File(new File(pluginsFolder, "PluginMetrics"), "config.yml");
	    }

	    /*
	     * WARNING - Removed try catching itself - possible behaviour change.
	     */
	    private void postPlugin(boolean isPing) throws IOException {
	        PluginDescriptionFile description = this.plugin.getDescription();
	        String pluginName = description.getName();
	        boolean onlineMode = Bukkit.getServer().getOnlineMode();
	        String pluginVersion = description.getVersion();
	        String serverVersion = Bukkit.getVersion();
	        int playersOnline = Bukkit.getServer().getOnlinePlayers().size();
	        StringBuilder json = new StringBuilder(1024);
	        json.append('{');
	        Metrics.appendJSONPair(json, "guid", this.guid);
	        Metrics.appendJSONPair(json, "plugin_version", pluginVersion);
	        Metrics.appendJSONPair(json, "server_version", serverVersion);
	        Metrics.appendJSONPair(json, "players_online", Integer.toString(playersOnline));
	        String osname = System.getProperty("os.name");
	        String osarch = System.getProperty("os.arch");
	        String osversion = System.getProperty("os.version");
	        String java_version = System.getProperty("java.version");
	        int coreCount = Runtime.getRuntime().availableProcessors();
	        if (osarch.equals("amd64")) {
	            osarch = "x86_64";
	        }
	        Metrics.appendJSONPair(json, "osname", osname);
	        Metrics.appendJSONPair(json, "osarch", osarch);
	        Metrics.appendJSONPair(json, "osversion", osversion);
	        Metrics.appendJSONPair(json, "cores", Integer.toString(coreCount));
	        Metrics.appendJSONPair(json, "auth_mode", onlineMode ? "1" : "0");
	        Metrics.appendJSONPair(json, "java_version", java_version);
	        if (isPing) {
	            Metrics.appendJSONPair(json, "ping", "1");
	        }
	        if (this.graphs.size() > 0) {
	            Set<Graph> set = this.graphs;
	            synchronized (set) {
	                json.append(',');
	                json.append('\"');
	                json.append("graphs");
	                json.append('\"');
	                json.append(':');
	                json.append('{');
	                boolean firstGraph = true;
	                for (Graph graph : this.graphs) {
	                    StringBuilder graphJson = new StringBuilder();
	                    graphJson.append('{');
	                    for (Plotter plotter : graph.getPlotters()) {
	                        Metrics.appendJSONPair(graphJson, plotter.getColumnName(), Integer.toString(plotter.getValue()));
	                    }
	                    graphJson.append('}');
	                    if (!firstGraph) {
	                        json.append(',');
	                    }
	                    json.append(Metrics.escapeJSON(graph.getName()));
	                    json.append(':');
	                    json.append(graphJson);
	                    firstGraph = false;
	                }
	                json.append('}');
	            }
	        }
	        json.append('}');
	        URL url = new URL("http://report.mcstats.org" + String.format("/plugin/%s", Metrics.urlEncode(pluginName)));
	        URLConnection connection = this.isMineshafterPresent() ? url.openConnection(Proxy.NO_PROXY) : url.openConnection();
	        byte[] uncompressed = json.toString().getBytes();
	        byte[] compressed = Metrics.gzip(json.toString());
	        connection.addRequestProperty("User-Agent", "MCStats/7");
	        connection.addRequestProperty("Content-Type", "application/json");
	        connection.addRequestProperty("Content-Encoding", "gzip");
	        connection.addRequestProperty("Content-Length", Integer.toString(compressed.length));
	        connection.addRequestProperty("Accept", "application/json");
	        connection.addRequestProperty("Connection", "close");
	        connection.setDoOutput(true);
	        if (this.debug) {
	            System.out.println("[Metrics] Prepared request for " + pluginName + " uncompressed=" + uncompressed.length + " compressed=" + compressed.length);
	        }
	        OutputStream os = connection.getOutputStream();
	        os.write(compressed);
	        os.flush();
	        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        String response = reader.readLine();
	        os.close();
	        reader.close();
	        if (response == null || response.startsWith("ERR") || response.startsWith("7")) {
	            if (response == null) {
	                response = "null";
	            } else if (response.startsWith("7")) {
	                response = response.substring(response.startsWith("7,") ? 2 : 1);
	            }
	            throw new IOException(response);
	        }
	        if (response.equals("1") || response.contains("This is your first update this hour")) {
	            Set<Graph> set = this.graphs;
	            synchronized (set) {
	                for (Graph graph : this.graphs) {
	                    for (Plotter plotter : graph.getPlotters()) {
	                        plotter.reset();
	                    }
	                }
	            }
	        }
	    }

	    public static byte[] gzip(String input) {
	        ByteArrayOutputStream baos;
	        block14 : {
	            GZIPOutputStream gzos;
	            baos = new ByteArrayOutputStream();
	            gzos = null;
	            try {
	                try {
	                    gzos = new GZIPOutputStream(baos);
	                    gzos.write(input.getBytes("UTF-8"));
	                }
	                catch (IOException e) {
	                    e.printStackTrace();
	                    if (gzos != null) {
	                        try {
	                            gzos.close();
	                        }
	                        catch (IOException var5_4) {}
	                    }
	                    break block14;
	                }
	            }
	            catch (Throwable var4_7) {
	                if (gzos != null) {
	                    try {
	                        gzos.close();
	                    }
	                    catch (IOException var5_5) {
	                        // empty catch block
	                    }
	                }
	                throw var4_7;
	            }
	            if (gzos != null) {
	                try {
	                    gzos.close();
	                }
	                catch (IOException var5_6) {
	                    // empty catch block
	                }
	            }
	        }
	        return baos.toByteArray();
	    }

	    private boolean isMineshafterPresent() {
	        try {
	            Class.forName("mineshafter.MineServer");
	            return true;
	        }
	        catch (Exception e) {
	            return false;
	        }
	    }

	    private static void appendJSONPair(StringBuilder json, String key, String value) throws UnsupportedEncodingException {
	        boolean isValueNumeric = false;
	        try {
	            if (value.equals("0") || !value.endsWith("0")) {
	                Double.parseDouble(value);
	                isValueNumeric = true;
	            }
	        }
	        catch (NumberFormatException e) {
	            isValueNumeric = false;
	        }
	        if (json.charAt(json.length() - 1) != '{') {
	            json.append(',');
	        }
	        json.append(Metrics.escapeJSON(key));
	        json.append(':');
	        if (isValueNumeric) {
	            json.append(value);
	        } else {
	            json.append(Metrics.escapeJSON(value));
	        }
	    }

	    private static String escapeJSON(String text) {
	        StringBuilder builder = new StringBuilder();
	        builder.append('\"');
	        int index = 0;
	        while (index < text.length()) {
	            char chr = text.charAt(index);
	            switch (chr) {
	                case '\"': 
	                case '\\': {
	                    builder.append('\\');
	                    builder.append(chr);
	                    break;
	                }
	                case '\b': {
	                    builder.append("\\b");
	                    break;
	                }
	                case '\t': {
	                    builder.append("\\t");
	                    break;
	                }
	                case '\n': {
	                    builder.append("\\n");
	                    break;
	                }
	                case '\r': {
	                    builder.append("\\r");
	                    break;
	                }
	                default: {
	                    if (chr < ' ') {
	                        String t = "000" + Integer.toHexString(chr);
	                        builder.append("\\u" + t.substring(t.length() - 4));
	                        break;
	                    }
	                    builder.append(chr);
	                }
	            }
	            ++index;
	        }
	        builder.append('\"');
	        return builder.toString();
	    }

	    private static String urlEncode(String text) throws UnsupportedEncodingException {
	        return URLEncoder.encode(text, "UTF-8");
	    }

	    static void access$2(Metrics metrics, BukkitTask bukkitTask) {
	        metrics.task = bukkitTask;
	    }

	    public static class Graph {
	        private final String name;
	        private final Set<Plotter> plotters;

	        public Graph(String name) {
	            this.plotters = new LinkedHashSet<Plotter>();
	            this.name = name;
	        }

	        public String getName() {
	            return this.name;
	        }

	        public void addPlotter(Plotter plotter) {
	            this.plotters.add(plotter);
	        }

	        public void removePlotter(Plotter plotter) {
	            this.plotters.remove(plotter);
	        }

	        public Set<Plotter> getPlotters() {
	            return Collections.unmodifiableSet(this.plotters);
	        }

	        public int hashCode() {
	            return this.name.hashCode();
	        }

	        public boolean equals(Object object) {
	            if (!(object instanceof Graph)) {
	                return false;
	            }
	            Graph graph = (Graph)object;
	            return graph.name.equals(this.name);
	        }

	        protected void onOptOut() {
	        }

	        Graph(String string, Graph graph) {
	            Graph graph76;
	            graph76(string, graph);
	        }


			

	    }

	    public static abstract class Plotter {
	        private final String name;

	        public Plotter() {
	            this("Default");
	        }

	        public Plotter(String name) {
	            this.name = name;
	        }

	        public abstract int getValue();

	        public String getColumnName() {
	            return this.name;
	        }

	        public void reset() {
	        }

	        public int hashCode() {
	            return this.getColumnName().hashCode();
	        }

	        public boolean equals(Object object) {
	            if (!(object instanceof Plotter)) {
	                return false;
	            }
	            Plotter plotter = (Plotter)object;
	            if (plotter.name.equals(this.name) && plotter.getValue() == this.getValue()) {
	                return true;
	            }
	            return false;
	        }
	    }

	

	}

