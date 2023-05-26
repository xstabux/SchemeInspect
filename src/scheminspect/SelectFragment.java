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
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.power.*;

public class SelectFragment{
    static float x1, y1, x2, y2;
    static int tx1, ty1, tx2, ty2;
    static NormalizeResult normalized;
    static boolean selecting = false;

    static Seq<Building> selected = new Seq<>();

    static Table indicator;
    static Table dataTable;
    static InspectData data;
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
                if(!Core.input.keyTap(KeyBlind.selecting_key) && selecting){
                    toggle();
                }
                if(Core.input.keyTap(KeyBlind.selecting_key) && !event.handled){
                    toggle();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(!selecting || button != KeyCode.mouseLeft) return false;
                x1 = x2 = x;
                y1 = y2 = y;

                if(event.handled){
                    toggle();
                    return false;
                }

                Tmp.v1.set(Core.input.mouseWorld(x1, y1));
                tx1 = tx2 = World.toTile(Tmp.v1.x);
                ty1 = ty2 = World.toTile(Tmp.v1.y);

                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                if(!selecting || event.keyCode != KeyCode.mouseLeft) return;

                x2 = x;
                y2 = y;

                Tmp.v2.set(Core.input.mouseWorld(x2, y2));

                int oldtx2 = tx2, oldty2 = ty2;

                tx2 = World.toTile(Tmp.v2.x);
                ty2 = World.toTile(Tmp.v2.y);

                // disgusting..
                if(oldtx2 != tx2 || oldty2 != ty2){
                    UISounds.clickMove.play(0.25f);
                    updateSelection();
                }
            }
        });

        Table table = new Table(t -> {
            t.setFillParent(true);
            t.touchable(() -> selecting ? Touchable.enabled : Touchable.disabled);

            t.bottom();
            t.table(Styles.black5, t1 -> {
                indicator = t1;
                t1.margin(10f);
                t1.table(t2 -> {
                    t2.image(Icon.zoomSmall).size(15f).center().padRight(15f).color(col2);
                    t2.label(() -> Core.bundle.get("scheminspect.inspecting")).grow().center().get().setAlignment(Align.center);
                    t2.image(Icon.zoomSmall).size(15f).center().padLeft(15f).color(col2);
                }).growX();
                t1.row();
                t1.label(() -> "< " + Core.bundle.get("scheminspect.exit") + " >").color(Pal.lightishGray).padTop(5f);

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

        dataTable = new Table(Styles.black5, t1 -> {
            t1.visible(() -> selecting && normalized != null);
            t1.margin(5f);
        });

        indicator.actions(Actions.alpha(0));

        parent.addChildAt(0, table);
        parent.fill(t -> t.add(dataTable).fill());
    }

    public static void toggle(){
        selecting = !selecting;

        if(selecting){
            UISounds.clickOpen.play();
            indicator.actions(
            Actions.moveBy(0, -80f),
            Actions.alpha(1),
            Actions.moveBy(0, 80f, 0.3f, Interp.pow3Out)
            );
        }else{
            UISounds.clickClose.play();
            indicator.actions(
            Actions.moveBy(0, -80f, 0.3f, Interp.pow3In),
            Actions.alpha(0),
            Actions.moveBy(0, 80f)
            );

            selected.clear();
            x1 = y1 = x2 = y2 = 0f;
            normalized = null;
        }
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

        data = inspectSelection(selected);

        Group oldParent = dataTable.parent;
        dataTable.parent = null; // i love arc ui
        dataTable.clearChildren();

        // to avoid edge cases where the label is still shown but normalized is null, causing a crash.
        String header = data.blockCount + " " + Core.bundle.get("scheminspect.buildings") + " [#" + Pal.lightishGray.toString() + "]@" + (normalized.x2 - normalized.x + 1) + "x" + (normalized.y2 - normalized.y + 1);
        dataTable.label(() -> header).growX().top().left().padBottom(5f).get().setAlignment(Align.left);
        dataTable.row();

        dataTable.table(Styles.black5, t -> {
            t.margin(10f);
            t.top().left();
            t.image(Icon.powerSmall).size(30f).color(Pal.power).top().left().padRight(5f);
            t.table(t1 -> {
                t1.label(() -> (data.powerProduction - data.powerConsumption) * 60 + "/s").growX().pad(5f);
                t1.label(() -> "[green]+" + data.powerProduction * 60 + "/s").growX().pad(5f);
                t1.label(() -> "[scarlet]-" + data.powerConsumption * 60 + "/s").growX().pad(5f);
                t1.label(() -> "[accent]" + data.powerCapacity).growX();
            }).fill().top().left().growX();
        }).fill().top().left().growX();

        dataTable.pack();
        dataTable.row();

        dataTable.table(Styles.black5, t -> {
            t.margin(10f);
            t.top().left();

            int l = 0;
            for(ItemStack stack : data.requirements){
                Table itemTable = new Table(t1 -> {
                    t1.image(stack.item.uiIcon).size(15).left().padLeft(5f);
                    t1.label(() -> String.valueOf(stack.amount)).left();
                });
                itemTable.pack();

                if(l + itemTable.getWidth() >= dataTable.getWidth()){
                    t.row();
                    l = 0;
                }
                t.add(itemTable).top().left();
                l += itemTable.getWidth();
            }
        }).fill().top().left().growX().padTop(5f);
        dataTable.row();

        dataTable.parent = oldParent;
        dataTable.pack();

        Tmp.v3.set(Core.input.mouseScreen(normalized.x2 * 8 + 8, normalized.y2 * 8 + 4));
        dataTable.actions(Actions.moveToAligned(Tmp.v3.x, Tmp.v3.y, Align.topLeft, 0.5f, Interp.pow3Out));
    }

    public static InspectData inspectSelection(Seq<Building> buildings){
        InspectData data = new InspectData();
        for(Building b : buildings){
            data.blockCount++;
            data.requirements.add(b.block.requirements);

            if(b.block instanceof PowerBlock p && p.consPower != null){
                data.powerConsumption += p.consPower.usage;
                data.powerCapacity += p.consPower.capacity;
            }
            if(b.block instanceof PowerGenerator p){
                data.powerProduction += p.powerProduction;
            }
        }
        return data;
    }

    static class InspectData{
        public float powerConsumption, powerProduction, powerCapacity;
        public ItemSeq requirements;
        public int blockCount;

        public InspectData(){
            requirements = new ItemSeq();
        }
    }
}
