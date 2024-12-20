package yyz;

import VASSAL.build.module.documentation.HelpFile;
import VASSAL.command.Command;
import VASSAL.counters.Decorator;
import VASSAL.counters.GamePiece;
import VASSAL.counters.KeyCommand;
import VASSAL.tools.SequenceEncoder;

import javax.swing.*;
import java.awt.*;

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

    }

    @Override
    public String myGetState() {
        return "";
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

    @Override
    protected KeyCommand[] myGetKeyCommands() {
        return KeyCommand.NONE;
    }

    @Override
    public Command myKeyEvent(KeyStroke keyStroke) {
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
