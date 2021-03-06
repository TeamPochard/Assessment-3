package com.superduckinvaders.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.superduckinvaders.game.assets.Assets;
import com.superduckinvaders.game.entity.*;
import com.superduckinvaders.game.entity.Character;
import com.superduckinvaders.game.entity.item.PowerupManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Screen for interaction with the game.
 */
public class GameScreen implements Screen {

    /**
     * The scale of the game pixels.
     */
    private static final float SCALE = 2;

    /**
     * The game camera.
     */
    private OrthographicCamera camera;

    /**
     * The renderer for the tile map.
     */
    private OrthogonalTiledMapRenderer mapRenderer;

    /**
     * The sprite batches for rendering.
     */
    private SpriteBatch spriteBatch, uiBatch, uiBatch2;

    /**
     * The Round this GameScreen renders.
     */
    private Round round;

    /**
     * How constrained the camera is to the player.
     */
    private final float PLAYER_CAMERA_BOUND = 8f;

    /**
     * A list of water cells in the map.
     */
    ArrayList<TiledMapTileLayer.Cell> waterCellsInScene;

    /**
     * A map of water tiles.
     */
    Map<String,TiledMapTile> waterTiles;

    /**
     * The time that has elapsed since the animation.
     */
    float elapsedSinceAnimation = 0.0f;

    /**
     * A timer for the current round.
     */
    float roundTimer = 0f;

    /**
     * The current level of the round.
     */
    private int level;


    /**
     * Initialises this GameScreen for the specified round.
     *
     * @param round the round to be displayed
     */
    public GameScreen(Round round, int level) {
        this.round = round;
        this.level = level;
    }

    /**
     * Converts screen coordinates to world coordinates.
     *
     * @param x the x coordinate on screen
     * @param y the y coordinate on screen
     * @return a Vector3 containing the world coordinates (x and y)
     */
    public Vector3 unproject(int x, int y) {
        return camera.unproject(new Vector3(x, y, 0));
    }

    /**
     * @return the Round currently on this GameScreen
     */
    public Round getRound() {
        return round;
    }

