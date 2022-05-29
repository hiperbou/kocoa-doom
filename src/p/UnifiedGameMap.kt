package p


import data.Limits
import data.sounds.sfxenum_t
import doom.DoomMain
import doom.SourceCode
import doom.SourceCode.*
import doom.thinker_t
import m.*
import m.fixed_t.Companion.MAPFRACUNIT
import mochadoom.Engine
import mochadoom.Loggers
import rr.ISpriteManager
import rr.line_t
import utils.C2JUtils
import utils.GenericCopy
import java.util.*
import java.util.function.Supplier
import java.util.logging.Level

// // FROM SIGHT
abstract class UnifiedGameMap(DOOM: DoomMain<*, *>) : ThinkerList {
    /////////////////// STATUS ///////////////////
    val DOOM: DoomMain<*, *>

    // //////////// Internal singletons //////////////
    var A: ActionFunctions? = null
    var SPECS: UnifiedGameMap.Specials
    var SW: Switches

    // ////////////////////////////////////////////
    //
    // THING POSITION SETTING
    //
    //
    // BLOCK MAP ITERATORS
    // For each line/thing in the given mapblock,
    // call the passed PIT_* function.
    // If the function returns false,
    // exit with false without checking anything else.
    //
    var ptflags = 0

    /**
     * killough's code for thinkers seems to be totally broken in M.D,
     * this method is unused
     */
    /*protected void UpdateThinker(thinker_t thinker) {
        thinker_t th;
        // find the class the thinker belongs to

        th_class cls = thinker.thinkerFunction == NOP
            ? th_class.th_delete
            : (thinker.thinkerFunction == P_MobjThinker
                && ((mobj_t) thinker).health > 0
                && (eval((((mobj_t) thinker).flags) & MF_COUNTKILL)
                || ((mobj_t) thinker).type == mobjtype_t.MT_SKULL)
                    ? (
                        eval((((mobj_t) thinker).flags) & MF_FRIEND)
                            ? th_class.th_friends
                            : th_class.th_enemies
                    ) : th_class.th_misc
            );

        {
            / * Remove from current thread, if in one */
    /*if ((th = thinker.cnext) != null) {
                (th.cprev = thinker.cprev).cnext = th;
            }
        }

        // Add to appropriate thread
        th = thinkerclasscap[cls.ordinal()];
        th.cprev.cnext = thinker;
        thinker.cnext = th;
        thinker.cprev = th.cprev;
        th.cprev = thinker;
    }

    protected final thinker_t[] thinkerclasscap=new thinker_t[th_class.NUMTHCLASS];*/
    var sight_debug = false
    //
    // P_InitPicAnims
    //
    /**
     * Floor/ceiling animation sequences, defined by first and last frame, i.e.
     * the flat (64x64 tile) name to be used. The full animation sequence is
     * given using all the flats between the start and end entry, in the order
     * found in the WAD file.
     */
    private val animdefs = arrayOf(
        animdef_t(false, "NUKAGE3", "NUKAGE1", 8),
        animdef_t(false, "FWATER4", "FWATER1", 8),
        animdef_t(false, "SWATER4", "SWATER1", 8),
        animdef_t(false, "LAVA4", "LAVA1", 8),
        animdef_t(false, "BLOOD3", "BLOOD1", 8),  // DOOM II flat animations.
        animdef_t(false, "RROCK08", "RROCK05", 8),
        animdef_t(false, "SLIME04", "SLIME01", 8),
        animdef_t(false, "SLIME08", "SLIME05", 8),
        animdef_t(false, "SLIME12", "SLIME09", 8),
        animdef_t(true, "BLODGR4", "BLODGR1", 8),
        animdef_t(true, "SLADRIP3", "SLADRIP1", 8),
        animdef_t(true, "BLODRIP4", "BLODRIP1", 8),
        animdef_t(true, "FIREWALL", "FIREWALA", 8),
        animdef_t(true, "GSTFONT3", "GSTFONT1", 8),
        animdef_t(true, "FIRELAVA", "FIRELAV3", 8),
        animdef_t(true, "FIREMAG3", "FIREMAG1", 8),
        animdef_t(true, "FIREBLU2", "FIREBLU1", 8),
        animdef_t(true, "ROCKRED3", "ROCKRED1", 8),
        animdef_t(true, "BFALL4", "BFALL1", 8),
        animdef_t(true, "SFALL4", "SFALL1", 8),
        animdef_t(true, "WFALL4", "WFALL1", 8),
        animdef_t(true, "DBRAIN4", "DBRAIN1", 8)
    )

