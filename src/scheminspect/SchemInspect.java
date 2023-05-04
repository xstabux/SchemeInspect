package scheminspect;

import arc.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

public class SchemInspect extends Mod{

    public SchemInspect(){
        Events.on(ClientLoadEvent.class, e -> {
            SelectFragment.build(Vars.ui.hudGroup);
        });

        Events.on(FileTreeInitEvent.class, h -> UISounds.load());
    }

}
