package yyz;

import VASSAL.build.GameModule;
import VASSAL.build.module.Chatter;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.command.ChangePiece;
import VASSAL.command.ChangeTracker;
import VASSAL.command.Command;
import VASSAL.command.RemovePiece;
import VASSAL.configure.IntConfigurer;
import VASSAL.counters.Decorator;
import VASSAL.counters.GamePiece;
import VASSAL.counters.KeyCommand;
import VASSAL.counters.PieceFinder;
import VASSAL.tools.SequenceEncoder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class AircraftSheet extends Decorator implements MouseListener {
    // ID is used as the serialization identification head.
    // ";" is introduced to remove ambiguity in a "startswith" based custom Command Encoder, you can use other format if you really know what you are doing.
    public static final String ID = "AircraftSheet;";

    // Used to handle `clone = (GamePiece)piece.getClass().getConstructor().newInstance()` like inner logic.
    public AircraftSheet(){
        this(ID, null); //
    }

    // Used by the custom encoder
    public AircraftSheet(String type, GamePiece p){
        setInner(p);
        mySetType(type);
    }

    // "State" is the state in a running game
    @Override
    public void mySetState(String s) {
        var sd = new SequenceEncoder.Decoder(s, ';');

        mediumDetectionRange = sd.nextInt(0);
        var numWaypoints = sd.nextInt(0);
        waypoints.clear();
        for(var i=0; i<numWaypoints; i++){
            var x = sd.nextInt(0);
            var y = sd.nextInt(0);
            waypoints.add(new Point(x, y));
        }
    }

    @Override
    public String myGetState() {
        var se = new SequenceEncoder(';');

        se.append(mediumDetectionRange);

        se.append(waypoints.size());
        for (Point waypoint : waypoints) {
            se.append(waypoint.x);
            se.append(waypoint.y);
        }

        return se.getValue();
    }

    // "Type" is the state specified in the editor and frozen in a game session.
    @Override
    public String myGetType() {
        var se = new SequenceEncoder(';');
        return ID + se.getValue();
    }

    @Override
    public void mySetType(String s) {
        var sd = new SequenceEncoder.Decoder(s, ';');
        sd.nextToken(); // Drop head;
    }

    private KeyCommand[] commands;
    private final KeyStroke openCommand = KeyStroke.getKeyStroke(KeyEvent.VK_A, 0);
    private final KeyStroke fireCommand = KeyStroke.getKeyStroke(KeyEvent.VK_F, 0);
    private final KeyStroke waypointCommand = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
    private final KeyStroke concludeMouseCommand = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    @Override
    protected KeyCommand[] myGetKeyCommands() {
        if(commands == null){
            commands = new KeyCommand[]{
                    new KeyCommand("Open Aircraft Sheet", openCommand, this),
                    new KeyCommand("Fire", fireCommand, this),
                    new KeyCommand("Plot Waypoint", waypointCommand, this),
                    new KeyCommand("Conclude Plot Waypoint", concludeMouseCommand, this)
            };
        }
        return commands;
    }

    JDialog frame;
    IntConfigurer mediumDetectionRangeConfigurer;

    int mediumDetectionRange;

    String oldState;

    public void handleOpen(){
        if(frame == null){
            var mod = GameModule.getGameModule();
            var win = mod.getPlayerWindow();

            frame = new JDialog(win, false);
            mediumDetectionRangeConfigurer = new IntConfigurer(null, "Medium", 0);

            var vBox = Box.createVerticalBox();
            vBox.add(mediumDetectionRangeConfigurer.getControls());
            frame.add(vBox);
            frame.setLocationRelativeTo(win);
            frame.pack();

            frame.addWindowListener(new WindowAdapter(){
                @Override
                public void windowClosing(WindowEvent evt){
                    mediumDetectionRange = mediumDetectionRangeConfigurer.getIntValue(0);

                    var outermost = getOutermost(AircraftSheet.this);
                    var newState = outermost.getState();
                    if(!oldState.equals(newState)){
                        var mod = GameModule.getGameModule();
                        var command = new Chatter.DisplayText(mod.getChatter(), "Change Piece");
                        command.execute();
                        command.append(new ChangePiece(outermost.getId(), oldState, newState));
                        mod.sendAndLog(command);

                        AircraftSheet.this.getMap().repaint(); // Force-repaint when closing
                    }
                }

                @Override
                public void windowActivated(WindowEvent evt){
                    var outermost = getOutermost(AircraftSheet.this);
                    oldState = outermost.getState();

                    mediumDetectionRangeConfigurer.setValue(mediumDetectionRange);
                }
            });
        }
        frame.setTitle(getName());
        frame.setVisible(true);
    }

    enum MouseMode{
        FIRING,
        WAYPOINTPLOTTING
    }

    MouseMode mouseMode;

    @Override
    public Command myKeyEvent(KeyStroke keyStroke) {
        myGetKeyCommands();
        if(keyStroke.equals(openCommand)){
            // JOptionPane.showMessageDialog(null, "Hello custom code for Vassal");
            handleOpen();
        }else if(keyStroke.equals(fireCommand)){
            if(mouseMode == null){ // Permit only one "queued" firing target selection.
                mouseMode = MouseMode.FIRING;
                getMap().pushMouseListener(this); // replace default handler with AircraftSheet
            }
        }else if(keyStroke.equals(waypointCommand)){
            if(mouseMode == null){
                mouseMode = MouseMode.WAYPOINTPLOTTING;
                getMap().pushMouseListener(this);

                // waypoints.clear();
                tempWaypoints.clear();
            }
        }else if(keyStroke.equals(concludeMouseCommand)){
            getMap().popMouseListener();
            if(mouseMode == MouseMode.FIRING){ // cancel firing
                mouseMode = null;
            }else if(mouseMode == MouseMode.WAYPOINTPLOTTING){ // commit temp waypoints
                var changeTracker = new ChangeTracker(getOutermost(this));

                waypoints.clear();
                waypoints.addAll(tempWaypoints);
                tempWaypoints.clear();
                mouseMode = null;

                var changeCommand = changeTracker.getChangeCommand();
                var mod = GameModule.getGameModule();
                var c = new Chatter.DisplayText(mod.getChatter(), "Set Waypoint");
                c.execute();
                c.append(changeCommand);
                mod.sendAndLog(c);
            }
        }

        return null;
    }

    @Override
    public String getDescription() {
        return "AircraftSheet";
    }

    @Override
    public HelpFile getHelpFile() {
        return null;
    }

    @Override
    public void draw(Graphics g, int x, int y, Component obs, double zoom){
        piece.draw(g, x, y, obs, zoom); // delegate to the inner piece

        if(mediumDetectionRange > 0){
            var g2d = (Graphics2D)g;
            g2d.setColor(Color.BLUE);

            var r = mediumDetectionRange;
            var xs = x - r / 2;
            var ys = y - r / 2;

            g2d.drawOval(xs, ys, r, r);
        }

        if(isSelected()){
            if(!waypoints.isEmpty()){
                drawWaypoints(g, waypoints, new Color(0,0,0));
            }
            if(!tempWaypoints.isEmpty()){
                drawWaypoints(g, tempWaypoints, new Color(100, 100, 100));
            }
        }
    }

    void drawWaypoints(Graphics g, ArrayList<Point> waypoints, Color color){
        // TODO: Move to static or utilities
        // https://stackoverflow.com/questions/9771924/animating-dashed-line-with-java-awt-basicstroke
        float dash[] = {5.0f,5.0f};
        BasicStroke dashedStroke = new BasicStroke(
                3f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_MITER,
                1.5f, //miter limit
                dash,
                0
        );
        final Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(dashedStroke);

        g2d.setColor(color);

        final double os_scale = g2d.getDeviceConfiguration().getDefaultTransform().getScaleX();
        var map = piece.getMap();

        var p0 = piece.getPosition();
        var pp = p0;
        for(var p: waypoints) {
            var ppd = map.mapToDrawing(pp, os_scale);
            var pd = map.mapToDrawing(p, os_scale);
            g.drawLine(ppd.x, ppd.y, pd.x, pd.y);
            pp = p;
        }
    }

    @Override
    public Rectangle boundingBox() {
        return piece.boundingBox(); // delegate to the inner piece
    }

    @Override
    public Shape getShape() {
        return piece.getShape(); // delegate to the inner piece
    }

    @Override
    public String getName() {
        return piece.getName(); // delegate to the inner piece
    }

    static Random rand = new Random();

    @Override
    public void mouseClicked(MouseEvent e) {
//        switch(mouseMode){
//            case FIRING:
//                handleFire(e);
//                break;
//            case WAYPOINTPLOTTING:
//                handleWaypointPlotting(e);
//                break;
//        }
    }

    void handleFire(MouseEvent e){
        getMap().popMouseListener(this); // restore default behaviour
        mouseMode = null;

        var mapPos = e.getPoint(); // Position in the map coordinate
        // JOptionPane.showMessageDialog(null, mapPos);
        var piece = getMap().findPiece(e.getPoint(), PieceFinder.PIECE_IN_STACK);
        if(piece != null){
            var roll = rand.nextDouble();
            var hit = roll <= 0.5;

            var mod = GameModule.getGameModule();
            var c = new Chatter.DisplayText(mod.getChatter(), String.format("Firing Resolution: %f => %b", roll, hit));

            if(hit){
                c.append(new RemovePiece(piece));
            }
            c.execute();
            mod.sendAndLog(c);
        }
    }

    ArrayList<Point> waypoints = new ArrayList<Point>();
    ArrayList<Point> tempWaypoints = new ArrayList<Point>();

    void handleWaypointPlotting(MouseEvent e){
        tempWaypoints.add(e.getPoint());
        getMap().repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        switch(mouseMode){
            case FIRING:
                handleFire(e);
                break;
            case WAYPOINTPLOTTING:
                handleWaypointPlotting(e);
                break;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
