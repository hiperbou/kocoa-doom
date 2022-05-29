/*
 * Copyright (C) 2017 Good Sign
 * Copyright (C) 2022 hiperbou
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package v.graphics


import mochadoom.Loggers
import rr.patch_t
import utils.C2JUtils
import v.DoomGraphicSystem
import v.graphics.Screens.BadRangeException
import v.scale.VideoScale
import java.util.logging.Level.INFO

/**
 * Rewritten unified patch-drawing methods with parallelism (yeah, multithread!)
 * Note that now most of the functions now support FLAGS now as a separate argument, I totally needed screen id safety.
 * Reimplemented counter-flags, that work the next way:
 * - there is a Default Behavior chose when flag is not present
 * - if the flag is present Default Behavior is changed
 * - if both the flag and the opposite flag are present, then the flag that restores Default Behavior takes precedence
 *
 * I tried my best to preserve all of the features done by prior contributors. - Good Sign 2017/04/02
 *
 * @author Good Sign
 *
 * About all DrawPatch functions:
 * It uses FLAGS (see above) (now as a separate - Good Sign 2017/04/04) parameter, to be
 * parsed afterwards. Shamelessly ripped from Doom Legacy (for menus, etc) by _D_ ;-)
 *
 * added:05-02-98: default params : scale patch and scale start
 *
 * Iniially implemented for Mocha Doom by _D_ (shamelessly ripped from Eternity Engine ;-), adapted to scale based
 * on a scaling info object (VSI).
 *
 * Unless overriden by flags, starting x and y are automatically scaled (implied V_SCALESTART)
 */
interface Patches<V, E : Enum<E>> : Columns<V, E> {
    /**
     * V_DrawPatch
     *
     * Draws a patch to the screen without centering or scaling
     */
    fun DrawPatch(screen: E, patch: patch_t, x: Int, y: Int, vararg flags: Int) {
        DrawPatchScaled(screen, patch, null, x, y, *flags)
    }

    /**
     * V_DrawPatch
     *
     * Draws a patch to the screen without centering or scaling
     */
    fun DrawPatchCentered(screen: E, patch: patch_t, y: Int, vararg flags: Int) {
        DrawPatchCenteredScaled(screen, patch, null, y, *flags)
    }

    /**
     * V_DrawScaledPatch like V_DrawPatch, but scaled with IVideoScale object scaling
     * Centers the x coordinate on a screen based on patch width and offset
     * I have completely reworked column drawing code, so it resides in another class, and supports parallelism
     * - Good Sign 2017/04/04
     *
     * It uses FLAGS (see above) (now as a separate - Good Sign 2017/04/04) parameter, to be
     * parsed afterwards. Shamelessly ripped from Doom Legacy (for menus, etc) by _D_ ;-)
     */
    fun DrawPatchCenteredScaled(screen: E, patch: patch_t, vs: VideoScale?, y: Int, vararg flags: Int) {
        var y = y
        val flagsV = if (flags.size > 0) flags[0] else 0
        var dupx: Int
        var dupy: Int
        if (vs != null) {
            if (C2JUtils.flags(flagsV, DoomGraphicSystem.V_SAFESCALE)) {
                dupy = vs.getSafeScaling()
                dupx = dupy
            } else {
                dupx = vs.getScalingX()
                dupy = vs.getScalingY()
            }
        } else {
            dupy = 1
            dupx = dupy
        }
        val predevide = C2JUtils.flags(flagsV, DoomGraphicSystem.V_PREDIVIDE)
        // By default we scale, if V_NOSCALEOFFSET we dont scale unless V_SCALEOFFSET (restores Default Behavior)
        val scaleOffset = !C2JUtils.flags(flagsV, DoomGraphicSystem.V_NOSCALEOFFSET) || C2JUtils.flags(
            flagsV,
            DoomGraphicSystem.V_SCALEOFFSET
        )
        // By default we scale, if V_NOSCALESTART we dont scale unless V_SCALESTART (restores Default Behavior)
        val scaleStart = !C2JUtils.flags(flagsV, DoomGraphicSystem.V_NOSCALESTART) || C2JUtils.flags(
            flagsV,
            DoomGraphicSystem.V_SCALESTART
        )
        // By default we do dup, if V_NOSCALEPATCH we dont dup unless V_SCALEPATCH (restores Default Behavior)
        val noScalePatch = C2JUtils.flags(flagsV, DoomGraphicSystem.V_NOSCALEPATCH) && !C2JUtils.flags(
            flagsV,
            DoomGraphicSystem.V_SCALEPATCH
        )
        val flip = C2JUtils.flags(flagsV, DoomGraphicSystem.V_FLIPPEDPATCH)
        val halfWidth = if (noScalePatch) patch.width / 2 else patch.width * dupx / 2
        val x: Int = getScreenWidth() / 2 - halfWidth - if (scaleOffset) patch.leftoffset * dupx else patch.leftoffset.toInt()
        y = applyScaling(y, patch.topoffset.toInt(), dupy, predevide, scaleOffset, scaleStart)
        if (noScalePatch) {
            dupy = 1
            dupx = dupy
        }
        try {
            doRangeCheck(x, y, patch, dupx, dupy)
            DrawPatchColumns(getScreen(screen)!!, patch, x, y, dupx, dupy, flip)
        } catch (ex: BadRangeException) {
            printDebugPatchInfo(patch, x, y, predevide, scaleOffset, scaleStart, dupx, dupy)
        }
    }

