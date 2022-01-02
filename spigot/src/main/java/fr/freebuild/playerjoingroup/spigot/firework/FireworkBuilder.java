package fr.freebuild.playerjoingroup.spigot.firework;

import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.logging.Level;

public class FireworkBuilder {

    private Boolean activateOnJoin = false;
    private FireworkEffect effect;

    /**
     * Get a color from config
     *
     * @param path Path to config
     * @return
     */
    private Color getColor(String path) {
        final FileConfiguration config = PlayerJoinGroup.plugin.getConfig();
        final Integer red = config.getInt(path + ".R", 255);
        final Integer green = config.getInt(path + ".G", 255);
        final Integer blue = config.getInt(path + ".B", 255);

        return Color.fromRGB(red, green, blue);
    }

    /**
     * Get type of firework effect
     *
     * @return
     * @throws FireworkException
     */
    private FireworkEffect.Type getEffectType() throws FireworkException {
        final String type = PlayerJoinGroup.plugin.getConfig().getString("Firework.Type", "BALL_LARGE");

        try {
            final FireworkEffect.Type effectType = FireworkEffect.Type.valueOf(type);
            return effectType;
        } catch (IllegalArgumentException e) {
            throw new FireworkException(e.getMessage());
        }
    }

    /**
     * Build the properties of a firework
     *
     * @throws FireworkException
     */
    private FireworkEffect buildEffect() throws FireworkException {
        return FireworkEffect
                .builder()
                .with(this.getEffectType())
                .withColor(this.getColor("Firework.Color"))
                .withFade(this.getColor("Firework.ColorFade"))
                .build();
    }

    /**
     * Spawn a firework one bloc above the player
     *
     * @param player Player target
     */
    public void spawn(Player player) {
        final Location location = player.getLocation().add(0, 1, 0);
        Firework firework = (Firework)location.getWorld().spawnEntity(location, EntityType.FIREWORK);

        final FireworkMeta meta = firework.getFireworkMeta();
        meta.clearEffects();
        meta.addEffect(effect);
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    /**
     * Load fireworkbuilder
     */
    public void load() {
        try {
            this.activateOnJoin = PlayerJoinGroup.plugin.getConfig().getBoolean("Firework.ActivateOnJoin", false);
            this.effect = this.buildEffect();
        } catch (FireworkException e) {
            this.activateOnJoin = false;
            PlayerJoinGroup.plugin.getLogger().log(Level.SEVERE, e.getMessage());
        }
    }

    public Boolean getActivateOnJoin() {
        return activateOnJoin;
    }
}
