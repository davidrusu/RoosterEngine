package gameengine.collisiondetection.tree;

import gameengine.collisiondetection.Collision;
import gameengine.collisiondetection.shapes.Shape;
import gameengine.entities.Entity;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * documentation
 * User: davidrusu
 * Date: 15/01/13
 * Time: 9:30 PM
 */
public class Quad extends Tree implements Parent {
    private Tree topLeft = null;
    private Tree topRight = null;
    private Tree bottomLeft = null;
    private Tree bottomRight = null;

    private static final int INITIAL_NUM_QUADS = Leaf.INITIAL_NUM_LEAFS / 4 + 1;
    private static final int EXPANSION_FACTOR = 2;
    private static Quad[] recycledQuads = new Quad[INITIAL_NUM_QUADS];
    private static int numRecycledQuads = INITIAL_NUM_QUADS;

    static {
        for (int i = 0; i < INITIAL_NUM_QUADS; i++) {
            recycledQuads[i] = new Quad();
        }
    }

    private Quad(Parent parent, double centerX, double centerY, double halfLength, CollisionList list) {
        super(parent, centerX, centerY, halfLength, list);
        initQuads(list);
    }

    private Quad(Parent parent, double centerX, double centerY, double halfLength, Tree topLeft, Tree topRight,
                 Tree bottomLeft, Tree bottomRight, CollisionList list) {
        super(parent, centerX, centerY, halfLength, list);
        initQuads(topLeft, topRight, bottomLeft, bottomRight);
    }

    private Quad() {
        super();
    }

    public static Quad createInstance(Parent parent, double centerX, double centerY, double halfLength, CollisionList list) {
        if (numRecycledQuads == 0) {
            return new Quad(parent, centerX, centerY, halfLength, list);
        }
        numRecycledQuads--;
        Quad quad = recycledQuads[numRecycledQuads];

        quad.init(parent, centerX, centerY, halfLength, list);
        quad.initQuads(list);
        return quad;
    }

    public static Quad createInstance(Parent parent, double centerX, double centerY, double halfLength, Tree topLeft,
                                      Tree topRight, Tree bottomLeft, Tree bottomRight, CollisionList list) {
        if (numRecycledQuads == 0) {
            return new Quad(parent, centerX, centerY, halfLength, topLeft, topRight, bottomLeft, bottomRight, list);
        }
        numRecycledQuads--;
        Quad quad = recycledQuads[numRecycledQuads];
        quad.init(parent, centerX, centerY, halfLength, list);
        quad.initQuads(topLeft, topRight, bottomLeft, bottomRight);
        return quad;
    }

    private void initQuads(Tree topLeft, Tree topRight, Tree bottomLeft, Tree bottomRight) {
        this.topLeft = topLeft;
        topLeft.parent = this;
        this.topRight = topRight;
        topRight.parent = this;
        this.bottomLeft = bottomLeft;
        bottomLeft.parent = this;
        this.bottomRight = bottomRight;
        bottomRight.parent = this;
        entityCount = topLeft.entityCount + topRight.entityCount + bottomLeft.entityCount + bottomRight.entityCount;
    }

    private void initQuads(CollisionList list) {
        double quadLength = getHalfLength() * 0.5;
        double left = getCenterX() - quadLength;
        double right = getCenterX() + quadLength;
        double top = getCenterY() - quadLength;
        double bottom = getCenterY() + quadLength;

        topLeft = Leaf.createInstance(this, left, top, quadLength, list);
        topRight = Leaf.createInstance(this, right, top, quadLength, list);
        bottomLeft = Leaf.createInstance(this, left, bottom, quadLength, list);
        bottomRight = Leaf.createInstance(this, right, bottom, quadLength, list);
    }

    @Override
    public void addEntity(Entity entity) {
        insertEntity(entity);
        entityCount++;
    }

    private void insertEntity(Entity entity) {
        assert checkEntities();
        assert entity != null;

        Shape shape = entity.getShape();
        if (shape.getBoundingMaxX() < getCenterX()) {
            insertVertically(entity, topLeft, bottomLeft);
        } else if (shape.getBoundingMinX() > getCenterX()) {
            insertVertically(entity, topRight, bottomRight);
        } else {
            addToThis(entity);
        }
        assert checkEntities();
    }

