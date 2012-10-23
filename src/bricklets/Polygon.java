package bricklets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * @author davidrusu
 */
public class Polygon extends Shape{
    private static Random rand = null;
    private Color color = Color.orange;
    private Vector2D[] normals, points; // points are relative to the center
    private double[] normalMins, normalMaxs;
    private int[] xInts, yInts;
    private double width, height, minShadowX, maxShadowX, minShadowY, maxShadowY;
    private int numPoints;
    private boolean usingBoundingBox = true;
    
    /**
     * Constructs a polygon shape. the points must be relative to the center
     * @param x
     * @param y
     * @param dx
     * @param dy
     * @param xPoints
     * @param yPoints 
     */
    public Polygon(double x, double y, double dx, double dy, double[] xPoints, double[] yPoints, Entity parentEntity){
        super(x, y, dx, dy, getRadius(xPoints, yPoints), parentEntity);
        numPoints = xPoints.length;
        points = new Vector2D[numPoints];
        normals = new Vector2D[numPoints];
        normalMins = new double[numPoints];
        normalMaxs = new double[numPoints];
        xInts = new int[numPoints];
        yInts = new int[numPoints];
        setupPoints(xPoints, yPoints);
        setupMaxMin();
        setupNormalsAndShadows();
        setupBounding();
    }
    
    public static void setRandomSeed(long seed){
        if(rand == null){
            rand = new Random(seed);
        }else{
            rand.setSeed(seed);
        }
    }
    
    public static Polygon getRectanglePolygon(double x, double y, double width, double height, Entity parentEntity){
        double[] xPoints = {0, width, width, 0};
        double[] yPoints = {0, 0, height, height};
        return new Polygon(x, y, 0, 0, xPoints, yPoints, parentEntity);
    }
    
    public static Polygon getRandomConvexPolygon(double x, double y, double radiusMin, double radiusMax, int numPointsMin, int numPointsMax, long seed, Entity parentEntity){
        if(rand == null){
            rand = new Random(seed);
        }
        double radius = rand.nextDouble() * (radiusMax - radiusMin) + radiusMin;
        int numPoints = (int)(rand.nextDouble() * (numPointsMax - numPointsMin)) + numPointsMin;
        double[] xPoints = new double[numPoints];
        double[] yPoints = new double[numPoints];
        double angle = 2 * Math.PI / numPoints;
        double initAngle = rand.nextDouble() * 2 * Math.PI;
        for(int p = 0; p < numPoints; p++){
            double radomAngle = rand.nextDouble() * angle / 2 - angle;
            double angleFluctuated = angle * p + radomAngle + initAngle;
            double pX = Math.cos(angleFluctuated) * radius;
            double pY = Math.sin(angleFluctuated) * radius;
            xPoints[p] = pX;
            yPoints[p] = pY;
        }
        return new Polygon(x, y, 0, 0, xPoints, yPoints, parentEntity);
    }
    
    private static double getRadius(double[] xPoints, double[] yPoints){
        double maxDistSquared = 0;
        for(int p = 0; p < xPoints.length; p++){
            double x = xPoints[p];
            double y = yPoints[p];
            double distSquared = x * x + y * y;
            maxDistSquared = Math.max(distSquared, maxDistSquared);
        }
        return Math.sqrt(maxDistSquared);
    }
    
    @Override
    public int getShapeType(){
        return TYPE_POLYGON;
    }
    
    /**
     * the first normal in the array is for the line points[points.length - 1] and points[0],
     * the second normal is for line points[0] and points[1].
     * @return an array of {@link Vector2D} point outwards
     */
    public Vector2D[] getNormals(){
        return normals;
    }
    
    public double[] getNormalMins(){
        return normalMins;
    }
    public double[] getNormalMaxs(){
        return normalMaxs;
    }
    
    public double getWidth(){
        return width;
    }
    
    public double getHeight(){
        return height;
    }
    
    public boolean isUsingBoundingBox(){
        return usingBoundingBox;
    }
    
    /**
     * Points are relative to the center
     * @return {@link Vector2D} array with points that are relative to the center
     */
    public Vector2D[] getPoints(){
        return points;
    }
    
    public int getNumPoints(){
        return numPoints;
    }
    
    public double getArea(){
        double sum = 0;
        double lastX = points[numPoints - 1].getX(), lastY = points[numPoints - 1].getY();
        for(int i = 0; i < numPoints; i++){
            double x = points[i].getX();
            double y = points[i].getY();
            sum += lastX * y - lastY * x;
            lastX = x;
            lastY = y;
        }
        return Math.abs(sum * 0.5);
    }
    
