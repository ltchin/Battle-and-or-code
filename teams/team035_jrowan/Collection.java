package team035_jrowan;

import java.util.ArrayList;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Collection {

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
}
