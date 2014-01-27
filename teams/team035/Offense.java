package team035;

import java.util.*;

import battlecode.common.*;
import battlecode.engine.instrumenter.lang.System;
import battlecode.world.Util;
import team035.Job;
import team035.Collection;
import team035.Defense;
import team035.MapFunctions;
import team035.BasicPathing;

public class Offense{
	static int cohortbstart=0; //where the range of channels for bomberbots begins
	static int pastrlocations=100; //list of enemy pastr locations, formatted as 4-digit integers, xxyy.
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    static final int CLOSE_ENOUGH = 3;		
	static int cohortinfo;
	static int cohort;
	static int target;
	static MapLocation mtarget = new MapLocation(0,0);
	static int soldierType=0;
	static int bomberStatus=0;
	static boolean badSuicide;
	static int directionalLooks[] = new int[]{0,1,-1,2,-2};
	static Direction allDirections[] = Direction.values();
	static int ATTACKCHANNEL = 42;

	public static boolean moveTo(RobotController rc, Random rand, MapLocation dest) throws GameActionException {
		MapLocation myLoc = rc.getLocation();

        //If close enough, return true;
        if(myLoc.distanceSquaredTo(dest) <= CLOSE_ENOUGH) {
        	return true;
        
        }

        // Else, keep moving
        Direction moveDirection = rc.getLocation().directionTo(dest);
        BasicPathing.tryToMove(moveDirection, true, rc, directionalLooks, allDirections);
        /*//this stuff is true
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
			//System.out.println("bad8");
		}
		*/
         return false;
     }
	public static void runHQOffense(RobotController rc) throws GameActionException{
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

	public static void runSoldier(RobotController rc, Random rand) throws GameActionException{
		try {
			if (rc.readBroadcast(pastrlocations)>0) {
				//Don't run unless the enemy has pastures built
					bomber(rc, rand);
			}
			else{ 
				MapLocation middle=new MapLocation(rc.getMapWidth()/2,rc.getMapHeight()/2);
				moveTo(rc, rand, middle);
				soldierType=0;
				bomberStatus=0;
				rc.yield();
			}
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//System.out.println("bad2");
		}
	}
	public static void bomber(RobotController rc, Random rand) {
		Robot[] threats=rc.senseNearbyGameObjects(Robot.class,10);
		if(!badSuicide){
			int numthreats=0;
			for(Robot r:threats){
				RobotInfo rinfo=null;
				try {
					rinfo = rc.senseRobotInfo(r);
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					//System.out.println("bad3");
				}
				if(rinfo.type==RobotType.SOLDIER&&rinfo.team==rc.getTeam().opponent()&&rinfo.actionDelay<=2){
					numthreats++;
				}
				if(numthreats!=0){
					//System.out.println(""+numthreats);
				}
			}
			if(numthreats*10>=rc.getHealth()){
				bomberStatus=3;
			}
		}
		switch(bomberStatus){
			case 0: //INITIALIZE COHORT DATA
				rc.setIndicatorString(0,"0");
				Robot[] nearbyEnemies3 = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
				MapLocation target3 = new MapLocation(-1, -1); //the target square; -1, -1 if no good target
				/*if (nearbyEnemies.length > 0) {
					RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
					rc.attackSquare(robotInfo.location);
				}*/ //this code works; seeking to optimize now
				//new plan; if you seee a PASTR, choose that to attack regardless (this might be bad micro)
				//if you see an enemy constructing something, don't kill it yet (save 
				try{
					for (Robot enemy:nearbyEnemies3){
						RobotInfo enemyInfo = rc.senseRobotInfo(enemy);
						if (enemyInfo.type == RobotType.PASTR){
							target3 = enemyInfo.location;
							break;
						}
						else if(enemyInfo.isConstructing){
						}
						else{
							target3 = enemyInfo.location;
						}
					}
					/*else{
						bomberStatus=0;
					}*/
					if(target3.x == -1){
						bomberStatus = 0;
					}
					else if(rc.isActive()&&rc.canAttackSquare(target3)){
						rc.attackSquare(target3);
					}
				}
				catch (Exception e){
					e.printStackTrace();
				}
				try{
					cohortinfo = rc.readBroadcast(cohortbstart); 
					cohort=cohortinfo / 10;
					if(cohortinfo % 10==2){
						rc.broadcast(cohortbstart,cohortinfo+8);
					} else{
						rc.broadcast(cohortbstart,cohortinfo+1);
					}
					target=rc.readBroadcast(cohortbstart+cohort+1);
					soldierType=1;
					bomberStatus=1;
					bomber(rc, rand);
					break;
				}
				catch(GameActionException e){
					e.printStackTrace();
					//System.out.println("bad4");
				}
			case 1: //INITIALIZE TARGET DATA
				rc.setIndicatorString(0,"1");
				try{
					//System.out.println(""+rc.readBroadcast(pastrlocations));
					//System.out.println(pastrlocations);
					target=rc.readBroadcast(pastrlocations+1+rand.nextInt(rc.readBroadcast(pastrlocations)));
					mtarget=new MapLocation(target/100,target%100);
					rc.broadcast(cohortbstart+cohort+1,target);
					bomberStatus=2;
					//System.out.println(target);
					bomber(rc, rand);
					break;
				}
				catch(GameActionException e){
					e.printStackTrace();
					//System.out.println("bad5");
				}
			case 2: //GO TO TARGET
				rc.setIndicatorString(0,"2");
				Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
				MapLocation target = new MapLocation(-1, -1); //the target square; -1, -1 if no good target
				/*if (nearbyEnemies.length > 0) {
					RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
					rc.attackSquare(robotInfo.location);
				}*/ //this code works; seeking to optimize now
				//new plan; if you seee a PASTR, choose that to attack regardless (this might be bad micro)
				//if you see an enemy constructing something, don't kill it yet (save 
				try{
					for (Robot enemy:nearbyEnemies){
						RobotInfo enemyInfo = rc.senseRobotInfo(enemy);
						if (enemyInfo.type == RobotType.PASTR){
							target = enemyInfo.location;
							break;
						}
						else if(enemyInfo.isConstructing){
						}
						else{
							target = enemyInfo.location;
						}
					}
					/*else{
						bomberStatus=0;
					}*/
					if(target.x == -1){
						bomberStatus = 0;
					}
					else if(rc.isActive()&&rc.canAttackSquare(target)){
						rc.attackSquare(target);
					}
				}
				catch (Exception e){
					e.printStackTrace();
				}
				try{
					if(rc.isActive()){
						/*if(moveTo(rc, rand, mtarget)){
							bomberStatus=5;
							bomber(rc, rand);
						}*/
						if(Defense.near(rc.getLocation(), mtarget, 9)){ //if we're close enough, fight
							bomberStatus = 5;
							bomber(rc, rand);
						}
						else{ //else move
							Direction moveDirection = rc.getLocation().directionTo(mtarget);
							BasicPathing.tryToMove(moveDirection, true, rc, directionalLooks, allDirections);
						}
					}
				}
				catch(GameActionException e){
					e.printStackTrace();
					//System.out.println("bad6");
				}
				break;
			case 3: //SELF-DESTRUCT
				rc.setIndicatorString(0,"3");
				int damage=0;
				try {
					for(Robot r:threats){
						RobotInfo threat=rc.senseRobotInfo(r);
						int howfar=threat.location.distanceSquaredTo(rc.getLocation());
						if(howfar<=2){
							if(threat.team==rc.getTeam()){
								damage++;
							} else{
								damage--;
							}
						}
					}
					rc.setIndicatorString(2,""+damage);
					if(damage<0){
						rc.selfDestruct();
					}
					badSuicide=true;
					bomberStatus=5;
					bomber(rc, rand);
					break;
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					//System.out.println("bad7");
					
				}
				break;
			case 4: //PIVOT AROUND TARGET
				rc.setIndicatorString(0,"4");
				
				break;
				//IF YOU CAN INCREASE THE DISTANCE TO THE CLOSEST ALLY, WHILE STAYING NEXT TO THE ENEMY PASTR, DO SO
			case 5: //FIRE AT TARGET
				rc.setIndicatorString(0,"5");
				badSuicide=false;
				if (threats.length > 0) {
					try{
						Robot[] nearbyEnemies1 = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
						MapLocation target1 = new MapLocation(-1, -1); //the target square; -1, -1 if no good target
						/*if (nearbyEnemies.length > 0) {
							RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
							rc.attackSquare(robotInfo.location);
						}*/ //this code works; seeking to optimize now
						//new plan; if you seee a PASTR, choose that to attack regardless (this might be bad micro)
						//if you see an enemy constructing something, don't kill it yet (save 
						for (Robot enemy:nearbyEnemies1){
							RobotInfo enemyInfo = rc.senseRobotInfo(enemy);
							if (enemyInfo.type == RobotType.PASTR){
								target = enemyInfo.location;
								break;
							}
							else if(enemyInfo.isConstructing){
							}
							else{
								target1 = enemyInfo.location;
							}
						}
						/*else{
							bomberStatus=0;
						}*/
						if(target1.x == -1){
							bomberStatus = 0;
						}
						else{
							rc.attackSquare(target1);
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
}