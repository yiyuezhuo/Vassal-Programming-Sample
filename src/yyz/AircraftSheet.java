package yyz;

import VASSAL.build.GameModule;
import VASSAL.build.module.Chatter;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.command.ChangePiece;
import VASSAL.command.Command;
import VASSAL.configure.IntConfigurer;
import VASSAL.counters.Decorator;
import VASSAL.counters.GamePiece;
import VASSAL.counters.KeyCommand;
import VASSAL.tools.SequenceEncoder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AircraftSheet extends Decorator {
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
    }

    @Override
    public String myGetState() {
        var se = new SequenceEncoder(';');

        se.append(mediumDetectionRange);
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

    @Override
    protected KeyCommand[] myGetKeyCommands() {
        if(commands == null){
            commands = new KeyCommand[]{
                new KeyCommand("Open Aircraft Sheet", openCommand, this)
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

    @Override
    public Command myKeyEvent(KeyStroke keyStroke) {
        myGetKeyCommands();
        if(keyStroke.equals(openCommand)){
            // JOptionPane.showMessageDialog(null, "Hello custom code for Vassal");
            handleOpen();
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
}
