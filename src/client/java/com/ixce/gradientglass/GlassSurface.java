package com.ixce.gradientglass;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

// When a CTM pack is active, crossfade its ordinary and white glass materials.
// In the existing vertex grid ordinary glass has alpha 85, all dyes have alpha 255.
record GlassSurface(Sprite clear, Sprite stained) {
    // One cache shared by all wrappers from a single model reload. Resolve only
    // after baking, when the stitched block atlas is installed.
    static final class Cache {
        private volatile Textures textures;

        Textures get() {
            var current = textures;
            return current != null ? current : initialize();
        }

        private synchronized Textures initialize() {
            if (textures == null) textures = new Textures(MinecraftClient.getInstance()
                    .getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
            return textures;
        }
    }

    static final class Textures {
        final GlassSurface[] blocks = new GlassSurface[47];
        final GlassSurface[] panes = new GlassSurface[16];
        final GlassSurface cap;

        Textures(SpriteAtlasTexture atlas) {
            for (int mask = 0; mask < 16; mask++) {
                for (int diagonals = 0; diagonals <= 170; diagonals++) {
                    if ((diagonals & ~170) != 0) continue;
                    int tile = GlassCtmAtlas.tile(mask, diagonals);
                    if (blocks[tile] == null) blocks[tile] = resolve(atlas, mask, tile, false);
                }
                panes[mask] = resolve(atlas, mask, GlassCtmAtlas.tile(mask, 255), true);
            }
            cap = new GlassSurface(atlas.getSprite(Identifier.ofVanilla("block/glass_pane_top")),
                    atlas.getSprite(Identifier.ofVanilla("block/white_stained_glass_pane_top")));
        }
    }

    private static GlassSurface resolve(SpriteAtlasTexture atlas, int mask, int tile, boolean pane) {
        Sprite clear = find(atlas, tile, 0, pane);
        Sprite stained = find(atlas, tile, 1, pane);
        if (clear == null && stained == null) {
            return new GlassSurface(pane ? null : atlas.getSprite(GradientGlass.CLEAR_TEXTURES[mask]),
                    atlas.getSprite(GradientGlass.TEXTURES[mask]));
        }
        if (clear == null) clear = atlas.getSprite(Identifier.ofVanilla("block/glass"));
        if (stained == null) stained = atlas.getSprite(GradientGlass.TEXTURES[mask]);
        return new GlassSurface(clear, stained);
    }

    private static Sprite find(SpriteAtlasTexture atlas, int tile, int material, boolean pane) {
        if (pane) {
            Identifier id = GlassCtmAtlas.IDS[material + 2][tile];
            var sprite = atlas.getSprite(id);
            if (sprite.getContents().getId().equals(id)) return sprite;
        }
        Identifier id = GlassCtmAtlas.IDS[material][tile];
        var sprite = atlas.getSprite(id);
        return sprite.getContents().getId().equals(id) ? sprite : null;
    }

    Sprite sprite(int layer) {
        return layer == 0 ? clear : stained;
    }

    int tint(int argb, int layer) {
        if (clear == null) return argb;
        int weight = ((argb >>> 24) - 85) * 255 / 170;
        return layer == 0 ? ((255 - weight) << 24) | 0xffffff : (weight << 24) | (argb & 0xffffff);
    }
}
