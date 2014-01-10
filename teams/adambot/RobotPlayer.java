package adambot;

import java.util.Random;

import battlecode.common.*;


public class RobotPlayer {
	static int cohortbstart=0; //where the range of channels for bomberbots begins
	static int pastrlocations=100; //list of enemy pastr locations, formatted as 4-digit integers, xxyy.
	static Random rand;
    static final int CLOSE_ENOUGH = 3;		
    static RobotController rc;
    static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int cohortinfo;
	static int cohort;
	static int target;
	static MapLocation mtarget = new MapLocation(0,0);
	static int soldiertype=0;
	static int bomberstatus=0;
	static Boolean badsuicide=false;
    public static void run(RobotController roboCont){
		rc=roboCont;
		rand = new Random();
		while(true){
			switch(rc.getType()){
			case HQ:
		        runHQ();
		        break;
			case SOLDIER:
				runSoldier();
				break;
			case NOISETOWER:
				break;
			case PASTR:
				break;
			default:
				break;	
			}
		}
	}
	public static void runHQ(){
		//Check if a robot is spawnable and spawn one if it is
		try{
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
		catch(GameActionException e){
			e.printStackTrace();
			System.out.println("bad0");
		}
	}
	public static void runSoldier(){
		try {
			if (rc.readBroadcast(pastrlocations)>0) {
				//Don't run unless the enemy has pastures built
					bomber();
			}
			else{
				MapLocation l=new MapLocation(rc.getMapWidth()/2,rc.getMapHeight()/2);
				moveTo(l);
				soldiertype=0;
				bomberstatus=0;
				rc.yield();
			}
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("bad2");
		}
	}
	public static void bomber() {
		Robot[] threats=rc.senseNearbyGameObjects(Robot.class,10);
		if(badsuicide=true){
			int numthreats=0;
			for(Robot r:threats){
				RobotInfo rinfo=null;
				try {
					rinfo = rc.senseRobotInfo(r);
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("bad3");
				}
				if(rinfo.type==RobotType.SOLDIER&&rinfo.actionDelay<1&&rinfo.team==rc.getTeam().opponent()){
					numthreats++;
				}
				
			}
			if(numthreats*10>=rc.getHealth()){
				bomberstatus=3;
			}
		}
		switch(bomberstatus){
			case 0: //INITIALIZE COHORT DATA
				rc.setIndicatorString(0,"0");
				try{
					cohortinfo = rc.readBroadcast(cohortbstart); 
					cohort=cohortinfo / 10;
					if(cohortinfo % 10==2){
						rc.broadcast(cohortbstart,cohortinfo+8);
					} else{
						rc.broadcast(cohortbstart,cohortinfo+1);
					}
					target=rc.readBroadcast(cohortbstart+cohort+1);
					soldiertype=1;
					bomberstatus=1;
					bomber();
					break;
				}
				catch(GameActionException e){
					e.printStackTrace();
					System.out.println("bad4");
				}
			case 1: //INITIALIZE TARGET DATA
				rc.setIndicatorString(0,"1");
				try{
					System.out.println(""+rc.readBroadcast(pastrlocations));
					System.out.println(pastrlocations);
					target=rc.readBroadcast(pastrlocations+1+rand.nextInt(rc.readBroadcast(pastrlocations)));
					mtarget=new MapLocation(target/100,target%100);
					rc.broadcast(cohortbstart+cohort+1,target);
					mtarget=new MapLocation(target/100,target%100);
					bomberstatus=2;
					System.out.println(target);
					bomber();
					break;
				}
				catch(GameActionException e){
					e.printStackTrace();
					System.out.println("bad5");
				}
			case 2: //GO TO TARGET
				rc.setIndicatorString(0,"2");
				try{
					if(rc.isActive()){
						if(moveTo(mtarget)){
							bomberstatus=5;
							bomber();
						}
					}
				}
				catch(GameActionException e){
					e.printStackTrace();
					System.out.println("bad6");
				}
				break;
			case 3: //SELF-DESTRUCT
				rc.setIndicatorString(0,"3");
				int damage=0;
				for(Robot r:threats){
					try {
						RobotInfo threat=rc.senseRobotInfo(r);
						if(threat.location.distanceSquaredTo(rc.getLocation())<=2){
							if(threat.team==rc.getTeam()){
								damage++;
							} else{
								damage--;
							}
						}
						if(damage<0){
							rc.selfDestruct();
						}
						badsuicide=true;
						bomberstatus=5;
						bomber();
						break;
					} catch (GameActionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.out.println("bad7");
					}
				}
				break;
			case 4: //PIVOT AROUND TARGET
				rc.setIndicatorString(0,"4");
				
				break;
				//IF YOU CAN INCREASE THE DISTANCE TO THE CLOSEST ALLY, WHILE STAYING NEXT TO THE ENEMY PASTR, DO SO
			case 5: //FIRE AT TARGET
				rc.setIndicatorString(0,"5");
				badsuicide=false;
				if (threats.length > 0) {
					try{
						Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
						if (nearbyEnemies.length > 0) {
							RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
							rc.attackSquare(robotInfo.location);
						}
						else{
							bomberstatus=0;
						}
					}
					catch(GameActionException e){
						
					}
				}
				//IF YOU CAN'T GET AWAY FROM YOUR ALLIES, FIRE AT AN ENEMY
				break;
		}

				
				rc.yield();
			
				
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
        }
         catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("bad8");
		}

         return false;
     }
}
