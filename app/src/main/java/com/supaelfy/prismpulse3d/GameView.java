package com.supaelfy.prismpulse3d;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameView extends View {
    private enum Screen { HOME, LEVELS, GAME }
    private static final int LEVEL_COUNT = 50;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SharedPreferences prefs;
    private final RectF play = new RectF(), choose = new RectF(), back = new RectF();
    private final RectF undo = new RectF(), reset = new RectF(), hint = new RectF();
    private final RectF[] levelRects = new RectF[LEVEL_COUNT];
    private final ArrayDeque<Integer> history = new ArrayDeque<>();
    private final ScaleGestureDetector scaleDetector;
    private ToneGenerator tones;
    private Screen screen = Screen.HOME;
    private Board board;
    private int level = 1, unlocked, moves, par;
    private float yaw = -0.65f, pitch = -0.45f, zoom = 1f;
    private float downX, downY, lastX, lastY;
    private boolean dragged;

    public GameView(Context c) {
        super(c);
        prefs = c.getSharedPreferences("pp3d_progress", Context.MODE_PRIVATE);
        unlocked = Math.max(1, Math.min(LEVEL_COUNT, prefs.getInt("unlocked", 1)));
        for (int i=0;i<LEVEL_COUNT;i++) levelRects[i] = new RectF();
        text.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        try { tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 30); } catch (Throwable t) { tones = null; }
        scaleDetector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector d) {
                if (screen != Screen.GAME) return false;
                zoom = clamp(zoom * d.getScaleFactor(), .65f, 1.7f); invalidate(); return true;
            }
        });
        setBackgroundColor(Color.rgb(7,10,19));
    }

    @Override protected void onDetachedFromWindow() {
        if (tones != null) { try { tones.release(); } catch(Throwable ignored){} tones=null; }
        super.onDetachedFromWindow();
    }

    public boolean handleBack() {
        if (screen == Screen.GAME) { screen = Screen.LEVELS; invalidate(); return true; }
        if (screen == Screen.LEVELS) { screen = Screen.HOME; invalidate(); return true; }
        return false;
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawColor(Color.rgb(7,10,19));
        if (screen == Screen.HOME) drawHome(c);
        else if (screen == Screen.LEVELS) drawLevels(c);
        else drawGame(c);
    }

    private void drawHome(Canvas c) {
        float w=getWidth(), h=getHeight();
        label(c,"PRISM",w/2,h*.19f,52,Color.WHITE,true);
        label(c,"PULSE 3D",w/2,h*.255f,36,Color.rgb(255,72,190),true);
        label(c,"Twist it. Tap it. Black it out.",w/2,h*.31f,16,Color.rgb(180,190,215),false);
        drawLogo(c,w/2,h*.47f,Math.min(w,h)*.13f);
        float bw=Math.min(w*.76f,dp(380)), bh=dp(60);
        play.set(w/2-bw/2,h*.65f,w/2+bw/2,h*.65f+bh);
        choose.set(w/2-bw/2,h*.65f+bh+dp(16),w/2+bw/2,h*.65f+2*bh+dp(16));
        button(c,play,"CONTINUE  •  LEVEL "+unlocked,Color.rgb(52,220,255));
        button(c,choose,"LEVEL SELECT",Color.rgb(36,43,67));
        int stars=0; for(int i=1;i<=LEVEL_COUNT;i++) stars+=prefs.getInt("stars_"+i,0);
        label(c,stars+" / 150 STARS",w/2,h*.9f,13,Color.rgb(135,145,170),true);
    }

    private void drawLogo(Canvas c,float cx,float cy,float s) {
        p.setStyle(Paint.Style.FILL);
        Path a=new Path(); a.moveTo(cx,cy-s); a.lineTo(cx+s,cy-s*.45f); a.lineTo(cx,cy+.1f*s); a.lineTo(cx-s,cy-s*.45f); a.close();
        p.setColor(Color.rgb(52,220,255)); c.drawPath(a,p);
        Path b=new Path(); b.moveTo(cx-s,cy-s*.45f); b.lineTo(cx,cy+.1f*s); b.lineTo(cx,cy+s); b.lineTo(cx-s,cy+s*.45f); b.close();
        p.setColor(Color.rgb(180,55,170)); c.drawPath(b,p);
        Path d=new Path(); d.moveTo(cx+s,cy-s*.45f); d.lineTo(cx,cy+.1f*s); d.lineTo(cx,cy+s); d.lineTo(cx+s,cy+s*.45f); d.close();
        p.setColor(Color.rgb(105,75,220)); c.drawPath(d,p);
    }

    private void drawLevels(Canvas c) {
        float w=getWidth();
        label(c,"LEVEL SELECT",w/2,dp(54),25,Color.WHITE,true);
        back.set(dp(14),dp(20),dp(66),dp(68)); button(c,back,"‹",Color.rgb(35,42,62));
        float left=dp(14), top=dp(100), gap=dp(7), cell=(w-left*2-gap*4)/5f;
        for(int i=0;i<LEVEL_COUNT;i++) {
            int row=i/5,col=i%5; float y=top+row*(cell*.78f+gap);
            RectF r=levelRects[i]; r.set(left+col*(cell+gap),y,left+col*(cell+gap)+cell,y+cell*.78f);
            boolean open=i+1<=unlocked; int stars=prefs.getInt("stars_"+(i+1),0);
            p.setColor(open?Color.rgb(28,35,55):Color.rgb(17,20,31)); c.drawRoundRect(r,dp(9),dp(9),p);
            label(c,open?String.valueOf(i+1):"•",r.centerX(),r.centerY()-dp(2),17,open?Color.WHITE:Color.rgb(75,80,95),true);
            if(stars>0) label(c,"★".repeat(stars),r.centerX(),r.bottom-dp(7),9,Color.rgb(255,205,75),true);
        }
    }

    private void drawGame(Canvas c) {
        float w=getWidth(), h=getHeight();
        label(c,"LEVEL "+level,w/2,dp(48),20,Color.WHITE,true);
        label(c,"MOVES "+moves+"   •   PAR "+par,w/2,dp(75),12,Color.rgb(160,170,195),true);
        back.set(dp(12),dp(18),dp(62),dp(65)); button(c,back,"‹",Color.rgb(35,42,62));
        drawBoard(c,w/2,h*.48f);
        float gap=dp(9), bw=(w-dp(32)-gap*2)/3f, y=h-dp(90);
        undo.set(dp(16),y,dp(16)+bw,y+dp(56)); reset.set(undo.right+gap,y,undo.right+gap+bw,y+dp(56)); hint.set(reset.right+gap,y,reset.right+gap+bw,y+dp(56));
        button(c,undo,"UNDO",Color.rgb(35,42,62)); button(c,reset,"RESTART",Color.rgb(35,42,62)); button(c,hint,"HINT",Color.rgb(45,55,80));
    }

    private void drawBoard(Canvas c,float cx,float cy) {
        if(board==null) return;
        List<CubeProj> cubes=new ArrayList<>();
        float spacing=Math.min(getWidth(),getHeight())*.12f*zoom;
        float ox=(board.sx-1)/2f, oy=(board.sy-1)/2f, oz=(board.sz-1)/2f;
        for(int z=0;z<board.sz;z++) for(int y=0;y<board.sy;y++) for(int x=0;x<board.sx;x++) {
            int idx=board.index(x,y,z); float X=(x-ox),Y=(y-oy),Z=(z-oz);
            float cyaw=(float)Math.cos(yaw), syaw=(float)Math.sin(yaw), cp=(float)Math.cos(pitch), sp=(float)Math.sin(pitch);
            float x1=X*cyaw-Z*syaw, z1=X*syaw+Z*cyaw;
            float y1=Y*cp-z1*sp, z2=Y*sp+z1*cp;
            float persp=1f/(1f+z2*.11f);
            cubes.add(new CubeProj(idx,cx+x1*spacing*persp,cy+y1*spacing*persp,z2,spacing*.36f*persp));
        }
        Collections.sort(cubes,(a,b)->Float.compare(b.depth,a.depth));
        for(CubeProj q:cubes) drawCube(c,q,board.lit[q.index]);
    }

    private void drawCube(Canvas c,CubeProj q,boolean lit) {
        float s=q.r, x=q.x, y=q.y;
        int base=lit?Color.rgb(65,225,255):Color.rgb(28,34,48);
        p.setStyle(Paint.Style.FILL); p.setColor(base);
        RectF front=new RectF(x-s,y-s,x+s,y+s); c.drawRoundRect(front,s*.18f,s*.18f,p);
        p.setColor(lit?Color.argb(80,150,250,255):Color.argb(90,255,255,255)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1.2f)); c.drawRoundRect(front,s*.18f,s*.18f,p); p.setStyle(Paint.Style.FILL);
        if(lit){ p.setColor(Color.argb(38,90,230,255)); c.drawCircle(x,y,s*1.35f,p); }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        float x=e.getX(),y=e.getY();
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){ downX=lastX=x; downY=lastY=y; dragged=false; return true; }
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE && e.getPointerCount()==1 && screen==Screen.GAME){
            float dx=x-lastX,dy=y-lastY; if(Math.hypot(x-downX,y-downY)>dp(8)) dragged=true;
            if(dragged){ yaw+=dx*.012f; pitch=clamp(pitch+dy*.012f,-1.25f,1.25f); invalidate(); }
            lastX=x; lastY=y; return true;
        }
        if(e.getActionMasked()==MotionEvent.ACTION_UP){ if(!dragged) tap(x,y); return true; }
        return true;
    }

    private void tap(float x,float y) {
        if(screen==Screen.HOME){
            if(play.contains(x,y)){ loadLevel(unlocked); screen=Screen.GAME; }
            else if(choose.contains(x,y)) screen=Screen.LEVELS;
        } else if(screen==Screen.LEVELS){
            if(back.contains(x,y)) screen=Screen.HOME;
            else for(int i=0;i<LEVEL_COUNT;i++) if(levelRects[i].contains(x,y)&&i+1<=unlocked){loadLevel(i+1);screen=Screen.GAME;break;}
        } else {
            if(back.contains(x,y)){screen=Screen.LEVELS;invalidate();return;}
            if(undo.contains(x,y)){undo();invalidate();return;}
            if(reset.contains(x,y)){loadLevel(level);invalidate();return;}
            if(hint.contains(x,y)){hint();invalidate();return;}
            int idx=pickCube(x,y); if(idx>=0) press(idx);
        }
        invalidate();
    }

    private int pickCube(float sx,float sy){
        if(board==null)return-1; float cx=getWidth()/2f,cy=getHeight()*.48f,spacing=Math.min(getWidth(),getHeight())*.12f*zoom;
        int best=-1; float bestD=Float.MAX_VALUE,bestDepth=-999;
        float ox=(board.sx-1)/2f,oy=(board.sy-1)/2f,oz=(board.sz-1)/2f;
        for(int z=0;z<board.sz;z++)for(int y=0;y<board.sy;y++)for(int x=0;x<board.sx;x++){
            int idx=board.index(x,y,z);float X=x-ox,Y=y-oy,Z=z-oz;float cyaw=(float)Math.cos(yaw),syaw=(float)Math.sin(yaw),cp=(float)Math.cos(pitch),sp=(float)Math.sin(pitch);
            float x1=X*cyaw-Z*syaw,z1=X*syaw+Z*cyaw,y1=Y*cp-z1*sp,z2=Y*sp+z1*cp,persp=1f/(1f+z2*.11f);
            float px=cx+x1*spacing*persp,py=cy+y1*spacing*persp,r=spacing*.55f*persp,d=(float)Math.hypot(sx-px,sy-py);
            if(d<r && (d<bestD*.8f || z2>bestDepth)){best=idx;bestD=d;bestDepth=z2;}
        }
        return best;
    }

    private void loadLevel(int n){
        level=Math.max(1,Math.min(LEVEL_COUNT,n)); board=LevelFactory.make(level); moves=0; history.clear(); yaw=-.65f; pitch=-.45f; zoom=1f; par=Solver.solve(board).size(); if(par<1)par=1;
    }
    private void press(int idx){ history.addLast(idx); board.tap(idx); moves++; feedback(); if(board.solved()) complete(); invalidate(); }
    private void undo(){ if(history.isEmpty())return; int i=history.removeLast();board.tap(i);moves=Math.max(0,moves-1);feedback(); }
    private void hint(){ List<Integer>s=Solver.solve(board); if(!s.isEmpty())press(s.get(0)); }
    private void complete(){
        int stars=moves<=par?3:moves<=par+2?2:1; int old=prefs.getInt("stars_"+level,0); int next=Math.min(LEVEL_COUNT,Math.max(unlocked,level+1));
        SharedPreferences.Editor e=prefs.edit().putInt("unlocked",next); if(stars>old)e.putInt("stars_"+level,stars); e.apply(); unlocked=next;
        try{ if(tones!=null)tones.startTone(ToneGenerator.TONE_PROP_ACK,180);}catch(Throwable ignored){}
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        if(level<LEVEL_COUNT){ loadLevel(level+1); } else { screen=Screen.LEVELS; }
    }
    private void feedback(){ try{if(tones!=null)tones.startTone(ToneGenerator.TONE_PROP_BEEP,35);}catch(Throwable ignored){} performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); }

    private void button(Canvas c,RectF r,String s,int color){ p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(r,dp(12),dp(12),p);label(c,s,r.centerX(),r.centerY()+dp(5),14,Color.WHITE,true); }
    private void label(Canvas c,String s,float x,float y,float sp,int color,boolean bold){ text.setTextSize(dp(sp));text.setColor(color);text.setTextAlign(Paint.Align.CENTER);text.setTypeface(android.graphics.Typeface.create("sans-serif",bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL));c.drawText(s,x,y,text); }
    private float dp(float v){return v*getResources().getDisplayMetrics().density;}
    private static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}

    private static class CubeProj{final int index;final float x,y,depth,r;CubeProj(int i,float x,float y,float d,float r){index=i;this.x=x;this.y=y;depth=d;this.r=r;}}
    private static class Board{
        final int sx,sy,sz;final boolean[]lit;Board(int x,int y,int z){sx=x;sy=y;sz=z;lit=new boolean[x*y*z];}
        int index(int x,int y,int z){return x+sx*(y+sy*z);} int count(){return lit.length;}
        void toggle(int x,int y,int z){if(x<0||y<0||z<0||x>=sx||y>=sy||z>=sz)return;int i=index(x,y,z);lit[i]=!lit[i];}
        void tap(int i){int x=i%sx,y=(i/sx)%sy,z=i/(sx*sy);toggle(x,y,z);toggle(x-1,y,z);toggle(x+1,y,z);toggle(x,y-1,z);toggle(x,y+1,z);toggle(x,y,z-1);toggle(x,y,z+1);}
        boolean affects(int p,int t){int px=p%sx,py=(p/sx)%sy,pz=p/(sx*sy),tx=t%sx,ty=(t/sx)%sy,tz=t/(sx*sy);return Math.abs(px-tx)+Math.abs(py-ty)+Math.abs(pz-tz)<=1;}
        boolean solved(){for(boolean b:lit)if(b)return false;return true;}
    }
    private static class LevelFactory{
        static Board make(int level){int sx,sy,sz,presses;if(level<=10){sx=2;sy=2;sz=2;presses=2+(level-1)/2;}else if(level<=20){sx=3;sy=2;sz=2;presses=4+(level-11)/2;}else if(level<=30){sx=3;sy=3;sz=2;presses=6+(level-21)/2;}else if(level<=40){sx=3;sy=3;sz=3;presses=8+(level-31)/2;}else{sx=4;sy=3;sz=3;presses=11+(level-41)/2;}
            Board b=new Board(sx,sy,sz);Random r=new Random(7717L*level+20260906L);boolean[]used=new boolean[b.count()];int made=0;presses=Math.min(presses,b.count()-1);while(made<presses){int i=r.nextInt(b.count());if(!used[i]){used[i]=true;b.tap(i);made++;}}if(b.solved())b.tap(level%b.count());return b;}
    }
    private static class Solver{
        static List<Integer> solve(Board b){int n=b.count();boolean[][]m=new boolean[n][n+1];for(int r=0;r<n;r++){for(int c=0;c<n;c++)m[r][c]=b.affects(c,r);m[r][n]=b.lit[r];}int row=0;int[]pc=new int[n];java.util.Arrays.fill(pc,-1);for(int col=0;col<n&&row<n;col++){int f=-1;for(int r=row;r<n;r++)if(m[r][col]){f=r;break;}if(f<0)continue;boolean[]tmp=m[f];m[f]=m[row];m[row]=tmp;for(int r=0;r<n;r++)if(r!=row&&m[r][col])for(int c=col;c<=n;c++)m[r][c]^=m[row][c];pc[row]=col;row++;}boolean[]x=new boolean[n];for(int r=0;r<row;r++)if(pc[r]>=0)x[pc[r]]=m[r][n];List<Integer>out=new ArrayList<>();for(int i=0;i<n;i++)if(x[i])out.add(i);return out;}
    }
}
