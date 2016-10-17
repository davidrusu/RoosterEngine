package bricklets;

import gameengine.GameController;
import gameengine.collisiondetection.Collision;
import gameengine.collisiondetection.EntityType;
import gameengine.collisiondetection.tree.Tree;
import gameengine.context.Context;
import gameengine.context.ContextType;
import gameengine.entities.Entity;
import gameengine.entities.Pointer;
import gameengine.graphics.OvalGraphic;
import gameengine.input.Action;
import gameengine.input.ActionHandler;
import gameengine.input.InputCode;
import gameengine.physics.Material;
import gameengine.physics.Physics;

import java.awt.*;
import java.util.Random;

public class Benchmark extends Context implements ActionHandler {
    private Random rand = new Random(0);
    private Material ballMaterial = Material.createMaterial(0, 1, 1);
    private double currentTime = 0;
    private double lastTime = 0;
    private int balls = 0, maxBalls = 1000;

    public Benchmark(GameController controller) {
        super(controller, ContextType.GAME);
        init();
    }

    public void init() {
        world.clear();
        world.setCollisionGroups(EntityType.BALL, EntityType.BALL, EntityType.WALL);
        balls = 0;

        Entity.setDefaultMaterial(Material.createMaterial(0, 1, 1));
        Entity.setDefaultEntityType(EntityType.STANDARD);
        Pointer pointer = new Pointer(new OvalGraphic(15, 15, Color.RED), width / 2, height / 2);
        pointer.setMass(1);
//        world.addEntity(pointer);
        initBounding();
        setupInput();
    }

    public void initBounding() {
        double borderThickness = 0.0000001;

        Entity.setDefaultMaterial(Material.createMaterial(0, 1, Double.POSITIVE_INFINITY));
        Entity.setDefaultEntityType(EntityType.WALL);
        BoxEntity topBounds = new BoxEntity(width / 2, 0, width, borderThickness);
        BoxEntity bottomBounds = new BoxEntity(width / 2, height, width, borderThickness);
        BoxEntity leftBounds = new BoxEntity(0, height / 2, borderThickness, height);
        BoxEntity rightBounds = new BoxEntity(width, height / 2, borderThickness, height);

        world.addEntity(topBounds);
        world.addEntity(bottomBounds);
        world.addEntity(leftBounds);
        world.addEntity(rightBounds);
    }

    @Override
    public void update(double elapsedTime) {
        currentTime += elapsedTime;
        double timeBetweenBalls = 25;
        if (lastTime + timeBetweenBalls <= currentTime && balls < maxBalls) {
            int ballSize = 2;
            double halfWidth = width * 0.5;
            double halfHeight = height * 0.5;
            double xLength = width - 50;
            double yLength = height - 100;
            for (int i = 0; i < 100; i++) {
                addBall(halfWidth + (rand.nextDouble() - 0.5) * xLength,
                        halfHeight + (rand.nextDouble() - 0.5) * yLength, ballSize);
                balls++;
            }
            lastTime = currentTime;// - (currentTime - lastTime + timeBetweenBalls);
        }
    }

    public static void main(String[] args) {
        GameController controller = new GameController(120, 100, true);
        Context bench = new Benchmark(controller);
        controller.enterContext(bench);
        controller.startGame();
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        world.draw(this, g);
        world.drawTree(g, Color.red);
        drawStats(g);
    }

    private void drawStats(Graphics2D g) {
        g.setColor(Color.red);
        g.drawString("fps: " + controller.getFrameRate(), 25, 25);
        g.drawString("balls: " + balls + " / " + maxBalls, 25, 75);
    }

    @Override
    public void handleCollision(Collision collision) {
        Physics.performCollision(collision);
    }

    private void setupInput() {
        controller.setContextBinding(contextType, InputCode.KEY_ESCAPE, Action.EXIT_GAME);
        controller.setContextBinding(contextType, InputCode.MOUSE_LEFT_BUTTON, Action.MOUSE_CLICK);
        controller.setContextBinding(contextType, InputCode.MOUSE_WHEEL_UP, Action.ZOOM_OUT);
        controller.setContextBinding(contextType, InputCode.MOUSE_WHEEL_DOWN, Action.ZOOM_IN);
    }

    @Override
    public void startAction(Action action, int inputCode) {
        double scaleAmount = 0.1;
        switch (action) {
            case EXIT_GAME:
                break;
            case MOUSE_CLICK:
                break;
            case ZOOM_IN:
                viewPort.scaleScale(1 - scaleAmount);
                break;
            case ZOOM_OUT:
                viewPort.scaleScale(1 + scaleAmount);
                break;
        }
    }

    @Override
    public void stopAction(Action action, int inputCode) {
        switch (action) {
            case EXIT_GAME:
                controller.exitContext();
                break;
            case MOUSE_CLICK:
                int ballSize = 2;
                double halfWidth = width * 0.5;
                double halfHeight = height * 0.5;
                double xLength = width - 50;
                double yLength = height - 100;
                for (int i = 0; i < 200; i++) {
                    addBall(halfWidth + (rand.nextDouble() - 0.5) * xLength,
                            halfHeight + (rand.nextDouble() - 0.5) * yLength, ballSize);
                    balls++;
                }
                break;
        }
    }

    private void addBall(double x, double y, double radius) {
        double speed = 0.1;
        CircleEntity entity = new CircleEntity(x, y, radius);
        entity.setMass(1);
        entity.setVelocity((Math.random() - 0.5) * speed, (Math.random() - 0.5) * speed);
        entity.setMaterial(ballMaterial);
        entity.setEntityType(EntityType.BALL);
        world.addEntity(entity);
    }
}
