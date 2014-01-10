package noiseexample;

//developing CollectionBot, codenamed JamesBot
//the bot whose job is to find fertile ground and peacefully build pastures

//TODO develop some sort of heuristic for how "costly" a square is.

//jrowa - really stupid; half the robots swarm, half go toward first optimal most fertile square and then build a pasture there.

import battlecode.common.*;
import battlecode.world.Util;

import java.util.*;

//modified from class Watson, discussed in lecture 2 by teh_devs

public class RobotPlayer{
	
	public static RobotController rc;
	static Direction allDirections[] = Direction.values();
	static Random randall = new Random();
	static int directionalLooks[] = new int[]{0,1,-1,2,-2};
	static double cowGrowth[][];
	static int type;
	static boolean initialized;
	static MapLocation fertileGround;
	static Direction toFertile;
	static MapLocation myLoc;
	
	public static MapLocation mostFertile(double[][] map){
		int maxX = 0;
		int maxY = 0;
		for(int i=0; i<cowGrowth.length; i++){
			for(int j=0; j<cowGrowth[0].length; j++){
				if(map[i][j]>map[maxX][maxY]){
					maxX = i;
					maxY = j;
				}
			}
		}
		MapLocation optimal  = new MapLocation(maxX, maxY);
		return optimal;
	}
	
	public static void run(RobotController rcin){
		rc = rcin;
		randall.setSeed(rc.getRobot().getID());
		type = rc.getRobot().getID()%10;
		initialized = false;
		while(true){
			try{
				if(rc.getType()==RobotType.HQ){//if I'm a headquarters
					initialized = true;
					runHeadquarters();
				}else if(rc.getType()==RobotType.SOLDIER){
					if(!initialized){ //TODO Can this initialize check be eliminated by moving it outside loop?
						cowGrowth = rc.senseCowGrowth();
						fertileGround = mostFertile(cowGrowth);
						
					}
					initialized = true;
					runSoldier();
				}
				else if(rc.getType()==RobotType.NOISETOWER){
					rc.attackSquare(new MapLocation(randall.nextInt(rc.getMapWidth()), randall.nextInt(rc.getMapHeight())));
				}
				rc.yield();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	private static void runSoldier() throws GameActionException {
		if(type == 1){
			if(Clock.getRoundNum()%50 == 0)
			{
				rc.construct(RobotType.NOISETOWER);
			}
			else if(rc.isActive()){
				Direction moveDirection = allDirections[randall.nextInt(8)];
				if(rc.canMove(moveDirection)&&rc.isActive()){
					rc.move(moveDirection);
				}
			}
		}
		else {
			myLoc = rc.getLocation();
			if(myLoc.equals(fertileGround)){
				rc.construct(RobotType.PASTR);
			}
			else{
				Direction moveDirection = myLoc.directionTo(fertileGround);
				if(rc.isActive()&&rc.canMove(moveDirection)){
					rc.move(moveDirection);
				}
				else if(rc.isActive()){
					moveDirection = allDirections[randall.nextInt(8)];
					if(rc.canMove(moveDirection)&&rc.isActive()){
						rc.move(moveDirection);
					}
				}
			}
			
		}
	}
	
	private static void swarmMove(MapLocation averagePositionOfSwarm) throws GameActionException{
		Direction chosenDirection = rc.getLocation().directionTo(averagePositionOfSwarm);
		if(rc.isActive()){
			if(randall.nextDouble()<0.5){//go to swarm center
				for(int directionalOffset:directionalLooks){
					int forwardInt = chosenDirection.ordinal();
					Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
					if(rc.canMove(trialDir)){
						rc.move(trialDir);
						break;
					}
				}
			}else{//go wherever the wind takes you
				Direction d = allDirections[(int)(randall.nextDouble()*8)];
				if(rc.isActive()&&rc.canMove(d)){
					rc.move(d);
				}
			}
		}
	}
	
	private static MapLocation mladd(MapLocation m1, MapLocation m2){
		return new MapLocation(m1.x+m2.x,m1.y+m2.y);
	}
	
	private static MapLocation mldivide(MapLocation bigM, int divisor){
		return new MapLocation(bigM.x/divisor, bigM.y/divisor);
	}

	private static int locToInt(MapLocation m){
		return (m.x*100 + m.y);
	}
	
	private static MapLocation intToLoc(int i){
		return new MapLocation(i/100,i%100);
	}
	
	private static void tryToShoot() throws GameActionException {
		//shooting
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		if(enemyRobots.length>0){//if there are enemies
			Robot anEnemy = enemyRobots[0];
			RobotInfo anEnemyInfo;
			anEnemyInfo = rc.senseRobotInfo(anEnemy);
			if(anEnemyInfo.location.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared){
				if(rc.isActive()){
					rc.attackSquare(anEnemyInfo.location);
				}
			}
		}else{//there are no enemies, so build a tower
			if(randall.nextDouble()<0.001&&rc.sensePastrLocations(rc.getTeam()).length<5){
				//rc.senseCowsAtLocation(arg0);
				if(rc.isActive()){
					rc.construct(RobotType.PASTR);
				}
			}
		}
	}

	private static void runHeadquarters() throws GameActionException {
		Direction spawnDir = Direction.NORTH;
		if(rc.isActive()&&rc.canMove(spawnDir)&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
			rc.spawn(Direction.NORTH);
		}
		
		int editingChannel = (Clock.getRoundNum()%2);
		int usingChannel = ((Clock.getRoundNum()+1)%2);
		rc.broadcast(editingChannel, 0);
		rc.broadcast(editingChannel+2, 0);
	}
}
