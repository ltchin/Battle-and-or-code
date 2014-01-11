package team035_jrowan;

//developing CollectionBot, codenamed JamesBot
//the bot whose job is to find fertile ground and peacefully build pastures
//the naming convention for the derpy versions of this bot is to cycle through past nicknames of mine
//jayrow is current as of 1/9/14

//TODO develop some sort of heuristic for how "costly" a square is.

//main changes: 
import battlecode.common.*;
import battlecode.engine.instrumenter.lang.System;
import battlecode.world.Util;
import jayrow.Job;

import java.util.*;

//important new features: computation of fertile grounds and directing robots where to build is done by HQ
//each individual soldier only computes the most fertile territory if it needs to
//such as if the original target now is within 4 units of another pasture.
//Robots won't build within 4 units of a PASTR or any ally constructing a PASTR

public class RobotPlayer{
	
	public static RobotController rc;
	static Direction allDirections[] = Direction.values();
	static Random randall = new Random();
	static int directionalLooks[] = new int[]{0,1,-1,2,-2};
	static double cowGrowth[][];
	static boolean initialized;
	static MapLocation fertileGround;
	static Direction toFertile;
	static MapLocation[] fertileGrounds;
	static MapLocation myLoc;
	static Job myJob;
	static int myID;
	static boolean fGComputed; //tells whether the soldier has computed fertileGrounds yet so he does it at most once
	static MapLocation[] PASTRs;
	static int defenderChannel = 10;
	static boolean inPosition = false;
	static int lastID = 0;
	
