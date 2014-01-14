package team035_jrowan;

//developing CollectionBot, codenamed JamesBot
//the bot whose job is to find fertile ground and peacefully build pastures
//the naming convention for the derpy versions of this bot is to cycle through past nicknames of mine
//jayrow is current as of 1/9/14

//TODO develop some sort of heuristic for how "costly" a square is.

//main changes: 
import battlecode.common.*;
import team035_jrowan.Job;
import team035_jrowan.Collection;
import team035_jrowan.Defense;
import team035_jrowan.Offense;

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
	static boolean inPosition = false;
	static int lastID = 0;
		
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
						fertileGrounds = Collection.mostFertile(cowGrowth, rc);
					}
					fertileGround = fertileGrounds[randall.nextInt(fertileGrounds.length)];
					rc.setIndicatorString(0, "COMPUTED");
					rc.broadcast(0, MapFunctions.locToInt(fertileGround));
					rc.broadcast(1, counter%10); //we spend a lot on this computation i think
					if(true){ //TODO fix this find-a-new-place-to-defend code
						PASTRs = rc.sensePastrLocations(rc.getTeam());
						if(PASTRs.length>0){
							MapLocation defendSpot = PASTRs[randall.nextInt(PASTRs.length)];  
							rc.broadcast(Defense.defenderChannel, MapFunctions.locToInt(defendSpot));
						}
						else{
							rc.broadcast(Defense.defenderChannel, MapFunctions.locToInt(fertileGround));
						}
					}
					if(rc.readBroadcast(999)!=lastID)
					{
						lastID = rc.readBroadcast(999);
						counter++;
					}
					Offense.runHQOffense(rc);
				}else if(rc.getType()==RobotType.SOLDIER){
					if(myJob == Job.UNASSIGNED){
						if(rc.readBroadcast(1)==0)
						{
							myJob = Job.PASTRBUILDER;
						}
						else if(rc.readBroadcast(1)>6)
						{
							rc.setIndicatorString(1, "BECOMING OFFENSE");
							myJob = Job.OFFENSE;
						}
						else {
							rc.setIndicatorString(1,"BECOMING DEFENSE");
							rc.setIndicatorString(2, ""+myJob);
							myJob = Job.DEFENDER;
						}
					}
					switch(myJob) {
					case PASTRBUILDER:
						//initialization has been moved inside Collection
						Collection.runPASTRBuilder(rc, randall, rc.getLocation());
						break;
					case NOISEBUILDER:
						//runNoiseBuilder();
						break;
					case DEFENDER: //if assigned to defense, go to a pasture and attack nearby enemies
						rc.setIndicatorString(0, "DEFENDER");
						Defense.runDefender(rc, randall, rc.getLocation());
						break;
					case OFFENSE:
						Offense.runSoldier(rc, randall);
						break;
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
	
	
	


	/*private static void runNoiseBuilder() throws GameActionException {
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
	}*/
	

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
