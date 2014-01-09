package adambot;

import java.util.Random;

import battlecode.common.*;


public class RobotPlayer {
	static int cohortbstart=0; //where the range of channels for bomberbots begins
	static int pastrlocations=2; //list of enemy pastr locations, formatted as 4-digit integers, xxyy.
	static Random rand;
    static final int CLOSE_ENOUGH = 3;		
    static RobotController rc;
    static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static void run(RobotController roboCont){
		try {
		rc=roboCont;
		rand = new Random();
		if (rc.getType() == RobotType.HQ) {
			while(true){                                
                     //Check if a robot is spawnable and spawn one if it is
				if (rc.isActive() && rc.senseRobotCount() < 25) {
					Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
					if (rc.senseObjectAtLocation(rc.getLocation().add(toEnemy)) == null) {
						rc.spawn(toEnemy);
					}
				}
				MapLocation[] ptargets=rc.sensePastrLocations(rc.getTeam().opponent());
				int bptargets=0; //broadcast of possible targets
				for(MapLocation m:ptargets){
					bptargets++;
					rc.broadcast(bptargets+pastrlocations, m.x*100+m.y);
				}
					rc.broadcast(pastrlocations,bptargets);
				
				rc.yield();
			}
		}
		if (rc.getType() == RobotType.SOLDIER && rc.readBroadcast(pastrlocations)>0) {
		//Don't run unless the enemy has pastures built
			bomber();
		}
		}
		catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void bomber() {
		int cohortinfo;
		try {
			cohortinfo = rc.readBroadcast(cohortbstart); 
		int cohort=cohortinfo / 10;
		if(cohortinfo % 10==2){
			rc.broadcast(cohortbstart,cohortinfo+8);
			pastrlocations=+1;
		} else{
			rc.broadcast(cohortbstart,cohortinfo+1);
		}
		int target=rc.readBroadcast(cohortbstart+cohort+1);
		MapLocation mtarget = new MapLocation(0,0);
		while(true){
			//System.out.println(target);
			if(target==0){
				target=rc.readBroadcast(pastrlocations+1+rand.nextInt(rc.readBroadcast(pastrlocations)));
				mtarget=new MapLocation(target/100,target%100);
				rc.broadcast(cohortbstart+cohort+1,target);
			}				
			mtarget=new MapLocation(target/100,target%100);
 			System.out.println(cohort);
			
			rc.yield();
		}
		
			
		}
		catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
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
         try {
        	 if(rc.isActive()){
        	 	rc.move(moveDirection);
        	 }
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

         return false;
     }
}
