package savegame


import data.Limits
import data.info
import defines.GameMode
import doom.*
import doom.SourceCode.P_SaveG
import m.Settings
import mochadoom.Engine
import mochadoom.Loggers
import p.*
import p.Actions.ActionsLights.glow_t
import p.Actions.ActionsLights.lightflash_t
import rr.line_t
import rr.sector_t
import rr.side_t
import s.*
import utils.C2JUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.function.Consumer
import java.util.logging.Level

class VanillaDSG<T, V>(val DOOM: DoomMain<T, V>) : IDoomSaveGame {
    var header: VanillaDSGHeader? = null
    override fun setThinkerList(li: ThinkerList?) {
        // TODO Auto-generated method stub
    }

    override fun getHeader(): IDoomSaveGameHeader? {
        return header
    }

    override fun setHeader(header: IDoomSaveGameHeader?) {
        this.header = header as VanillaDSGHeader?
    }

    private lateinit var f: DataInputStream
    private lateinit var fo: DataOutputStream
    private var maxsize = 0
    override fun doLoad(f: DataInputStream): Boolean {
        return try {
            this.f = f
            maxsize = f.available()
            println("Max size $maxsize")
            header = VanillaDSGHeader()
            header!!.read(f)
            UnArchivePlayers()
            UnArchiveWorld()
            UnArchiveThinkers()
            UnArchiveSpecials()
            val terminator = f.readByte()
            terminator.toInt() == 0x1D
        } catch (e: IOException) {
            Loggers.getLogger(VanillaDSG::class.java.name)
                .log(Level.WARNING, e) { String.format("Error while loading savegame! Cause: %s", e.message) }
            false // Needed to shut up compiler.
        }
    }

    /**
     * P_UnArchivePlayers
     *
     * @throws IOException
     */
    @P_SaveG.C(P_SaveG.P_UnArchivePlayers)
    @Throws(IOException::class)
    protected fun UnArchivePlayers() {
        var i: Int
        var j: Int
        i = 0
        while (i < Limits.MAXPLAYERS) {

            // Multiplayer savegames are different!
            if (!DOOM.playeringame[i]) {
                i++
                continue
            }
            PADSAVEP(f, maxsize) // this will move us on the 52th byte, instead of 50th.
            DOOM.players[i].read(f)

            //memcpy (&players[i],save_p, sizeof(player_t));
            //save_p += sizeof(player_t);
            // will be set when unarc thinker
            DOOM.players[i].mo = null
            DOOM.players[i].message = null
            DOOM.players[i].attacker = null
            j = 0
            while (j < player_t.NUMPSPRITES) {
                if (C2JUtils.eval(DOOM.players[i].psprites[j].state)) {
                    // MAES HACK to accomoadate state_t type punning a-posteriori
                    DOOM.players[i].psprites[j].state = info.states[DOOM.players[i].psprites[j].readstate]
                }
                j++
            }
            i++
        }
    }

    /**
     * P_ArchivePlayers
     *
     * @throws IOException
     */
    @P_SaveG.C(P_SaveG.P_ArchivePlayers)
    @Throws(IOException::class)
    protected fun ArchivePlayers() {
        for (i in 0 until Limits.MAXPLAYERS) {
            // Multiplayer savegames are different!
            if (!DOOM.playeringame[i]) {
                continue
            }
            PADSAVEP(fo) // this will move us on the 52th byte, instead of 50th.

            // State will have to be serialized when saving.
            DOOM.players[i].write(fo)

            //System.out.printf("Player %d has mobj hashcode %d",(1+i),DS.players[i].mo.hashCode());
        }
    }

