/*-----------------------------------------------------------------------------
//
// Copyright (C) 1993-1996 Id Software, Inc.
// Copyright (C) 2017 Good Sign
// Copyright (C) 2022 hiperbou
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// From m_misc.c
//-----------------------------------------------------------------------------*/
package m


import awt.FullscreenOptions.*
import doom.ConfigBase
import doom.ConfigManager.UpdateStatus
import doom.englsh
import g.Signals.ScanCode
import mochadoom.Engine
import utils.QuoteType
import v.graphics.Plotter
import v.renderers.BppMode
import v.renderers.SceneRendererMode
import v.tables.GreyscaleFilter
import java.util.*

/**
 * An enumeration with the most basic default Doom settings their default values, used if nothing else is available.
 * They are applied first thing, and then updated from the .cfg file.
 *
 * The file now also contains settings on many features introduced by this new version of Mocha Doom
 * - Good Sign 2017/04/11
 *
 * TODO: find a trick to separate settings groups in the same file vanilla-compatibly
 */
enum class Settings {
    /**
     * Defaults (default.cfg) defined in vanilla format, ordered in vanilla order
     */
    mouse_sensitivity(ConfigBase.FILE_DOOM, 5), sfx_volume(ConfigBase.FILE_DOOM, 8), music_volume(
        ConfigBase.FILE_DOOM,
        8
    ),
    show_messages(ConfigBase.FILE_DOOM, 1), key_right(
        ConfigBase.FILE_DOOM,
        ScanCode.SC_RIGHT.ordinal
    ),
    key_left(ConfigBase.FILE_DOOM, ScanCode.SC_LEFT.ordinal), key_up(
        ConfigBase.FILE_DOOM,
        ScanCode.SC_W.ordinal
    ),
    key_down(ConfigBase.FILE_DOOM, ScanCode.SC_S.ordinal), key_strafeleft(
        ConfigBase.FILE_DOOM,
        ScanCode.SC_A.ordinal
    ),
    key_straferight(ConfigBase.FILE_DOOM, ScanCode.SC_D.ordinal), key_fire(
        ConfigBase.FILE_DOOM,
        ScanCode.SC_LCTRL.ordinal
    ),
    key_use(ConfigBase.FILE_DOOM, ScanCode.SC_SPACE.ordinal), key_strafe(
        ConfigBase.FILE_DOOM,
        ScanCode.SC_LALT.ordinal
    ),
    key_speed(ConfigBase.FILE_DOOM, ScanCode.SC_RSHIFT.ordinal), use_mouse(
        ConfigBase.FILE_DOOM,
        1
    ),
    mouseb_fire(ConfigBase.FILE_DOOM, 0), mouseb_strafe(ConfigBase.FILE_DOOM, 1),  // AX: Fixed
    mouseb_forward(ConfigBase.FILE_DOOM, 2),  // AX: Value inverted with the one above
    use_joystick(ConfigBase.FILE_DOOM, 0), joyb_fire(ConfigBase.FILE_DOOM, 0), joyb_strafe(
        ConfigBase.FILE_DOOM,
        1
    ),
    joyb_use(ConfigBase.FILE_DOOM, 3), joyb_speed(
        ConfigBase.FILE_DOOM,
        2
    ),
    screenblocks(ConfigBase.FILE_DOOM, 9), detaillevel(ConfigBase.FILE_DOOM, 0), snd_channels(
        ConfigBase.FILE_DOOM,
        8
    ),
    snd_musicdevice(ConfigBase.FILE_DOOM, 3),  // unused, here for compatibility
    snd_sfxdevice(ConfigBase.FILE_DOOM, 3),  // unused, here for compatibility
    snd_sbport(ConfigBase.FILE_DOOM, 0),  // unused, here for compatibility
    snd_sbirq(ConfigBase.FILE_DOOM, 0),  // unused, here for compatibility
    snd_sbdma(ConfigBase.FILE_DOOM, 0),  // unused, here for compatibility
    snd_mport(ConfigBase.FILE_DOOM, 0),  // unused, here for compatibility
    usegamma(ConfigBase.FILE_DOOM, 0), chatmacro0(
        ConfigBase.FILE_DOOM,
        englsh.HUSTR_CHATMACRO0
    ),
    chatmacro1(ConfigBase.FILE_DOOM, englsh.HUSTR_CHATMACRO1), chatmacro2(
        ConfigBase.FILE_DOOM,
        englsh.HUSTR_CHATMACRO2
    ),
    chatmacro3(ConfigBase.FILE_DOOM, englsh.HUSTR_CHATMACRO3), chatmacro4(
        ConfigBase.FILE_DOOM,
        englsh.HUSTR_CHATMACRO4
    ),
    chatmacro5(ConfigBase.FILE_DOOM, englsh.HUSTR_CHATMACRO5), chatmacro6(
        ConfigBase.FILE_DOOM,
        englsh.HUSTR_CHATMACRO6
    ),
    chatmacro7(ConfigBase.FILE_DOOM, englsh.HUSTR_CHATMACRO7), chatmacro8(
        ConfigBase.FILE_DOOM,
        englsh.HUSTR_CHATMACRO8
    ),
    chatmacro9(ConfigBase.FILE_DOOM, englsh.HUSTR_CHATMACRO9),