    public void setColor(Color color){
        this.color = color;
    }
    
    private boolean willBoundingCollide(Polygon b, double maxTime){
        if(!isUsingBoundingBox() && !b.isUsingBoundingBox()){
            return willBoundingCircleCircleCollide(b, maxTime);
        }else if(!isUsingBoundingBox() && b.isUsingBoundingBox()){
            return willBoundingCircleBoxCollide(this, b, maxTime);
        }else if(isUsingBoundingBox() && !b.isUsingBoundingBox()){
            return willBoundingCircleBoxCollide(b, this, maxTime);
        }else if(isUsingBoundingBox() && isUsingBoundingBox()){
            return willBoundingBoxBoxCollide(b, maxTime);
        }
        return false;
    }
    
    private boolean willBoundingCircleCircleCollide(Polygon b, double maxTime){
        double combinedVelX = b.dx - dx;
        double combinedVelY = b.dy - dy;
        double distToLineSquared = Vector2D.distToLineSquared(x, y, b.x, b.y, b.x + combinedVelX, b.y + combinedVelY);
        double radiiSum = radius + b.radius;
        if(distToLineSquared > radiiSum * radiiSum){
            return false;
        }

        double deltaX = b.x - x;
        double deltaY = b.y - y;
        double distBetween = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        double projVel = Vector2D.scalarProject(combinedVelX, combinedVelY, deltaX, deltaY, distBetween);
        if(projVel < 0){
            return false;
        }

        distBetween -= radiiSum;
        double travelTime = distBetween / projVel;
        return travelTime <= maxTime;
    }
        
    /**
     * 
     * @param a the polygon with a bounding circle
     * @param b the polygon with a bounding box
     * @param maxTime
     * @return 
     */
    private boolean willBoundingCircleBoxCollide(Polygon a, Polygon b, double maxTime){
        double relativeVelX = a.dx - b.dx, relativeVelY = a.dy - b.dy;
        double maxTimeToCollision;
        
        double timeToCollide = getTimeToCollisionPlanePlane(b.x + b.minShadowX, a.x + a.radius, relativeVelX);
        if(timeToCollide < 0){
            return false;
        }
        maxTimeToCollision = timeToCollide;
        
        timeToCollide = getTimeToCollisionPlanePlane(b.y + b.minShadowY, a.y + a.radius, relativeVelY);
        if(timeToCollide < 0){
            return false;
        }
        maxTimeToCollision = Math.max(timeToCollide, maxTimeToCollision);
        
        timeToCollide = getTimeToCollisionPlanePlane(b.x + b.maxShadowX, a.x - a.radius, relativeVelX);
        if(timeToCollide < 0){
            return false;
        }
        maxTimeToCollision = Math.max(timeToCollide, maxTimeToCollision);
        
        timeToCollide = getTimeToCollisionPlanePlane(b.y + b.maxShadowY, a.y - a.radius, relativeVelY);
        if(timeToCollide < 0){
            return false;
        }
        maxTimeToCollision = Math.max(timeToCollide, maxTimeToCollision);
        
        //checking collision with points
        relativeVelX *= -1;
        relativeVelY *= -1;
        double radiusSquared = a.radius * a.radius;
        maxTimeToCollision = Math.max(getTimeToCollisionPointAndCircle(a.x, a.y, radiusSquared, b.minShadowX, b.minShadowY, relativeVelX, relativeVelY), maxTimeToCollision);
        maxTimeToCollision = Math.max(getTimeToCollisionPointAndCircle(a.x, a.y, radiusSquared, b.maxShadowX, b.minShadowY, relativeVelX, relativeVelY), maxTimeToCollision);
        maxTimeToCollision = Math.max(getTimeToCollisionPointAndCircle(a.x, a.y, radiusSquared, b.maxShadowX, b.maxShadowY, relativeVelX, relativeVelY), maxTimeToCollision);
        maxTimeToCollision = Math.max(getTimeToCollisionPointAndCircle(a.x, a.y, radiusSquared, b.minShadowX, b.maxShadowY, relativeVelX, relativeVelY), maxTimeToCollision);
        return maxTimeToCollision <= maxTime;
    }
    
