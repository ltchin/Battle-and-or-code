package team035_new_meta;

import java.util.ArrayList;

import battlecode.common.*;
import battlecode.engine.instrumenter.lang.System;
import battlecode.world.Util;
import team035_new_meta.Job;
import team035_new_meta.Collection;
import team035_new_meta.Defense;
import team035_new_meta.Offense;
import team035_new_meta.MapFunctions;

//noise tower functions for the new metagame

//for now, pull  in along 8 cardinal directions.  Later, implement spiraling or some sort of pathing.

public class NoiseTower {
	static Direction allDirections[] = Direction.values();
	static MapLocation PASTRToHelp;
	static int noiseChannel = 5000; //the channel to which its 
	static int pathChannelStart = 6000; //the start of the block of channels that the targeting path will be broadcast to
	static boolean done;
	static ArrayList<MapLocation> targetingPath; //the path of targeting
	static int counter;
	static int pathLength;
	static boolean locationChosen;
	static MapLocation pathStart;
	static int myBand = 60;
	static MapLocation myLoc;
	
	static public void runHQNoiseTower(RobotController rc) throws GameActionException{
		Direction toEnemyHQ = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		if(Clock.getRoundNum()<50&&rc.isActive()){
			rc.spawn(toEnemyHQ);
		}
		/*if(locationChosen){
			rc.setIndicatorString(0, "LOCATION FOUND");
			pathStart = rc.getLocation();
			for(int i=0; i<16; i++){
				pathStart = pathStart.add(toEnemyHQ);
			}
			Comms.findPathAndBroadcast(60,pathStart,PASTRToHelp,5,60);
		}
		else
		{
			PASTRToHelp = rc.getLocation().add(rc.getLocation().directionTo(rc.senseEnemyHQLocation()).rotateLeft().rotateLeft().rotateLeft().rotateLeft());
			locationChosen = true;
			rc.spawn(toEnemyHQ.rotateLeft().rotateLeft().rotateLeft()); //spawn the noise tower
		}*/
	}
	
	static public MapLocation[] buildTargets(RobotController rc){
		MapLocation myLoc = rc.getLocation();
		MapLocation[] answer={myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc, myLoc};
		//pull in along 8 directions sequentially
		/*for(int j=0; j<8; j++){
			for(int i=0; i<5; i++)
			{
				answer[i+5*j] = myLoc.add(allDirections[j], 12-2*i);
			}
		}*/
		//spiral/better
		for(int i=0; i<5; i++){
			for(int j=0; j<8; j++)
			{
				answer[i+5*j] = myLoc.add(allDirections[j], 12-2*i);
			}
		}
		//actual spiral
		/*for(int j=0; j<8; j++){
			for(int i=0; i<5; i++)
			{
				answer[j+i*8] = myLoc.add(allDirections[j], 12-2*i);
			}
		}*/
		answer[40] = new MapLocation(-1, -1);
		rc.setIndicatorString(1, "A");
		return answer;
	}
	
	static public MapLocation addMultDirection(MapLocation input, Direction dir, int n){ //add dir to input mapLocatoin n times
		MapLocation ans = input;
		for(int i=n; --i>0;){
			ans = ans.add(dir);
		}
		return ans;
	}
	
	static public void runNoiseTower(RobotController rc, MapLocation[] targetPath) throws GameActionException{
		MapLocation myLoc = rc.getLocation();
		rc.setIndicatorString(0, ""+counter);
		if(rc.isActive()&&targetPath[counter].x!=-1){
			rc.attackSquare(targetPath[counter]);
			counter++;
		}
		else if(rc.isActive()){
			rc.setIndicatorString(2, "C");
		}
		else if(targetPath[counter].x!=-1){
			rc.setIndicatorString(2, "D");
		}
		else{
			counter = 0;
		}
		
		/*targetingPath = Comms.downloadPath();
		rc.setIndicatorString(0, ""+MapFunctions.locToInt(targetingPath.get(0)));
		if(!done){
			MapLocation target = rc.getLocation();
			if(targetingPath.size()>0){
				targetingPath.get(counter);
			}
			rc.attackSquare(target);
			counter++;
			if(counter == targetingPath.size()){
				done = true;
			}
		}
		else {
			counter = 0;
			targetingPath.clear();
			//targetingPath = Comms.downloadPath();
			done = false;
		}*/
	}
}