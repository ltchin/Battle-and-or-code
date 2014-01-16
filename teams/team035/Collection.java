package team035;

import java.util.*;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import team035.MapFunctions;
//Collection class contains methods for determining where to build PASTRs and for running PASTR-constructors

public class Collection {
	static int directionalLooks[] = new int[]{0,1,-1,2,-2};
	static Direction allDirections[] = Direction.values();
	static MapLocation fertileGround;
	static MapLocation[] fertileGrounds;
	static boolean fGComputed;
	static double[][] cowGrowth;
	static MapLocation myloc;
	static boolean initialized;
	
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

	
	//feed in the RobotController, randomness source, and current location
	//then will move toward the 
	static void runPASTRBuilder(RobotController rc, Random randall, MapLocation myLoc) throws GameActionException {
		if(!initialized){ //TODO Can this initialize check be eliminated by moving it outside loop?
			cowGrowth = rc.senseCowGrowth();
			int orders = rc.readBroadcast(0);
			if(orders!=0){
				fertileGround = MapFunctions.intToLoc(orders);
			}
			else {
				fertileGrounds = Collection.mostFertile(cowGrowth, rc);
				fGComputed = true;
				fertileGround = fertileGrounds[randall.nextInt(fertileGrounds.length)];
			}						
		}
		initialized = true;
		MapLocation[] friendlyPASTRs = rc.sensePastrLocations(rc.getTeam());
		for(MapLocation past : friendlyPASTRs){
			if(fertileGround.distanceSquaredTo(past) < 16){
				if(!fGComputed){
					cowGrowth = rc.senseCowGrowth();
					fertileGrounds = Collection.mostFertile(cowGrowth, rc);
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
					fertileGrounds = Collection.mostFertile(cowGrowth, rc);
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

}
