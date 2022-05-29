package p.Actions

import doom.thinker_t
import mochadoom.Loggers
import p.*
import rr.line_t
import rr.sector_t
import utils.GenericCopy
import utils.TraitFactory.ContextKey
import java.util.function.Supplier
import java.util.logging.Level


interface ActionsSlideDoors : ActionTrait {
    override fun RemoveThinker(t: thinker_t)
    class SlideDoors {
        var slideFrames =
            GenericCopy.malloc({ slideframe_t() }, ActionsSlideDoors.MAXSLIDEDOORS)
    }

    fun SlidingDoor(door: slidedoor_t) {
        val ll = levelLoader()
        val sd = contextRequire<SlideDoors>(ActionsSlideDoors.KEY_SLIDEDOORS)
        val line = door.line!!
        when (door.status) {
            sd_e.sd_opening -> if (door.timer-- == 0) {
                if (++door.frame == ActionsSlideDoors.SNUMFRAMES) {
                    // IF DOOR IS DONE OPENING...
                    ll.sides[line.sidenum[0].code].midtexture = 0
                    ll.sides[line.sidenum[1].code].midtexture = 0
                    line.flags = (line.flags.toInt() and (line_t.ML_BLOCKING xor 0xff)).toShort()
                    if (door.type == sdt_e.sdt_openOnly) {
                        door.frontsector!!.specialdata = null
                        RemoveThinker(door)
                    } else {
                        door.timer = ActionsSlideDoors.SDOORWAIT
                        door.status = sd_e.sd_waiting
                    }
                } else {
                    // IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
                    door.timer = ActionsSlideDoors.SWAITTICS
                    ll.sides[line.sidenum[0].code].midtexture =
                        sd.slideFrames[door.whichDoorIndex].frontFrames[door.frame].toShort()
                    ll.sides[line.sidenum[1].code].midtexture =
                        sd.slideFrames[door.whichDoorIndex].backFrames[door.frame].toShort()
                }
            }
            sd_e.sd_waiting ->                 // IF DOOR IS DONE WAITING...
                if (door.timer-- == 0) {
                    // CAN DOOR CLOSE?
                    if (door.frontsector!!.thinglist != null
                        || door.backsector!!.thinglist != null)
                    {
                        door.timer = ActionsSlideDoors.SDOORWAIT
                    }
                    else {
                        // door.frame = SNUMFRAMES-1;
                        door.status = sd_e.sd_closing
                        door.timer = ActionsSlideDoors.SWAITTICS
                    }
                }
            sd_e.sd_closing -> if (door.timer-- == 0) {
                if (--door.frame < 0) {
                    // IF DOOR IS DONE CLOSING...
                    line.flags = (line.flags.toInt() or line_t.ML_BLOCKING).toShort()
                    door.frontsector!!.specialdata = null
                    RemoveThinker(door)
                } else {
                    // IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
                    door.timer = ActionsSlideDoors.SWAITTICS
                    ll.sides[line.sidenum[0].code].midtexture =
                        sd.slideFrames[door.whichDoorIndex].frontFrames[door.frame].toShort()
                    ll.sides[line.sidenum[1].code].midtexture =
                        sd.slideFrames[door.whichDoorIndex].backFrames[door.frame].toShort()
                }
            }
        }
    }