    // MAES: this was a cheap trick to mark the end of the sequence
    // with a value of "-1".
    // It won't work in Java, so just use animdefs.length-1
    // new animdef_t(false, "", "", 0) };
    //
    // SPECIAL SPAWNING
    //
    inner class Specials  {
        var linespeciallist = arrayOfNulls<line_t>(Limits.MAXLINEANIMS)
        var numlinespecials: Short = 0

        /**
         * These are NOT the same anims found in defines. Dunno why they fucked up
         * this one so badly. Even the type has the same name, but is entirely
         * different. No way they could be overlapped/unionized either. So WTF.
         * Really. WTF.
         */
        var anims = arrayOfNulls<anim_t>(Limits.MAXANIMS)

        // MAES: was a pointer
        var lastanim = 0

        //
        // P_UpdateSpecials
        // Animate planes, scroll walls, etc.
        //
        var levelTimer = false
        var levelTimeCount = 0
        fun UpdateSpecials() {
            var pic: Int
            var line: line_t?
            var anim: anim_t?

            // LEVEL TIMER
            if (levelTimer == true) {
                levelTimeCount--
                if (levelTimeCount == 0) DOOM.ExitLevel()
            }

            // ANIMATE FLATS AND TEXTURES GLOBALLY
            for (j in 0 until lastanim) {
                anim = anims[j]
                for (i in anim!!.basepic until anim.basepic + anim.numpics) {
                    pic = anim.basepic + (DOOM.leveltime / anim.speed + i) % anim.numpics
                    if (anim.istexture) DOOM.textureManager.setTextureTranslation(
                        i,
                        pic
                    ) else DOOM.textureManager.setFlatTranslation(i, pic)
                }
            }

            // ANIMATE LINE SPECIALS
            for (i in 0 until numlinespecials) {
                line = linespeciallist[i]
                when (line!!.special.toInt()) {
                    48 ->                     // EFFECT FIRSTCOL SCROLL +
                        DOOM.levelLoader.sides[line.sidenum[0].code].textureoffset += MAPFRACUNIT
                }
            }

            // DO BUTTONS
            SW.doButtons()
        }

        fun InitPicAnims() {
            Arrays.setAll(anims) { i: Int -> anim_t() }
            var lstanim: anim_t?
            // Init animation. MAES: sneaky base pointer conversion ;-)
            lastanim = 0
            // MAES: for (i=0 ; animdefs[i].istexture != -1 ; i++)
            for (i in 0 until animdefs.size - 1) {
                lstanim = anims[lastanim]
                if (animdefs[i].istexture) {
                    // different episode ?
                    if (DOOM.textureManager.CheckTextureNumForName(animdefs[i].startname!!) == -1) {
                        continue
                    }
                    // So, if it IS a valid texture, it goes straight into anims.
                    lstanim!!.picnum = DOOM.textureManager.TextureNumForName(animdefs[i].endname!!)
                    lstanim.basepic = DOOM.textureManager.TextureNumForName(animdefs[i].startname!!)
                } else { // If not a texture, it's a flat.
                    if (DOOM.wadLoader.CheckNumForName(animdefs[i].startname!!) == -1) {
                        continue
                    }
                    UnifiedGameMap.LOGGER.log(Level.FINER, Supplier { animdefs[i].toString() })
                    // Otherwise, lstanim seems to go nowhere :-/
                    lstanim!!.picnum = DOOM.textureManager.FlatNumForName(animdefs[i].endname!!)
                    lstanim.basepic = DOOM.textureManager.FlatNumForName(animdefs[i].startname!!)
                }
                lstanim.istexture = animdefs[i].istexture
                lstanim.numpics = lstanim.picnum - lstanim.basepic + 1
                if (lstanim.numpics < 2) {
                    DOOM.doomSystem.Error(
                        "P_InitPicAnims: bad cycle from %s to %s",
                        animdefs[i].startname, animdefs[i].endname
                    )
                }
                lstanim.speed = animdefs[i].speed
                lastanim++
            }
        }

        fun resizeLinesSpecialList() {
            linespeciallist = C2JUtils.resize(linespeciallist[0], linespeciallist, linespeciallist.size * 2)
        }

        //internal companion object { //TODO: is this used at all?
            /*const*/ val OK = 0
            /*const*/ val CRUSHED = 1
            /*const*/ val PASTDEST = 2
        //}
    }

