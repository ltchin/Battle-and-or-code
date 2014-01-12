package team035_jrowan;

import battlecode.common.*;
import battlecode.engine.instrumenter.lang.System;
import battlecode.world.Util;
import team035_jrowan.Job;
import team035_jrowan.Collection;
import team035_jrowan.Defense;
import team035_jrowan.Offense;

import java.util.*;

public class MapFunctions {
	public static MapLocation mladd(MapLocation m1, MapLocation m2){
		return new MapLocation(m1.x+m2.x,m1.y+m2.y);
	}
	
	public static MapLocation mldivide(MapLocation bigM, int divisor){
		return new MapLocation(bigM.x/divisor, bigM.y/divisor);
	}

	public static int locToInt(MapLocation m){
		return (m.x*100 + m.y);
	}
	
	public static MapLocation intToLoc(int i){
		return new MapLocation(i/100,i%100);
	}
}
