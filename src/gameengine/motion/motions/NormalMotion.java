package gameengine.motion.motions;

import gameengine.entities.Entity;

/**
 * This {@link Motion} will not affect the velocity of the {@link Entity}
 *
 * @author davidrusu
 */
public class NormalMotion implements Motion {
    private double velocityX, velocityY;

    @Override
    public double getVelocityX() {
        return velocityX;
    }

    @Override
    public double getVelocityY() {
        return velocityY;
    }

    @Override
    public void reset() {
        velocityX = 0;
        velocityY = 0;
    }

    @Override
    public void update(Entity entity, double elapsedTime) {
        velocityX = entity.getDX();
        velocityY = entity.getDY();
    }
}