    inner class Switches {
        fun doButtons() {
            for (buttonlist1 in buttonlist) {
                if (C2JUtils.eval(buttonlist1.btimer)) {
                    buttonlist1.btimer--
                    if (!C2JUtils.eval(buttonlist1.btimer)) {
                        val line = buttonlist1.line!!
                        when (buttonlist1.where) {
                            bwhere_e.top -> DOOM.levelLoader.sides[line.sidenum[0].code].toptexture =
                                buttonlist1.btexture.toShort()
                            bwhere_e.middle -> DOOM.levelLoader.sides[line.sidenum[0].code].midtexture =
                                buttonlist1.btexture.toShort()
                            bwhere_e.bottom -> DOOM.levelLoader.sides[line.sidenum[0].code].bottomtexture =
                                buttonlist1.btexture.toShort()
                        }
                        DOOM.doomSound.StartSound(buttonlist1.soundorg, sfxenum_t.sfx_swtchn)
                        buttonlist1.reset()
                    }
                }
            }
        }

        //
        // CHANGE THE TEXTURE OF A WALL SWITCH TO ITS OPPOSITE
        //
        var alphSwitchList = arrayOf( // Doom shareware episode 1 switches
            switchlist_t("SW1BRCOM", "SW2BRCOM", 1),
            switchlist_t("SW1BRN1", "SW2BRN1", 1),
            switchlist_t("SW1BRN2", "SW2BRN2", 1),
            switchlist_t("SW1BRNGN", "SW2BRNGN", 1),
            switchlist_t("SW1BROWN", "SW2BROWN", 1),
            switchlist_t("SW1COMM", "SW2COMM", 1),
            switchlist_t("SW1COMP", "SW2COMP", 1),
            switchlist_t("SW1DIRT", "SW2DIRT", 1),
            switchlist_t("SW1EXIT", "SW2EXIT", 1),
            switchlist_t("SW1GRAY", "SW2GRAY", 1),
            switchlist_t("SW1GRAY1", "SW2GRAY1", 1),
            switchlist_t("SW1METAL", "SW2METAL", 1),
            switchlist_t("SW1PIPE", "SW2PIPE", 1),
            switchlist_t("SW1SLAD", "SW2SLAD", 1),
            switchlist_t("SW1STARG", "SW2STARG", 1),
            switchlist_t("SW1STON1", "SW2STON1", 1),
            switchlist_t("SW1STON2", "SW2STON2", 1),
            switchlist_t("SW1STONE", "SW2STONE", 1),
            switchlist_t("SW1STRTN", "SW2STRTN", 1),  // Doom registered episodes 2&3 switches
            switchlist_t("SW1BLUE", "SW2BLUE", 2),
            switchlist_t("SW1CMT", "SW2CMT", 2),
            switchlist_t("SW1GARG", "SW2GARG", 2),
            switchlist_t("SW1GSTON", "SW2GSTON", 2),
            switchlist_t("SW1HOT", "SW2HOT", 2),
            switchlist_t("SW1LION", "SW2LION", 2),
            switchlist_t("SW1SATYR", "SW2SATYR", 2),
            switchlist_t("SW1SKIN", "SW2SKIN", 2),
            switchlist_t("SW1VINE", "SW2VINE", 2),
            switchlist_t("SW1WOOD", "SW2WOOD", 2),  // Doom II switches
            switchlist_t("SW1PANEL", "SW2PANEL", 3),
            switchlist_t("SW1ROCK", "SW2ROCK", 3),
            switchlist_t("SW1MET2", "SW2MET2", 3),
            switchlist_t("SW1WDMET", "SW2WDMET", 3),
            switchlist_t("SW1BRIK", "SW2BRIK", 3),
            switchlist_t("SW1MOD1", "SW2MOD1", 3),
            switchlist_t("SW1ZIM", "SW2ZIM", 3),
            switchlist_t("SW1STON6", "SW2STON6", 3),
            switchlist_t("SW1TEK", "SW2TEK", 3),
            switchlist_t("SW1MARB", "SW2MARB", 3),
            switchlist_t("SW1SKULL", "SW2SKULL", 3),
            switchlist_t("\u0000", "\u0000", 0)
        )

        /** A (runtime generated) list of the KNOWN button types  */
        var switchlist: IntArray
        var numswitches = 0
        lateinit var buttonlist: Array<button_t>

        init {
            switchlist = IntArray(Limits.MAXSWITCHES)
            initButtonList()
        }

        //
        // P_InitSwitchList
        // Only called at game initialization.
        //
        fun InitSwitchList() {
            var i: Int
            var index: Int
            var episode: Int
            episode = 1

            // MAES: if this isn't changed Ultimate Doom's switches
            // won't work visually.
            if (DOOM.isRegistered()) {
                episode = 2
            } else if (DOOM.isCommercial()) {
                episode = 3
            }
            index = 0
            i = 0
            while (i < Limits.MAXSWITCHES) {
                if (index >= switchlist.size) {
                    // Remove limit
                    switchlist = Arrays.copyOf(switchlist, if (switchlist.size > 0) switchlist.size * 2 else 8)
                }

                // Trickery. Looks for "end of list" marker
                // Since the list has pairs of switches, the
                // actual number of distinct switches is index/2
                if (alphSwitchList[i].episode.toInt() == 0) {
                    numswitches = index / 2
                    switchlist[index] = -1
                    break
                }
                if (alphSwitchList[i].episode <= episode) {
                    /*
                     * // UNUSED - debug? int value; if
                     * (R_CheckTextureNumForName(alphSwitchList[i].name1) < 0) {
                     * system.Error("Can't find switch texture '%s'!",
                     * alphSwitchList[i].name1); continue; } value =
                     * R_TextureNumForName(alphSwitchList[i].name1);
                     */
                    switchlist[index++] = DOOM.textureManager.TextureNumForName(alphSwitchList[i].name1!!)
                    switchlist[index++] = DOOM.textureManager.TextureNumForName(alphSwitchList[i].name2!!)
                }
                i++
            }
        }

        //
        // Start a button counting down till it turns off.
        //
        fun StartButton(line: line_t, w: bwhere_e?, texture: Int, time: Int) {
            // See if button is already pressed
            for (buttonlist1 in buttonlist) {
                if (buttonlist1.btimer != 0 && buttonlist1.line === line) {
                    return
                }
            }

            // At this point, it may mean that THE button of that particular
            // line was not active, or simply that there were not enough 
            // buttons in buttonlist to support an additional entry.
            // Search for a free button slot.
            for (buttonlist1 in buttonlist) {
                if (buttonlist1.btimer == 0) {
                    buttonlist1.line = line
                    buttonlist1.where = w!!
                    buttonlist1.btexture = texture
                    buttonlist1.btimer = time
                    buttonlist1.soundorg = line.soundorg
                    return
                }
            }
            /**
             * Added config option to disable resize
             * - Good Sign 2017/04/26
             */
            // Extremely rare event, We must be able to push more than MAXBUTTONS buttons
            // in one tic, which can't normally happen except in really pathological maps.
            // In any case, resizing should solve this problem.
            if (Engine.getConfig().equals(Settings.extend_button_slots_limit, java.lang.Boolean.TRUE)) {
                buttonlist = C2JUtils.resize(buttonlist[0], buttonlist, buttonlist.size * 2)
                // Try again
                StartButton(line, w, texture, time)
            } else {
                UnifiedGameMap.LOGGER.log(Level.SEVERE, "P_StartButton: no button slots left!")
                System.exit(1)
            }
        }

        //
        // Function that changes wall texture.
        // Tell it if switch is ok to use again (true=yes, it's a button).
        //
        fun ChangeSwitchTexture(line: line_t, useAgain: Boolean) {
            val texTop: Int
            val texMid: Int
            val texBot: Int
            var sound: Int
            if (!useAgain) line.special = 0
            texTop = DOOM.levelLoader.sides[line.sidenum[0].code].toptexture.toInt()
            texMid = DOOM.levelLoader.sides[line.sidenum[0].code].midtexture.toInt()
            texBot = DOOM.levelLoader.sides[line.sidenum[0].code].bottomtexture.toInt()
            sound = sfxenum_t.sfx_swtchn.ordinal

            // EXIT SWITCH?
            if (line.special.toInt() == 11) {
                sound = sfxenum_t.sfx_swtchx.ordinal
            }
            for (i in 0 until numswitches * 2) {
                if (switchlist[i] == texTop) {
                    DOOM.doomSound.StartSound(buttonlist[0].soundorg, sound)
                    DOOM.levelLoader.sides[line.sidenum[0].code].toptexture = switchlist[i xor 1].toShort()
                    if (useAgain) {
                        StartButton(line, bwhere_e.top, switchlist[i], Limits.BUTTONTIME)
                    }
                    return
                } else {
                    if (switchlist[i] == texMid) {
                        DOOM.doomSound.StartSound(buttonlist[0].soundorg, sound)
                        DOOM.levelLoader.sides[line.sidenum[0].code].midtexture = switchlist[i xor 1].toShort()
                        if (useAgain) {
                            StartButton(line, bwhere_e.middle, switchlist[i], Limits.BUTTONTIME)
                        }
                        return
                    } else {
                        if (switchlist[i] == texBot) {
                            DOOM.doomSound.StartSound(buttonlist[0].soundorg, sound)
                            DOOM.levelLoader.sides[line.sidenum[0].code].bottomtexture = switchlist[i xor 1].toShort()
                            if (useAgain) {
                                StartButton(line, bwhere_e.bottom, switchlist[i], Limits.BUTTONTIME)
                            }
                            return
                        }
                    }
                }
            }
        }

        fun initButtonList() {
            // Unlike plats, buttonlist needs statically allocated and reusable
            // objects. The MAXBUTTONS limit actually applied to buttons PRESSED
            // or ACTIVE at once, not how many there can actually be in a map.
            buttonlist = GenericCopy.malloc({ button_t() }, Limits.MAXBUTTONS)
        }
    }
    /* enum PTR {
        SlideTraverse,
        AimTraverse,
        ShootTraverse,
        UseTraverse
    } */
    /////////// BEGIN MAP OBJECT CODE, USE AS BASIC
    // //////////////////////////////// THINKER CODE, GLOBALLY VISIBLE
    // /////////////////
    //
    // THINKERS
    // All thinkers should be allocated by Z_Malloc
    // so they can be operated on uniformly.
    // The actual structures will vary in size,
    // but the first element must be thinker_t.
    //
    /** Both the head and the tail of the thinkers list  */
    var thinkercap: thinker_t