    private boolean willBoundingBoxBoxCollide(Polygon b, double maxTime){
        double relativeVelX = dx - b.dx, relativeVelY = dy - b.dy;
        double maxTimeToCollision = -Double.MAX_VALUE;
        
        if(relativeVelX > 0){
            double actualBX = b.x + b.minShadowX, actualAX = x + maxShadowX;
            double timeToCollision= getTimeToCollisionPlanePlane(actualBX, actualAX, relativeVelX);
            if(timeToCollision < 0){
                return false;
            }
            maxTimeToCollision = timeToCollision;
        }else if(relativeVelX < 0){
            double actualBX = b.x + b.maxShadowX, actualAX = x + minShadowX;
            double timeToCollision= getTimeToCollisionPlanePlane(actualBX, actualAX, relativeVelX);
            if(timeToCollision < 0){
                return false;
            }
            maxTimeToCollision = timeToCollision;
        }else{
            double minA = x + minShadowX, maxA = x + maxShadowX;
            double minB = b.x + b.minShadowX, maxB = b.x + b.maxShadowX;
            if(minA > maxB || maxA < minB){
                return false;
            }
            maxTimeToCollision = 0;
        }
        
        if(relativeVelY > 0){
            double actualBY = b.y + minShadowY, actualAY = y + maxShadowY;
            double timeToCollision = getTimeToCollisionPlanePlane(actualBY, actualAY, relativeVelY);
            if(timeToCollision < 0){
                return false;
            }
            maxTimeToCollision = Math.max(timeToCollision, maxTimeToCollision);
        }else if(relativeVelY < 0){
            double actualBY = b.y + b.maxShadowY, actualAY = y + minShadowY;
            double timeToCollision = getTimeToCollisionPlanePlane(actualBY, actualAY, relativeVelY);
            if(timeToCollision < 0){
                return false;
            }
            maxTimeToCollision = Math.max(timeToCollision, maxTimeToCollision);
        }else{
            double minA = y + minShadowY, maxA = y + maxShadowY;
            double minB = b.y + b.minShadowY, maxB = b.y + b.maxShadowY;
            if(minA > maxB || maxA < minB){
                return false;
            }
        }
        
        return maxTimeToCollision < maxTime;
    }
    
    private double getTimeToCollisionPointAndCircle(double circleX, double circleY, double radiusSquared, double pointX, double pointY, double pointVelX, double pointVelY){
        double distToLineSquared = Vector2D.distToLineSquared(circleX, circleY, pointX, pointY, pointX + pointVelX, pointY + pointVelY);
        if(distToLineSquared <= radiusSquared){
            double subLength = Math.sqrt(radiusSquared - distToLineSquared);
            double velLength = Math.sqrt(pointVelX * pointVelX + pointVelY * pointVelY);
            double projLength = Vector2D.scalarProject(circleX - pointX, circleY - pointY, pointVelX, pointVelY, velLength);
            if(projLength > 0){
                return (projLength - subLength) / velLength;
            }
        }
        return -Double.MAX_VALUE;
    }
    
    private double getTimeToCollisionPlanePlane(double actualBPos, double actualAPos, double relativeVel){
        double dist = actualBPos - actualAPos;
        if(relativeVel == 0){
            if(dist < 0){
                return 0;
            }
            return -Double.MAX_VALUE;
        }
        return dist / relativeVel;
    }
    