	public static MapLocation[] mostFertile(double[][] map, RobotController rc){ //builds an array containing all locations with maximal cow birthrate
		int maxX = 0; //definitely could be optimized 
		int maxY = 0; //currently, it finds the highest birthrate, counts number with that birthrate
		for(int i=0; i<map.length; i++){ //then puts all locations with that birthrate in an array
			for(int j=0; j<map[0].length; j++){
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
		ArrayList<MapLocation> answer = new ArrayList<MapLocation>(100);
		MapLocation[] caster = {new MapLocation(maxX, maxY)};
		for(int i=0; i<rc.getMapWidth(); i++){
			for(int j=0; j<rc.getMapHeight(); j++){
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
		myID = rc.getRobot().getID();
		randall.setSeed(myID);
		myJob = Job.PASTRBUILDER; //for now, only build PASTRs
		initialized = false;
		fGComputed = false;
		myJob = Job.UNASSIGNED;
		try{
			rc.broadcast(999, myID);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		int counter = 0;
		while(true){
			try{
				if(rc.getType()==RobotType.HQ){//if I'm a headquarters, do the computation of where the next bot should build
					initialized = true;
					runHeadquarters();
					if(!fGComputed){
						cowGrowth = rc.senseCowGrowth();
						fertileGrounds = mostFertile(cowGrowth, rc);
					}
					fertileGround = fertileGrounds[randall.nextInt(fertileGrounds.length)];
					rc.setIndicatorString(0, "COMPUTED");
					rc.broadcast(0, locToInt(fertileGround));
					rc.broadcast(1, counter%10); //we spend a lot on this computation i think
					if(1 == 1){ //every 40 turns, reset the PASTRs array and broadcast a new place to defend
						PASTRs = rc.sensePastrLocations(rc.getTeam());
						if(PASTRs.length>0){
							MapLocation defendSpot = PASTRs[randall.nextInt(PASTRs.length)];  
							rc.broadcast(defenderChannel, locToInt(defendSpot));
						}
						else{
							rc.broadcast(defenderChannel, locToInt(fertileGround));
						}
					}
					if(rc.readBroadcast(999)!=lastID)
					{
						lastID = rc.readBroadcast(999);
						counter++;
					}
				}else if(rc.getType()==RobotType.SOLDIER){
					if(myJob == Job.UNASSIGNED){
						if(rc.readBroadcast(1)==0)
						{
							myJob = Job.PASTRBUILDER;
						}
						else {
							rc.setIndicatorString(1,"BECOMING DEFENSE");
							rc.setIndicatorString(2, ""+myJob);
							myJob = Job.DEFENDER;
						}
					}
					switch(myJob) {
					case PASTRBUILDER:{
						if(!initialized){ //TODO Can this initialize check be eliminated by moving it outside loop?
							cowGrowth = rc.senseCowGrowth();
							int orders = rc.readBroadcast(0);
							if(orders!=0){
								fertileGround = intToLoc(orders);
							}
							else {
								fertileGrounds = mostFertile(cowGrowth, rc);
								fGComputed = true;
								fertileGround = fertileGrounds[randall.nextInt(fertileGrounds.length)];
							}						
						}
						initialized = true;
						runPASTRBuilder();
						}
					case NOISEBUILDER:{
						runNoiseBuilder();
					}
					case DEFENDER:{ //if assigned to defense, go to a pasture and attack nearby enemies
						rc.setIndicatorString(0, "DEFENDER");
						runDefender();
					}
					}
					
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
	
	
	
	//is this spot non-overlapping?	
	private static boolean goodSpot(RobotController rc, MapLocation myLoc) throws GameActionException { 	
		//sense nearby robots on our team
		Robot[] nearby = rc.senseNearbyGameObjects(Robot.class, myLoc, 16, rc.getTeam());
		//check to see if those are PASTRs or constructing PASTRs
		for(Robot obj : nearby) {
			RobotInfo objInfo = rc.senseRobotInfo(obj);
			if(objInfo.isConstructing||objInfo.type == RobotType.PASTR){
				return false;
			}
		}
		return true;
	}

	private static void runNoiseBuilder() throws GameActionException {
		MapLocation PASTRToHelp;
		PASTRToHelp = rc.sensePastrLocations(rc.getTeam())[0];
		if(myLoc.distanceSquaredTo(PASTRToHelp)<16 && rc.isActive())
		{
			rc.construct(RobotType.NOISETOWER);
		}
		else if(rc.isActive()){
			Direction moveDirection = myLoc.directionTo(PASTRToHelp);
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
	
	private static boolean nearPastr(MapLocation myLoc, MapLocation[] searchArray, int radius) throws GameActionException{
		for(MapLocation possible : searchArray){
			if(myLoc.distanceSquaredTo(possible) < radius){ //checks if you're close to any site in the searchArray
				return true;
			}
		}
		return false;
	}
	
	
	
	private static boolean near(MapLocation myLoc, MapLocation target, int radius) throws GameActionException{
		if(myLoc.distanceSquaredTo(target)<radius){
			return true;
		}
		return false;
	}
	
	private static void runDefender() throws GameActionException { //primitive defender code
		myLoc = rc.getLocation();
		MapLocation target = intToLoc(rc.readBroadcast(defenderChannel));
		rc.setIndicatorString(2,""+locToInt(target));
		//int turnNum = Clock.getRoundNum();
		/*if(turnNum % 30 == 0) //reupdate the PASTR locations only once in a while
		{
			PASTRs = rc.sensePastrLocations(rc.getTeam());
		}*/
		if(near(myLoc, target, 16))
		{
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
			if (nearbyEnemies.length > 0) {
				RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
				rc.attackSquare(robotInfo.location);
			}
			Robot[] nearbys = rc.senseNearbyGameObjects(Robot.class);
			for(Robot r:nearbys)
			{
				RobotInfo robotInfo = rc.senseRobotInfo(r);
				if(robotInfo.type==RobotType.PASTR){
					inPosition = true;
				}
				else{
					inPosition = false;
				}
			}
		}
		else if(!inPosition){
			Direction moveDirection = myLoc.directionTo(target);
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
	
	private static void runPASTRBuilder() throws GameActionException {
			myLoc = rc.getLocation();
			MapLocation[] friendlyPASTRs = rc.sensePastrLocations(rc.getTeam());
			for(MapLocation past : friendlyPASTRs){
				if(fertileGround.distanceSquaredTo(past) < 16){
					if(!fGComputed){
						cowGrowth = rc.senseCowGrowth();
						fertileGrounds = mostFertile(cowGrowth, rc);
						fGComputed = true;
					}
					fertileGround = fertileGrounds[randall.nextInt(fertileGrounds.length)];
				}
			}
			if(myLoc.equals(fertileGround)&&rc.isActive()){ //if we can and there isn't another PASTR 
				//or PASTR construction nearby, build a PASTR
				if(goodSpot(rc, myLoc)){
					rc.construct(RobotType.PASTR); 
				}
				else{ //otherwise, go find a new spot
					if(!fGComputed){
						cowGrowth = rc.senseCowGrowth();
						fertileGrounds = mostFertile(cowGrowth, rc);
						fGComputed = true;
					}
					fertileGround = fertileGrounds[randall.nextInt(fertileGrounds.length)];
				}
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