    private void insertVertically(Entity entity, Tree top, Tree bottom) {
        Shape shape = entity.getShape();
        if (shape.getBoundingMaxY() < getCenterY()) {
            top.addEntity(entity);
        } else if (shape.getBoundingMinY() > getCenterY()) {
            bottom.addEntity(entity);
        } else {
            addToThis(entity);
        }
    }

    private void addToThis(Entity entity) {
        addEntityToList(entity);
    }

    @Override
    public void ensureEntitiesAreContained(double time) {
        int index = 0;
        while (index < entityListPos) {
            Entity entity = entities[index];
            Shape shape = entity.getShape();
            shape.calculateBoundingBox(time);

            if (!isContainedInTree(entity)) {
                assert isEntityCountCorrect();

                preRelocateRemove(index);
                parent.relocate(entity);

                assert isEntityCountCorrect();
            } else if (shape.getBoundingMinX() > getCenterX()) {
                assert isEntityCountCorrect();
                index = ensureVerticallyContained(index, entity, shape.getBoundingMinY(), shape.getBoundingMaxY(), bottomRight, topRight);
                assert isEntityCountCorrect();
            } else if (shape.getBoundingMaxX() < getCenterX()) {
                assert isEntityCountCorrect();
                index = ensureVerticallyContained(index, entity, shape.getBoundingMinY(), shape.getBoundingMaxY(), bottomLeft, topLeft);
                assert isEntityCountCorrect();
            } else {
                index++;
            }
        }

        assert isEntityCountCorrect();
        topLeft.ensureEntitiesAreContained(time);
        topRight.ensureEntitiesAreContained(time);
        bottomLeft.ensureEntitiesAreContained(time);
        bottomRight.ensureEntitiesAreContained(time);

        assert isEntityCountCorrect();
    }

    private int ensureVerticallyContained(int index, Entity entity, double minY, double maxY, Tree bottom, Tree top) {
        if (minY > getCenterY()) {
            removeEntityFromList(index);
            bottom.addEntity(entity);
        } else if (maxY < getCenterY()) {
            removeEntityFromList(index);
            top.addEntity(entity);
        } else {
            return index + 1;
        }
        return index;
    }

    @Override
    public Tree tryResize(CollisionList list) {
        assert getRealEntityCount() == entityCount : getRealEntityCount() + " " + entityCount;

        if (entityCount == 0) {
            Leaf leaf = Leaf.createInstance(parent, getCenterX(), getCenterY(), getHalfLength(), list);
            clear(list);
            recycle();
            return leaf;
        }
        topLeft = topLeft.tryResize(list);
        topRight = topRight.tryResize(list);
        bottomLeft = bottomLeft.tryResize(list);
        bottomRight = bottomRight.tryResize(list);

        assert getRealEntityCount() == entityCount : getRealEntityCount() + " " + entityCount;
        return this;
    }

    @Override
    public void updateEntities(double elapsedTime) {
        for (int i = 0; i < entityListPos; i++) {
            entities[i].update(elapsedTime);
        }
        topLeft.updateEntities(elapsedTime);
        topRight.updateEntities(elapsedTime);
        bottomLeft.updateEntities(elapsedTime);
        bottomRight.updateEntities(elapsedTime);
    }

    @Override
    public void updateEntityPositions(double elapsedTime) {
        for (int i = 0; i < entityListPos; i++) {
            entities[i].updatePosition(elapsedTime);
        }
        topLeft.updateEntityPositions(elapsedTime);
        topRight.updateEntityPositions(elapsedTime);
        bottomLeft.updateEntityPositions(elapsedTime);
        bottomRight.updateEntityPositions(elapsedTime);
    }

    @Override
    public void updateEntityMotions(double elapsedTime) {
        for (int i = 0; i < entityListPos; i++) {
            entities[i].updateMotion(elapsedTime);
        }
        topLeft.updateEntityMotions(elapsedTime);
        topRight.updateEntityMotions(elapsedTime);
        bottomLeft.updateEntityMotions(elapsedTime);
        bottomRight.updateEntityMotions(elapsedTime);
    }

