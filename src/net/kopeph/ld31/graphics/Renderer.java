package net.kopeph.ld31.graphics;

import java.util.ArrayList;
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

		font = new Font("res/font-16-white.png"); //$NON-NLS-1$
	}
	
	public void cropTextures(int width, int height) {
		textureRed     = Util.crop(rawTextureRed    , 0, 0, width, height);
		textureGreen   = Util.crop(rawTextureGreen  , 0, 0, width, height);
		textureBlue    = Util.crop(rawTextureBlue   , 0, 0, width, height);
		textureCyan    = Util.crop(rawTextureCyan   , 0, 0, width, height);
		textureMagenta = Util.crop(rawTextureMagenta, 0, 0, width, height);
		textureYellow  = Util.crop(rawTextureYellow , 0, 0, width, height);
		textureGrey    = Util.crop(rawTextureGrey   , 0, 0, width, height);
		textureWhite   = Util.crop(rawTextureWhite  , 0, 0, width, height);
	}
	
	public void background(PImage img) {
		for (int x = 0; x < context.width; x += img.width)
			for (int y = 0; y < context.height; y += img.height)
				context.image(img, x, y);
	}
	
	public void calculateLighting(int[] lighting, Level level) {
		viewX = PApplet.max(0, PApplet.min(level.LEVEL_WIDTH - context.lastWidth, level.player.x() - context.lastWidth/2));
		viewY = PApplet.max(0, PApplet.min(level.LEVEL_HEIGHT - context.lastHeight, level.player.y() - context.lastHeight/2));
		
		//crop the tiles array into the pixels array
		//assumes the bottom left of the screen won't be outside the level unless the level is smaller than the screen
		for (int y = 0; y < PApplet.min(context.lastHeight, level.LEVEL_HEIGHT); ++y) {
			System.arraycopy(level.tiles, (y + viewY)*level.LEVEL_WIDTH + viewX, lighting, y*context.lastWidth, PApplet.min(context.lastWidth, level.LEVEL_WIDTH));
		}
		
		List<Enemy> order = new ArrayList<Enemy>(); //really just a stack, to be honest
		order.add(level.enemies[0]);
		
		
		
		//TODO: lighting scheduler
		
		
		
		for (final Enemy e : level.enemies) {
			//create a new thread to run the lighting process of each enemy
			if (e.screenX() > -e.viewDistance + 1 && e.screenX() < context.lastWidth + e.viewDistance - 2 &&
				e.screenY() > -e.viewDistance + 1 && e.screenY() < context.lastHeight + e.viewDistance - 2) {
				renderingPool.post(() -> { e.rayTrace(lighting, e.viewDistance, e.color); });
			}
		}
		
		renderingPool.forceSync();
	}
	
	private Enemy firstOutOfRange(List<Enemy> order, Level level) {
		//find the first enemy that is unlit and out of lighting range of all current lighting threads
		for (Enemy e : level.enemies)
			if (!order.contains(e) && safeToLight(order, e, renderingPool.poolSize - 1))
				return e;
		return null;
	}
	
	private boolean safeToLight(List<Enemy> order, Enemy e, int size) {
		//if the lighting range overlaps with any currently running lighting threads, cancel
		for (int i = PApplet.max(0, order.size() - size); i < order.size(); ++i)
			if (PApplet.dist(order.get(i).screenX(), order.get(i).screenY(), e.screenX(), e.screenY()) < e.viewDistance*2)
				return false; //TODO: optimize this if needed
		return true;
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
				case Level.FLOOR_NONE: break; //I don't know if this helps speed or not
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
