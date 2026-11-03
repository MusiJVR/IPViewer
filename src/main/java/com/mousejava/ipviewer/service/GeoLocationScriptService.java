package com.mousejava.ipviewer.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mousejava.ipviewer.data.GeoLocation;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class GeoLocationScriptService implements AutoCloseable {
    public static final String DEFAULT_SCRIPT = "default.js";
    private static final Pattern SCRIPT_NAME = Pattern.compile("[A-Za-z0-9_-][A-Za-z0-9._-]*\\.js");

    private final JavaPlugin plugin;
    private final Path scriptsDirectory;
    private final HttpClient httpClient;
    private final Engine engine;

    public GeoLocationScriptService(JavaPlugin plugin) {
        this.plugin = plugin;
        scriptsDirectory = plugin.getDataFolder().toPath().resolve("scripts");

        try {
            if (!Files.exists(scriptsDirectory.resolve(DEFAULT_SCRIPT)))
                generateDefaultScript();
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the default GeoIP script", e);
        }

        engine = Engine.newBuilder("js").build();
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void generateDefaultScript() throws IOException {
        try (InputStream resource = plugin.getResource("scripts/" + DEFAULT_SCRIPT)) {
            if (resource == null)
                throw new IOException("Bundled GeoIP script is missing");

            Files.createDirectories(scriptsDirectory);
            Files.copy(resource, scriptsDirectory.resolve(DEFAULT_SCRIPT), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static boolean isValidScriptName(String file) {
        return file != null && SCRIPT_NAME.matcher(file).matches();
    }

    public String getSelectedScript() {
        return plugin.getConfig().getString("script_geo_location", DEFAULT_SCRIPT);
    }

    public void setSelectedScript(String file) throws IOException {
        if (!Files.isRegularFile(resolveScript(file)))
            throw new NoSuchFileException(file);

        Object previous = plugin.getConfig().get("script_geo_location");
        plugin.getConfig().set("script_geo_location", file);
        try {
            plugin.getConfig().save(plugin.getDataFolder().toPath().resolve("config.yml").toFile());
        } catch (IOException e) {
            plugin.getConfig().set("script_geo_location", previous);
            throw e;
        }
    }

    public List<String> listScripts() throws IOException {
        try (Stream<Path> files = Files.list(scriptsDirectory)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(GeoLocationScriptService::isValidScriptName)
                    .sorted()
                    .toList();
        }
    }

    public GeoLocation lookup(String scriptName, String ip) throws IOException {
        Path script = resolveScript(scriptName);
        Source source = Source.newBuilder("js", Files.readString(script, StandardCharsets.UTF_8), script.toString())
                .buildLiteral();
        try (Context context = Context.newBuilder("js")
                .engine(engine)
                .allowHostAccess(HostAccess.NONE)
                .build()) {
            context.getBindings("js").putMember("httpGet", (ProxyExecutable) this::httpGet);
            context.eval(source);
            Value function = context.getBindings("js").getMember("getGeoLocation");
            if (function == null || !function.canExecute())
                throw new IllegalArgumentException("GeoIP script must define getGeoLocation(ip)");

            Value result = function.execute(ip);
            String jsonString = result.isString() ? result.asString()
                    : context.eval("js", "JSON.stringify").execute(result).asString();
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            return new GeoLocation(getRequiredString(json, "country"), getRequiredString(json, "state"));
        }
    }

    private Path resolveScript(String file) {
        if (!isValidScriptName(file))
            throw new IllegalArgumentException("GeoIP script filename must match <name>.js (letters, digits, dots, underscores and hyphens)");
        return scriptsDirectory.resolve(file);
    }

    private Object httpGet(Value... arguments) {
        if (arguments.length < 1 || arguments.length > 2 || !arguments[0].isString())
            throw new IllegalArgumentException("Usage: httpGet(url, headers?)");

        URI uri = URI.create(arguments[0].asString());
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
            throw new IllegalArgumentException("httpGet requires an HTTP or HTTPS URL");

        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET();
        if (arguments.length == 2) {
            Value headers = arguments[1];
            if (!headers.hasMembers())
                throw new IllegalArgumentException("httpGet headers must be an object of strings");

            for (String name : headers.getMemberKeys()) {
                Value value = headers.getMember(name);
                if (!value.isString())
                    throw new IllegalArgumentException("HTTP header values must be strings");

                request.header(name, value.asString());
            }
        }

        try {
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IOException("GeoIP service returned HTTP " + response.statusCode());

            return response.body();
        } catch (IOException e) {
            throw new UncheckedIOException("GeoIP HTTP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GeoIP HTTP request interrupted", e);
        }
    }

    private static String getRequiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isString())
            throw new IllegalArgumentException("GeoIP result must contain a string field: " + key);

        return json.get(key).getAsString();
    }

    @Override
    public void close() {
        httpClient.shutdownNow();
        engine.close(true);
    }
}
