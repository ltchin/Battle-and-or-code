package team035_jrowan;

import java.util.*;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Defense{
	static Direction allDirections[] = Direction.values();
	static int defenderChannel = 10;
	static boolean inPosition;
	
	private static boolean near(MapLocation myLoc, MapLocation target, int radius) throws GameActionException{
		if(myLoc.distanceSquaredTo(target)<radius){
			return true;
		}
		return false;
	}
	
	public static void runDefender(RobotController rc, Random randall, MapLocation myLoc) throws GameActionException { //primitive defender code
		MapLocation target = MapFunctions.intToLoc(rc.readBroadcast(defenderChannel));
		rc.setIndicatorString(2,""+MapFunctions.locToInt(target));
		//int turnNum = Clock.getRoundNum();
		/*if(turnNum % 30 == 0) //reupdate the PASTR locations only once in a while
		{
			PASTRs = rc.sensePastrLocations(rc.getTeam());
		}*/
		if(near(myLoc, target, 16))
		{
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
			if (nearbyEnemies.length > 0 &&rc.isActive()) {
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
}