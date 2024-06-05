package CyberSquad;

import robocode.*;
import java.awt.Color;
import java.awt.geom.Point2D;
import robocode.util.Utils;
import java.io.*;

public class Manivela extends AdvancedRobot {

    private static final double MIN_FIRE_POWER = 0.8;
    private static final double MAX_FIRE_DISTANCE = 200;
    private static final double FIRE_POWER_DECAY = 0.02;
    private static final double RADAR_TURN_ANGLE = 30;
    private static final double GUN_TURN_ANGLE = 10;


    double larguraArena;
    double alturaArena;
    boolean movendoParaFrente = true;
    ScannedRobotEvent alvo = null;
    boolean bloqueandoRadar = false;
    double oldEnemyHeading = 0;
    double amplitude = 50; // A amplitude do movimento oscilatório.
    double frequencia = 0.01; // A frequência do movimento oscilatório.
    double fase = 0; // A fase do movimento oscilatório.

// para imprimir
    private PrintWriter writer;
    private boolean ganhouBatalha = false;

//configuração
    public void configurarRobo() {
        setColors(Color.red, Color.blue, Color.pink);
        larguraArena = getBattleFieldWidth();
        alturaArena = getBattleFieldHeight();
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        setTurnRadarRight(360);

        try { 
            RobocodeFileOutputStream onScannerEventFile = new RobocodeFileOutputStream(getDataFile("coletandodados_data_onScannedRobot.csv"));
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(onScannerEventFile)));
            writer.println("Time;Event;Name;MyEnergie;MyGunHeat;MyGunHeading;MyHeading;MyRadarHeading;MyVelocity;EnemyName;Energy;Distance;Bearing;Heading;Velocity;Rank");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        configurarRobo();
        while (true) {
            movimentoOscilatorio();
            rastrearAlvo();
            atirarNoAlvo();
            execute();
        }
    }

    // Método de movimento oscilatório
    public void movimentoOscilatorio() {
        double distancia = amplitude * Math.sin(frequencia * getTime() + fase);
        if (distancia > 0) {
            setAhead(distancia);
        } else {
            setBack(-distancia);
        }
        setTurnRight(5);
    }

    public void movimentoInteligente() {
        if (estaPertoDaParede() || estaPertoDeUmRobo()) {
            if (!estaPresoNaParede()) {
                movendoParaFrente = !movendoParaFrente;
            }
        }

        if (movendoParaFrente) {
            setAhead(100);
        } else {
            setBack(100);
        }

        if (alvo != null) {
            double angleToTarget = normalizeAngle(getHeading() - getGunHeading() + alvo.getBearing());
            setTurnRight(normalizeAngle(angleToTarget));
        }
    }

    public boolean estaPertoDeUmRobo() {
        return alvo != null && alvo.getDistance() < 100;
    }

    public void rastrearAlvo() {
        if (alvo != null) {
            double radarTurn = normalizeAngle(getHeading() + alvo.getBearing() - getRadarHeading());
            setTurnRadarRight(radarTurn);
            bloqueandoRadar = true;
        } else {
            setTurnRadarRight(RADAR_TURN_ANGLE);
        }
    }

    public double normalizeAngle(double angle) {
        while (angle > 180) {
            angle -= 360;
        }
        while (angle < -180) {
            angle += 360;
        }
        return angle;
    }

    public boolean estaPertoDaParede() {
        double x = getX();
        double y = getY();
        double angle = getHeading();
        double aheadDistance = 50;

        if (Math.abs(angle) < RADAR_TURN_ANGLE) {
            if (Math.abs(x - larguraArena / 2) < aheadDistance || Math.abs(y - alturaArena / 2) < aheadDistance) {
                return true;
            }
        } else if (Math.abs(angle) < RADAR_TURN_ANGLE * 2) {
            if (x <= 0 || y <= 0 || x >= larguraArena || y >= alturaArena) {
                return true;
            }
        }

        return false;
    }

    public boolean estaPresoNaParede() {
        double x = getX();
        double y = getY();
        double angle = getHeading();
        double aheadDistance = 50;

        if (Math.abs(angle) < RADAR_TURN_ANGLE) {
            if (Math.abs(x - larguraArena / 2) < aheadDistance && Math.abs(y - alturaArena / 2) < aheadDistance) {
                return true;
            }
        } else if (Math.abs(angle) < RADAR_TURN_ANGLE * 2) {
            if (x <= 0 || y <= 0 || x >= larguraArena || y >= alturaArena) {
                return true;
            }
        }

        if (x < 10 || x > larguraArena - 10 || y < 10 || y > alturaArena - 10) {
            double leftDistance = Math.min(Math.abs(x - 10), Math.abs(x - larguraArena + 10));
            double rightDistance = Math.min(Math.abs(y - 10), Math.abs(y - alturaArena + 10));
            if (leftDistance < aheadDistance && rightDistance < aheadDistance) {
                return true;
            }
        }

        return false;
    }

    public void atirarNoAlvo() {
        if (alvo != null) {
            double gunTurn = normalizeAngle(getHeading() - getGunHeading() + alvo.getBearing());
            setTurnGunRight(gunTurn);

            if (Math.abs(gunTurn) < GUN_TURN_ANGLE) {
                double firePower = Math.max(MIN_FIRE_POWER, getEnergy() * 0.1);
                double bulletSpeed = 20 - 3 * firePower;
                setFire(firePower);
                if (getEnergy() <= 0) {
                    setTurnGunRight(Double.POSITIVE_INFINITY);
                }
            }
        }
    }

