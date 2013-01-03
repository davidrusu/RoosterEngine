package gameengine.effects.integrators;

import bricklets.Entity;

/**
 *
 * @author davidrusu
 */
public interface Integrator {

    /**
     * Returns velocity based on the supplied values
     * @param entity the {@link Entity} the velocity will affect
     * @param displacementToDestination the displacement of the entity from the destination, this is a positive when the
     *                                  entity has not passed the destination, and negative if the destination is passed
     * @param elapsedTime the amount of time over which the velocity will be applied
     * @return
     */
    public double getVelocity(Entity entity, double displacementToDestination, double elapsedTime);
    
    public void reset();
}