    //
    //P_ArchiveWorld
    //
    @P_SaveG.C(P_SaveG.P_ArchiveWorld)
    @Throws(IOException::class)
    protected fun ArchiveWorld() {
        var i: Int
        var j: Int
        var sec: sector_t
        var li: line_t
        var si: side_t

        // do sectors (allocate 14 bytes per sector)
        var buffer = ByteBuffer.allocate(DOOM.levelLoader.numsectors * 14)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        deAdaptSectors()
        i = 0
        while (i < DOOM.levelLoader.numsectors) {
            sec = DOOM.levelLoader.sectors[i]
            // MAES: sectors are actually carefully
            // marshalled, so we don't just read/write
            // their entire memory footprint to disk.
            sec.pack(buffer)
            i++
        }
        adaptSectors()
        fo!!.write(buffer.array(), 0, buffer.position())

        // do lines 
        // Allocate for the worst-case scenario (6+20 per line)
        buffer = ByteBuffer.allocate(DOOM.levelLoader.numlines * (6 + 20))
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(0)

        //final side_t test1=new side_t(0x11111111,0x11111111,(short) 0x1111,(short)0x1111,(short)0x1111,null);
        //final side_t test2=new side_t(0x22222222,0x22222222,(short) 0x2222,(short)0x2222,(short)0x2222,null);
        i = 0
        while (i < DOOM.levelLoader.numlines) {
            li = DOOM.levelLoader.lines[i]
            li.pack(buffer)
            j = 0
            while (j < 2) {
                if (li.sidenum[j] == line_t.NO_INDEX) {
                    j++
                    continue
                }
                si = DOOM.levelLoader.sides[li.sidenum[j].code]
                si.pack(buffer)
                j++
            }
            i++
        }
        val write = buffer.position()
        fo!!.write(buffer.array(), 0, write)
    }

    //
    //P_UnArchiveWorld
    //
    @P_SaveG.C(P_SaveG.P_UnArchiveWorld)
    @Throws(IOException::class)
    protected fun UnArchiveWorld() {
        var i: Int
        var j: Int
        var sec: sector_t
        var li: line_t
        var si: side_t
        // short      get;
        //get = (short *)save_p;

        //List<sector_t> sectors=new ArrayList<sector_t>();
        // do sectors
        i = 0
        while (i < DOOM.levelLoader.numsectors) {
            sec = DOOM.levelLoader.sectors[i]
            // MAES: sectors were actually carefully
            // unmarshalled, so we don't just read/write
            // their entire memory footprint to disk.
            sec.read(f)
            sec.specialdata = null
            sec.soundtarget = null
            i++
        }
        adaptSectors()
        // do lines
        i = 0
        while (i < DOOM.levelLoader.numlines) {
            li = DOOM.levelLoader.lines[i]
            // MAES: something similar occurs with lines, too.
            li.read(f)
            //System.out.println("Line "+i+": "+li);
            //System.out.print(i+ " {");
            j = 0
            while (j < 2) {

                //  System.out.print(li.sidenum[j]);
                //  if (j<2) System.out.print(",");
                //   System.out.printf("Skipped sidenum %d for line %d\n",j,i);
                if (li.sidenum[j] == line_t.NO_INDEX) {
                    //        System.out.printf("Skipped sidenum %d for line %d\n",j,i);
                    j++
                    continue
                }
                // Similarly, sides also get a careful unmarshalling even
                // in vanilla. No "dumb" block reads here.
                si = DOOM.levelLoader.sides[li.sidenum[j].code]
                si.read(f)
                j++
            }
            i++
        }
    }