    fun P_InitSlidingDoorFrames() {
        val tm = DOOM().textureManager
        val sd = contextRequire<SlideDoors>(ActionsSlideDoors.KEY_SLIDEDOORS)
        var i: Int
        var f1: Int
        var f2: Int
        var f3: Int
        var f4: Int

        // DOOM II ONLY...
        if (!DOOM().isCommercial()) {
            return
        }
        i = 0
        while (i < ActionsSlideDoors.MAXSLIDEDOORS) {
            if (ActionsSlideDoors.slideFrameNames.get(i).frontFrame1 == null) {
                break
            }
            f1 = tm.TextureNumForName(ActionsSlideDoors.slideFrameNames.get(i).frontFrame1!!)
            f2 = tm.TextureNumForName(ActionsSlideDoors.slideFrameNames.get(i).frontFrame2!!)
            f3 = tm.TextureNumForName(ActionsSlideDoors.slideFrameNames.get(i).frontFrame3!!)
            f4 = tm.TextureNumForName(ActionsSlideDoors.slideFrameNames.get(i).frontFrame4!!)
            sd.slideFrames[i].frontFrames[0] = f1
            sd.slideFrames[i].frontFrames[1] = f2
            sd.slideFrames[i].frontFrames[2] = f3
            sd.slideFrames[i].frontFrames[3] = f4
            f1 = tm.TextureNumForName(ActionsSlideDoors.slideFrameNames.get(i).backFrame1!!)
            f2 = tm.TextureNumForName(ActionsSlideDoors.slideFrameNames.get(i).backFrame2!!)
            f3 = tm.TextureNumForName(ActionsSlideDoors.slideFrameNames.get(i).backFrame3!!)
            f4 = tm.TextureNumForName(ActionsSlideDoors.slideFrameNames.get(i).backFrame4!!)
            sd.slideFrames[i].backFrames[0] = f1
            sd.slideFrames[i].backFrames[1] = f2
            sd.slideFrames[i].backFrames[2] = f3
            sd.slideFrames[i].backFrames[3] = f4
            i++
        }
    }

    //
    // Return index into "slideFrames" array
    // for which door type to use
    //
    fun P_FindSlidingDoorType(line: line_t): Int {
        val ll = levelLoader()
        val sd = contextRequire<SlideDoors>(ActionsSlideDoors.KEY_SLIDEDOORS)
        for (i in 0 until ActionsSlideDoors.MAXSLIDEDOORS) {
            val `val` = ll.sides[line.sidenum[0].code].midtexture.toInt()
            if (`val` == sd.slideFrames[i].frontFrames[0]) {
                return i
            }
        }
        return -1
    }

    fun EV_SlidingDoor(line: line_t, thing: mobj_t) {
        val sec: sector_t
        var door: slidedoor_t?

        // DOOM II ONLY...
        if (!DOOM().isCommercial()) {
            return
        }
        Loggers.getLogger(ActionsSlideDoors::class.java.name).log(Level.WARNING, "EV_SlidingDoor")

        // Make sure door isn't already being animated
        sec = line.frontsector!!
        door = null
        if (sec.specialdata != null) {
            if (thing.player == null) {
                return
            }
            door = sec.specialdata as slidedoor_t
            if (door!!.type == sdt_e.sdt_openAndClose) {
                if (door.status == sd_e.sd_waiting) {
                    door.status = sd_e.sd_closing
                }
            } else {
                return
            }
        }

        // Init sliding door vars
        if (door == null) {
            door = slidedoor_t()
            AddThinker(door)
            sec.specialdata = door
            door.type = sdt_e.sdt_openAndClose
            door.status = sd_e.sd_opening
            door.whichDoorIndex = P_FindSlidingDoorType(line)
            if (door.whichDoorIndex < 0) {
                doomSystem()?.Error("EV_SlidingDoor: Can't use texture for sliding door!")
            }
            door.frontsector = sec
            door.backsector = line.backsector
            door.thinkerFunction = ActiveStates.T_SlidingDoor
            door.timer = ActionsSlideDoors.SWAITTICS
            door.frame = 0
            door.line = line
        }
    }

    companion object {
        val KEY_SLIDEDOORS: ContextKey<SlideDoors> = ActionTrait.ACTION_KEY_CHAIN.newKey<SlideDoors>(
            ActionsSlideDoors::class.java, Supplier { SlideDoors() })

        // UNUSED
        // Separate into p_slidoor.c?
        // ABANDONED TO THE MISTS OF TIME!!!
        //
        // EV_SlidingDoor : slide a door horizontally
        // (animate midtexture, then set noblocking line)
        //
        const val MAXSLIDEDOORS = 5

        // how many frames of animation
        const val SNUMFRAMES = 4
        const val SDOORWAIT = 35 * 3
        const val SWAITTICS = 4
        val slideFrameNames = arrayOf(
            slidename_t(
                "GDOORF1", "GDOORF2", "GDOORF3", "GDOORF4",  // front
                "GDOORB1", "GDOORB2", "GDOORB3", "GDOORB4" // back
            ),
            slidename_t(), slidename_t(), slidename_t(), slidename_t()
        )
    }
}