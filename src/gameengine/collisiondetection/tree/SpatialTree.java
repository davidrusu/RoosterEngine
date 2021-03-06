package gameengine.collisiondetection.tree;

import Utilities.UnorderedArrayList;
import gameengine.collisiondetection.Collision;
import gameengine.collisiondetection.World;
import gameengine.collisiondetection.shapes.Shape;
import gameengine.context.Context;
import gameengine.entities.Entity;
import gameengine.entities.RegionSensor;
import gameengine.graphics.RColor;
import gameengine.graphics.Renderer;
import gameengine.motion.environmentmotions.WorldEffect;

/**
 * The root of the spatial tree, used to access the spatial tree.
 *
 * @author davidrusu
 */
public class SpatialTree implements Parent {
    private Tree tree;
    private double initCenterX, initCenterY, initHalfLength;
    private World world;

    public SpatialTree(World world, double centerX, double centerY, double halfLength) {
        this.world = world;
        tree = Leaf.createInstance(world, this, centerX, centerY, halfLength);
        initCenterX = centerX;
        initCenterY = centerY;
        initHalfLength = halfLength;
    }

    public void addEntity(Entity entity) {
        // TODO enforce adding an entity only once
        entity.calculateBoundingBox(0);
        if (isNotContainedInTree(entity)) {
            relocate(entity);
        } else {
            tree.addEntity(entity);
        }
    }

    public void clear() {
        tree.clear();
        tree.recycle();
        // TODO tree already removes all the nodes from the list, may not have to do this
        world.getCollisionList().clear();
        tree = Leaf.createInstance(world, this, initCenterX, initCenterY, initHalfLength);
    }

    public void ensureEntitiesAreContained(double time) {
        assert tree.isEntityCountCorrect();

        tree.ensureEntitiesAreContained(time);

        assert tree.isEntityCountCorrect();
    }

    public void updateMotions(double elapsedTime, UnorderedArrayList<WorldEffect> worldEffects) {
        tree.updateMotions(elapsedTime, worldEffects);
    }

    public void calcCollision(double elapsedTime, Context context) {
        CollisionList list = world.getCollisionList();
        assert list.doAllNodesHaveNoCollision(-1);
        assert list.areNodesSorted();
        assert tree.isEntityCountCorrect();

        double currentTime;
        double timeLeft = elapsedTime;
        tree.initCalcCollision(timeLeft);

        assert tree.isEntityCountCorrect();
        assert list.areNodesSorted();

        Collision collision = list.getNextCollision();
        double timeToUpdate = collision.getCollisionTime();
        while (timeToUpdate <= timeLeft) {
            currentTime = collision.getCollisionTime();

            Entity a = collision.getA();
            Entity b = collision.getB();
            assert a != null;
            assert b != null;
            assert a.getContainingTree() != null;
            assert b.getContainingTree() != null;
            a.getContainingTree().updateEntityPositions(currentTime);
            b.getContainingTree().updateEntityPositions(currentTime);

            Tree aTree = a.getContainingTree();
            Tree bTree = b.getContainingTree();
            boolean isRegion = false;
            //region sensors could be configured to be aware of other region sensors so they need
            //to be checked independently and possibly add each other to both
            if (a instanceof RegionSensor) {
                ((RegionSensor) a).addEntity(b);
                isRegion = true;
            }
            if (b instanceof RegionSensor) {
                ((RegionSensor) b).addEntity(a);
                isRegion = true;
            }
            if (!isRegion) {
                context.handleCollision(collision);
            }

//            assert ensureNoCollisionAfterHandleCollision(collision);
            assert tree.isEntityCountCorrect();

            timeLeft -= timeToUpdate;
            if (a.getContainingTree() != null) {
                aTree.removeEntityFromList(a.getIndexInTree());
                a.calculateBoundingBox(timeLeft);
                if (b.getContainingTree() != null) {
                    bTree.removeEntityFromList(b.getIndexInTree());
                    b.calculateBoundingBox(timeLeft);
                    bTree.entityUpdated(timeLeft, b);
                } else {
                    bTree.entityRemovedDuringCollision(timeLeft, b, currentTime);
                }
                aTree.entityUpdated(timeLeft, a);
            } else if (b.getContainingTree() != null) {
                bTree.removeEntityFromList(b.getIndexInTree());
                aTree.entityRemovedDuringCollision(timeLeft, a, currentTime);
                b.calculateBoundingBox(timeLeft);
                bTree.entityUpdated(timeLeft, b);
            } else {
                aTree.entityRemovedDuringCollision(timeLeft, a, currentTime);
                bTree.entityRemovedDuringCollision(timeLeft, b, currentTime);
            }

            assert tree.isEntityCountCorrect();
            assert list.checkNodeCollision();
            collision = list.getNextCollision();
            timeToUpdate = collision.getCollisionTime() - currentTime;
        }
        tree = tree.updateAllEntityPositionsAndResize(elapsedTime);
        assert list.checkNodeCollision();
        assert list.doAllNodesHaveNoCollision(elapsedTime);
        assert tree.isEntityCountCorrect();
    }