    /**
     * Convert loaded sectors from vanilla savegames into the internal,
     * continuous index progression, by intercepting breaks corresponding to markers.
     */
    protected fun adaptSectors() {
        var sec: sector_t
        when (DOOM.getGameMode()) {
            GameMode.registered, GameMode.shareware -> {
                var i = 0
                while (i < DOOM.levelLoader.numsectors) {
                    sec = DOOM.levelLoader.sectors[i]
                    // Between the F1_START and F1_END mark (in vanilla)
                    sec.floorpic = if (sec.floorpic <= 54) {
                        (sec.floorpic - 1)
                    } else {
                        // Between the F2_START and F2_END mark (in vanilla)
                        (sec.floorpic - 3)
                    }.toShort()
                    sec.ceilingpic = if (sec.ceilingpic <= 54) {
                        (sec.ceilingpic - 1)
                    } else {
                        // Between the F2_START and F2_END mark (in vanilla)
                        (sec.ceilingpic - 3)
                    }.toShort()
                    i++
                }
            }
            GameMode.commercial, GameMode.pack_plut, GameMode.pack_tnt -> {
                var i = 0
                while (i < DOOM.levelLoader.numsectors) {
                    sec = DOOM.levelLoader.sectors[i]
                    // Between the F1_START and F1_END mark (in vanilla)
                    sec.floorpic = if (sec.floorpic <= 54) {
                        (sec.floorpic - 1).toShort()
                    } else if (sec.floorpic <= 99) {
                        // Between the F2_START and F2_END mark (in vanilla)
                        (sec.floorpic - 3)
                    } else {
                        (sec.floorpic - 5)
                    }.toShort()
                    sec.ceilingpic = if (sec.ceilingpic <= 54) {
                        (sec.ceilingpic - 1).toShort()
                    } else if (sec.ceilingpic <= 99) {
                        // Between the F2_START and F2_END mark (in vanilla)
                        (sec.ceilingpic - 3)
                    } else {
                        (sec.ceilingpic - 5)
                    }.toShort()
                    i++
                }
            }
            else -> {}
        }
    }

    /**
     * De-convert sectors from an absolute to a vanilla-like index
     * progression, by adding proper skips
     */
    protected fun deAdaptSectors() {
        var sec: sector_t
        when (DOOM.getGameMode()) {
            GameMode.registered, GameMode.shareware -> {
                var i = 0
                while (i < DOOM.levelLoader.numsectors) {
                    sec = DOOM.levelLoader.sectors[i]
                    // Between the F1_START and F1_END mark (in vanilla)
                    sec.floorpic = if (sec.floorpic < 54) {
                        (sec.floorpic + 1).toShort()
                    } else {
                        // Between the F2_START and F2_END mark (in vanilla)
                        (sec.floorpic + 3).toShort()
                    }
                    sec.ceilingpic = if (sec.ceilingpic < 54) {
                        (sec.ceilingpic + 1).toShort()
                    } else {
                        // Between the F2_START and F2_END mark (in vanilla)
                        (sec.ceilingpic + 3).toShort()
                    }
                    i++
                }
            }
            GameMode.commercial, GameMode.pack_plut, GameMode.pack_tnt -> {
                var i = 0
                while (i < DOOM.levelLoader.numsectors) {
                    sec = DOOM.levelLoader.sectors[i]
                    // Between the F1_START and F1_END mark (in vanilla)
                    sec.floorpic = if (sec.floorpic < 54) {
                        (sec.floorpic + 1).toShort()
                    } else if (sec.floorpic < 99) {
                        // Between the F2_START and F2_END mark (in vanilla)
                        (sec.floorpic + 3).toShort()
                    } else {
                        (sec.floorpic + 5).toShort()
                    }
                    sec.ceilingpic = if (sec.ceilingpic < 54) {
                        (sec.ceilingpic + 1).toShort()
                    } else if (sec.ceilingpic < 99) {
                        // Between the F2_START and F2_END mark (in vanilla)
                        (sec.ceilingpic + 3).toShort()
                    } else {
                        (sec.ceilingpic + 5).toShort()
                    }
                    i++
                }
            }
            else -> {}
        }
    }

    //
    //Thinkers
    //
    protected enum class thinkerclass_t {
        tc_end, tc_mobj
    }

    var TL: MutableList<mobj_t> = ArrayList()

