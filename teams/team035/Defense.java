package team035;

import java.util.*;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import team035.Job;
import team035.Collection;
import team035.Offense;
import team035.MapFunctions;
import team035.BasicPathing;

public class Defense{
	static Direction allDirections[] = Direction.values();
	static int defenderChannel = 10;
	static boolean inPosition;
	static int PASTRHelpChannel = 75; //for PASTRs to ask for help
	static int directionalLooks[] = new int[]{0,1,-1,2,-2};
	static int ATTACKCHANNEL = 42;
	static boolean helping;

	public static boolean near(MapLocation myLoc, MapLocation target, int radius) throws GameActionException{
		if(myLoc.distanceSquaredTo(target)<radius){
			return true;
		}
		return false;
	}
	
	public static void runDefender(RobotController rc, Random randall, MapLocation myLoc) throws GameActionException { //primitive defender code
		MapLocation target = MapFunctions.intToLoc(rc.readBroadcast(defenderChannel));
		rc.setIndicatorString(1, ""+rc.readBroadcast(PASTRHelpChannel));
		if(rc.readBroadcast(PASTRHelpChannel)!=0){ //if a PASTR is calling for help
			target = MapFunctions.intToLoc(rc.readBroadcast(PASTRHelpChannel));
			helping = true;
		}
		else{
			helping = false;//
		}
		rc.setIndicatorString(2,""+MapFunctions.locToInt(target));
		//int turnNum = Clock.getRoundNum();
		/*if(turnNum % 30 == 0) //reupdate the PASTR locations only once in a while
		{
			PASTRs = rc.sensePastrLocations(rc.getTeam());
		}*/
		if(near(myLoc, target, 16)&!helping)
		{
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
			if (nearbyEnemies.length > 0 && rc.isActive()) {
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
		else if(helping&&near(myLoc, target, 10)){
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
			if (nearbyEnemies.length > 0 && rc.isActive()) {
				RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
				rc.attackSquare(robotInfo.location);
			}
			else{
				helping = false;
			}
		}
		else if(!inPosition){
			Direction moveDirection = myLoc.directionTo(target);
			//this works but too slow
			/*if(rc.isActive()&&rc.canMove(moveDirection)){
				rc.move(moveDirection);
				
			}
			else if(rc.isActive()){
				moveDirection = allDirections[randall.nextInt(8)];
				if(rc.canMove(moveDirection)&&rc.isActive()){
					rc.move(moveDirection);
				}
			}*/
			BasicPathing.tryToMove(moveDirection, true, rc, directionalLooks, allDirections);
		}
	}
}