    /**
     * killough's code for thinkers seems to be totally broken in M.D,
     * so commented it out and will not probably restore, but may invent
     * something new in future
     * - Good Sign 2017/05/1
     *
     * P_InitThinkers
     */
    @Suspicious(CauseOfDesyncProbability.MEDIUM)
    @P_Tick.C(P_Tick.P_InitThinkers)
    override fun InitThinkers() {

        /*for (int i=0; i<th_class.NUMTHCLASS; i++)  // killough 8/29/98: initialize threaded lists
            thinkerclasscap[i].cprev = thinkerclasscap[i].cnext = thinkerclasscap[i];*/
        val next = thinkercap.next
        val prev = thinkercap.prev

        // Unlink the "dangling" thinkers that may still be attached
        // to the thinkercap. When loading a new level, they DO NOT get unloaded,
        // wtf...
        if (next != null && next !== thinkercap) {
            //System.err.println("Next link to thinkercap nulled");
            next.prev = null
        }
        if (prev != null && prev !== thinkercap) {
            //System.err.println("Prev link to thinkercap nulled");
            prev.next = null
        }
        thinkercap.next = thinkercap
        thinkercap.prev = thinkercap
    }
    /**
     * killough's code for thinkers seems to be totally broken in M.D,
     * so commented it out and will not probably restore, but may invent
     * something new in future
     * - Good Sign 2017/05/1
     *
     * cph 2002/01/13 - iterator for thinker list
     * WARNING: Do not modify thinkers between calls to this functin
     */
    /*thinker_t NextThinker(thinker_t th, th_class cl) {
        thinker_t top = thinkerclasscap[cl.ordinal()];
        if (th == null) {
            th = top;
        }
        th = cl == th_class.th_all ? th.next : th.cnext;
        return th == top ? null : th;
    }*/
    /**
     * killough's code for thinkers seems to be totally broken in M.D,
     * so commented it out and will not probably restore, but may invent
     * something new in future
     * - Good Sign 2017/05/1
     *
     * P_AddThinker
     * Adds a new thinker at the end of the list.
     */
    @SourceCode.Exact
    @P_Tick.C(P_Tick.P_AddThinker)
    override fun AddThinker(thinker: thinker_t) {
        thinkercap.prev!!.next = thinker
        thinker.next = thinkercap
        thinker.prev = thinkercap.prev
        thinkercap.prev = thinker

        // killough 8/29/98: set sentinel pointers, and then add to appropriate list
        /*thinker.cnext = thinker.cprev = null;
        UpdateThinker(thinker);*/

        // [Maes] seems only used for interpolations
        //newthinkerpresent = true;
    }

