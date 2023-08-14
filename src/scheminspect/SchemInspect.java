package scheminspect;

import arc.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

import static mindustry.Vars.*;

public class SchemInspect extends Mod{

    public SchemInspect(){
        Events.on(ClientLoadEvent.class, e -> {
            if(!mobile){
                SelectFragment.build(Vars.ui.hudGroup);
                KeyBlind.load();
            }else{

            }
        });

        Events.on(FileTreeInitEvent.class, h -> UISounds.load());
    }

}
