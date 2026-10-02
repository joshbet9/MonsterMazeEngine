package me.monstermaze.engine.trace;

/** One recorded tick for differential comparison with live/plugin traces. */
public final class TraceFrame {
    public final long tick;
    public final double px, py, pz, pvx, pvy, pvz;
    public final float yaw;
    public final double health;
    public final int stage;
    public final int phaseTimer;
    public final int monsterCount;

    // Shared AI action contract.
    public final double forward, strafe;
    public final boolean sprint, jump, ability;
    public final float yawDelta;

    // Backward-compatible names used by the original trace prototype.
    public final double moveX, moveZ;
    public final boolean holdJump, usePrimary, useEnhanced;

    public final String events;

    public TraceFrame(long tick,
                      double px, double py, double pz,
                      double pvx, double pvy, double pvz,
                      float yaw, double health,
                      int stage, int phaseTimer, int monsterCount,
                      double forward, double strafe,
                      boolean sprint, boolean jump, float yawDelta,
                      boolean ability, String events) {
        this.tick=tick;
        this.px=px; this.py=py; this.pz=pz;
        this.pvx=pvx; this.pvy=pvy; this.pvz=pvz;
        this.yaw=yaw; this.health=health;
        this.stage=stage; this.phaseTimer=phaseTimer; this.monsterCount=monsterCount;
        this.forward=forward; this.strafe=strafe;
        this.sprint=sprint; this.jump=jump; this.yawDelta=yawDelta; this.ability=ability;
        this.moveX=strafe; this.moveZ=forward;
        this.holdJump=jump; this.usePrimary=ability; this.useEnhanced=false;
        this.events=events==null?"":events;
    }

    /** Legacy constructor retained for old CSV/test callers. */
    public TraceFrame(long tick,
                      double px, double py, double pz,
                      double pvx, double pvy, double pvz,
                      float yaw, double health,
                      int stage, int phaseTimer, int monsterCount,
                      double moveX, double moveZ,
                      boolean sprint, boolean jump, boolean holdJump,
                      boolean usePrimary, boolean useEnhanced,
                      String events) {
        this(tick, px, py, pz, pvx, pvy, pvz, yaw, health,
                stage, phaseTimer, monsterCount,
                moveZ, moveX, sprint, jump, 0.0f,
                usePrimary || useEnhanced, events);
    }

    public static String csvHeader() {
        return "tick,px,py,pz,pvx,pvy,pvz,yaw,health,stage,phaseTimer,monsterCount,"
                + "forward,strafe,sprint,jump,yawDelta,ability,events";
    }

    public String toCsv() {
        return tick+","+px+","+py+","+pz+","+pvx+","+pvy+","+pvz+","
                +yaw+","+health+","+stage+","+phaseTimer+","+monsterCount+","
                +forward+","+strafe+","+sprint+","+jump+","+yawDelta+","+ability+","
                +events.replace(',',';');
    }

    public static TraceFrame fromCsv(String line) {
        String[] p=line.split(",",-1);
        if (p.length >= 19) {
            return new TraceFrame(
                    Long.parseLong(p[0]),
                    Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3]),
                    Double.parseDouble(p[4]),Double.parseDouble(p[5]),Double.parseDouble(p[6]),
                    Float.parseFloat(p[7]),Double.parseDouble(p[8]),
                    Integer.parseInt(p[9]),Integer.parseInt(p[10]),Integer.parseInt(p[11]),
                    Double.parseDouble(p[12]),Double.parseDouble(p[13]),
                    Boolean.parseBoolean(p[14]),Boolean.parseBoolean(p[15]),
                    Float.parseFloat(p[16]),Boolean.parseBoolean(p[17]),
                    p.length>18?p[18]:"");
        }
        return new TraceFrame(
                Long.parseLong(p[0]),
                Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3]),
                Double.parseDouble(p[4]),Double.parseDouble(p[5]),Double.parseDouble(p[6]),
                Float.parseFloat(p[7]),Double.parseDouble(p[8]),
                Integer.parseInt(p[9]),Integer.parseInt(p[10]),Integer.parseInt(p[11]),
                Double.parseDouble(p[12]),Double.parseDouble(p[13]),
                Boolean.parseBoolean(p[14]),Boolean.parseBoolean(p[15]),Boolean.parseBoolean(p[16]),
                Boolean.parseBoolean(p[17]),Boolean.parseBoolean(p[18]),
                p.length>19?p[19]:"");
    }
}