    //
    //P_ArchiveThinkers
    //
    @P_SaveG.C(P_SaveG.P_ArchiveThinkers)
    @Throws(IOException::class)
    protected fun ArchiveThinkers() {
        var th: thinker_t
        var mobj: mobj_t

        // save off the current thinkers
        th = DOOM.actions.getThinkerCap().next!!
        while (th !== DOOM.actions.getThinkerCap()) {
            if (th.thinkerFunction != null && th.thinkerFunction == ActiveStates.P_MobjThinker) {
                // Indicate valid thinker
                fo!!.writeByte(thinkerclass_t.tc_mobj.ordinal)
                // Pad...
                PADSAVEP(fo)
                mobj = th as mobj_t
                mobj.write(fo)

                // MAES: state is explicit in state.id
                // save_p += sizeof(*mobj);
                // mobj->state = (state_t *)(mobj->state - states);
                // MAES: player is automatically generated at runtime and handled by the writer.
                //if (mobj->player)
                //mobj->player = (player_t *)((mobj->player-players) + 1);
            }
            th = th.next!!
        }

        // add a terminating marker
        fo!!.writeByte(thinkerclass_t.tc_end.ordinal)
    }

    //
    //P_UnArchiveThinkers
    //
    @P_SaveG.C(P_SaveG.P_UnArchiveThinkers)
    @Throws(IOException::class)
    protected fun UnArchiveThinkers() {
        var tclass: thinkerclass_t // was "byte", therefore unsigned
        var currentthinker: thinker_t?
        var next: thinker_t?
        var mobj: mobj_t
        var id = 0

        // remove all the current thinkers
        currentthinker = DOOM.actions.getThinkerCap().next
        while (currentthinker != null && currentthinker !== DOOM.actions.getThinkerCap()) {
            next = currentthinker.next
            if (currentthinker.thinkerFunction == ActiveStates.P_MobjThinker) {
                DOOM.actions.RemoveMobj(currentthinker as mobj_t)
            } // else {
            //currentthinker.next.prev=currentthinker.prev;
            //currentthinker.prev.next=currentthinker.next;
            //currentthinker = null;
            //}
            currentthinker = next
        }
        DOOM.actions.InitThinkers()

        // read in saved thinkers
        var end = false
        while (!end) {
            val tmp = f!!.readUnsignedByte()
            tclass = VanillaDSG.thinkerclass_t.values()[tmp]
            when (tclass) {
                thinkerclass_t.tc_end ->                     // That's how we know when to stop.
                    end = true
                thinkerclass_t.tc_mobj -> {
                    PADSAVEP(f, maxsize)
                    mobj = mobj_t.createOn(DOOM)
                    mobj.read(f)
                    mobj.id = ++id
                    TL.add(mobj)
                    mobj.mobj_state = info.states[mobj.stateid]
                    mobj.target = null
                    if (mobj.playerid != 0) {
                        mobj.player = DOOM.players[mobj.playerid - 1]
                        mobj.player!!.mo = mobj
                    }
                    DOOM.levelLoader.SetThingPosition(mobj)
                    mobj.info = info.mobjinfo[mobj.type!!.ordinal]
                    mobj.floorz = mobj.subsector!!.sector!!.floorheight
                    mobj.ceilingz = mobj.subsector!!.sector!!.ceilingheight
                    mobj.thinkerFunction = ActiveStates.P_MobjThinker
                    DOOM.actions.AddThinker(mobj)
                }
                else -> DOOM.doomSystem.Error("Unknown tclass %d in savegame", tclass)
            }
        }
        if (Engine.getConfig().equals(Settings.reconstruct_savegame_pointers, java.lang.Boolean.TRUE)) {
            reconstructPointers()
            rewirePointers()
        }
    }

    val pointindex = HashMap<Int, mobj_t>()