    public void checkPolyPolyCollision(Polygon b, double maxTime, Collision result){
        if(!willBoundingCollide(b, maxTime)){
            result.set(Double.MAX_VALUE, null, this, b);
            return;
        }
        Vector2D[] aNormals = normals;
        Vector2D[] bNormals = b.normals;
        Vector2D[] aPoints = points;
        Vector2D collisionNormal = null;
        double[] bMins = b.normalMins;
        double[] bMaxs = b.normalMaxs;
        double combinedVelocityX = b.dx - dx, combinedVelocityY = b.dy - dy;
        double maxEntryTime = -Double.MAX_VALUE, minLeaveTime = Double.MAX_VALUE;
        
        for(int i = 0; i < bNormals.length; i++){
            Vector2D normal = bNormals[i];
            double aMin = Double.MAX_VALUE;
            double aMax = -Double.MAX_VALUE;
            for(Vector2D point: aPoints){
                double dist = point.unitScalarProject(normal);
                if(dist < aMin){
                    aMin = dist;
                }
                if(dist > aMax){
                    aMax = dist;
                }
            }
            double centerA = Vector2D.unitScalarProject(x, y, normal);
            double centerB = Vector2D.unitScalarProject(b.x, b.y, normal);
            aMin += centerA;
            aMax += centerA;
            double bMin = bMins[i] + centerB;
            double bMax = bMaxs[i] + centerB;
            double projVel = Vector2D.unitScalarProject(combinedVelocityX, combinedVelocityY, normal);
            if(aMax <= bMin){
                if(projVel < 0){
                    double timeToOverlap = (aMax - bMin) / projVel;
                    if(timeToOverlap > maxEntryTime){
                        maxEntryTime = timeToOverlap;
                        collisionNormal = normal;
                    }
                }else{
                    // not travelling away from each other
                    //TODO should have an early return here
                    maxEntryTime = NO_COLLISION;
                }
            }else if(bMax <= aMin){
                if(projVel > 0){
                    double timeToOverlap = (aMin - bMax) / projVel;
                    if(timeToOverlap > maxEntryTime){
                        maxEntryTime = timeToOverlap;
                        collisionNormal = normal;
                    }
                }else{
                    // not travelling away from each other
                    //TODO should have an early return here
                    maxEntryTime = NO_COLLISION;
                }
            }
            
            if(bMax > aMin && projVel < 0){
                double timeToLeave = (aMin - bMax) / projVel;
                if(timeToLeave < minLeaveTime){
                    minLeaveTime = timeToLeave;
                }
            }else if(aMax > bMin && projVel > 0){
                double timeToLeave = (aMax - bMin) / projVel;
                if(timeToLeave < minLeaveTime){
                    minLeaveTime = timeToLeave;
                }
            }
        }
        
        Vector2D[] bPoints = b.points;
        for(int i = 0; i < aNormals.length; i++){
            Vector2D normal = aNormals[i];
            double bMin = Double.MAX_VALUE;
            double bMax = -Double.MAX_VALUE;
            for(Vector2D point: bPoints){
                double dist = point.unitScalarProject(normal);
                if(dist < bMin){
                    bMin = dist;
                }
                if(dist > bMax){
                    bMax = dist;
                }
            }
            double centerA = Vector2D.unitScalarProject(x, y, normal);
            double centerB = Vector2D.unitScalarProject(b.x, b.y, normal);
            bMin += centerB;
            bMax += centerB;
            double aMin = normalMins[i] + centerA;
            double aMax = normalMaxs[i] + centerA;
            double projVel = -Vector2D.unitScalarProject(combinedVelocityX, combinedVelocityY, normal);
            if(bMax <= aMin){
                if(projVel < 0){
                    double timeToOverlap = (bMax - aMin) / projVel;
                    if(timeToOverlap > maxEntryTime){
                        maxEntryTime = timeToOverlap;
                        collisionNormal = normal;
                    }
                }else{
                    // not travelling towards each other
                    //TODO should have an early return here
                    maxEntryTime = NO_COLLISION;
                }
            }else if(aMax <= bMin){
                if(projVel > 0){
                    double timeToOverlap = (bMin - aMax) / projVel;
                    if(timeToOverlap > maxEntryTime){
                        maxEntryTime = timeToOverlap;
                        collisionNormal = normal;
                    }
                }else{
                    // not travelling towards each other
                    //TODO should have an early return here
                    maxEntryTime = NO_COLLISION;
                }
            }
            
            if(aMax > bMin && projVel < 0){
                double timeToLeave = (bMin - aMax) / projVel;
                if(timeToLeave < minLeaveTime){
                    minLeaveTime = timeToLeave;
                }
            }else if(bMax > aMin && projVel > 0){
                double timeToLeave = (bMax - aMin) / projVel;
                if(timeToLeave < minLeaveTime){
                    minLeaveTime = timeToLeave;
                }
            }
        }
        if(maxEntryTime == -Double.MAX_VALUE || maxEntryTime > minLeaveTime){
            maxEntryTime = NO_COLLISION;
        }
        result.set(maxEntryTime, collisionNormal, b, this);
    }
    
