
package com.superduckinvaders.game.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.superduckinvaders.game.Round;
import com.superduckinvaders.game.ai.AI;
import com.superduckinvaders.game.ai.DummyAI;
import com.superduckinvaders.game.assets.Assets;
import com.superduckinvaders.game.assets.TextureSet;
import com.superduckinvaders.game.entity.item.PowerupManager;
import com.superduckinvaders.game.objective.BossObjective;
import com.superduckinvaders.game.objective.KillObjective;
import com.superduckinvaders.game.objective.Objective;

public class Mob extends Character {

    /**
     * The texture set to use for this Mob.
     */
    private TextureSet walkingTextureSet, swimmingTextureSet;
    
    /**
     * AI class for the mob
     */
    private AI ai;
    
    /**
     * checks whether mob should be updated
     */
    private boolean active = false;

    /**
     * The score this mob will give when killed
     */
    private int score;

    /**
     * The type of mob this is
     */
    private MobType type;

    /**
     * speed of the mob in pixels per second
     */
    private int speed;

    /**
     * Create a Mob
     * @param parent The round this mob resides in
     * @param x The starting x position
     * @param y The starting y position
     * @param health The health(maximum) of the mob
     * @param speed The speed of the mob
     * @param score The score the mob will give when killed
     * @param walkingTextureSet The textureset of the mob when on land
     * @param swimmingTextureSet The textureset of the mob when on water
     * @param ai The ai for the mob
     * @param type The type of the mob
     */
    public Mob(Round parent, float x, float y, int health, int speed, int score, TextureSet walkingTextureSet, TextureSet swimmingTextureSet, AI ai, MobType type) {
        super(parent, x, y, health);

        this.walkingTextureSet = walkingTextureSet;
        this.swimmingTextureSet = swimmingTextureSet;
        this.speed = speed;
        this.score = score;
        this.ai = ai;

        this.type = type;

        if(type==MobType.BOSS)
            disableCollision();
    }

    
    /**
     * Sets the AI for this Mob.
     * @param ai the new AI to use
     */
    public void setAI(AI ai) {
        this.ai = ai;
    }

    /**
     * Sets the speed of the mob
     * @param newSpeed the updated speed
     */
    public void setSpeed(int newSpeed){
        this.speed = newSpeed;
    }
    
    /**
     * Change where the given mob moves to according to its speed and a new direction vector.
     * @param dirX x component of the direction vector
     * @param dirY y component of the direction vector
     */
    public void setVelocity(float dirX, float dirY){
    	if(dirX == 0 && dirY==0){
    		velocityX=0;
    		velocityY=0;
    		return;
    	}
    	float magnitude = (float) Math.sqrt(dirX*dirX + dirY*dirY);
    	velocityX = (dirX*speed)/magnitude;
    	velocityY = (dirY*speed)/magnitude;

    }

    /**
     * @return The width of the Mob. Uses the smaller swimming sprites
     */
    @Override
    public int getWidth() {
            return walkingTextureSet.getTexture(TextureSet.FACING_FRONT, 0).getRegionWidth();
    }

    /**
     * @return The height of the Mob. Uses the smaller swimming sprites
     */
    @Override
    public int getHeight() {
            return walkingTextureSet.getTexture(TextureSet.FACING_FRONT, 0).getRegionHeight()*3/4;

    }

    /**
     * Damages this Character's health by the specified number of points.
     *
     * @param health the number of points to damage
     */
    public void damage(int health) {
        this.currentHealth -= health;
        parent.floatyNumbersManager.createDamageNumber(health, x, y);
    }

    /**
     * @return returns the walking texture set of the mob
     */
    public TextureSet getWalkingTextureSet() {
        return walkingTextureSet;
    }
    /**
     * @return returns the swimming texture set of the mob
     */
    public TextureSet getSwimmingTextureSet() {
        return swimmingTextureSet;
    }

    /**
     * @return The score this mob should give
     */
    public int getScore() {
        return score;
    }

    /**
     * @return The type of the mob
     */
    public MobType getType() {
        return type;
    }

    /**
     * Updates the Mob. Checks for death, updates animation and movement using it's ai
     * @param delta how much time has passed since the last update
     */
    @Override
    public void update(float delta) {
        ai.update(this, delta);

        // Chance of spawning a random powerup.
        if (isDead()) {
            float random = MathUtils.random();
            PowerupManager.powerupTypes powerup = null;

            if (random < 0.05) {
                powerup = PowerupManager.powerupTypes.SCORE_MULTIPLIER;
            } else if (random >= 0.05 && random < 0.1) {
                powerup = PowerupManager.powerupTypes.INVULNERABLE;
            } else if (random >= 0.1 && random < 0.15) {
                powerup = PowerupManager.powerupTypes.SUPER_SPEED;
            } else if (random >= 0.15 && random < 0.2) {
                powerup = PowerupManager.powerupTypes.RATE_OF_FIRE;
            }

            if (powerup != null) {
                parent.createPowerup(x, y, powerup, 10);
            }

            if(type==MobType.BOSS) {
                ((BossObjective)parent.getObjective()).setCompleted();
            }
            else{
                if (parent.getObjective().getObjectiveType() == Objective.objectiveType.KILL) {
                    KillObjective objective = (KillObjective) parent.getObjective();
                    objective.decrementKills();
                }
            }
        }

        // Update animation state time.
        if (velocityX != 0 || velocityY != 0) {
            stateTime += delta;
        } else {
            stateTime = 0;
        }

        super.update(delta);
    }

    /**
     * Renders the Mob with correct textures/animations
     * @param spriteBatch the sprite batch on which to render
     */
    @Override
    public void render(SpriteBatch spriteBatch) {

        if(type==MobType.RANGED) {
            if(isOnWater()){
                spriteBatch.draw(Assets.shadow2, x, y+3);
                spriteBatch.draw(swimmingTextureSet.getTexture(facing, stateTime), x, y );
            }
            else{
                spriteBatch.draw(Assets.shadow2, x, y+3);
                spriteBatch.draw(walkingTextureSet.getTexture(facing, stateTime), x, y );
            }


        }
        else if(type==MobType.MELEE){
            if(isOnWater()) {
                spriteBatch.draw(Assets.shadow, x-5, y);
                spriteBatch.draw(swimmingTextureSet.getTexture(facing, stateTime), x, y);
            }
            else {
                spriteBatch.draw(Assets.shadow, x - 5, y - 5);
                spriteBatch.draw(walkingTextureSet.getTexture(facing, stateTime), x, y);
            }
        }
        else if(type==MobType.BOSS){
            spriteBatch.draw(Assets.bossShadow, x-10, y-20);
            spriteBatch.draw(walkingTextureSet.getTexture(facing, stateTime), x, y);
        }
    }

    /**
     * Enum of mob types
     */
    public enum MobType{
        MELEE,RANGED,BOSS
    }
}
