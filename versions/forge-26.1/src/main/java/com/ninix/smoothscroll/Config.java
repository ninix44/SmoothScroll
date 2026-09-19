package com.ninix.smoothscroll;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
public class Config {

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path FILE = FMLPaths.CONFIGDIR.get().resolve("smoothscroll.json");
    private final float FORMAT = 2.2F;

    private final String[] NOTES = {
            "Safe values for settings are 0 - 1 (inclusive).",
            "0 means animation off (no smoothness) and bigger values mean slower animation speed (high smoothness).",
            "Press F3+T in a world to update the config.",
            "Config file is shared with the fabric version of the mod."
    };

    public float hotbar = 0.2F;
    public boolean rollover = true;
    public float chat = 0.5F;
    public float chatOpening = 0.5F;
    public float creative = 0.5F;
    public float list = 0.5F;
    public double listSpeed = 30.0D;

    public void load() {
        JsonObject root = read();

        hotbar = number(root, "Hotbar", "Smoothness", hotbar);
        rollover = flag(root, "Hotbar", "Rollover", rollover);
        chat = number(root, "Chat", "Smoothness", chat);
        chatOpening = number(root, "Chat", "Opening Speed", chatOpening);
        creative = number(root, "Creative Screen", "Smoothness", creative);
        list = number(root, "Entry List", "Smoothness", list);
        listSpeed = number(root, "Entry List", "Speed", (float) listSpeed);

        write(root);
    }

    private JsonObject read() {
        if (!Files.isRegularFile(FILE)) {
            return new JsonObject();
        }

        try {
            return JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            return new JsonObject();
        }
    }

    private void write(JsonObject root) {
        JsonArray notes = new JsonArray();
        for (String note : NOTES) {
            notes.add(note);
        }
        root.add("Notes", notes);

        section(root, "Hotbar").addProperty("Smoothness", hotbar);
        section(root, "Hotbar").addProperty("Rollover", rollover);

        section(root, "Chat").addProperty("Smoothness", chat);
        section(root, "Chat").addProperty("Opening Speed", chatOpening);

        section(root, "Creative Screen").addProperty("Smoothness", creative);

        section(root, "Entry List").addProperty("Smoothness", list);
        section(root, "Entry List").addProperty("Speed", listSpeed);

        root.addProperty("Format", FORMAT);

        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root));
        } catch (IOException ignored) {
        }
    }

    private JsonObject section(JsonObject root, String name) {
        JsonObject values = root.getAsJsonObject(name);

        if (values == null) {
            values = new JsonObject();
            root.add(name, values);
        }

        return values;
    }

    private float number(JsonObject root, String section, String key, float fallback) {
        JsonObject values = root.getAsJsonObject(section);
        return values != null && values.has(key) ? values.get(key).getAsFloat() : fallback;
    }

    private boolean flag(JsonObject root, String section, String key, boolean fallback) {
        JsonObject values = root.getAsJsonObject(section);
        return values != null && values.has(key) ? values.get(key).getAsBoolean() : fallback;
    }
}
