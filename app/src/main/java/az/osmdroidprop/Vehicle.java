package az.osmdroidprop;

public class Vehicle {
    String brigade;
    Double lat;
    String line;
    Double lon;
    String time;
    String type;

    Vehicle(String brigade, Double lat, String line, Double lon, String time, String type) {
        this.brigade = brigade;
        this.lat = lat;
        this.lon = lon;
        this.line = line;
        this.time = time;
        this.type = type;
    }
}

