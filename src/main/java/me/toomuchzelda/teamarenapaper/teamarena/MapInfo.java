package me.toomuchzelda.teamarenapaper.teamarena;

//basic config info for maps
public class MapInfo {
    public String name;
    public String author;
    public String description;

    public boolean doDaylightCycle;
    //0 for clear, 1 for downpour/rain/snow, 2 for thunder
    public int weatherType;
    public boolean doWeatherCycle;

    public MapInfo() {

    }
}