    /**
     * This method should help to debug bad patches or bad placement of them
     * - Good Sign 2017/04/22
     */
    fun printDebugPatchInfo(
        patch: patch_t,
        x: Int,
        y: Int,
        predevide: Boolean,
        scaleOffset: Boolean,
        scaleStart: Boolean,
        dupx: Int,
        dupy: Int
    ) {
        Loggers.getLogger(Patches::class.java.name).log(INFO) {
            String.format(
                """V_DrawPatch: bad patch (ignored)
Patch %s at %d, %d exceeds LFB
	predevide: %s
	scaleOffset: %s
	scaleStart: %s
	dupx: %s, dupy: %s
	leftoffset: %s
	topoffset: %s
""",
                patch.name, x, y,
                predevide, scaleOffset, scaleStart, dupx, dupy, patch.leftoffset, patch.topoffset
            )
        }
    }

    /**
     * V_DrawPatch
     *
     * V_DrawScaledPatch like V_DrawPatch, but scaled with IVideoScale object scaling
     * I have completely reworked column drawing code, so it resides in another class, and supports parallelism
     * - Good Sign 2017/04/04
     */
    fun DrawPatchScaled(screen: E, patch: patch_t, vs: VideoScale?, x: Int, y: Int, vararg flags: Int) {
        var x = x
        var y = y
        val flagsV = if (flags.size > 0) flags[0] else 0
        var dupx: Int
        var dupy: Int
        if (vs != null) {
            if (C2JUtils.flags(flagsV, DoomGraphicSystem.V_SAFESCALE)) {
                dupy = vs.getSafeScaling()
                dupx = dupy
            } else {
                dupx = vs.getScalingX()
                dupy = vs.getScalingY()
            }
        } else {
            dupy = 1
            dupx = dupy
        }
        val predevide = C2JUtils.flags(flagsV, DoomGraphicSystem.V_PREDIVIDE)
        // By default we scale, if V_NOSCALEOFFSET we dont scale unless V_SCALEOFFSET (restores Default Behavior)
        val scaleOffset = !C2JUtils.flags(flagsV, DoomGraphicSystem.V_NOSCALEOFFSET) || C2JUtils.flags(
            flagsV,
            DoomGraphicSystem.V_SCALEOFFSET
        )
        // By default we scale, if V_NOSCALESTART we dont scale unless V_SCALESTART (restores Default Behavior)
        val scaleStart = !C2JUtils.flags(flagsV, DoomGraphicSystem.V_NOSCALESTART) || C2JUtils.flags(
            flagsV,
            DoomGraphicSystem.V_SCALESTART
        )
        // By default we do dup, if V_NOSCALEPATCH we dont dup unless V_SCALEPATCH (restores Default Behavior)
        val noScalePatch = C2JUtils.flags(flagsV, DoomGraphicSystem.V_NOSCALEPATCH) && !C2JUtils.flags(
            flagsV,
            DoomGraphicSystem.V_SCALEPATCH
        )
        val flip = C2JUtils.flags(flagsV, DoomGraphicSystem.V_FLIPPEDPATCH)
        x = applyScaling(x, patch.leftoffset.toInt(), dupx, predevide, scaleOffset, scaleStart)
        y = applyScaling(y, patch.topoffset.toInt(), dupy, predevide, scaleOffset, scaleStart)
        if (noScalePatch) {
            dupy = 1
            dupx = dupy
        }
        try {
            doRangeCheck(x, y, patch, dupx, dupy)
            DrawPatchColumns(getScreen(screen)!!, patch, x, y, dupx, dupy, flip)
        } catch (ex: BadRangeException) {
            // Do not abort!
            printDebugPatchInfo(patch, x, y, predevide, scaleOffset, scaleStart, dupx, dupy)
        }
    }

    /**
     * Replaces DrawPatchCol for bunny scrolled in Finale.
     * Also uses my reworked column code, but that one is not parallelized
     * - Good Sign 2017/04/04
     */
    fun DrawPatchColScaled(screen: E?, patch: patch_t, vs: VideoScale, x: Int, col: Int) {
        var x = x
        val dupx = vs.getScalingX()
        val dupy = vs.getScalingY()
        x -= patch.leftoffset.toInt()
        x *= dupx
        DrawColumn(
            getScreen(screen!!)!!,
            patch.columns[col]!!,
            Horizontal(point(x, 0), dupx),
            convertPalettedBlock(*patch.columns[col]!!.data),
            getScreenWidth(),
            dupy
        )
    }

    fun applyScaling(
        c: Int,
        offset: Int,
        dup: Int,
        predevide: Boolean,
        scaleOffset: Boolean,
        scaleStart: Boolean
    ): Int {
        // A very common operation, eliminates the need to pre-divide.
        var c = c
        if (predevide) c /= getScalingX()

        // Scale start before offsetting, it seems right to do so - Good Sign 2017/04/04
        if (scaleStart) c *= dup

        // MAES: added this fix so that non-zero patch offsets can be
        // taken into account, regardless of whether we use pre-scaled
        // coords or not. Only Doomguy's face needs this hack for now.
        c -= if (scaleOffset) offset * dup else offset
        return c
    }
}