    /**
     * Allows reconstructing infighting targets from stored pointers/indices.
     * Works even with vanilla savegames as long as whatever it is that you
     * store is unique. A good choice would be progressive indices or hash values.
     *
     */
    protected fun reconstructPointers() {
        var player = 0
        for (th in TL) {
            if (th.player != null) {
                player = th.id
                // Player found, so that's our first key.
                pointindex[th.player!!.p_mobj] = th
            }
        }
        if (player == 0) {
            Loggers.getLogger(VanillaDSG::class.java.name).log(
                Level.WARNING,
                "Player not found, cannot reconstruct pointers!"
            )
            return
        }
        var curr: Int // next or prev index

        // We start from the player's index, if found.
        // We subtract -1 so it matches that inside the thinkers list.
        for (i in player - 1 until TL.size - 1) {
            // Get "next" pointer.
            curr = TL[i].nextid
            pointindex[curr] = TL[i + 1]
        }

        // We also search backwards, in case player wasn't first object
        // (can this even happen, in vanilla?)
        // -1 so it matches that of the TL list.
        for (i in player - 1 downTo 1) {
            // Get "prev" pointer.
            curr = TL[i].previd
            pointindex[curr] = TL[i - 1]
        }
    }

    /**
     * Allows reconstructing infighting targets from stored pointers/indices from
     * the hashtable created by reconstructPointers.
     *
     */
    protected fun rewirePointers() {
        TL.forEach(Consumer { th: mobj_t ->
            if (th.p_target != 0) {
                th.target = pointindex[th.p_target]
                th.tracer = pointindex[th.p_tracer]
                // System.out.printf("Object %s has target %s\n",th.type.toString(),th.target.type.toString());
            }
        })
    }

    protected enum class specials_e {
        tc_ceiling, tc_door, tc_floor, tc_plat, tc_flash, tc_strobe, tc_glow, tc_endspecials
    }

    //
    //P_ArchiveSpecials
    //
    @P_SaveG.C(P_SaveG.P_ArchiveSpecials)
    @Throws(IOException::class)
    protected fun ArchiveSpecials() {
        var ceiling: ceiling_t
        var door: vldoor_t
        var floor: floormove_t
        var plat: plat_t
        var flash: lightflash_t
        var strobe: strobe_t
        var glow: glow_t
        var i: Int

        // Most of these objects are quite hefty, but estimating 128 bytes tops
        // for each should do (largest one is 56);
        val buffer = ByteBuffer.allocate(128)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // save off the current thinkers
        var th = DOOM.actions.getThinkerCap().next!!
        while (th !== DOOM.actions.getThinkerCap()) {


            // Write out any pending objects.
            if (buffer.position() > 0) {
                fo!!.write(buffer.array(), 0, buffer.position())
                //System.out.println("Wrote out "+buffer.position()+" bytes");
            }

            // Back to the beginning.
            buffer.position(0)

            // So ceilings don't think?
            if (th.thinkerFunction == null) {
                // i maintains status between iterations
                i = 0
                while (i < DOOM.actions.getMaxCeilings()) {
                    if (th is ceiling_t && DOOM.actions.getActiveCeilings()[i] === th) {
                        break
                    }
                    i++
                }
                if (i < Limits.MAXCEILINGS) {
                    fo!!.writeByte(specials_e.tc_ceiling.ordinal)
                    PADSAVEP(fo)
                    // Set id for saving        
                    ceiling = th as ceiling_t
                    ceiling.sectorid = ceiling.sector!!.id
                    ceiling.pack(buffer)
                }
                th = th.next!!
                continue
            }

            // Well, apparently some do.
            if (th.thinkerFunction == ActiveStates.T_MoveCeiling) {
                fo!!.writeByte(specials_e.tc_ceiling.ordinal)
                PADSAVEP(fo)
                ceiling = th as ceiling_t
                ceiling.sectorid = ceiling.sector!!.id
                ceiling.pack(buffer)
                th = th.next!!
                continue
            }

            // Well, apparently some do.
            if (th.thinkerFunction == ActiveStates.T_VerticalDoor) {
                fo!!.writeByte(specials_e.tc_door.ordinal)
                PADSAVEP(fo)
                door = th as vldoor_t
                door.sectorid = door.sector!!.id
                door.pack(buffer)
                th = th.next!!
                continue
            }

            // Well, apparently some do.
            if (th.thinkerFunction == ActiveStates.T_MoveFloor) {
                fo!!.writeByte(specials_e.tc_floor.ordinal)
                PADSAVEP(fo)
                floor = th as floormove_t
                floor.sectorid = floor.sector!!.id
                floor.pack(buffer)
                th = th.next!!
                continue
            }

            // Well, apparently some do.
            if (th.thinkerFunction == ActiveStates.T_PlatRaise) {
                fo!!.writeByte(specials_e.tc_plat.ordinal)
                PADSAVEP(fo)
                plat = th as plat_t
                plat.sectorid = plat.sector!!.id
                plat.pack(buffer)
                th = th.next!!
                continue
            }

            // Well, apparently some do.
            if (th.thinkerFunction == ActiveStates.T_LightFlash) {
                fo!!.writeByte(specials_e.tc_flash.ordinal)
                PADSAVEP(fo)
                flash = th as lightflash_t
                flash.sectorid = flash.sector!!.id
                flash.pack(buffer)
                th = th.next!!
                continue
            }

            // Well, apparently some do.
            if (th.thinkerFunction == ActiveStates.T_StrobeFlash) {
                fo!!.writeByte(specials_e.tc_strobe.ordinal)
                PADSAVEP(fo)
                strobe = th as strobe_t
                strobe.sectorid = strobe.sector!!.id
                strobe.pack(buffer)
                th = th.next!!
                continue
            }

            // Well, apparently some do.
            if (th.thinkerFunction == ActiveStates.T_Glow) {
                fo!!.writeByte(specials_e.tc_glow.ordinal)
                PADSAVEP(fo)
                glow = th as glow_t
                glow.sectorid = glow.sector!!.id
                glow.pack(buffer)
            }
            th = th.next!!
        }
        if (buffer.position() > 0) {
            fo!!.write(buffer.array(), 0, buffer.position())
        }

        // Finito!
        fo!!.writeByte(specials_e.tc_endspecials.ordinal.toByte().toInt())
    }