    @Override
    public void checkCollisionWithEntity(int[] collisionGroups, Collision temp, Collision result,
                                         double timeToCheck, double currentTime, Entity entity) {
        Shape a = entity.getShape();
        for (int i = 0; i < entityListPos; i++) {
            Shape b = entities[i].getShape();
            collideShapes(collisionGroups, temp, result, timeToCheck, currentTime, a, b);
        }
        checkCollisionInSubTrees(collisionGroups, temp, result, timeToCheck, currentTime, entity);
    }

    private void checkCollisionInSubTrees(int[] collisionGroups, Collision temp, Collision result,
                                          double timeToCheck, double currentTime, Entity entity) {
        checkHalfTree(collisionGroups, temp, result, timeToCheck, currentTime, entity, topLeft, bottomLeft);
        checkHalfTree(collisionGroups, temp, result, timeToCheck, currentTime, entity, topRight, bottomRight);
    }

    private void checkHalfTree(int[] collisionGroups, Collision temp, Collision result,
                               double timeToCheck, double currentTime, Entity entity, Tree top, Tree bottom) {
        Shape shape = entity.getShape();
        if (Math.abs(top.getCenterX() - shape.getBoundingCenterX())
                < shape.getBoundingHalfWidth() + top.getHalfLength()) {
            if (Math.abs(top.getCenterY() - shape.getBoundingCenterY())
                    < shape.getBoundingHalfHeight() + top.getHalfLength()) {
                top.checkCollisionWithEntity(collisionGroups, temp, result, timeToCheck, currentTime, entity);
            }
            if (Math.abs(bottom.getCenterY() - shape.getBoundingCenterY())
                    < shape.getBoundingHalfHeight() + bottom.getHalfLength()) {
                bottom.checkCollisionWithEntity(collisionGroups, temp, result, timeToCheck, currentTime, entity);
            }
        }
    }

    @Override
    public void calcCollision(int[] collisionGroups, Collision temp, double timeToCheck, double currentTime, CollisionList list) {
        assert node.getCollision().getCollisionTime() == Shape.NO_COLLISION;
        assert getRealEntityCount() == entityCount : getRealEntityCount() + " " + entityCount;

        calcCollisionsAtLevel(collisionGroups, temp, timeToCheck, currentTime, list);

        topLeft.calcCollision(collisionGroups, temp, timeToCheck, currentTime, list);
        topRight.calcCollision(collisionGroups, temp, timeToCheck, currentTime, list);
        bottomLeft.calcCollision(collisionGroups, temp, timeToCheck, currentTime, list);
        bottomRight.calcCollision(collisionGroups, temp, timeToCheck, currentTime, list);

        assert getRealEntityCount() == entityCount : getRealEntityCount() + " " + entityCount;
    }

    private void calcCollisionsAtLevel(int[] collisionGroups, Collision temp, double timeToCheck, double currentTime, CollisionList list) {
        for (int i = 0; i < entityListPos; i++) {
            Entity entity = entities[i];
            Shape a = entity.getShape();
            for (int j = i + 1; j < entityListPos; j++) {
                Shape b = entities[j].getShape();
                collideShapes(collisionGroups, temp, node.getCollision(), timeToCheck, currentTime, a, b);
            }
            checkCollisionInSubTrees(collisionGroups, temp, node.getCollision(), timeToCheck, currentTime, entity);
        }
        list.collisionUpdated(this);
    }

    @Override
    public void relocateAndCheck(int[] collisionGroups, Collision temp, double timeToCheck, double currentTime,
                                 Entity entity, CollisionList list) {
        assert !isEntityInTree(entity) : "Entity should not be in the this tree when this method is called";

        entityCount--;
        Collision collision = node.getCollision();
        if (entity == collision.getA() || entity == collision.getB()) {
            collision.setNoCollision();
            calcCollisionsAtLevel(collisionGroups, temp, timeToCheck, currentTime, list);
        }
        if (isContainedInTree(entity)) {
            addAndCheck(collisionGroups, temp, timeToCheck, currentTime, entity, list);
        } else {
            parent.relocateAndCheck(collisionGroups, temp, timeToCheck, currentTime, entity, list);
        }

        assert getRealEntityCount() == entityCount : getRealEntityCount() + " " + entityCount;
    }

