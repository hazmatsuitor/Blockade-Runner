package net.kopeph.ld31.graphics;

import java.util.List;
import java.util.Map;

import net.kopeph.ld31.LD31;
import net.kopeph.ld31.Level;
import net.kopeph.ld31.entity.Enemy;
import net.kopeph.ld31.util.ThreadPool;
import net.kopeph.ld31.util.Util;
import processing.core.PApplet;
import processing.core.PImage;

public class Renderer {
	public PImage textureRed    , rawTextureRed;
	public PImage textureGreen  , rawTextureGreen;
	public PImage textureBlue   , rawTextureBlue;
	public PImage textureCyan   , rawTextureCyan;
	public PImage textureMagenta, rawTextureMagenta;
	public PImage textureYellow , rawTextureYellow;
	public PImage textureGrey   , rawTextureGrey;
	public PImage textureWhite  , rawTextureWhite;
	public PImage textureBlack  , rawTextureBlack;
	public Font font;

	public int viewX = 0, viewY = 0;

	private final LD31 context;
	private final ThreadPool renderingPool = new ThreadPool();

	private Map<Integer, List<Renderable>> states;

	public Renderer() {
		context = LD31.getContext();

		//load raw textures
		rawTextureRed     = context.loadImage("res/red-background.jpg"    ); //$NON-NLS-1$
		rawTextureGreen   = context.loadImage("res/green-background.jpg"  ); //$NON-NLS-1$
		rawTextureBlue    = context.loadImage("res/blue-background.jpg"   ); //$NON-NLS-1$
		rawTextureCyan    = context.loadImage("res/cyan-background.jpg"   ); //$NON-NLS-1$
		rawTextureMagenta = context.loadImage("res/magenta-background.jpg"); //$NON-NLS-1$
		rawTextureYellow  = context.loadImage("res/yellow-background.jpg" ); //$NON-NLS-1$
		rawTextureGrey    = context.loadImage("res/grey-background.jpg"   ); //$NON-NLS-1$
		rawTextureWhite   = context.loadImage("res/white-background.jpg"  ); //$NON-NLS-1$
		rawTextureBlack   = context.loadImage("res/black-background.jpg"  ); //$NON-NLS-1$

		font = new Font("res/font-16-white.png"); //$NON-NLS-1$
	}

	public void cropTextures(int width, int height) {
		textureRed     = Util.crop(rawTextureRed    , width, height);
		textureGreen   = Util.crop(rawTextureGreen  , width, height);
		textureBlue    = Util.crop(rawTextureBlue   , width, height);
		textureCyan    = Util.crop(rawTextureCyan   , width, height);
		textureMagenta = Util.crop(rawTextureMagenta, width, height);
		textureYellow  = Util.crop(rawTextureYellow , width, height);
		textureGrey    = Util.crop(rawTextureGrey   , width, height);
		textureWhite   = Util.crop(rawTextureWhite  , width, height);
		textureBlack   = Util.crop(rawTextureBlack  , width, height);
	}

	public void calculateLighting(int[] lighting, Level level) {
		viewX = PApplet.max(0, PApplet.min(level.LEVEL_WIDTH - context.lastWidth, level.player.x() - context.lastWidth/2));
		viewY = PApplet.max(0, PApplet.min(level.LEVEL_HEIGHT - context.lastHeight, level.player.y() - context.lastHeight/2));

		//crop the tiles array into the pixels array
		//assumes the bottom left of the screen won't be outside the level unless the level is smaller than the screen
		for (int y = 0; y < PApplet.min(context.lastHeight, level.LEVEL_HEIGHT); ++y) {
			System.arraycopy(level.tiles, (y + viewY)*level.LEVEL_WIDTH + viewX, lighting, y*context.lastWidth, PApplet.min(context.lastWidth, level.LEVEL_WIDTH));
		}

		for (final Enemy e : level.enemies) {
			//create a new thread to run the lighting process of each enemy
			if (e.screenX() > -e.viewDistance + 1 && e.screenX() < context.lastWidth + e.viewDistance - 2 &&
				e.screenY() > -e.viewDistance + 1 && e.screenY() < context.lastHeight + e.viewDistance - 2) {
				renderingPool.post(() -> { e.rayTrace(lighting, e.viewDistance, e.color); });
			}
		}

		renderingPool.forceSync();
	}

	public void applyTexture(int[] pixels) {
		float taskSize = pixels.length/renderingPool.poolSize;
		for (int i = 0; i < renderingPool.poolSize; ++i) {
			final int j = i;
			renderingPool.post(() -> { applyTextureImpl(pixels, PApplet.round(j*taskSize), PApplet.round((j+1)*taskSize)); });
		}

		renderingPool.forceSync();
	}

	private void applyTextureImpl(final int[] pixels, int iBegin, int iEnd) {
		for (int i = iBegin; i < iEnd; ++i) {
			switch (pixels[i]) {
				case Level.FLOOR_NONE:    pixels[i] = textureBlack.pixels[i];   break;
				case Level.FLOOR_RED:     pixels[i] = textureRed.pixels[i];     break;
				case Level.FLOOR_GREEN:   pixels[i] = textureGreen.pixels[i];   break;
				case Level.FLOOR_BLUE:    pixels[i] = textureBlue.pixels[i];    break;
				case Level.FLOOR_CYAN:    pixels[i] = textureCyan.pixels[i];    break;
				case Level.FLOOR_MAGENTA: pixels[i] = textureMagenta.pixels[i]; break;
				case Level.FLOOR_YELLOW:  pixels[i] = textureYellow.pixels[i];  break;
				case Level.FLOOR_BLACK:   pixels[i] = textureGrey.pixels[i];    break;
				case Level.FLOOR_WHITE:   pixels[i] = textureWhite.pixels[i];   break;
			}
		}
	}

	public void addContext(Integer state, List<Renderable> targets) {
		states.put(state, targets);
	}

	public void renderContext(Integer state) {
		for (Renderable r : states.get(state))
			r.render();
	}

	public void renderEntities(Level level) {
		level.objective.render();
		level.player.render();
		for (Enemy e : level.enemies)
			e.render();
	}
}
