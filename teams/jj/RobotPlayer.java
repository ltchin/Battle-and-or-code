package jj;

//developing CollectionBot, codenamed JamesBot
//the bot whose job is to find fertile ground and peacefully build pastures

//TODO develop some sort of heuristic for how "costly" a square is.

//jrowa - really stupid; half the robots swarm, half go toward first optimal most fertile square and then build a pasture there.
//jj  - first attempts at a semi-sane collection bot and a "fertility heuristic"

//CURRENTLY: each collection bot picks a random maximal-growth-rate square and goes to it.
//			 pathfinding currently done by straight-line then random motion until straight-line works again

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
	static MapLocation[] fertileGrounds;
	static MapLocation fertileGround;
	static Direction toFertile;
	static MapLocation myLoc;
	static int SHEEPDOG = 1;
	static int BUILDER = 0;
	static MapLocation[] PASTRs;
	static MapLocation targetPASTR;
	
	public static MapLocation[] mostFertile(double[][] map){ //builds an array containing all locations with maximal cow birthrate
		int maxX = 0; //definitely could be optimized 
		int maxY = 0; //currently, it finds the highest birthrate, counts number with that birthrate
		for(int i=0; i<cowGrowth.length; i++){ //then puts all locations with that birthrate in an array
			for(int j=0; j<cowGrowth[0].length; j++){
				if(map[i][j]>map[maxX][maxY]){
					maxX = i;
					maxY = j;
				}
			}
		}
		double maxRate = map[maxX][maxY];
		/*for(int i=0; i<cowGrowth.length; i++){
			for(int j=0; j<cowGrowth[0].length; j++){
				if(map[i][j]==maxRate){
					numOptimal++;
				}
			}
		}*/
		int counter = 0;
		ArrayList<MapLocation> answer = new ArrayList<MapLocation>();
		MapLocation[] caster = {new MapLocation(maxX, maxY)};
		for(int i=0; i<cowGrowth.length; i++){
			for(int j=0; j<cowGrowth[0].length; j++){
				if(map[i][j]==maxRate){
					answer.add(counter, new MapLocation(i,j));
					counter++;
				}
			}
		}
		return (MapLocation[]) answer.toArray(caster);
	}
	
	public static void run(RobotController rcin){
		rc = rcin;
		randall.setSeed(rc.getRobot().getID());
		type = rc.getRobot().getID()%2;
		initialized = false;
		PASTRs = rc.sensePastrLocations(rc.getTeam());
		targetPASTR = new MapLocation(10, 10);
		while(true){
			try{
				if(rc.getType()==RobotType.HQ){//if I'm a headquarters
					initialized = true;
					runHeadquarters();
				}else if(rc.getType()==RobotType.SOLDIER){
					if(!initialized){ //TODO Can this initialize check be eliminated by moving it outside loop?
						cowGrowth = rc.senseCowGrowth(); //this should be done by the hq and told to the robot
						if(mostFertile(cowGrowth).length > 0){
							fertileGround = mostFertile(cowGrowth)[randall.nextInt(cowGrowth.length)];
						}
					}
					if(type == BUILDER){
						if(!initialized){ //TODO Can this initialize check be eliminated by moving it outside loop?
							cowGrowth = rc.senseCowGrowth(); //this should be done by the hq and told to the robot
							if(mostFertile(cowGrowth).length > 0){
								fertileGround = mostFertile(cowGrowth)[randall.nextInt(cowGrowth.length)];
							}
						}
						initialized = true;
						runPASTR();
					}
					else{
						if(Clock.getRoundNum()%50 == 0){
							PASTRs = rc.sensePastrLocations(rc.getTeam());
							targetPASTR = PASTRs[randall.nextInt(PASTRs.length)];
						}
						runSheepdog();
					}
				}
				rc.yield();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	private static void runSwarmer() throws GameActionException {  //controls swarmers
		tryToShoot();
		//communication
		//rc.setIndicatorString(0, "read ID: "+rc.readBroadcast(0));
		int editingChannel = (Clock.getRoundNum()%2)+100; //shifted up so HQ can tell builders what to do.
		int usingChannel = ((Clock.getRoundNum()+1)%2)+100;
		
		int runningTotal = rc.readBroadcast(editingChannel);
		rc.broadcast(editingChannel, runningTotal+1);
		
		MapLocation runningVectorTotal = intToLoc(rc.readBroadcast(editingChannel+2));
		rc.broadcast(editingChannel+2,locToInt(mladd(runningVectorTotal,rc.getLocation())));
		MapLocation averagePositionOfSwarm = mldivide(intToLoc(rc.readBroadcast(usingChannel+2)),rc.readBroadcast(usingChannel));
		
		rc.setIndicatorString(0, ""+locToInt(averagePositionOfSwarm));
		
		//movement
//		Direction chosenDirection = allDirections[(int)(randall.nextDouble()*8)];
//		if(rc.isActive()&&rc.canMove(chosenDirection)){
//			rc.move(chosenDirection);
//		}
		swarmMove(averagePositionOfSwarm);
	}
	
	private static void runSheepdog() throws GameActionException { //controls robots destined to herd cows
		myLoc = rc.getLocation();
		if(myLoc.distanceSquaredTo(targetPASTR)<3){
			targetPASTR = PASTRs[randall.nextInt(PASTRs.length)];
		}
		else{
			Direction moveDirection = myLoc.directionTo(targetPASTR);
			if(rc.isActive()&&rc.canMove(moveDirection)){
				rc.move(moveDirection);
			}
			else if(rc.isActive()){
				moveDirection = allDirections[randall.nextInt(8)];
				if(rc.canMove(moveDirection)){
					rc.move(moveDirection);
				}
			}
		}
	}

	
	private static void runPASTR() throws GameActionException { //controls robots destined to build PASTRs
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
				if(rc.canMove(moveDirection)){
					rc.move(moveDirection);
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

	//these functions have been modified from those given in lecture to prevent the y=100->y=0, x++ bug
	
	private static int locToInt(MapLocation m){
		return (m.x*1000 + m.y);
	}
	
	private static MapLocation intToLoc(int i){
		return new MapLocation(i/1000,i%1000);
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