    @Override
    public void addAndCheck(int[] collisionGroups, Collision temp, double timeToCheck, double currentTime,
                            Entity entity, CollisionList list) {
        checkCollisionWithEntity(collisionGroups, temp, node.getCollision(), timeToCheck, currentTime, entity);
        list.collisionUpdated(this);
        addEntityToList(entity);
        entityCount++;

        assert getRealEntityCount() == entityCount : getRealEntityCount() + " " + entityCount;
    }

    @Override
    public void entityRemovedDuringCollision(int[] collisionGroups, Collision temp, double timeToCheck, double currentTime,
                                             Entity entity, CollisionList list) {
        assert isEntityCountCorrect();
        //entitycount has already been decremented by the removeFromWorld method
        Collision collision = node.getCollision();
        if (collision.getA() == entity || collision.getB() == entity) {
            calcCollisionsAtLevel(collisionGroups, temp, timeToCheck, currentTime, list);
        }
        parent.entityRemovedDuringCollision(collisionGroups, temp, timeToCheck, currentTime, entity, list);
    }

    @Override
    public void decrementEntityCount() {
        entityCount--;
        parent.decrementEntityCount();
        assert getRealEntityCount() == entityCount : getRealEntityCount() + " " + entityCount;
    }

    @Override
    public void recycle() {
        assert isClean();

        topLeft.recycle();
        topRight.recycle();
        bottomLeft.recycle();
        bottomRight.recycle();
        if (numRecycledQuads == recycledQuads.length) {
            Quad[] temp = new Quad[numRecycledQuads * EXPANSION_FACTOR];
            System.arraycopy(recycledQuads, 0, temp, 0, numRecycledQuads);
            recycledQuads = temp;
        }
        recycledQuads[numRecycledQuads] = this;
        numRecycledQuads++;
    }

    @Override
    public void draw(Graphics2D g, Color color) {
        topLeft.draw(g, color);
        topRight.draw(g, color);
        bottomLeft.draw(g, color);
        bottomRight.draw(g, color);
        g.setColor(color);
        g.drawLine((int) getMinX(), (int) getCenterY(), (int) getMaxX(), (int) getCenterY());
        g.drawLine((int) getCenterX(), (int) getMinY(), (int) getCenterX(), (int) getMaxY());
        drawNumEntities(g, Color.BLACK);
    }

    private void drawNumEntities(Graphics2D g, Color color) {
        g.setColor(color);
        int offset = 10;
        int size = offset * 2;
        g.fillRect((int) getCenterX() - offset, (int) getCenterY() - offset, size, size);
        g.setColor(Color.WHITE);

        String string = "" + entityCount;
        FontMetrics metrics = g.getFontMetrics();
        Rectangle2D rect = metrics.getStringBounds(string, g);

        g.drawString(string, (int) (getCenterX() - rect.getWidth() / 2), (int) (getCenterY()));
    }

    @Override
    public int getRealEntityCount() {
        int count = 0;
        for (int i = 0; i < entities.length; i++) {
            Entity entity = entities[i];
            if (i >= entityListPos) {
                assert entity == null;
            } else {
                assert entity != null;
                count++;
            }
        }
        count += topLeft.getRealEntityCount();
        count += topRight.getRealEntityCount();
        count += bottomLeft.getRealEntityCount();
        count += bottomRight.getRealEntityCount();
        return count;
    }

    @Override
    public void clear(CollisionList list) {
        super.clear(list);
        topLeft.clear(list);
        topRight.clear(list);
        bottomLeft.clear(list);
        bottomRight.clear(list);
    }

    public void relocate(Entity entity) {
        if (!isContainedInTree(entity)) {
            entityCount--;
            parent.relocate(entity);
        } else {
            insertEntity(entity);
        }
    }
}
