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

    /** Amount of garbage to add to the environment */
    public static final int GARB_AMOUNT = 5;

    enum SearchType {
        LEFT_RIGHT, TOP_DOWN, ZIG_ZAG_LEFT_RIGHT, ZIG_ZAG_TOP_DOWN
    }

    /** Type of search that will follow the agent */
    public static SearchType SEARCH_TYPE = SearchType.ZIG_ZAG_TOP_DOWN;

    public static final Term ns = Literal.parseLiteral("next(slot)");
    public static final Term ns3 = Literal.parseLiteral("nextr3(slot)");
    public static final Term pg = Literal.parseLiteral("pick(garb)");
    public static final Term dg = Literal.parseLiteral("drop(garb)");
    public static final Term bg = Literal.parseLiteral("burn(garb)");
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
        try {
            if (action.equals(ns)) {
                model.nextSlot();
            } else if (action.equals(ns3)) {
                model.nextSlotR3();
            } else if (action.getFunctor().equals("move_towards")) {
                int x = (int) ((NumberTerm) action.getTerm(0)).solve();
                int y = (int) ((NumberTerm) action.getTerm(1)).solve();
                model.moveTowards(x, y);
            } else if (action.equals(pg)) {
                model.pickGarb();
            } else if (action.equals(dg)) {
                model.dropGarb();
            } else if (action.equals(bg)) {
                model.burnGarb();
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updatePercepts();

        try {
            Thread.sleep(200);
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
    }

    class MarsModel extends GridWorldModel {

        public static final int MErr = 3; // max error in pick garb and burn garb
        int nerr; // number of tries of pick garb
        int burnError; // number of tries to burn trash
        boolean r1HasGarb = false; // whether r1 is carrying garbage or not

        /** Number of tries of burning garb */
        int nBurnErr = 0;

        boolean reverse = false;

        Random random = new Random(System.currentTimeMillis());

        private MarsModel() {
            super(GSize, GSize, 4);

            // initial location of agents
            try {

                int x = randomNumber(0, GSize - 1);
                int y = randomNumber(0, GSize - 1);
                Location r1Loc = new Location(x, y);
                setAgPos(0, r1Loc);

                x = randomNumber(0, GSize - 1);
                y = randomNumber(0, GSize - 1);
                Location r2Loc = new Location(x, y);
                setAgPos(1, r2Loc);

                x = randomNumber(0, GSize - 1);
                y = randomNumber(0, GSize - 1);
                Location r3Loc = new Location(x, y);
                setAgPos(2, r3Loc);

                setAgPos(3, r2Loc);

            } catch (Exception e) {
                e.printStackTrace();
            }

            // Initial location of garbage
            initGarb(GARB_AMOUNT);
        }

        void initGarb(int num) {

            int i = 0;
            while (i < num) {

                int x = randomNumber(0, GSize - 1);
                int y = randomNumber(0, GSize - 1);

                if (!hasObject(GARB, x, y)) {
                    add(GARB, x, y);
                    i++;
                }

            }

        }

        int randomNumber(int min, int max) {
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }

        void nextSlot() throws Exception {

            switch (SEARCH_TYPE) {
            case TOP_DOWN:
                leftRightSearch();
                break;
            case LEFT_RIGHT:
                topDownSearch();
                break;
            case ZIG_ZAG_LEFT_RIGHT:
                zigZagLeftRightSearch();
                break;
            case ZIG_ZAG_TOP_DOWN:
            default:
                zigZagTopDownSearch();
                break;
            }

        }

        void nextSlotR3() throws Exception {

            Location r3 = getAgPos(2);
            int row = randomNumber(0, 3);
            int column = randomNumber(0, 3);

            moveChoice(row, column, r3);
            // 25% of droping garbage
            if (randomNumber(0, 10) == 0) {
                add(GARB, r3);
            }

            updateAgentPos(2, r3);

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

        void leftRightSearch() {

            Location pos = getAgPos(0);
            pos.x++;

            if (pos.x == getWidth()) {
                pos.x = 0;
                pos.y++;
            }

            // Finished searching the whole grid
            if (pos.y == getHeight()) {
                pos.y = 0;
            }

            updateAgentPos(0, pos);

        }

        void topDownSearch() {

            Location pos = getAgPos(0);
            pos.y++;

            if (pos.y == getHeight()) {
                pos.y = 0;
                pos.x++;
            }

            if (pos.x == getWidth()) {
                pos.x = 0;
            }

            updateAgentPos(0, pos);

        }

        void zigZagLeftRightSearch() {

            Location pos = getAgPos(0);

            if (reverse) {
                // Check if it reached the extreme
                if (pos.x == 0) {
                    reverse = false;
                    pos.y++;
                } else {
                    pos.x--;
                }
            } else {
                // Check if it reached the extreme
                if (pos.x == getWidth() - 1) {
                    reverse = true;
                    pos.y++;
                } else {
                    pos.x++;
                }
            }

            if (pos.y == getHeight()) {
                pos.x = 0;
                pos.y = 0;
                reverse = false;
            }

            updateAgentPos(0, pos);

        }

        void zigZagTopDownSearch() {

            Location pos = getAgPos(0);

            if (reverse) {
                // Check if it reached the extreme
                if (pos.y == 0) {
                    reverse = false;
                    pos.x++;
                } else {
                    pos.y--;
                }
            } else {
                // Check if it reached the extreme
                if (pos.y == getHeight() - 1) {
                    reverse = true;
                    pos.x++;
                } else {
                    pos.y++;
                }
            }

            if (pos.x == getHeight()) {
                pos.x = 0;
                pos.y = 0;
                reverse = false;
            }

            updateAgentPos(0, pos);

        }

        void moveTowards(int x, int y) throws Exception {

            Location pos = getAgPos(3);

            if (pos.x < x) {
                pos.x++;
            } else if (pos.x > x) {
                pos.x--;
            }

            if (pos.y < y) {
                pos.y++;
            } else if (pos.y > y) {
                pos.y--;
            }

            updateAgentPos(3, pos);

        }

        void pickGarb() {
            // r3 location has garbage
            if (getAgPos(1) != getAgPos(3) && model.hasObject(GARB, getAgPos(3))) {
                // sometimes the "picking" action doesn't work
                // but never more than MErr times
                if (random.nextBoolean() || nerr == MErr) {
                    remove(GARB, getAgPos(3));
                    nerr = 0;
                    r1HasGarb = true;
                } else {
                    nerr++;
                }
            }
        }

        void dropGarb() {
            if (r1HasGarb) {
                r1HasGarb = false;
                add(GARB, getAgPos(3));
            }
        }

        void burnGarb() {
            // r2 location has garbage
            if (model.hasObject(GARB, getAgPos(1))) {
                if (random.nextBoolean() || burnError == MErr) {
                    remove(GARB, getAgPos(1));
                    burnError = 0;
                } else {
                    burnError++;
                }
            }
        }

        private void updateAgentPos(int ag, Location pos) {

            int numAg = getNbOfAgs();
            for (int i = 0; i < numAg; i++) {

                if (i == ag) {
                    setAgPos(ag, pos);
                } else {
                    setAgPos(i, getAgPos(i));
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
                // if (((MarsModel) model).r1HasGarb) {
                //     label += " - G";
                //     c = Color.orange;
                // }
            }
            if (id == 3) {
                c = Color.yellow;
                if (((MarsModel) model).r1HasGarb) {
                    label += " - G";
                    c = Color.orange;
                }
            }
            if (id == 2) {
                c = Color.red;
            }
            super.drawAgent(g, x, y, c, -1);
            if (id == 0) {
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
