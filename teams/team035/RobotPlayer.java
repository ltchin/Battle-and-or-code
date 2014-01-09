package team035;

import java.util.Random;

import battlecode.common.*;


/*
 * This bot will go over to where the opponent is building its pastures and build there.
 * Basically it assumes that the opponent's logic is better than its own
 */

public class RobotPlayer {
	static final int CLOSE_ENOUGH = 3;

	static Random rand;
	static RobotController rc; 

	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static SoldierStatus soldierStatus = SoldierStatus.INITIALIZE;

	static MapLocation[] enemyPastrs = {};
	static int enemyPastrsIndex = 0;

	static MapLocation targetPastr;

	public static void run(RobotController roboCont) {
		rc = roboCont;		
		rand = new Random(rc.getRobot().getID());
		while(true) {
			try{
				switch(rc.getType()) {
				case HQ:
					runHQ();
					break;
				case SOLDIER:
					runSoldier();
					break;
				default:
					System.out.println("Shouldn't be happening");
					break;
				}				
			} catch (Exception e) {
				e.printStackTrace();
			}
			rc.yield();
		}
	}

	public static void runHQ() throws GameActionException {
		//Check if a robot is spawnable and spawn one if it is
		if (rc.isActive() && rc.senseRobotCount() < 25) {
			Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			if (rc.senseObjectAtLocation(rc.getLocation().add(toEnemy)) == null) {
				rc.spawn(toEnemy);
			}
		}
	}

	/*
	 * TODO: Insert better pathing algorithim
	 * Temporarily using modified Monte Carlo pathing
	 */
	public static boolean moveTo(MapLocation dest) throws GameActionException {
		MapLocation myLoc = rc.getLocation();

		//If close enough, return true;
		if(myLoc.distanceSquaredTo(dest) <= CLOSE_ENOUGH) {
			return true;
		}

		// Else, keep moving
		Direction moveDirection = rc.getLocation().directionTo(dest);

		while(!rc.canMove(moveDirection)) {
			moveDirection = directions[rand.nextInt(8)];			
		}
		rc.move(moveDirection);

		return false;
	}

	/*
	 * Basically just examplefuncsplayer
	 */
	public static void randAct() throws GameActionException {
		int action = (rc.getRobot().getID()*rand.nextInt(101) + 50)%101;
		if (action < 1 && rc.getLocation().distanceSquaredTo(rc.senseHQLocation()) > 2) {
			rc.construct(RobotType.PASTR);
		} else 	if (action < 30) {
			Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
			if (nearbyEnemies.length > 0) {
				RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
				rc.attackSquare(robotInfo.location);
			}
			//Move in a random direction
		} else if (action < 80) {
			Direction moveDirection = directions[rand.nextInt(8)];
			if (rc.canMove(moveDirection)) {
				rc.move(moveDirection);
			}
			//Sneak towards the enemy
		} else {
			Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			if (rc.canMove(toEnemy)) {
				rc.sneak(toEnemy);
			}
		}
	}


	// Locate enemy pasture
	// Build pasture next to them
	// If pasture exists next to enemy pasture, destroy pasture
	public static void runSoldier() throws GameActionException {
		if (rc.isActive()) {
			switch(soldierStatus) {
			case INITIALIZE:
				if(enemyPastrsIndex >= enemyPastrs.length) {
					enemyPastrsIndex = 0;
					enemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
					randAct(); //TODO: Have better plan if no pastures have been built yet
				} else {
					targetPastr = enemyPastrs[enemyPastrsIndex];
					enemyPastrsIndex++;
					soldierStatus = SoldierStatus.MOVETOPASTR;
				}
				break;
			case MOVETOPASTR:
				if(moveTo(targetPastr)) {
					soldierStatus = SoldierStatus.PASTRFOUND;
				}
				break;
			case PASTRFOUND:
				Robot[] nearbyFriends = rc.senseNearbyGameObjects(Robot.class,10, rc.getTeam());

				if(rc.senseObjectAtLocation(targetPastr) == null) { //If the pasture is already blown up
					for(Robot r : nearbyFriends) { //TODO: make more efficient. This is really really bad bytecode wise
						RobotInfo info = rc.senseRobotInfo(r);

						if(info.isConstructing || info.type == RobotType.PASTR) { //If someone is constructing a pasture already, do something else
							if(rand.nextBoolean()) {
								soldierStatus = SoldierStatus.INITIALIZE; //TODO: Allow other probabilites instead of just 50/50
							} else {
								soldierStatus = SoldierStatus.DEFENSE;
							}												
						} else {
							rc.construct(RobotType.PASTR);
						}
					}
				} else {
					rc.attackSquare(targetPastr);
				}				
				break;
			case DEFENSE: //Currently set as perma defense mode
				Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 10, rc.getTeam().opponent());
				if (nearbyEnemies.length > 0) {
					RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
					rc.attackSquare(robotInfo.location);
				} 
				break;
			default:
				break;
			}
		}
	}

	public static void runPASTR() throws GameActionException {

	}

	public static void runNoiseTower() throws GameActionException {

	}

}