    //
    //P_UnArchiveSpecials
    //
    @P_SaveG.C(P_SaveG.P_UnArchiveSpecials)
    @Throws(IOException::class)
    protected fun UnArchiveSpecials() {
        var tclass: specials_e
        var ceiling: ceiling_t
        var door: vldoor_t
        var floor: floormove_t
        var plat: plat_t
        var flash: lightflash_t
        var strobe: strobe_t
        var glow: glow_t

        //List<thinker_t> A=new ArrayList<thinker_t>();
        DOOM.actions.ClearPlatsBeforeLoading()
        DOOM.actions.ClearCeilingsBeforeLoading()

        // read in saved thinkers
        while (true) {
            val tmp = f!!.readUnsignedByte()
            //tmp&=0x00ff; // To "unsigned byte"
            tclass = VanillaDSG.specials_e.values()[tmp]
            when (tclass) {
                specials_e.tc_endspecials -> return  // end of list
                specials_e.tc_ceiling -> {
                    PADSAVEP(f, maxsize)
                    ceiling = ceiling_t()
                    ceiling.read(f)
                    ceiling.sector = DOOM.levelLoader.sectors[ceiling.sectorid]
                    ceiling.sector!!.specialdata = ceiling
                    if (ceiling.functionid != 0) {
                        ceiling.thinkerFunction = ActiveStates.T_MoveCeiling
                    }
                    DOOM.actions.AddThinker(ceiling)
                    DOOM.actions.AddActiveCeiling(ceiling)
                }
                specials_e.tc_door -> {
                    PADSAVEP(f, maxsize)
                    door = vldoor_t()
                    door.read(f)
                    door.sector = DOOM.levelLoader.sectors[door.sectorid]
                    door.sector!!.specialdata = door
                    door.thinkerFunction = ActiveStates.T_VerticalDoor
                    DOOM.actions.AddThinker(door)
                }
                specials_e.tc_floor -> {
                    PADSAVEP(f, maxsize)
                    floor = floormove_t()
                    floor.read(f)
                    floor.sector = DOOM.levelLoader.sectors[floor.sectorid]
                    floor.sector!!.specialdata = floor
                    floor.thinkerFunction = ActiveStates.T_MoveFloor
                    DOOM.actions.AddThinker(floor)
                }
                specials_e.tc_plat -> {
                    PADSAVEP(f, maxsize)
                    plat = plat_t()
                    plat.read(f)
                    plat.sector = DOOM.levelLoader.sectors[plat.sectorid]
                    plat.sector!!.specialdata = plat
                    if (plat.functionid != 0) {
                        plat.thinkerFunction = ActiveStates.T_PlatRaise
                    }
                    DOOM.actions.AddThinker(plat)
                    DOOM.actions.AddActivePlat(plat)
                }
                specials_e.tc_flash -> {
                    PADSAVEP(f, maxsize)
                    flash = lightflash_t()
                    flash.read(f)
                    flash.sector = DOOM.levelLoader.sectors[flash.sectorid]
                    flash.thinkerFunction = ActiveStates.T_LightFlash
                    DOOM.actions.AddThinker(flash)
                }
                specials_e.tc_strobe -> {
                    PADSAVEP(f, maxsize)
                    strobe = strobe_t()
                    strobe.read(f)
                    strobe.sector = DOOM.levelLoader.sectors[strobe.sectorid]
                    strobe.thinkerFunction = ActiveStates.T_StrobeFlash
                    DOOM.actions.AddThinker(strobe)
                }
                specials_e.tc_glow -> {
                    PADSAVEP(f, maxsize)
                    glow = glow_t()
                    glow.read(f)
                    glow.sector = DOOM.levelLoader.sectors[glow.sectorid]
                    glow.thinkerFunction = ActiveStates.T_Glow
                    DOOM.actions.AddThinker(glow)
                }
                else -> DOOM.doomSystem.Error("P_UnarchiveSpecials:Unknown tclass %d in savegame", tmp)
            }
        }
    }