    //
    // P_AllocateThinker
    // Allocates memory and adds a new thinker at the end of the list.
    //
    override fun getRandomThinker(): thinker_t {
        val pick = (Math.random() * 128).toInt()
        var th = this.getThinkerCap()
        for (i in 0 until pick) {
            th = th.next!!
        }
        return th
    }

    //
    // P_Init
    //
    fun Init() {
        SW.InitSwitchList()
        SPECS.InitPicAnims()
        DOOM.spriteManager.InitSprites(ISpriteManager.doomsprnames)
    }

    override fun getThinkerCap(): thinker_t {
        return thinkercap
    }

    /**
     * killough 11/98:
     *
     * Make currentthinker external, so that P_RemoveThinkerDelayed
     * can adjust currentthinker when thinkers self-remove.
     */
    protected var currentthinker: thinker_t? = null //protected final P_RemoveThinkerDelayed RemoveThinkerDelayed; 
    //public class P_RemoveThinkerDelayed implements p.ActionFunctions.TypedAction <thinker_t>{
    //@Override
    //public void accept(thinker_t thinker) {
    /*
        try {
        System.err.printf("Delete: %s %d<= %s %d => %s %d\n",
            ((mobj_t)thinker.prev).type,((mobj_t)thinker.prev).thingnum,
            ((mobj_t)thinker).type,((mobj_t)thinker).thingnum,
            ((mobj_t)thinker.next).type,((mobj_t)thinker.next).thingnum);
        } catch (ClassCastException e){
            
        } */
    // Unlike Boom, if we reach here it gets zapped anyway
    //if (!thinker->references)
    //{
    //{ /* Remove from main thinker list */
    //thinker_t next = thinker.next;
    /* Note that currentthinker is guaranteed to point to us,
             * and since we're freeing our memory, we had better change that. So
             * point it to thinker->prev, so the iterator will correctly move on to
             * thinker->prev->next = thinker->next */
    //(next.prev = currentthinker = thinker.prev).next = next;
    //thinker.next=thinker.prev=null;
    //try {
    // System.err.printf("Delete: %s %d <==> %s %d\n",
    //     ((mobj_t)currentthinker.prev).type,((mobj_t)currentthinker.prev).thingnum,
    //     ((mobj_t)currentthinker.next).type,((mobj_t)currentthinker.next).thingnum);
    //} catch (ClassCastException e){
    //}
    //}
    //{
    /* Remove from current thinker class list */ //thinker_t th = thinker.cnext;
    //(th.cprev = thinker.cprev).cnext = th;
    //thinker.cnext=thinker.cprev=null;
    //}
    //}
    //}
    /**
     * killough's code for thinkers seems to be totally broken in M.D,
     * so commented it out and will not probably restore, but may invent
     * something new in future
     * - Good Sign 2017/05/1
     */
    init {
        SW = Switches()
        SPECS = Specials()
        thinkercap = thinker_t()
        /*for (int i=0; i<th_class.NUMTHCLASS; i++) { // killough 8/29/98: initialize threaded lists
            thinkerclasscap[i]=new thinker_t();
        }*/

        // Normally unused. It clashes with line attribute 124, and looks like ass
        // anyway. However it's fully implemented.
        //this.SL=new SlideDoor(DS);
        //DS.SL=SL;
        this.DOOM = DOOM
        // "Wire" all states to the proper functions.
        /*for (state_t state : states) {
            FUNS.doWireState(state);
        }*/
    }

    companion object {
        private val LOGGER = Loggers.getLogger(UnifiedGameMap::class.java.name)
    }
} // End unified map