    /**
     * Shows this GameScreen. Called by libGDX to set up the graphics.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);

        camera = new OrthographicCamera(DuckGame.GAME_WIDTH/SCALE, DuckGame.GAME_HEIGHT/SCALE);
//        camera.zoom -= 0.5;

//        mapBatch = new SpriteBatch();
        spriteBatch = new SpriteBatch();
        uiBatch = new SpriteBatch();
        uiBatch2 = new SpriteBatch();

        mapRenderer = new OrthogonalTiledMapRenderer(round.getMap());

        // We created a second set of tiles for Water animations
        // For the record, this is bad for performance, use a single tileset if you can help it
        // Get a reference to the tileset named "Water"
        TiledMapTileSet tileset =  round.getMap().getTileSets().getTileSet("Tileset");


        // Now we are going to loop through all of the tiles in the Water tileset
        // and get any TiledMapTile with the property "WaterFrame" set
        // We then store it in a map with the frame as the key and the Tile as the value
        waterTiles = new HashMap<String,TiledMapTile>();
        for(TiledMapTile tile:tileset){
            Object property = tile.getProperties().get("water");
            if(property != null)
                waterTiles.put((String)property,tile);
        }

        // Now we want to get a reference to every single cell ( Tile instance ) in the map
        // that refers to a water cell.  Loop through the entire world, checking if a cell's tile
        // contains the WaterFrame property.  If it does, add to the waterCellsInScene array
        // Note, this only pays attention to the very first layer of tiles.
        // If you want to support animation across multiple layers you will have to loop through each
        waterCellsInScene = new ArrayList<TiledMapTileLayer.Cell>();
        TiledMapTileLayer layer = (TiledMapTileLayer) round.getMap().getLayers().get(0);
        for(int x = 0; x < layer.getWidth();x++){
            for(int y = 0; y < layer.getHeight();y++){
                TiledMapTileLayer.Cell cell = layer.getCell(x,y);
                Object property = cell.getTile().getProperties().get("water");
                if(property != null){
                    waterCellsInScene.add(cell);
                }
            }
        }


    }

    /**
     * Main game loop.
     * @param delta how much time has passed since the last update
     */
    @Override
    public void render(float delta) {
        round.update(delta);

        //Update render order of entities
        try {
            round.getEntities().sort(new Entity.EntityComparator());
        } catch (IllegalArgumentException e) {
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Centre the camera on the player.
        updateCamera();
        camera.update();

        spriteBatch.setProjectionMatrix(camera.combined);
        uiBatch2.setProjectionMatrix(camera.combined.cpy().scl(0.5f));

        // Render base and collision layers.
        mapRenderer.setView(camera);
        mapRenderer.getBatch().begin();
        mapRenderer.renderTileLayer(round.getBaseLayer());
        mapRenderer.renderTileLayer(round.getCollisionLayer());
        mapRenderer.renderTileLayer(round.getWaterEdgeLayer());
//
//        // Render randomly-chosen obstacles layer.
        if (round.getObstaclesLayer() != null) {
            mapRenderer.renderTileLayer(round.getObstaclesLayer());
        }

        // Wait for half a second to elapse then call updateWaterAnimations
        // This could certainly be handled using an Action if you are using Scene2D
        elapsedSinceAnimation += Gdx.graphics.getDeltaTime();
        if(elapsedSinceAnimation > 0.5f){
            updateWaterAnimations();
            elapsedSinceAnimation = 0.0f;
        }

        mapRenderer.getBatch().end();

        spriteBatch.begin();
        // Draw all entities.
        for (Entity entity : round.getEntities()) {
            entity.render(spriteBatch);
        }

        spriteBatch.end();
        uiBatch2.begin();

        round.floatyNumbersManager.render(uiBatch2);

        //Render health bars above enemies
        for (Entity entity : round.getEntities()) {
            if (entity instanceof Mob) {
                Mob chars = (Mob) entity;
                float offsetX = chars.getX() * 2 - chars.getWidth() / 2;

                float offsetY = chars.getY() * 2 + chars.getHeight() * 2;

                if (chars.getType() == Mob.MobType.BOSS) {
                    offsetX += 40;
                    offsetY += 15;
                } else if (chars.getType() == Mob.MobType.RANGED) {
                    offsetX -= 5;
                    offsetY += 30;
                } else {
                    offsetX -= 17;
                    offsetY += 10;
                }

                uiBatch2.draw(Assets.healthEmpty, offsetX, offsetY);
                Assets.healthFull.setRegionWidth((int) Math.max(0, ((float) chars.getCurrentHealth() / chars.getMaximumHealth()) * 100));
                uiBatch2.draw(Assets.healthFull, offsetX, offsetY);
            }
        }

        uiBatch2.end();
        mapRenderer.getBatch().begin();
//        mapBatch.begin();
        // Render overhang layer (draws over the player).
        if (round.getOverhangLayer() != null) {
            mapRenderer.renderTileLayer(round.getOverhangLayer());
        }

        mapRenderer.getBatch().end();
//        mapBatch.end();

        uiBatch.begin();
        // TODO: finish UI
        Assets.font.setColor(0f, 0f, 0f, 1.0f);
        Assets.font.draw(uiBatch, "Objective: " + round.getObjective().getObjectiveString(), 10, 708);
        Assets.font.draw(uiBatch, "Score: " + round.getPlayer().getScore(), 10, 678);
        Assets.font.draw(uiBatch, Gdx.graphics.getFramesPerSecond() + " FPS", Gdx.graphics.getWidth()-10, Gdx.graphics.getHeight()-12, 0, Align.right, false);

        Assets.font.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        Assets.font.draw(uiBatch, "Objective: " + round.getObjective().getObjectiveString(), 10, 710);
        Assets.font.draw(uiBatch, "Score: " + round.getPlayer().getScore(), 10, 680);
        Assets.font.draw(uiBatch, Gdx.graphics.getFramesPerSecond() + " FPS", Gdx.graphics.getWidth()-10, Gdx.graphics.getHeight()-10, 0, Align.right, false);

        // Draw stamina bar (for flight);
		uiBatch.draw(Assets.staminaEmpty, 1080, 10);
        if (round.getPlayer().getFlyingTimer() > 0) {
            Assets.staminaFull.setRegionWidth((int) Math.max(0, Math.min(192, round.getPlayer().getFlyingTimer() / Player.PLAYER_MAX_FLIGHT_TIME * 192)));
        } else {
            Assets.staminaFull.setRegionWidth(0);
        }
		uiBatch.draw(Assets.staminaFull, 1080, 10);


        // Draw powerup bar.
        round.powerUpManager.render(uiBatch);


        //Draw health.
        int x = 0;
        while(x < round.getPlayer().getMaximumHealth()) {
        	if(x+2 <= round.getPlayer().getCurrentHealth())
        		uiBatch.draw(Assets.heartFull, x * 18 + (Gdx.graphics.getWidth()/2 - 50), 10);
        	else if(x+1 <= round.getPlayer().getCurrentHealth())
        		uiBatch.draw(Assets.heartHalf, x * 18 + (Gdx.graphics.getWidth()/2 - 50), 10);
        	else
        		uiBatch.draw(Assets.heartEmpty, x * 18 + (Gdx.graphics.getWidth()/2 - 50), 10);
        	x += 2;
        }

        // Draw round text at start of round.
        if (roundTimer < 3f) {
            roundTimer += delta;
            uiBatch.draw(Assets.roundText, (Gdx.graphics.getWidth() - Assets.roundText.getWidth() - Assets.roundNums[level].getWidth())/2,
                    (Gdx.graphics.getHeight() - Assets.roundText.getHeight())/2);
            uiBatch.draw(Assets.roundNums[level], (Gdx.graphics.getWidth() + Assets.roundText.getWidth())/2,
                    (Gdx.graphics.getHeight() - Assets.roundText.getHeight())/2);
        }

        uiBatch.end();
    }

    /**
     * Called every half a second to update animated water tiles.
     */
    private void updateWaterAnimations(){
        for(TiledMapTileLayer.Cell cell : waterCellsInScene){
            String property = (String) cell.getTile().getProperties().get("water");
            Integer currentAnimationFrame = Integer.parseInt(property);

            currentAnimationFrame++;
            if(currentAnimationFrame > waterTiles.size())
                currentAnimationFrame = 1;

            TiledMapTile newTile = waterTiles.get(currentAnimationFrame.toString());
            cell.setTile(newTile);
        }
    }

    /**
     * Updates the camera to be constrained to the player and to stay within the map.
     */
    private void updateCamera() {
//      Constrain camera to player
        if ((round.getPlayer().getX() + round.getPlayer().getWidth() > camera.position.x + camera.viewportWidth / PLAYER_CAMERA_BOUND))
            camera.position.x = ((round.getPlayer().getX() + round.getPlayer().getWidth())) - (camera.viewportWidth / PLAYER_CAMERA_BOUND);
        if ((round.getPlayer().getX() < camera.position.x - camera.viewportWidth / PLAYER_CAMERA_BOUND))
            camera.position.x = (round.getPlayer().getX()) + (camera.viewportWidth / PLAYER_CAMERA_BOUND);
        if ((round.getPlayer().getY() + round.getPlayer().getHeight() > camera.position.y + camera.viewportHeight / PLAYER_CAMERA_BOUND))
            camera.position.y = ((round.getPlayer().getY() + round.getPlayer().getHeight())) - (camera.viewportHeight / PLAYER_CAMERA_BOUND);
        if ((round.getPlayer().getY() < camera.position.y - camera.viewportHeight / PLAYER_CAMERA_BOUND))
            camera.position.y = (round.getPlayer().getY()) + (camera.viewportHeight / PLAYER_CAMERA_BOUND);

//      Constrain camera to map
        if (camera.position.x + camera.viewportWidth / 2f > round.getMapWidth())
            camera.position.x = round.getMapWidth() - camera.viewportWidth / 2f;
        if (camera.position.x < camera.viewportWidth / 2f)
            camera.position.x = camera.viewportWidth / 2f;
        if (camera.position.y + camera.viewportHeight / 2f > round.getMapHeight())
            camera.position.y = round.getMapHeight() - camera.viewportHeight / 2f;
        if (camera.position.y < camera.viewportHeight / 2f)
            camera.position.y = camera.viewportHeight / 2f;
    }

    /**
     * Not used since the game window cannot be resized.
     */
    @Override
    public void resize(int width, int height) {
    }

    /**
     * Not used.
     */
    @Override
    public void pause() {
    }

    /**
     * Not used.
     */
    @Override
    public void resume() {
    }

    /**
     * Not used.
     */
    @Override
    public void hide() {

    }

    /**
     * Called to dispose libGDX objects used by this GameScreen.
     */
    @Override
    public void dispose() {
        mapRenderer.dispose();
        spriteBatch.dispose();
        uiBatch.dispose();
    }

}