    /**
     * Pads save_p to a 4-byte boundary
     * so that the load/save works on SGI&Gecko.
     *
     * @param save_p
     */
    protected fun PADSAVEP(save_p: Int): Int {
        return save_p + (4 - (save_p and 3) and 3)
    }

    //protected final int PADSAVEP(ByteBuffer b, int save_p){
    //    ByteBuffer
    //    return (save_p += (4 - ((int) save_p & 3)) & 3);
    //}
    @Throws(IOException::class)
    protected fun PADSAVEP(f: DataInputStream?, maxsize: Int): Long {
        val save_p = (maxsize - f!!.available()).toLong()
        val padding = 4 - (save_p.toInt() and 3) and 3
        // System.out.printf("Current position %d Padding by %d bytes %d\n",save_p,padding,maxsize);        
        f.skip(padding.toLong())
        return padding.toLong()
    }

    @Throws(IOException::class)
    protected fun PADSAVEP(f: DataOutputStream?): Long {
        val save_p = f!!.size().toLong()
        val padding = 4 - (save_p.toInt() and 3) and 3
        // System.out.printf("Current position %d Padding by %d bytes\n",save_p,padding);
        for (i in 0 until padding) {
            f.write(0)
        }
        return padding.toLong()
    }

    override fun doSave(f: DataOutputStream): Boolean {
        try {
            // The header must have been set, at this point.
            fo = f
            //f.setLength(0); // Kill old info.
            header!!.write(f)

            //header.read(f);
            ArchivePlayers()
            ArchiveWorld()
            ArchiveThinkers()
            ArchiveSpecials()
            // TODO: the rest...
            f.write(0x1D)
        } catch (e: IOException) {
            Loggers.getLogger(VanillaDSG::class.java.name)
                .log(Level.WARNING, e) { String.format("Error while saving savegame! Cause: %s", e.message) }
            return false // Needed to shut up compiler.
        }
        return true
    }
}