    private boolean ensureNoCollisionAfterHandleCollision(Collision collision) {
        Collision tempCollision = world.getTempCollision();
        tempCollision.set(collision);
        Entity a = tempCollision.getA();
        Entity b = tempCollision.getB();
        Shape.collideShapes(a.getShape(), b.getShape(), Double.MAX_VALUE, tempCollision);
        if (tempCollision.getCollisionTime() != Shape.NO_COLLISION) {
            throw new AssertionError(tempCollision.getCollisionTime());
        }
        return true;
    }

    @Override
    public void decrementEntityCount() {
    }

    @Override
    public void entityRemovedDuringCollision(double timeToCheck, Entity entity, double
            currentTime) {
    }

    public void tryResize() {
        assert tree.isEntityCountCorrect();

        tree = tree.tryResize();

        assert tree.isEntityCountCorrect();
    }

    @Override
    public void childEntityUpdated(double timeToCheck, Entity entity) {
    }

    @Override
    public void relocateAndCheck(double timeToCheck, Entity entity) {
        relocate(entity);
        // TODO adding the entity in the relocate method and then removing it here
        assert tree == entity.getContainingTree();
        entity.getContainingTree().removeEntityFromList(entity.getIndexInTree());
        entity.getContainingTree().relocateAndCheck(timeToCheck, entity);
    }

    @Override
    public void relocate(Entity entity) {
        Shape shape = entity.getShape();
        double centerX = tree.getCenterX(), centerY = tree.getCenterY();
        Tree topLeft, topRight, bottomLeft, bottomRight;

        if (shape.getX() < tree.getCenterX()) {
            centerX -= tree.getHalfLength();
            topLeft = Leaf.createInstance(world);
            bottomLeft = Leaf.createInstance(world);
            if (shape.getY() < tree.getCenterY()) {
                centerY -= tree.getHalfLength();
                topRight = Leaf.createInstance(world);
                bottomRight = tree;
            } else {
                centerY += tree.getHalfLength();
                topRight = tree;
                bottomRight = Leaf.createInstance(world);
            }
        } else {
            centerX += tree.getHalfLength();
            topRight = Leaf.createInstance(world);
            bottomRight = Leaf.createInstance(world);
            if (shape.getY() < tree.getCenterY()) {
                centerY -= tree.getHalfLength();
                topLeft = Leaf.createInstance(world);
                bottomLeft = tree;
            } else {
                centerY += tree.getHalfLength();
                topLeft = tree;
                bottomLeft = Leaf.createInstance(world);
            }
        }
        grow(centerX, centerY, tree.getHalfLength() * 2, topLeft, topRight, bottomLeft,
                bottomRight);
        tree.addEntity(entity);
    }

    public void draw(double minX, double maxX, double minY, double maxY, Renderer renderer) {
        tree.draw(minX, maxX, minY, maxY, renderer);
    }

    public void drawTree(Renderer renderer, RColor color) {
        tree.drawTree(renderer, color);
    }

    private void grow(double centerX, double centerY, double halfLength, Tree topLeft, Tree
            topRight, Tree bottomLeft, Tree bottomRight) {
        double quartLength = halfLength / 2;
        double left = centerX - quartLength;
        double right = centerX + quartLength;
        double top = centerY - quartLength;
        double bottom = centerY + quartLength;
        topLeft.resize(left, top, quartLength);
        topRight.resize(right, top, quartLength);
        bottomLeft.resize(left, bottom, quartLength);
        bottomRight.resize(right, bottom, quartLength);
        tree = Quad.createInstance(world, this, centerX, centerY, halfLength, topLeft, topRight,
                bottomLeft, bottomRight);
    }

    private boolean isNotContainedInTree(Entity entity) {
        return !isContained(entity.getBBCenterX(), tree.getCenterX(), entity.getBBHalfWidth()) ||
                !isContained(entity.getBBCenterY(), tree.getCenterY(), entity.getBBHalfHeight());
    }

    private boolean isContained(double shapePosition, double treePosition, double shapeHalfLength) {
        return Math.abs(treePosition - shapePosition) < tree.getHalfLength() - shapeHalfLength;
    }

    public int getEntityCount() {
        return tree.getEntityCount();
    }
}
