package doom


import data.*
import data.sounds.sfxenum_t
import defines.ammotype_t
import defines.card_t
import defines.skill_t
import defines.statenum_t
import doom.SourceCode.Compatible
import doom.SourceCode.G_Game
import doom.SourceCode.P_Pspr
import m.fixed_t.Companion.FRACBITS
import m.fixed_t.Companion.FRACUNIT
import m.fixed_t.Companion.FixedMul
import m.fixed_t.Companion.MAPFRACUNIT
import p.PlayerSpriteActiveStates
import p.PlayerSpriteConsumer
import p.mobj_t
import p.pspdef_t
import rr.sector_t
import s.*
import utils.C2JUtils
import utils.GenericCopy
import v.graphics.Lights
import w.DoomBuffer
import w.DoomIO.readBooleanIntArray
import w.IPackableDoomObject
import w.IReadableDoomObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Extended player object info: player_t The player data structure depends on a
 * number of other structs: items (internal inventory), animation states
 * (closely tied to the sprites used to represent them, unfortunately).
 *
 * #include "d_items.h"
 * #include "p_pspr.h"
 *
 * In addition, the player is just a special
 * case of the generic moving object/actor.
 * NOTE: this doesn't mean it needs to extend it, although it would be
 * possible.
 *
 * #include "p_mobj.h"
 *
 * Finally, for odd reasons, the player input is buffered within
 * the player data struct, as commands per game tick.
 *
 * #include "d_ticcmd.h"
 */
