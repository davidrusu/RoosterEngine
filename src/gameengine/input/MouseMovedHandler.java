package gameengine.input;

/**
 * A handler for a mouse move event
 *
 * @author davidrusu
 */
public interface MouseMovedHandler {

    /**
     * Called each time the mouse moves. 
     * then the context is in relativeMouseMoved mode, the x and y coordinates
     * is a vector in the direction the mouse moved with a magnitude depending
     * on how fast the mouse was moved
     * @param x the mouse x position relative to the container
     * @param y the mouse y position relative to the container
     * @param dx the x velocity of the mouse;
     * @param dy the y velocity of the mouse
     */
    public void mouseMoved(int x, int y, double dx, double dy);
}