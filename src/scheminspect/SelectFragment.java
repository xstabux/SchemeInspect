package scheminspect;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.input.Placement.*;
import mindustry.ui.*;
import mindustry.world.blocks.ConstructBlock.*;

public class SelectFragment{
    static float x1, y1, x2, y2;
    static int tx1, ty1, tx2, ty2;
    static NormalizeResult normalized;
    static boolean selecting = false;

    static Seq<Building> selected = new Seq<>();

    static Table indicator;
    static Color col1 = Color.valueOf("2578b8"), col2 = Color.valueOf("75edff");

    static {
        Events.on(WorldLoadEvent.class, e -> {
            selected.clear();
            x1 = y1 = x2 = y2 = 0f;
            normalized = null;
            selecting = false;
        });
    }

    public static void build(Group parent){
        Core.scene.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode){
                if(keycode == KeyCode.i){
                    selecting = !selecting;
                    if(!selecting){
                        selected.clear();
                        x1 = y1 = x2 = y2 = 0f;
                        normalized = null;
                    }

                    if(selecting){
                        indicator.actions(
                            Actions.moveBy(0, -40f),
                            Actions.alpha(1),
                            Actions.moveBy(0, 40f, 0.3f, Interp.pow3Out)
                        );
                    }else{
                        indicator.actions(
                            Actions.moveBy(0, -40f, 0.3f, Interp.pow3In),
                            Actions.alpha(0),
                            Actions.moveBy(0, 40f)
                        );
                    }

                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(!selecting) return false; // sink input?
                x1 = x2 = x;
                y1 = y2 = y;

                Tmp.v1.set(Core.input.mouseWorld(x1, y1));
                tx1 = tx2 = World.toTile(Tmp.v1.x);
                ty1 = ty2 = World.toTile(Tmp.v1.y);

                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                if(!selecting) return;
                x2 = x;
                y2 = y;

                Tmp.v2.set(Core.input.mouseWorld(x2, y2));
                tx2 = World.toTile(Tmp.v2.x);
                ty2 = World.toTile(Tmp.v2.y);

                updateSelection();
            }
        });

        Table table = new Table(t -> {
            t.setFillParent(true);
            t.touchable(() -> selecting ? Touchable.enabled : Touchable.disabled);

            t.table(Styles.black5, t1 -> {
                t1.update(() -> {
                    if(normalized == null) return;
                    Tmp.v1.set(Core.input.mouseScreen(normalized.x2 * 8 + 8, normalized.y2 * 8 + 4));
                    t1.setPosition(Tmp.v1.x, Tmp.v1.y, Align.topLeft);
                });
                t1.visible(() -> selecting && normalized != null);

                t1.margin(5f);
                // TODO: Input & Output calculation

            }).fill().top().left();

            t.bottom();
            t.table(Styles.black5, t1 -> {
                indicator = t1;
                t1.margin(10f);
                t1.image(Icon.zoomSmall).size(15f).center().padRight(15f).color(col2);
                t1.label(() -> "Inspecting").grow().center().get().setAlignment(Align.center);

                t1.setTransform(true);
            }).fill().bottom();
        }){
            @Override
            public void draw(){
                if(selecting && normalized != null){
                    Tmp.m1.set(Draw.proj());
                    float z = Draw.z();

                    Draw.proj(Core.camera);
                    Draw.z(Layer.plans);

                    Lines.stroke(2f);
                    Draw.color(col1);
                    Lines.rect(normalized.x * 8 - 4f, (normalized.y * 8) - 5f, (normalized.x2 - normalized.x) * 8 + 8f, (normalized.y2 - normalized.y) * 8 + 8f);
                    Draw.color(col2);
                    Lines.rect(normalized.x * 8 - 4f, normalized.y * 8 - 4f, (normalized.x2 - normalized.x) * 8 + 8f, (normalized.y2 - normalized.y) * 8 + 8f);

                    Draw.proj(Tmp.m1);
                    Draw.z(z);
                }
                super.draw();
            }
        };
        table.setFillParent(true);
        table.pack();

        indicator.actions(Actions.alpha(0));

        parent.addChildAt(0, table);
    }

    public static void updateSelection(){
        NormalizeResult result = Placement.normalizeArea(tx1, ty1, tx2, ty2, 0, false, Vars.maxSchematicSize * 2);
        normalized = result;

        selected.clear();
        for(int cx = result.x; cx <= result.x2; cx++){
            for(int cy = result.y; cy <= result.y2; cy++){
                Building b = Vars.world.build(cx, cy);
                if(b == null || !b.interactable(Vars.player.team()) || b instanceof ConstructBuild || selected.contains(b)) continue;
                selected.add(b);
            }
        }
    }
}
