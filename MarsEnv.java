import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;

import java.util.concurrent.ThreadLocalRandom;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;

public class MarsEnv extends Environment {

    public static final int GSize = 7; // grid size
    public static final int GARB = 16; // garbage code in grid model

    public static final Term ns = Literal.parseLiteral("next(slot)");
    public static final Term ns4 = Literal.parseLiteral("next4(slot)");
    public static final Term ns3 = Literal.parseLiteral("nextr3(slot)");
    public static final Term pg = Literal.parseLiteral("pick(garb)");
    public static final Term pg4 = Literal.parseLiteral("pick4(garb)");
    public static final Term dg = Literal.parseLiteral("drop(garb)");
    public static final Term bg = Literal.parseLiteral("burn(garb)");
    public static final Term dg4 = Literal.parseLiteral("drop4(garb)");
    public static final Term rs1 = Literal.parseLiteral("resume(r1)");
    public static final Term rs4 = Literal.parseLiteral("resume4(r4)");
    public static final Literal g1 = Literal.parseLiteral("garbage(r1)");
    public static final Literal g2 = Literal.parseLiteral("garbage(r2)");
    public static final Literal g4 = Literal.parseLiteral("garbage(r4)");

    static Logger logger = Logger.getLogger(MarsEnv.class.getName());

    private MarsModel model;
    private MarsView view;

    @Override
    public void init(String[] args) {
        model = new MarsModel();
        view = new MarsView(model);
        model.setView(view);
        updatePercepts();
    }

