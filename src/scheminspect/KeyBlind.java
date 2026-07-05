package scheminspect;

import arc.input.KeyBind;
import arc.input.KeyCode;

public class KeyBlind{
    public static KeyBind selecting_key;

    public static void load(){
        selecting_key = KeyBind.add("schem_inspect", KeyCode.i, "scheminspect");
    }
}