    public boolean isIntersectingBounding(Polygon polygon){
        if(usingBoundingBox){
            if(polygon.usingBoundingBox){
                if((maxShadowX < polygon.minShadowX || minShadowX > polygon.maxShadowX) && (maxShadowY < polygon.minShadowY || minShadowY > polygon.maxShadowY)){
                    return false;
                }
            }else{ // polygon is using bounding circle
                if((polygon.x + polygon.radius < minShadowX || polygon.x - polygon.radius > maxShadowX) && (polygon.y + polygon.radius < minShadowY || polygon.y - polygon.radius > maxShadowY)){
                    return false;
                }
            }
        }else{
            if(polygon.usingBoundingBox){
                // not accurate circle is treated as a square
                if((x + radius < polygon.minShadowX || x - radius > polygon.maxShadowX) && (y + radius < polygon.minShadowY || y - radius > polygon.maxShadowY)){
                    return false;
                }
            }else{ // both polygons are using bounding circle
                double dx = polygon.x - x;
                double dy = polygon.y - y;
                double combinedRadius = radius + polygon.radius;
                if(dx * dx + dy * dy >= combinedRadius * combinedRadius){
                    return false;
                }
            }
        }
        return true;
    }
    
    public boolean isIntersecting(Polygon polygon){
        if(!isIntersectingBounding(polygon)){
            return false;
        }
        Polygon poly1 = this, poly2 = polygon;
        if(numPoints < polygon.numPoints){
            poly1 = polygon;
            poly2 = this;
        }
        int numPoints = poly2.numPoints;
        for(int p = 0; p < numPoints; p++){
            Vector2D normal = poly2.normals[p];
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for(int q = 0; q < poly1.numPoints; q++){
                double dist = poly1.points[q].unitScalarProject(normal);
                if(dist < min){
                    min = dist;
                }
                if(dist > max){
                    max = dist;
                }
            }
            
            double poly1Dist = Vector2D.unitScalarProject(poly1.x, poly1.y, normal);
            double poly2Dist = Vector2D.unitScalarProject(poly2.x, poly2.y, normal);
            if(poly1Dist < poly2Dist){
                double distBetween = poly2Dist - poly1Dist;
                if(max - poly2.normalMins[p] < distBetween){
                    return false;
                }
            }else{
                double distBetween = poly1Dist - poly2Dist;
                if(poly2.normalMaxs[p] - min < distBetween){
                    return false;
                }
            }
        }
        return true;
    }
    
    public void draw(Graphics2D g, Color color){
        for(int i = 0; i < numPoints; i++){
            xInts[i] = (int)(points[i].getX() + x);
            yInts[i] = (int)(points[i].getY() + y);
        }
        g.setColor(color);
        g.fillPolygon(xInts, yInts, numPoints);
//        drawBounding(g);
    }
    
    private void drawBounding(Graphics2D g){
        g.setColor(Color.ORANGE);
        if(usingBoundingBox){
            g.drawRect((int)(x + minShadowX), (int)(y + minShadowY), (int)(width), (int)(height));
        }else{
            g.drawOval((int)(x - radius), (int)(y - radius), (int)(radius * 2), (int)(radius * 2));
        }
    }
    
    private void setupPoints(double[] xPoints, double[] yPoints){
        for(int i = 0; i < numPoints; i++){
            points[i] = new Vector2D(xPoints[i], yPoints[i]);
        }
    }
    
    private void setupBounding(){
        double boxArea = (width) * (height);
        double circleArea = Math.PI * radius * radius;
        usingBoundingBox = circleArea > boxArea;
    }
    
    private void setupMaxMin(){
        minShadowX = Double.MAX_VALUE;
        maxShadowX = -Double.MAX_VALUE;
        minShadowY = Double.MAX_VALUE;
        maxShadowY = -Double.MAX_VALUE;
        for(Vector2D point: points){
            double x = point.getX();
            double y = point.getY();
            if(x < minShadowX){
                minShadowX = x;
            }
            if(x > maxShadowX){
                maxShadowX = x;
            }
            if(y < minShadowY){
                minShadowY = y;
            }
            if(y > maxShadowY){
                maxShadowY = y;
            }
        }
        width = maxShadowX - minShadowX;
        height = maxShadowY - minShadowY;
    }
    
    private void setupNormalsAndShadows(){
        Vector2D point = points[numPoints - 1];
        double lastX = point.getX();
        double lastY = point.getY();
        for(int i = 0; i < numPoints; i++){
            point = points[i];
            double x = point.getX();
            double y = point.getY();
            Vector2D normal = new Vector2D(y - lastY, lastX - x);
            normals[i] = normal.unit();
            lastX = x;
            lastY = y;
            
            //finding the min and max when each point is projected onto the normal
            //used when checking if there is an intersection
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for(int q = 0; q < numPoints; q++){
                double dist = points[q].unitScalarProject(normal);
                if(dist < min){
                    min = dist;
                }
                if(dist > max){
                    max = dist;
                }
            }
            normalMins[i] = min;
            normalMaxs[i] = max;
        }
    }
}