    @Override
    public boolean executeAction(String ag, Structure action) {
        logger.info(ag + " doing: " + action);
        int awakening = 0;
        int x = 0;
        int y = 0;
        try {
            if (action.equals(ns)) {
                model.nextSlot();
            } else if (action.equals(ns3)) {
                model.nextSlotR3();
            } else if (action.equals(ns4)) {
                model.nextSlot4();
            } else if (action.getFunctor().equals("move_towards")) {
                x = (int) ((NumberTerm) action.getTerm(0)).solve();
                y = (int) ((NumberTerm) action.getTerm(1)).solve();
                model.moveTowards(x, y);
            } else if (action.getFunctor().equals("awake")) {
                x = (int) ((NumberTerm) action.getTerm(0)).solve();
                y = (int) ((NumberTerm) action.getTerm(1)).solve();
                awakening = 1;
            } else if (action.getFunctor().equals("awake4")) {
                x = (int) ((NumberTerm) action.getTerm(0)).solve();
                y = (int) ((NumberTerm) action.getTerm(1)).solve();
                awakening = 2;
            } else if (action.getFunctor().equals("move_towards4")) {
                x = (int) ((NumberTerm) action.getTerm(0)).solve();
                y = (int) ((NumberTerm) action.getTerm(1)).solve();
                model.moveTowards4(x, y);
            } else if (action.equals(pg)) {
                model.pickGarb();
            } else if (action.equals(pg4)) {
                model.pickGarb4();
            } else if (action.equals(dg)) {
                model.dropGarb();
            } else if (action.equals(dg4)) {
                model.dropGarb4();
            } else if (action.equals(rs1)) {
                model.resume();
            } else if (action.equals(rs4)) {
                model.resume4();
            } else if (action.equals(bg)) {
                model.burnGarb();
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updatePercepts();

        if (awakening == 1) {
            addPercept(Literal.parseLiteral("wakeup(r4," + x + "," + y + ")"));
        } else if (awakening == 2) {
            addPercept(Literal.parseLiteral("wakeup(r1," + x + "," + y + ")"));
        }

        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
        informAgsEnvironmentChanged();
        return true;
    }

    /** creates the agents perception based on the MarsModel */
    void updatePercepts() {
        clearPercepts();

        Location r1Loc = model.getAgPos(0);
        Location r2Loc = model.getAgPos(1);
        Location r3Loc = model.getAgPos(2);
        Location r4Loc = model.getAgPos(3);

        Literal pos1 = Literal.parseLiteral("pos(r1," + r1Loc.x + "," + r1Loc.y + ")");
        Literal pos2 = Literal.parseLiteral("pos(r2," + r2Loc.x + "," + r2Loc.y + ")");
        Literal pos3 = Literal.parseLiteral("pos(r3," + r3Loc.x + "," + r3Loc.y + ")");
        Literal pos4 = Literal.parseLiteral("pos(r4," + r4Loc.x + "," + r4Loc.y + ")");

        addPercept(pos1);
        addPercept(pos2);
        addPercept(pos3);
        addPercept(pos4);

        if (model.hasObject(GARB, r1Loc)) {
            addPercept(g1);
        }
        if (model.hasObject(GARB, r2Loc)) {
            addPercept(g2);
        }
        if (model.hasObject(GARB, r4Loc)) {
            addPercept(g4);
        }

        if (model.getWorkingAgent()) {
            addPercept(Literal.parseLiteral("sleep(r4)"));
        } else {
            addPercept(Literal.parseLiteral("sleep(r1)"));
        }

    }

    class MarsModel extends GridWorldModel {

        public static final int MErr = 20; // max error in pick garb and burn garb
        int nerr; // number of tries of pick garb
        int burnerror; // number of tries to burn trash
        boolean r1HasGarb = false; // whether r1 is carrying garbage or not
        boolean r4HasGarb = false; // whether r4 is carrying garbage or not
        Location CoordinatesBack;
        boolean r1Working = true;

        Random random = new Random(System.currentTimeMillis());

        private MarsModel() {
            super(GSize, GSize, 4);

            // initial location of agents
            try {
                Location r1Loc = new Location(randomNumber(), randomNumber());
                setAgPos(0, r1Loc);

                Location r2Loc = new Location(randomNumber(), randomNumber());
                setAgPos(3, r2Loc);
                setAgPos(1, r2Loc);

                Location r3Loc = new Location(randomNumber(), randomNumber());
                setAgPos(2, r3Loc);

            } catch (Exception e) {
                e.printStackTrace();
            }

            // initial location of garbage
            add(GARB, randomNumber(), randomNumber());
            add(GARB, randomNumber(), randomNumber());
            add(GARB, randomNumber(), randomNumber());
            add(GARB, randomNumber(), randomNumber());
            add(GARB, randomNumber(), randomNumber());
        }

        boolean getWorkingAgent() {
            return r1Working;
        }

        int randomNumber() {
            return ThreadLocalRandom.current().nextInt(0, GSize);
        }

        void nextSlot() throws Exception {
            Location r1 = getAgPos(0);
            r1.y++;
            if (r1.y == getHeight()) {
                r1.y = 0;
                r1.x++;
            }
            // finished searching the whole grid
            if (r1.x == getWidth()) {
                r1.x = 0;
                r1.y = 0;
            }
            setAgPos(0, r1);
            setAgPos(1, getAgPos(1)); // just to draw it in the view
        }

        void nextSlot4() throws Exception {
            Location r4 = getAgPos(3);
            r4.y++;
            if (r4.y == getHeight()) {
                r4.y = 0;
                r4.x++;
            }
            // finished searching the whole grid
            if (r4.x == getWidth()) {
                r4.x = 0;
                r4.y = 0;
            }
            setAgPos(3, r4);
            setAgPos(1, getAgPos(1));
        }

        void nextSlotR3() throws Exception {
            Location r3 = getAgPos(2);
            int row = ThreadLocalRandom.current().nextInt(0, 3);
            int column = ThreadLocalRandom.current().nextInt(0, 3);
            moveChoice(row, column, r3);
            // 20% of droping garbage
            if (ThreadLocalRandom.current().nextInt(0, 5) == 0) {
                add(GARB, r3);
            }
            setAgPos(2, r3);
        }

        // r3 agents move decision
        void moveChoice(int row, int column, Location l) {
            switch (row) {
            case 0:
                l.x--;
                if (l.x < 0)
                    l.x = 1;
                break;
            case 2:
                l.x++;
                if (l.x >= GSize)
                    l.x = GSize - 1;
                break;
            default:
                break;
            }
            switch (column) {
            case 0:
                l.y--;
                if (l.y < 0)
                    l.y = 1;
                break;
            case 2:
                l.y++;
                if (l.y >= GSize)
                    l.y = GSize - 1;
                break;
            default:
                break;
            }
        }

        void moveTowards(int x, int y) throws Exception {
            Location r1 = getAgPos(0);
            if (r1.x < x)
                r1.x++;
            else if (r1.x > x)
                r1.x--;
            if (r1.y < y)
                r1.y++;
            else if (r1.y > y)
                r1.y--;
            setAgPos(0, r1);
            setAgPos(1, getAgPos(1)); // just to draw it in the view
        }

        void moveTowards4(int x, int y) throws Exception {
            Location r4 = getAgPos(3);
            if (r4.x < x)
                r4.x++;
            else if (r4.x > x)
                r4.x--;
            if (r4.y < y)
                r4.y++;
            else if (r4.y > y)
                r4.y--;
            setAgPos(3, r4);
        }

        void pickGarb() {
            // r1 location has garbage
            if (model.hasObject(GARB, getAgPos(0))) {
                // sometimes the "picking" action doesn't work
                // but never more than MErr times
                if (random.nextBoolean() || nerr == MErr) {
                    remove(GARB, getAgPos(0));
                    nerr = 0;
                    r1HasGarb = true;
                } else {
                    nerr++;
                }
            }
        }

        void pickGarb4() {
            // r4 location has garbage
            if (model.hasObject(GARB, getAgPos(3))) {
                // sometimes the "picking" action doesn't work
                // but never more than MErr times
                if (random.nextBoolean() || nerr == MErr) {
                    remove(GARB, getAgPos(3));
                    nerr = 0;
                    r4HasGarb = true;
                } else {
                    nerr++;
                }
            }
        }

        void dropGarb() {
            if (r1HasGarb) {
                r1HasGarb = false;
                add(GARB, getAgPos(0));
            }
        }

        void dropGarb4() {
            if (r4HasGarb) {
                r4HasGarb = false;
                add(GARB, getAgPos(3));
            }
        }

        void resume() {
            // setAgPos(0, CoordinatesBack);
            r1Working = true;
        }

        void resume4() {
            // setAgPos(3, CoordinatesBack);
            r1Working = false;
        }

        void burnGarb() {
            // r2 location has garbage
            if (model.hasObject(GARB, getAgPos(1))) {
                if (random.nextBoolean() || burnerror == MErr) {
                    remove(GARB, getAgPos(1));
                    burnerror = 0;
                } else {
                    burnerror++;
                }
            }
        }
    }

    class MarsView extends GridWorldView {

        public MarsView(MarsModel model) {
            super(model, "Mars World", 600);
            defaultFont = new Font("Arial", Font.BOLD, 18); // change default font
            setVisible(true);
            repaint();
        }

        /** draw application objects */
        @Override
        public void draw(Graphics g, int x, int y, int object) {
            switch (object) {
            case MarsEnv.GARB:
                drawGarb(g, x, y);
                break;
            }
        }

        @Override
        public void drawAgent(Graphics g, int x, int y, Color c, int id) {
            String label = "R" + (id + 1);
            c = Color.blue;
            if (id == 0) {
                c = Color.yellow;
                if (((MarsModel) model).r1HasGarb) {
                    label += " - G";
                    c = Color.orange;
                }
            }
            if (id == 3) {
                c = Color.green;
                if (((MarsModel) model).r4HasGarb) {
                    label += " - G";
                    c = Color.orange;
                }
            }
            if (id == 2) {
                c = Color.red;
            }
            super.drawAgent(g, x, y, c, -1);
            if (id == 0 || id == 3) {
                g.setColor(Color.black);
            } else {
                g.setColor(Color.white);
            }
            super.drawString(g, x, y, defaultFont, label);
            repaint(0);
        }

        public void drawGarb(Graphics g, int x, int y) {
            super.drawObstacle(g, x, y);
            g.setColor(Color.white);
            drawString(g, x, y, defaultFont, "G");
        }

    }
}