class player_t /*extends mobj_t */(
    /**
     * Probably doomguy needs to know what the fuck is going on
     */
    private val DOOM: DoomMain<*, *>
) : Cloneable, IReadableDoomObject, IPackableDoomObject {
    /**
     * The "mobj state" of the player is stored here, even though he "inherits"
     * all mobj_t properties (except being a thinker). However, for good or bad,
     * his mobj properties are modified by accessing player.mo
     */
    var mo: mobj_t? = null

    /**
     * playerstate_t
     */
    var playerstate = 0
    var cmd: ticcmd_t

    /**
     * Determine POV, including viewpoint bobbing during movement. (fixed_t)
     * Focal origin above r.z
     */
    var viewz = 0

    /**
     * (fixed_t) Base height above floor for viewz.
     */
    var viewheight = 0

    /**
     * (fixed_t) Bob/squat speed.
     */
    var deltaviewheight = 0

    /**
     * (fixed_t) bounded/scaled total momentum.
     */
    var bob = 0

    // Heretic stuff
    var flyheight = 0
    var lookdir = 0
    var centering = false

    /**
     * This is only used between levels, mo->health is used during levels.
     * CORRECTION: this is also used by the automap widget.
     * MAES: fugly hax, as even passing "Integers" won't work, as they are immutable.
     * Fuck that, I'm doing it the fugly MPI Java way!
     */
    var health = IntArray(1)

    /**
     * has to be passed around :-(
     */
    var armorpoints = IntArray(1)

    /**
     * Armor type is 0-2.
     */
    var armortype = 0

    /**
     * Power ups. invinc and invis are tic counters.
     */
    var powers: IntArray
    var cards: BooleanArray
    var backpack = false

    // Frags, kills of other players.
    var frags: IntArray
    var readyweapon: weapontype_t

    // Is wp_nochange if not changing.
    var pendingweapon: weapontype_t? = null
    var weaponowned: BooleanArray
    var ammo: IntArray
    var maxammo: IntArray

    /**
     * True if button down last tic.
     */
    var attackdown = false
    var usedown = false

    // Bit flags, for cheats and debug.
    // See cheat_t, above.
    var cheats = 0

    // Refired shots are less accurate.
    var refire = 0

    // For intermission stats.
    var killcount = 0
    var itemcount = 0
    var secretcount = 0

    // Hint messages.
    var message: String? = null

    // For screen flashing (red or bright).
    var damagecount = 0
    var bonuscount = 0

    // Who did damage (NULL for floors/ceilings).
    var attacker: mobj_t? = null

    // So gun flashes light up areas.
    var extralight = 0
    /**
     * Current PLAYPAL, ??? can be set to REDCOLORMAP for pain, etc. MAES: "int"
     * my ass. It's yet another pointer alias into colormaps. Ergo, array and
     * pointer.
     */
    // public byte[] fixedcolormap;
    /**
     * *NOT* preshifted index of colormap in light color maps.
     * It could be written when the player_t object is packed. Dont shift this value,
     * do shifts after retrieving this.
     */
    var fixedcolormap = 0

    // Player skin colorshift,
    // 0-3 for which color to draw player.
    var colormap = 0

    // TODO: Overlay view sprites (gun, etc).
    var psprites: Array<pspdef_t>

    // True if secret level has been done.
    var didsecret = false

    /**
     * It's probably faster to clone the null player
     */
    fun reset() {
        C2JUtils.memset(ammo, 0, ammo.size)
        C2JUtils.memset(armorpoints, 0, armorpoints.size)
        C2JUtils.memset(cards, false, cards.size)
        C2JUtils.memset(frags, 0, frags.size)
        C2JUtils.memset(health, 0, health.size)
        C2JUtils.memset(maxammo, 0, maxammo.size)
        C2JUtils.memset(powers, 0, powers.size)
        C2JUtils.memset(weaponowned, false, weaponowned.size)
        //memset(psprites, null, psprites.length);
        cheats = 0 // Forgot to clear up cheats flag...
        armortype = 0
        attackdown = false
        attacker = null
        backpack = false
        bob = 0
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): player_t {
        return super.clone() as player_t
    }

    /**
     * P_Thrust Moves the given origin along a given angle.
     *
     * @param angle
     * (angle_t)
     * @param move
     * (fixed_t)
     */
    fun Thrust(angle: Long, move: Int) {
        val mo = mo!!
        mo.momx += FixedMul(move, Tables.finecosine(angle))
        mo.momy += FixedMul(move, Tables.finesine(angle))
    }

    /**
     * P_MovePlayer
     */
    fun MovePlayer() {
        val cmd = cmd
        val mo = mo!!
        mo.angle += (cmd.angleturn.toInt() shl 16).toLong()
        mo.angle = mo.angle and Tables.BITS32

        // Do not let the player control movement
        // if not onground.
        onground = mo._z <= mo.floorz
        if (cmd.forwardmove.toInt() != 0 && onground) {
            Thrust(mo.angle, cmd.forwardmove * player_t.PLAYERTHRUST)
        }
        if (cmd.sidemove.toInt() != 0 && onground) {
            Thrust(mo.angle - Tables.ANG90 and Tables.BITS32, cmd.sidemove * player_t.PLAYERTHRUST)
        }
        if ((cmd.forwardmove.toInt() != 0 || cmd.sidemove.toInt() != 0)
            && mo.mobj_state === info.states[statenum_t.S_PLAY.ordinal]
        ) {
            mo.SetMobjState(statenum_t.S_PLAY_RUN1)
        }

        // Freelook code ripped off Heretic. Sieg heil!
        var look = cmd.lookfly.code and 15
        if (look > 7) {
            look -= 16
        }
        if (look != 0) {
            if (look == Defines.TOCENTER) {
                centering = true
            } else {
                lookdir += 5 * look
                if (lookdir > 90 || lookdir < -110) {
                    lookdir -= 5 * look
                }
            }
        }

        // Centering is done over several tics
        if (centering) {
            if (lookdir > 0) {
                lookdir -= 8
            } else if (lookdir < 0) {
                lookdir += 8
            }
            if (Math.abs(lookdir) < 8) {
                lookdir = 0
                centering = false
            }
        }
        /* Flight stuff from Heretic
    	fly = cmd.lookfly>>4;
    		
    	if(fly > 7)
    	{
    		fly -= 16;
    	}
    	if(fly && player->powers[pw_flight])
    	{
    		if(fly != TOCENTER)
    		{
    			player->flyheight = fly*2;
    			if(!(player->mo->flags2&MF2_FLY))
    			{
    				player->mo->flags2 |= MF2_FLY;
    				player->mo->flags |= MF_NOGRAVITY;
    			}
    		}
    		else
    		{
    			player->mo->flags2 &= ~MF2_FLY;
    			player->mo->flags &= ~MF_NOGRAVITY;
    		}
    	}
    	else if(fly > 0)
    	{
    		P_PlayerUseArtifact(player, arti_fly);
    	}
    	if(player->mo->flags2&MF2_FLY)
    	{
    		player->mo->momz = player->flyheight*FRACUNIT;
    		if(player->flyheight)
    		{
    			player->flyheight /= 2;
    		}
    	} */
    }

    /**
     * P_GiveAmmo Num is the number of clip loads, not the individual count (0=
     * 1/2 clip).
     *
     * @return false if the ammo can't be picked up at all
     * @param ammo
     * intended to be ammotype_t.
     */
    fun GiveAmmo(amm: ammotype_t, num: Int): Boolean {
        var num = num
        val oldammo: Int
        val ammo = amm.ordinal
        if (ammo == ammotype_t.am_noammo.ordinal) {
            return false
        }
        if (ammo < 0 || ammo > Defines.NUMAMMO) {
            DOOM.doomSystem.Error("P_GiveAmmo: bad type %i", ammo)
        }
        if (this.ammo[ammo] == maxammo[ammo]) {
            return false
        }
        if (num != 0) {
            num *= player_t.clipammo.get(ammo)
        } else {
            num = player_t.clipammo.get(ammo) / 2
        }
        if (DOOM.gameskill == skill_t.sk_baby
            || DOOM.gameskill == skill_t.sk_nightmare
        ) {
            // give double ammo in trainer mode,
            // you'll need in nightmare
            num = num shl 1
        }
        oldammo = this.ammo[ammo]
        this.ammo[ammo] += num
        if (this.ammo[ammo] > maxammo[ammo]) {
            this.ammo[ammo] = maxammo[ammo]
        }

        // If non zero ammo,
        // don't change up weapons,
        // player was lower on purpose.
        if (oldammo != 0) {
            return true
        }
        when (ammotype_t.values()[ammo]) {
            ammotype_t.am_clip -> if (readyweapon == weapontype_t.wp_fist) {
                pendingweapon = if (weaponowned[weapontype_t.wp_chaingun.ordinal]) {
                    weapontype_t.wp_chaingun
                } else {
                    weapontype_t.wp_pistol
                }
            }
            ammotype_t.am_shell -> if (readyweapon == weapontype_t.wp_fist
                || readyweapon == weapontype_t.wp_pistol
            ) {
                if (weaponowned[weapontype_t.wp_shotgun.ordinal]) {
                    pendingweapon = weapontype_t.wp_shotgun
                }
            }
            ammotype_t.am_cell -> if (readyweapon == weapontype_t.wp_fist
                || readyweapon == weapontype_t.wp_pistol
            ) {
                if (weaponowned[weapontype_t.wp_plasma.ordinal]) {
                    pendingweapon = weapontype_t.wp_plasma
                }
            }
            ammotype_t.am_misl -> if (readyweapon == weapontype_t.wp_fist) {
                if (weaponowned[weapontype_t.wp_missile.ordinal]) {
                    pendingweapon = weapontype_t.wp_missile
                }
            }
            else -> {}
        }
        return true
    }

    /**
     * P_GiveWeapon
     * The weapon name may have a MF_DROPPED flag ored in.
     */
    fun GiveWeapon(weapn: weapontype_t, dropped: Boolean): Boolean {
        val gaveammo: Boolean
        val gaveweapon: Boolean
        val weapon = weapn.ordinal
        if (DOOM.netgame && DOOM.deathmatch != true // ???? was "2"
            && !dropped
        ) {
            // leave placed weapons forever on net games
            if (weaponowned[weapon]) {
                return false
            }
            bonuscount += player_t.BONUSADD
            weaponowned[weapon] = true
            if (DOOM.deathmatch) {
                GiveAmmo(items.weaponinfo[weapon].ammo, 5)
            } else {
                GiveAmmo(items.weaponinfo[weapon].ammo, 2)
            }
            pendingweapon = weapn
            if (this === DOOM.players[DOOM.consoleplayer]) {
                DOOM.doomSound.StartSound(null, sfxenum_t.sfx_wpnup)
            }
            return false
        }
        gaveammo = if (items.weaponinfo[weapon].ammo != ammotype_t.am_noammo) {
            // give one clip with a dropped weapon,
            // two clips with a found weapon
            if (dropped) {
                GiveAmmo(items.weaponinfo[weapon].ammo, 1)
            } else {
                GiveAmmo(items.weaponinfo[weapon].ammo, 2)
            }
        } else {
            false
        }
        if (weaponowned[weapon]) {
            gaveweapon = false
        } else {
            gaveweapon = true
            weaponowned[weapon] = true
            pendingweapon = weapn
        }
        return gaveweapon || gaveammo
    }

    /**
     * P_GiveBody Returns false if the body isn't needed at all
     */
    fun GiveBody(num: Int): Boolean {
        if (health[0] >= Limits.MAXHEALTH) {
            return false
        }
        health[0] += num
        if (health[0] > Limits.MAXHEALTH) {
            health[0] = Limits.MAXHEALTH
        }
        mo!!.health = health[0]
        return true
    }

    /**
     * P_GiveArmor Returns false if the armor is worse than the current armor.
     */
    fun GiveArmor(armortype: Int): Boolean {
        val hits: Int
        hits = armortype * 100
        if (armorpoints[0] >= hits) {
            return false // don't pick up
        }
        this.armortype = armortype
        armorpoints[0] = hits
        return true
    }

    /**
     * P_GiveCard
     */
    fun GiveCard(crd: card_t) {
        val card = crd.ordinal
        if (cards[card]) {
            return
        }
        bonuscount = player_t.BONUSADD
        cards[card] = true
    }

    //
    // P_GivePower
    //
    fun GivePower(   /* powertype_t */power: Int): Boolean // MAES:
    // I
    // didn't
    // change
    // this!
    {
        if (power == Defines.pw_invulnerability) {
            powers[power] = Defines.INVULNTICS
            return true
        }
        if (power == Defines.pw_invisibility) {
            powers[power] = Defines.INVISTICS
            val mo = mo!!
            mo.flags = mo.flags or mobj_t.MF_SHADOW
            return true
        }
        if (power == Defines.pw_infrared) {
            powers[power] = Defines.INFRATICS
            return true
        }
        if (power == Defines.pw_ironfeet) {
            powers[power] = Defines.IRONTICS
            return true
        }
        if (power == Defines.pw_strength) {
            GiveBody(100)
            powers[power] = 1
            return true
        }
        if (powers[power] != 0) {
            return false // already got it
        }
        powers[power] = 1
        return true
    }

    /**
     * G_PlayerFinishLevel
     * Called when a player completes a level.
     */
    @Compatible
    @G_Game.C(G_Game.G_PlayerFinishLevel)
    fun PlayerFinishLevel() {
        C2JUtils.memset(powers, 0, powers.size)
        C2JUtils.memset(cards, false, cards.size)
        val mo = mo!!
        mo.flags = mo.flags and mobj_t.MF_SHADOW.inv() // cancel invisibility
        extralight = 0 // cancel gun flashes 
        fixedcolormap = Lights.COLORMAP_FIXED // cancel ir gogles 
        damagecount = 0 // no palette changes 
        bonuscount = 0
        lookdir = 0 // From heretic
    }

    /**
     * P_PlayerInSpecialSector
     * Called every tic frame
     * that the player origin is in a special sector
     */
    protected fun PlayerInSpecialSector() {
        val sector: sector_t
        val mo = mo!!
        sector = mo.subsector!!.sector!!

        // Falling, not all the way down yet?
        if (mo._z != sector.floorheight) {
            return
        }
        when (sector.special.toInt()) {
            5 ->                 // HELLSLIME DAMAGE
                if (powers[Defines.pw_ironfeet] == 0) {
                    if (!C2JUtils.flags(DOOM.leveltime, 0x1f)) {
                        DOOM.actions.DamageMobj(mo, null, null, 10)
                    }
                }
            7 ->                 // NUKAGE DAMAGE
                if (powers[Defines.pw_ironfeet] == 0) {
                    if (!C2JUtils.flags(DOOM.leveltime, 0x1f)) {
                        DOOM.actions.DamageMobj(mo, null, null, 5)
                    }
                }
            16, 4 ->                 // STROBE HURT
                if (!C2JUtils.eval(powers[Defines.pw_ironfeet]) || DOOM.random.P_Random() < 5) {
                    if (!C2JUtils.flags(DOOM.leveltime, 0x1f)) {
                        DOOM.actions.DamageMobj(mo, null, null, 20)
                    }
                }
            9 -> {
                // SECRET SECTOR
                secretcount++
                sector.special = 0
            }
            11 -> {
                // EXIT SUPER DAMAGE! (for E1M8 finale)
                cheats = cheats and player_t.CF_GODMODE.inv()
                if (!C2JUtils.flags(DOOM.leveltime, 0x1f)) {
                    DOOM.actions.DamageMobj(mo, null, null, 20)
                }
                if (health[0] <= 10) {
                    DOOM.ExitLevel()
                }
            }
            else -> DOOM.doomSystem.Error("P_PlayerInSpecialSector: unknown special %d", sector.special)
        }
    }

    //
    //P_CalcHeight
    //Calculate the walking / running height adjustment
    //
    fun CalcHeight() {
        val angle: Int
        val bob: Int // fixed

        // Regular movement bobbing
        // (needs to be calculated for gun swing
        // even if not on ground)
        // OPTIMIZE: tablify angle
        // Note: a LUT allows for effects
        //  like a ramp with low health.
        val mo = mo!!
        this.bob = (FixedMul(mo.momx, mo.momx)
                + FixedMul(mo.momy, mo.momy))
        this.bob = this.bob shr 2
        if (this.bob > player_t.MAXBOB) {
            this.bob = player_t.MAXBOB
        }
        if (C2JUtils.flags(cheats, player_t.CF_NOMOMENTUM) || !onground) {
            viewz = mo._z + Defines.VIEWHEIGHT
            if (viewz > mo.ceilingz - 4 * FRACUNIT) {
                viewz = mo.ceilingz - 4 * FRACUNIT
            }
            viewz = mo._z + viewheight
            return
        }
        angle = Tables.FINEANGLES / 20 * DOOM.leveltime and Tables.FINEMASK
        bob = FixedMul(this.bob / 2, Tables.finesine[angle])

        // move viewheight
        if (playerstate == Defines.PST_LIVE) {
            viewheight += deltaviewheight
            if (viewheight > Defines.VIEWHEIGHT) {
                viewheight = Defines.VIEWHEIGHT
                deltaviewheight = 0
            }
            if (viewheight < Defines.VIEWHEIGHT / 2) {
                viewheight = Defines.VIEWHEIGHT / 2
                if (deltaviewheight <= 0) {
                    deltaviewheight = 1
                }
            }
            if (deltaviewheight != 0) {
                deltaviewheight += FRACUNIT / 4
                if (deltaviewheight == 0) {
                    deltaviewheight = 1
                }
            }
        }
        viewz = mo._z + viewheight + bob
        if (viewz > mo.ceilingz - 4 * FRACUNIT) {
            viewz = mo.ceilingz - 4 * FRACUNIT
        }
    }

    /**
     * P_DeathThink
     * Fall on your face when dying.
     * Decrease POV height to floor height.
     *
     * DOOMGUY IS SO AWESOME THAT HE THINKS EVEN WHEN DEAD!!!
     *
     */
    fun DeathThink() {
        val angle: Long //angle_t
        val delta: Long
        MovePsprites()

        // fall to the ground
        if (viewheight > 6 * FRACUNIT) {
            viewheight -= FRACUNIT
        }
        if (viewheight < 6 * FRACUNIT) {
            viewheight = 6 * FRACUNIT
        }
        deltaviewheight = 0
        val mo = mo!!
        onground = mo._z <= mo.floorz
        CalcHeight()
        if (attacker != null && attacker !== mo) {
            angle = DOOM.sceneRenderer.PointToAngle2(
                mo._x,
                mo._y,
                attacker!!._x,
                attacker!!._y
            )
            delta = Tables.addAngles(angle, -mo.angle)
            if (delta < player_t.ANG5 || delta > -player_t.ANG5) {
                // Looking at killer,
                //  so fade damage flash down.
                mo.angle = angle
                if (damagecount != 0) {
                    damagecount--
                }
            } else if (delta < Tables.ANG180) {
                mo.angle += player_t.ANG5
            } else {
                mo.angle -= player_t.ANG5
            }
        } else if (damagecount != 0) {
            damagecount--
        }
        if (C2JUtils.flags(cmd.buttons.code, Defines.BT_USE)) {
            playerstate = Defines.PST_REBORN
        }
    }

    //
    // P_MovePsprites
    // Called every tic by player thinking routine.
    //
    fun MovePsprites() {
        var psp: pspdef_t
        var state:  // Shut up compiler
                state_t? = null
        for (i in 0 until player_t.NUMPSPRITES) {
            psp = psprites[i]
            // a null state means not active
            if (psp.state.also { state = it } != null) {
                // drop tic count and possibly change state

                // a -1 tic count never changes
                if (psp.tics != -1) {
                    psp.tics--
                    if (!C2JUtils.eval(psp.tics)) {
                        SetPsprite(i, psp.state!!.nextstate!!)
                    }
                }
            }
        }
        psprites[player_t.ps_flash].sx = psprites[player_t.ps_weapon].sx
        psprites[player_t.ps_flash].sy = psprites[player_t.ps_weapon].sy
    }

    /**
     * P_SetPsprite
     */
    @SourceCode.Exact
    @P_Pspr.C(P_Pspr.P_SetPsprite)
    fun SetPsprite(position: Int, newstate: statenum_t) {
        var newstate = newstate
        val psp: pspdef_t
        var state: state_t
        psp = psprites[position]
        do {
            if (!C2JUtils.eval(newstate)) {
                // object removed itself
                psp.state = null
                break
            }
            state = info.states[newstate.ordinal]
            psp.state = state
            psp.tics = state.tics // could be 0
            if (C2JUtils.eval(state.misc1)) {
                // coordinate set
                psp.sx = state.misc1 shl FRACBITS
                psp.sy = state.misc2 shl FRACBITS
            }

            // Call action routine.
            // Modified handling.
            if (state.action?.activeState is PlayerSpriteActiveStates) {
                (state.action!!.activeState as PlayerSpriteActiveStates).accept(DOOM.actions, PlayerSpriteConsumer(this, psp))
                if (!C2JUtils.eval(psp.state)) {
                    break
                }
            }
            newstate = psp.state!!.nextstate!!
        } while (!C2JUtils.eval(psp.tics))
        // an initial state of 0 could cycle through
    }

    /**
     * Accessory method to identify which "doomguy" we are.
     * Because we can't use the [target.player-players] syntax
     * in order to get an array index, in Java.
     *
     * If -1 is returned, then we have existential problems.
     *
     */
    fun identify(): Int {
        if (id >= 0) {
            return id
        }
        var i: Int
        // Let's assume that we know jack.
        i = 0
        while (i < DOOM.players.size) {
            if (this === DOOM.players[i]) {
                break
            }
            i++
        }
        return i.also { id = it }
    }

    private var id = -1
    private var onground = false
    /*
     P_SetPsprite
    
    
    public void
    SetPsprite
    ( player_t  player,
      int       position,
      statenum_t    newstate ) 
    {
        pspdef_t    psp;
        state_t state;
        
        psp = psprites[position];
        
        do
        {
        if (newstate==null)
        {
            // object removed itself
            psp.state = null;
            break;  
        }
        
        state = states[newstate.ordinal()];
        psp.state = state;
        psp.tics = (int) state.tics;    // could be 0

        if (state.misc1!=0)
        {
            // coordinate set
            psp.sx = (int) (state.misc1 << FRACBITS);
            psp.sy = (int) (state.misc2 << FRACBITS);
        }
        
        // Call action routine.
        // Modified handling.
        if (state.action.getType()==acp2)
        {
            P.A.dispatch(state.action,this, psp);
            if (psp.state==null)
            break;
        }
        
        newstate = psp.state.nextstate;
        
        } while (psp.tics==0);
        // an initial state of 0 could cycle through
    }
     */
    /**
     * fixed_t
     */
    var swingx = 0
    var swingy = 0

    /**
     * P_CalcSwing
     *
     * @param player
     */
    fun CalcSwing(player: player_t?) {
        val swing: Int // fixed_t
        var angle: Int

        // OPTIMIZE: tablify this.
        // A LUT would allow for different modes,
        //  and add flexibility.
        swing = bob
        angle = Tables.FINEANGLES / 70 * DOOM.leveltime and Tables.FINEMASK
        swingx = FixedMul(swing, Tables.finesine[angle])
        angle = Tables.FINEANGLES / 70 * DOOM.leveltime + Tables.FINEANGLES / 2 and Tables.FINEMASK
        swingy = -FixedMul(swingx, Tables.finesine[angle])
    }

    //
    // P_BringUpWeapon
    // Starts bringing the pending weapon up
    // from the bottom of the screen.
    // Uses player
    //
    @SourceCode.Exact
    @P_Pspr.C(P_Pspr.P_BringUpWeapon)
    fun BringUpWeapon() {
        val newstate: statenum_t
        if (pendingweapon == weapontype_t.wp_nochange) {
            pendingweapon = readyweapon
        }
        if (pendingweapon == weapontype_t.wp_chainsaw) {
            S_StartSound@ run {
                DOOM.doomSound.StartSound(mo, sfxenum_t.sfx_sawup)
            }
        }
        newstate = items.weaponinfo[pendingweapon!!.ordinal].upstate
        pendingweapon = weapontype_t.wp_nochange
        psprites[player_t.ps_weapon].sy = player_t.WEAPONBOTTOM
        P_SetPsprite@ run {
            SetPsprite(player_t.ps_weapon, newstate)
        }
    }

    /**
     * P_CheckAmmo
     * Returns true if there is enough ammo to shoot.
     * If not, selects the next weapon to use.
     */
    fun CheckAmmo(): Boolean {
        val ammo: ammotype_t
        val count: Int
        ammo = items.weaponinfo[readyweapon.ordinal].ammo

        // Minimal amount for one shot varies.
        count = if (readyweapon == weapontype_t.wp_bfg) {
            player_t.BFGCELLS
        } else if (readyweapon == weapontype_t.wp_supershotgun) {
            2 // Double barrel.
        } else {
            1 // Regular.
        }
        // Some do not need ammunition anyway.
        // Return if current ammunition sufficient.
        if (ammo == ammotype_t.am_noammo || this.ammo[ammo.ordinal] >= count) {
            return true
        }

        // Out of ammo, pick a weapon to change to.
        // Preferences are set here.
        do {
            pendingweapon =
                if (weaponowned[weapontype_t.wp_plasma.ordinal] && this.ammo[ammotype_t.am_cell.ordinal] != 0
                    && !DOOM.isShareware()
                ) {
                    weapontype_t.wp_plasma
                } else if (weaponowned[weapontype_t.wp_supershotgun.ordinal] && this.ammo[ammotype_t.am_shell.ordinal] > 2 && DOOM.isCommercial()) {
                    weapontype_t.wp_supershotgun
                } else if (weaponowned[weapontype_t.wp_chaingun.ordinal]
                    && this.ammo[ammotype_t.am_clip.ordinal] != 0
                ) {
                    weapontype_t.wp_chaingun
                } else if (weaponowned[weapontype_t.wp_shotgun.ordinal]
                    && this.ammo[ammotype_t.am_shell.ordinal] != 0
                ) {
                    weapontype_t.wp_shotgun
                } else if (this.ammo[ammotype_t.am_clip.ordinal] != 0) {
                    weapontype_t.wp_pistol
                } else if (weaponowned[weapontype_t.wp_chainsaw.ordinal]) {
                    weapontype_t.wp_chainsaw
                } else if (weaponowned[weapontype_t.wp_missile.ordinal]
                    && this.ammo[ammotype_t.am_misl.ordinal] != 0
                ) {
                    weapontype_t.wp_missile
                } else if (weaponowned[weapontype_t.wp_bfg.ordinal] && this.ammo[ammotype_t.am_cell.ordinal] > 40 && !DOOM.isShareware()) {
                    weapontype_t.wp_bfg
                } else {
                    // If everything fails.
                    weapontype_t.wp_fist
                }
        } while (pendingweapon == weapontype_t.wp_nochange)

        // Now set appropriate weapon overlay.
        SetPsprite(
            player_t.ps_weapon,
            items.weaponinfo[readyweapon.ordinal].downstate
        )
        return false
    }

    /**
     * P_DropWeapon
     * Player died, so put the weapon away.
     */
    fun DropWeapon() {
        SetPsprite(
            player_t.ps_weapon,
            items.weaponinfo[readyweapon.ordinal].downstate
        )
    }

    /**
     * P_SetupPsprites
     * Called at start of level for each
     */
    @SourceCode.Exact
    @P_Pspr.C(P_Pspr.P_SetupPsprites)
    fun SetupPsprites() {
        // remove all psprites
        for (i in 0 until player_t.NUMPSPRITES) {
            psprites[i].state = null
        }

        // spawn the gun
        pendingweapon = readyweapon
        BringUpWeapon()
    }
    /**
     * P_PlayerThink
     */
    /**
     * Called by Actions ticker
     */
    @JvmOverloads
    fun PlayerThink(player: player_t = this) {
        val cmd: ticcmd_t
        var newweapon: weapontype_t

        // fixme: do this in the cheat code
        val player_mo = player.mo!!
        if (C2JUtils.flags(player.cheats, player_t.CF_NOCLIP)) {
            player_mo.flags = player_mo.flags or mobj_t.MF_NOCLIP
        } else {
            player_mo.flags = player_mo.flags and mobj_t.MF_NOCLIP.inv()
        }

        // chain saw run forward
        cmd = player.cmd
        if (C2JUtils.flags(player_mo.flags, mobj_t.MF_JUSTATTACKED)) {
            cmd.angleturn = 0
            cmd.forwardmove = (0xc800 / 512).toByte()
            cmd.sidemove = 0
            player_mo.flags = player_mo.flags and mobj_t.MF_JUSTATTACKED.inv()
        }
        if (player.playerstate == Defines.PST_DEAD) {
            player.DeathThink()
            return
        }

        // Move around.
        // Reactiontime is used to prevent movement
        //  for a bit after a teleport.
        if (C2JUtils.eval(player_mo.reactiontime)) {
            player_mo.reactiontime--
        } else {
            player.MovePlayer()
        }
        player.CalcHeight()
        if (C2JUtils.eval(player_mo.subsector!!.sector!!.special.toInt())) {
            player.PlayerInSpecialSector()
        }

        // Check for weapon change.
        // A special event has no other buttons.
        if (C2JUtils.flags(cmd.buttons.code, Defines.BT_SPECIAL)) {
            cmd.buttons = 0.toChar()
        }
        if (C2JUtils.flags(cmd.buttons.code, Defines.BT_CHANGE)) {
            // The actual changing of the weapon is done
            //  when the weapon psprite can do it
            //  (read: not in the middle of an attack).
            // System.out.println("Weapon change detected, attempting to perform");
            newweapon = weapontype_t.values()[cmd.buttons.code and Defines.BT_WEAPONMASK shr Defines.BT_WEAPONSHIFT]

            // If chainsaw is available, it won't change back to the fist 
            // unless player also has berserk.
            if (newweapon == weapontype_t.wp_fist && player.weaponowned[weapontype_t.wp_chainsaw.ordinal]
                && !(player.readyweapon == weapontype_t.wp_chainsaw
                        && C2JUtils.eval(player.powers[Defines.pw_strength]))
            ) {
                newweapon = weapontype_t.wp_chainsaw
            }

            // Will switch between SG and SSG in Doom 2.
            if (DOOM.isCommercial() && newweapon == weapontype_t.wp_shotgun && player.weaponowned[weapontype_t.wp_supershotgun.ordinal] && player.readyweapon != weapontype_t.wp_supershotgun) {
                newweapon = weapontype_t.wp_supershotgun
            }
            if (player.weaponowned[newweapon.ordinal]
                && newweapon != player.readyweapon
            ) {
                // Do not go to plasma or BFG in shareware,
                //  even if cheated.
                if ((newweapon != weapontype_t.wp_plasma
                            && newweapon != weapontype_t.wp_bfg)
                    || !DOOM.isShareware()
                ) {
                    player.pendingweapon = newweapon
                }
            }
        }

        // check for use
        if (C2JUtils.flags(cmd.buttons.code, Defines.BT_USE)) {
            if (!player.usedown) {
                DOOM.actions.UseLines(player)
                player.usedown = true
            }
        } else {
            player.usedown = false
        }

        // cycle psprites
        player.MovePsprites()

        // Counters, time dependent power ups.
        // Strength counts up to diminish fade.
        if (C2JUtils.eval(player.powers[Defines.pw_strength])) {
            player.powers[Defines.pw_strength]++
        }
        if (C2JUtils.eval(player.powers[Defines.pw_invulnerability])) {
            player.powers[Defines.pw_invulnerability]--
        }
        if (C2JUtils.eval(player.powers[Defines.pw_invisibility])) {
            if (!C2JUtils.eval(--player.powers[Defines.pw_invisibility])) {
                player.mo!!.flags = player.mo!!.flags and mobj_t.MF_SHADOW.inv()
            }
        }
        if (C2JUtils.eval(player.powers[Defines.pw_infrared])) {
            player.powers[Defines.pw_infrared]--
        }
        if (C2JUtils.eval(player.powers[Defines.pw_ironfeet])) {
            player.powers[Defines.pw_ironfeet]--
        }
        if (C2JUtils.eval(player.damagecount)) {
            player.damagecount--
        }
        if (C2JUtils.eval(player.bonuscount)) {
            player.bonuscount--
        }

        // Handling colormaps.
        if (C2JUtils.eval(player.powers[Defines.pw_invulnerability])) {
            if (player.powers[Defines.pw_invulnerability] > 4 * 32 || C2JUtils.flags(
                    player.powers[Defines.pw_invulnerability],
                    8
                )
            ) {
                player.fixedcolormap = Lights.COLORMAP_INVERSE
            } else {
                player.fixedcolormap = Lights.COLORMAP_FIXED
            }
        } else if (C2JUtils.eval(player.powers[Defines.pw_infrared])) {
            if (player.powers[Defines.pw_infrared] > 4 * 32
                || C2JUtils.flags(player.powers[Defines.pw_infrared], 8)
            ) {
                // almost full bright
                player.fixedcolormap = Lights.COLORMAP_BULLBRIGHT
            } else {
                player.fixedcolormap = Lights.COLORMAP_FIXED
            }
        } else {
            player.fixedcolormap = Lights.COLORMAP_FIXED
        }
    }

    /**
     * G_PlayerReborn
     * Called after a player dies
     * almost everything is cleared and initialized
     *
     *
     */
    @G_Game.C(G_Game.G_PlayerReborn)
    fun PlayerReborn() {
        val localFrags = IntArray(Limits.MAXPLAYERS)
        val localKillCount: Int
        val localItemCount: Int
        val localSecretCount: Int

        // System.arraycopy(players[player].frags, 0, frags, 0, frags.length);
        // We save the player's frags here...
        C2JUtils.memcpy(localFrags, frags, localFrags.size)
        localKillCount = killcount
        localItemCount = itemcount
        localSecretCount = secretcount

        //MAES: we need to simulate an erasure, possibly without making
        // a new object.memset (p, 0, sizeof(*p));
        //players[player]=(player_t) player_t.nullplayer.clone();
        // players[player]=new player_t();
        reset()

        // And we copy the old frags into the "new" player. 
        C2JUtils.memcpy(frags, localFrags, frags.size)
        killcount = localKillCount
        itemcount = localItemCount
        secretcount = localSecretCount
        attackdown = true
        usedown = attackdown // don't do anything immediately 
        playerstate = Defines.PST_LIVE
        health[0] = Limits.MAXHEALTH
        pendingweapon = weapontype_t.wp_pistol
        readyweapon = pendingweapon!!
        weaponowned[weapontype_t.wp_fist.ordinal] = true
        weaponowned[weapontype_t.wp_pistol.ordinal] = true
        ammo[ammotype_t.am_clip.ordinal] = 50
        lookdir = 0 // From Heretic
        System.arraycopy(DoomStatus.maxammo, 0, maxammo, 0, Defines.NUMAMMO)
    }

    override fun toString(): String {
        val mo = mo!!
        player_t.sb.setLength(0)
        player_t.sb.append("player")
        player_t.sb.append(" momx ")
        player_t.sb.append(mo.momx)
        player_t.sb.append(" momy ")
        player_t.sb.append(mo.momy)
        player_t.sb.append(" x ")
        player_t.sb.append(mo._x)
        player_t.sb.append(" y ")
        player_t.sb.append(mo._y)
        return player_t.sb.toString()
    }

    @Throws(IOException::class)
    override fun read(f: DataInputStream) {

        // Careful when loading/saving:
        // A player only carries a pointer to a mobj, which is "saved"
        // but later discarded at load time, at least in vanilla. In any case,
        // it has the size of a 32-bit integer, so make sure you skip it.
        // TODO: OK, so vanilla's monsters lost "state" when saved, including non-Doomguy
        //  infighting. Did they "remember" Doomguy too?
        // ANSWER: they didn't.
        // The player is special in that it unambigously allows identifying
        // its own map object in an absolute way. Once we identify
        // at least one (e.g. object #45 is pointer 0x43545345) then, since
        // map objects are stored in a nice serialized order.
        p_mobj = w.DoomIO.readLEInt(f) // player mobj pointer
        playerstate = w.DoomIO.readLEInt(f)
        cmd.read(f)
        viewz = w.DoomIO.readLEInt(f)
        viewheight = w.DoomIO.readLEInt(f)
        deltaviewheight = w.DoomIO.readLEInt(f)
        bob = w.DoomIO.readLEInt(f)
        health[0] = w.DoomIO.readLEInt(f)
        armorpoints[0] = w.DoomIO.readLEInt(f)
        armortype = w.DoomIO.readLEInt(f)
        w.DoomIO.readIntArray(f, powers, ByteOrder.LITTLE_ENDIAN)
        readBooleanIntArray(f, cards)
        backpack = w.DoomIO.readIntBoolean(f)
        w.DoomIO.readIntArray(f, frags, ByteOrder.LITTLE_ENDIAN)
        readyweapon = weapontype_t.values()[w.DoomIO.readLEInt(f)]
        pendingweapon = weapontype_t.values()[w.DoomIO.readLEInt(f)]
        readBooleanIntArray(f, weaponowned)
        w.DoomIO.readIntArray(f, ammo, ByteOrder.LITTLE_ENDIAN)
        w.DoomIO.readIntArray(f, maxammo, ByteOrder.LITTLE_ENDIAN)
        // Read these as "int booleans"
        attackdown = w.DoomIO.readIntBoolean(f)
        usedown = w.DoomIO.readIntBoolean(f)
        cheats = w.DoomIO.readLEInt(f)
        refire = w.DoomIO.readLEInt(f)
        // For intermission stats.
        killcount = w.DoomIO.readLEInt(f)
        itemcount = w.DoomIO.readLEInt(f)
        secretcount = w.DoomIO.readLEInt(f)
        // Hint messages.
        f.skipBytes(4)
        // For screen flashing (red or bright).
        damagecount = w.DoomIO.readLEInt(f)
        bonuscount = w.DoomIO.readLEInt(f)
        // Who did damage (NULL for floors/ceilings).
        // TODO: must be properly denormalized before saving/loading
        f.skipBytes(4) // TODO: waste a read for attacker mobj.
        // So gun flashes light up areas.
        extralight = w.DoomIO.readLEInt(f)
        // Current PLAYPAL, ???
        //  can be set to REDCOLORMAP for pain, etc.
        fixedcolormap = w.DoomIO.readLEInt(f)
        colormap = w.DoomIO.readLEInt(f)
        // PSPDEF _is_ readable.
        for (p in psprites) {
            p.read(f)
        }
        didsecret = w.DoomIO.readIntBoolean(f)
        // Total size should be 280 bytes.
    }

    @Throws(IOException::class)
    fun write(f: DataOutputStream) {

        // It's much more convenient to pre-buffer, since
        // we'll be writing all Little Endian stuff.
        val b = ByteBuffer.allocate(280)
        pack(b)
        // Total size should be 280 bytes.
        // Write everything nicely and at once.        
        f.write(b.array())
    }

    // Used to disambiguate between objects
    var p_mobj = 0

    /* Fugly hack to "reset" the player. Not worth the fugliness.
    public static player_t nullplayer;
    static {
        nullplayer = new player_t();
    }
     */
    init {
        powers = IntArray(Defines.NUMPOWERS)
        frags = IntArray(Limits.MAXPLAYERS)
        ammo = IntArray(Defines.NUMAMMO)
        //maxammo = new int[NUMAMMO];
        maxammo = IntArray(Defines.NUMAMMO)
        cards = BooleanArray(card_t.NUMCARDS.ordinal)
        weaponowned = BooleanArray(Defines.NUMWEAPONS)
        psprites = GenericCopy.malloc({ pspdef_t() }, player_t.NUMPSPRITES)
        mo = mobj_t.createOn(DOOM)
        // If a player doesn't reference himself through his object, he will have an existential crisis.
        mo!!.player = this
        readyweapon = weapontype_t.wp_fist
        cmd = ticcmd_t()
        //weaponinfo=new weaponinfo_t();
    }

    @Throws(IOException::class)
    override fun pack(buf: ByteBuffer) {
        val bo = ByteOrder.LITTLE_ENDIAN
        buf.order(bo)
        // The player is special in that it unambiguously allows identifying
        // its own map object in an absolute way. Once we identify
        // at least one (e.g. object #45 is pointer 0x43545345) then, since
        // map objects are stored in a nice serialized order by using
        // their next/prev pointers, you can reconstruct their
        // relationships a posteriori.
        // Store our own hashcode or "pointer" if you wish.
        buf.putInt(C2JUtils.pointer(mo))
        buf.putInt(playerstate)
        cmd.pack(buf)
        buf.putInt(viewz)
        buf.putInt(viewheight)
        buf.putInt(deltaviewheight)
        buf.putInt(bob)
        buf.putInt(health[0])
        buf.putInt(armorpoints[0])
        buf.putInt(armortype)
        DoomBuffer.putIntArray(buf, powers, powers.size, bo)
        DoomBuffer.putBooleanIntArray(buf, cards, cards.size, bo)
        DoomBuffer.putBooleanInt(buf, backpack, bo)
        DoomBuffer.putIntArray(buf, frags, frags.size, bo)
        buf.putInt(readyweapon.ordinal)
        buf.putInt(pendingweapon!!.ordinal)
        DoomBuffer.putBooleanIntArray(buf, weaponowned, weaponowned.size, bo)
        DoomBuffer.putIntArray(buf, ammo, ammo.size, bo)
        DoomBuffer.putIntArray(buf, maxammo, maxammo.size, bo)
        // Read these as "int booleans"
        DoomBuffer.putBooleanInt(buf, attackdown, bo)
        DoomBuffer.putBooleanInt(buf, usedown, bo)
        buf.putInt(cheats)
        buf.putInt(refire)
        // For intermission stats.
        buf.putInt(killcount)
        buf.putInt(itemcount)
        buf.putInt(secretcount)
        // Hint messages.
        buf.putInt(0)
        // For screen flashing (red or bright).
        buf.putInt(damagecount)
        buf.putInt(bonuscount)
        // Who did damage (NULL for floors/ceilings).
        // TODO: must be properly denormalized before saving/loading
        buf.putInt(C2JUtils.pointer(attacker))
        // So gun flashes light up areas.
        buf.putInt(extralight)
        // Current PLAYPAL, ???
        //  can be set to REDCOLORMAP for pain, etc.
        /**
         * Here the fixed color map of player is written when player_t object is packed.
         * Make sure not to write any preshifted value there! Do not scale player_r.fixedcolormap,
         * scale dependent array accesses.
         * - Good Sign 2017/04/15
         */
        buf.putInt(fixedcolormap)
        buf.putInt(colormap)
        // PSPDEF _is_ readable.
        for (p in psprites) {
            p.pack(buf)
        }
        buf.putInt(if (didsecret) 1 else 0)
    }

    companion object {
        const val CF_NOCLIP = 1 // No damage, no health loss.
        const val CF_GODMODE = 2
        const val CF_NOMOMENTUM = 4 // Not really a cheat, just a debug aid.

        /**
         * 16 pixels of bob
         */
        private const val MAXBOB = 0x100000
        protected const val PLAYERTHRUST = 2048 / Defines.TIC_MUL

        //
        // GET STUFF
        //
        // a weapon is found with two clip loads,
        // a big item has five clip loads
        val clipammo = intArrayOf(10, 4, 20, 1)
        const val BONUSADD = 6
        private const val ANG5 = Tables.ANG90 / 18

        /* psprnum_t enum */
        var ps_weapon = 0
        var ps_flash = 1
        var NUMPSPRITES = 2
        var LOWERSPEED: Int = MAPFRACUNIT * 6
        var RAISESPEED: Int = MAPFRACUNIT * 6
        var WEAPONBOTTOM: Int = 128 * FRACUNIT
        var WEAPONTOP: Int = 32 * FRACUNIT

        // plasma cells for a bfg attack
        private const val BFGCELLS = 40
        private val sb = StringBuilder()
    }
}