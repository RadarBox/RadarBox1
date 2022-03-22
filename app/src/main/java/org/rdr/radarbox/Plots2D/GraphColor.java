package org.rdr.radarbox.Plots2D;

public enum GraphColor {
    RED(0xFFFF0000),    //0
    GREEN(0xFF00FF00),  //1
    YELLOW(0xFFFFFF00), //2
    BLUE(0xFF0000FF),   //3
    CYAN(0xFF00FFFF),     //4
    MAGNETA(0xFFFF00FF),//5
    MAROON(0xFF800000), //6
    PURPLE(0xFF9400D3), //7
    ORANGE(0xFFFFA500), //8
    NAVY(0xFF000080),   //9
    PINK(0xFFFF69B4),   //10
    SLATE_BLUE(0xFF6A5ACD),     //11
    DODGER_BLUE(0xFF1E90FF),    //12
    SPRING_GREEN(0xFF00FF7F),   //13
    YELLOW_GREEN(0xFF9ACD32),   //14
    DARK_KHAKI(0xFFBDB76B);     //15

    public final int argb;
    private GraphColor(int argb) {
        this.argb = argb;
    }
}