public void logDataScanner(String eventType, String enemyName, double energy, double distance, double bearing, double heading, double velocity) {
    if (writer != null) {
        // usar o . como ,
        String energyStr = String.format("%.2f", energy);
        String gunHeatStr = String.format("%.2f", getGunHeat());
        String gunHeadingStr = String.format("%.2f", getGunHeading());
        String headingStr = String.format("%.2f", getHeading());
        String velocityStr = String.format("%.2f", velocity);

        // Converter win status para 1 (win) ou 0 (loss)
        int rank = ganhouBatalha ? 1 : 0;

        // usar ; como separaador
        writer.printf("%d;%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%s;%s;%s;%s;%s;%s;%d%n",
                getTime(), eventType, getName(), energy, getGunHeat(), getGunHeading(), getHeading(), getRadarHeading(), getVelocity(),
                enemyName, energyStr, gunHeatStr, gunHeadingStr, headingStr, velocityStr, rank);
        writer.flush();
    }
}




@Override
public void onScannedRobot(ScannedRobotEvent e) {
    alvo = e;

    logDataScanner("ScannedRobot", e.getName(), e.getEnergy(), e.getDistance(), e.getBearing(), e.getHeading(), e.getVelocity());

}

@Override
public void onHitRobot(HitRobotEvent e) {
    if (alvo != null && e.getName().equals(alvo.getName())) {
        double angleToTarget = normalizeAngle(getHeading() - getGunHeading() + alvo.getBearing());
        double newGunTurn = normalizeAngle(getGunHeading() - angleToTarget);
        setTurnGunRight(newGunTurn);
    } else {
        movendoParaFrente = !movendoParaFrente;
    }
}

@Override
public void onHitWall(HitWallEvent e) {
    movendoParaFrente = !movendoParaFrente;
}

@Override
public void onStatus(StatusEvent e) {
    if (bloqueandoRadar && getRadarTurnRemaining() == 0) {
        alvo = null;
    }
}

@Override
public void onWin(WinEvent event) {
    ganhouBatalha = true;
    System.out.println("Ganhou a batalha!");
    if (writer != null) {
        writer.close();
        writer = null;
    }
    alvo = null; 
}

@Override
public void onDeath(DeathEvent event) {
    if (writer != null) {
        writer.close();
        writer = null;
    }
    ganhouBatalha = false;
}
}