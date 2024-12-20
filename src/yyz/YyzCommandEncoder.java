package yyz;

import VASSAL.counters.Decorator;
import VASSAL.counters.GamePiece;

public class YyzCommandEncoder extends VASSAL.build.module.BasicCommandEncoder{
    public Decorator createDecorator(String type, GamePiece inner){
        if(type.startsWith(AircraftSheet.ID)){
            return new AircraftSheet(type, inner);
        }
        return super.createDecorator(type, inner); // delegate to the builtin encoder
    }
}
