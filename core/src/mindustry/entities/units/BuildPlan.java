package mindustry.entities.units;

import arc.func.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** Class for storing build requests. Can be either a place or remove request. */
public class BuildPlan implements Position, Pool.Poolable, QuadTree.QuadTreeObject {
    /** Position and rotation of this request. */
    public int x, y, rotation;
    /** Block being placed. If null, this is a breaking request.*/
    public @Nullable Block block;
    /** Whether this is a break request.*/
    public boolean breaking;
    /** Config int. Not used unless hasConfig is true.*/
    public Object config;
    /** Original position, only used in schematics.*/
    public int originalX, originalY, originalWidth, originalHeight;

    /** Last progress.*/
    public float progress;
    /** Whether construction has started for this request, and other special variables.*/
    public boolean initialized, worldContext = true, stuck;

    /** Visual scale. Used only for rendering. */
    public float animScale = 0f;
    
    /** Cache */
    public boolean valid;
    /** Double freeing plans is a bad idea. */
    public boolean freed;

    /** Client: Client-side configs for plans (and subsequently a ConstructBuild then Building).*/
    public @Nullable Cons<Building> clientConfig;

    /** Whether to always prioritise the plan, regardless of ability to be built.*/
    public boolean priority = false;

    @Override
    public void reset() {
        config = null;
        clientConfig = null;
        progress = 0;
        initialized = false;
        stuck = false;
        freed = true;
        priority = false;
    }

    /** This creates a build request. */
    public BuildPlan(int x, int y, int rotation, Block block){
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.block = block;
        this.breaking = false;
    }

    /** This creates a build request with a config. */
    public BuildPlan(int x, int y, int rotation, Block block, Object config){
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.block = block;
        this.breaking = false;
        this.config = config;
    }

    public BuildPlan(int x, int y, int rotation, Block block, Object config, Cons<Building> clientConfig){
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.block = block;
        this.breaking = false;
        this.config = config;
        this.clientConfig = clientConfig;
    }

    /** This creates a remove request. */
    public BuildPlan(int x, int y){
        this.x = x;
        this.y = y;
        this.rotation = -1;
        this.block = world.tile(x, y).block();
        this.breaking = true;
    }

    public BuildPlan(){

    }

    public boolean placeable(Team team){
        return Build.validPlace(block, team, x, y, rotation);
    }

    public boolean isRotation(Team team){
        if(breaking) return false;
        Tile tile = tile();
        return tile != null && tile.team() == team && tile.block() == block && tile.build != null && tile.build.rotation != rotation;
    }

    public boolean samePos(BuildPlan other){
        return x == other.x && y == other.y;
    }

    /** Transforms the internal position of this config using the specified function, and return the result. */
    public static Object pointConfig(Block block, Object config, Cons<Point2> cons){
        if(config instanceof Point2){
            config = ((Point2)config).cpy();
            cons.get((Point2)config);
        }else if(config instanceof Point2[]){
            Point2[] result = new Point2[((Point2[])config).length];
            int i = 0;
            for(Point2 p : (Point2[])config){
                result[i] = p.cpy();
                cons.get(result[i++]);
            }
            config = result;
        }else if(block != null){
            config = block.pointConfig(config, cons);
        }
        return config;
    }

    /** Transforms the internal position of this config using the specified function. */
    public void pointConfig(Cons<Point2> cons){
        this.config = pointConfig(block, this.config, cons);
    }

    public BuildPlan copy(){
        BuildPlan copy = new BuildPlan();
        copy.x = x;
        copy.y = y;
        copy.rotation = rotation;
        copy.block = block;
        copy.breaking = breaking;
        copy.config = config;
        copy.originalX = originalX;
        copy.originalY = originalY;
        copy.progress = progress;
        copy.initialized = initialized;
        copy.animScale = animScale;
        copy.clientConfig = clientConfig;
        return copy;
    }

    public BuildPlan original(int x, int y, int originalWidth, int originalHeight){
        originalX = x;
        originalY = y;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        return this;
    }

    public Rect bounds(Rect rect){
        if(breaking){
            return rect.set(-100f, -100f, 0f, 0f);
        }else{
            return block.bounds(x, y, rect);
        }
    }

    public BuildPlan set(int x, int y, int rotation, Block block){
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.block = block;
        this.breaking = false;
        freed = false;
        return this;
    }

    public BuildPlan set(int x, int y, int rotation, Block block, Object config){
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.block = block;
        this.breaking = false;
        this.config = config;
        freed = false;
        return this;
    }

    public BuildPlan set(int x, int y){
        this.x = x;
        this.y = y;
        this.rotation = -1;
        this.block = world.tile(x, y).block();
        this.breaking = true;
        freed = false;
        return this;
    }

    public float drawx(){
        return x*tilesize + (block == null ? 0 : block.offset);
    }

    public float drawy(){
        return y*tilesize + (block == null ? 0 : block.offset);
    }

    public @Nullable Tile tile(){
        return world.tile(x, y);
    }

    public @Nullable Building build(){
        return world.build(x, y);
    }

    public boolean isDone(){
        Tile tile = world.tile(x, y);
        if(breaking){
            return tile.block() == null || tile.block() == Blocks.air || tile.block() == tile.floor();  // covering all the bases
        }else{
            return tile.block() == block && (tile.build == null || tile.build.rotation == rotation);
        }
    }

    @Override
    public float getX(){
        return drawx();
    }

    @Override
    public float getY(){
        return drawy();
    }

    @Override
    public String toString(){
        return "BuildPlan{" +
        "x=" + x +
        ", y=" + y +
        ", rotation=" + rotation +
        ", block=" + block +
        ", breaking=" + breaking +
        ", progress=" + progress +
        ", initialized=" + initialized +
        ", config=" + config +
        '}';
    }

    @Override
    public void hitbox(Rect out) {
        bounds(out);
    }
}
