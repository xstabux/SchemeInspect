package scheminspect;

import arc.KeyBinds.KeyBind;
import arc.KeyBinds.KeybindValue;
import arc.input.InputDevice.DeviceType;
import arc.input.KeyCode;
import mindustry.input.Binding;
import mindustry.ui.dialogs.KeybindDialog;

import static arc.Core.keybinds;
import static arc.Core.settings;
import static mindustry.Vars.ui;

public enum KeyBlind implements KeyBind {
    selecting_key(KeyCode.i,"schem_inspect");

    private final KeybindValue defaultValue;
    private final String category;

    KeyBlind(KeybindValue defaultValue, String category) {
        this.defaultValue = defaultValue;
        this.category = category;
    }

    @Override
    public KeybindValue defaultValue(DeviceType type) {
        return defaultValue;
    }

    @Override
    public String category() {
        return category;
    }

    public static void load() {
        KeyBind[] orign = Binding.values();
        KeyBind[] moded = values();
        KeyBind[] binds = new KeyBind[orign.length + moded.length];

        System.arraycopy(orign, 0, binds, 0, orign.length);
        System.arraycopy(moded, 0, binds, orign.length, moded.length);

        keybinds.setDefaults(binds);
        settings.load(); // update controls
        ui.controls = new KeybindDialog();
    }
}