    /**
     * Mocha Doom (mochadoom.cfg), these can be defined to anything and will be sorded by name
     */
    mb_used(ConfigBase.FILE_MOCHADOOM, 2), fullscreen(
        ConfigBase.FILE_MOCHADOOM,
        false
    ),
    fullscreen_mode(
        ConfigBase.FILE_MOCHADOOM,
        FullMode.Best
    ),
    fullscreen_stretch(
        ConfigBase.FILE_MOCHADOOM,
        StretchMode.Fit
    ),
    fullscreen_interpolation(
        ConfigBase.FILE_MOCHADOOM,
        InterpolationMode.Nearest
    ),
    alwaysrun(ConfigBase.FILE_MOCHADOOM, false),  // Always run is OFF
    vanilla_key_behavior(
        ConfigBase.FILE_MOCHADOOM,
        true
    ),  // Currently forces LSHIFT on RSHIFT, TODO: layouts, etc 
    automap_plotter_style(
        ConfigBase.FILE_MOCHADOOM,
        Plotter.Style.Thin
    ),  // Thin is vanilla, Thick is scaled, Deep slightly rounded scaled
    enable_colormap_lump(
        ConfigBase.FILE_MOCHADOOM,
        true
    ),  // Enables usage of COLORMAP lump read from wad during lights and specials generation
    color_depth(
        ConfigBase.FILE_MOCHADOOM,
        BppMode.Indexed
    ),  // Indexed: 256, HiColor: 32 768, TrueColor: 16 777 216
    extend_plats_limit(
        ConfigBase.FILE_MOCHADOOM,
        true
    ),  // Resize instead of "P_AddActivePlat: no more plats!"
    extend_button_slots_limit(
        ConfigBase.FILE_MOCHADOOM,
        true
    ),  // Resize instead of "P_StartButton: no button slots left!"
    fix_blockmap(ConfigBase.FILE_MOCHADOOM, true),  // Add support for 512x512 blockmap
    fix_gamma_ramp(
        ConfigBase.FILE_MOCHADOOM,
        false
    ),  // Vanilla do not use pure black color because Gamma LUT calculated without it, doubling 128
    fix_gamma_palette(
        ConfigBase.FILE_MOCHADOOM,
        false
    ),  // In vanilla, switching gamma with F11 hides Berserk or Rad suit tint
    fix_sky_change(
        ConfigBase.FILE_MOCHADOOM,
        false
    ),  // In vanilla, sky does not change when you exit the level and the next level with new sky
    fix_sky_palette(
        ConfigBase.FILE_MOCHADOOM,
        false
    ),  // In vanilla, sky color does not change when under effect of Invulnerability powerup
    fix_medi_need(
        ConfigBase.FILE_MOCHADOOM,
        false
    ),  // In vanilla, message "Picked up a medikit that you REALLY need!" never appears due to bug
    fix_ouch_face(
        ConfigBase.FILE_MOCHADOOM,
        false
    ),  // In vanilla, ouch face displayed only when acuired 25+ health when damaged for 25+ health
    line_of_sight(
        ConfigBase.FILE_MOCHADOOM,
        LOS.Vanilla
    ),  // Deaf monsters when thing pos corellates somehow with map vertex, change desync demos
    vestrobe(ConfigBase.FILE_MOCHADOOM, false),  // Strobe effect on automap cut off from vanilla
    scale_screen_tiles(ConfigBase.FILE_MOCHADOOM, true),  // If you scale screen tiles, it looks like vanilla
    scale_melt(
        ConfigBase.FILE_MOCHADOOM,
        true
    ),  // If you scale melt and use DoomRandom generator (not truly random), it looks exacly like vanilla
    semi_translucent_fuzz(
        ConfigBase.FILE_MOCHADOOM,
        false
    ),  // only works in AlphaTrueColor mode. Also ignored with fuzz_mix = true
    fuzz_mix(
        ConfigBase.FILE_MOCHADOOM,
        false
    ),  // Maes unique features on Fuzz effect. Vanilla dont have that, so they are switched off by default
    parallelism_realcolor_tint(
        ConfigBase.FILE_MOCHADOOM,
        Runtime.getRuntime().availableProcessors()
    ),  // Used for real color tinting to speed up
    parallelism_patch_columns(
        ConfigBase.FILE_MOCHADOOM,
        0
    ),  // When drawing screen graphics patches, this speeds up column drawing, <= 0 is serial
    greyscale_filter(
        ConfigBase.FILE_MOCHADOOM,
        GreyscaleFilter.Luminance
    ),  // Used for FUZZ effect or with -greypal comand line argument (for test)
    scene_renderer_mode(
        ConfigBase.FILE_MOCHADOOM,
        SceneRendererMode.Serial
    ),  // In vanilla, scene renderer is serial. Parallel can be faster
    reconstruct_savegame_pointers(ConfigBase.FILE_MOCHADOOM, true);

    /*constructor(config: ConfigBase.Files, defaultValue: T) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }*/

    constructor(config: ConfigBase.Files, defaultValue: InterpolationMode) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: SceneRendererMode) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: GreyscaleFilter) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: LOS) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: BppMode) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: Plotter.Style) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: StretchMode) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: FullMode) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: String) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: Char) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: Int) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: Long) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: Double) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    constructor(config: ConfigBase.Files, defaultValue: Boolean) {
        this.defaultValue = defaultValue
        valueType = defaultValue.javaClass
        configBase = config
    }

    val valueType: Class<*>
    val defaultValue: Any
    private var configBase: ConfigBase.Files
    fun `is`(obj: Any): Boolean {
        return Engine.getConfig() == obj
    }

    fun hasChange(b: Boolean): UpdateStatus {
        configBase.changed = configBase.changed || b
        return if (b) UpdateStatus.UPDATED else UpdateStatus.UNCHANGED
    }

    fun rebase(newConfig: ConfigBase.Files) {
        if (configBase === newConfig) {
            return
        }
        Settings.SETTINGS_MAP.get(configBase)!!.remove(this)
        configBase = newConfig
        updateConfig()
    }

    fun quoteType(): Optional<QuoteType> {
        if (valueType == String::class.java)
            return Optional.of(QuoteType.DOUBLE)
        else if (valueType == Character::class.java)
            return Optional.of(QuoteType.SINGLE)

        return Optional.empty()
    }

    enum class LOS {
        Vanilla, Boom
    }

    /*
    private void updateConfig() {
        SETTINGS_MAP.compute(configBase, (c, list) -> {
            if (list == null) {
                list = EnumSet.of(this);
            } else {
                list.add(this);
            }

            return list;
        });
    }
     */

    private fun updateConfig() {
        Settings.SETTINGS_MAP.compute(configBase) { c, list ->
            if (list == null) {
                EnumSet.of(this)
            } else {
                list.add(this);
                list
            }
        }
    }


/*
    public final static Map<Files, EnumSet<Settings>> SETTINGS_MAP = new HashMap<>();

    static {
        Arrays.stream(values()).forEach(Settings::updateConfig);
    }
 */
    companion object {
        // In vanilla, infighting targets are not restored on savegame load
        val SETTINGS_MAP = HashMap<ConfigBase.Files, EnumSet<Settings>>()

        init {
            //Arrays.stream(Settings.values()).forEach { obj: Settings? -> m.obj.updateConfig() }
            //Arrays.stream(values()).forEach ( Settings::updateConfig )//TODO: this is weird
            Arrays.stream(values()).forEach { it.updateConfig() }
        }
    }
}