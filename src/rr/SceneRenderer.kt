package rr

import data.Tables
import doom.SourceCode.R_Draw
import doom.player_t
import i.IDoomSystem
import m.fixed_t.Companion.FRACUNIT
import rr.drawfuns.ColFuncs
import rr.drawfuns.ColVars
import rr.drawfuns.SpanVars
import v.tables.LightsAndColors
import w.IWadLoader

interface SceneRenderer<T, V> {
    fun Init()
    fun RenderPlayerView(player: player_t)
    fun ExecuteSetViewSize()

    @R_Draw.C(R_Draw.R_FillBackScreen)
    fun FillBackScreen()
    fun DrawViewBorder()
    fun SetViewSize(size: Int, detaillevel: Int)
    fun PointToAngle2(x1: Int, y1: Int, x2: Int, y2: Int): Long
    fun PreCacheThinkers()
    fun getValidCount(): Int
    fun increaseValidCount(amount: Int)
    fun isFullHeight(): Boolean
    fun resetLimits()
    fun getSetSizeNeeded(): Boolean
    fun isFullScreen(): Boolean

    // Isolation methods
    fun getTextureManager(): TextureManager<T>
    fun getPlaneDrawer(): PlaneDrawer<T, V>
    fun getView(): ViewVars
    fun getDSVars(): SpanVars<T, V>
    fun getColorMap(): LightsAndColors<V>
    fun getDoomSystem(): IDoomSystem
    fun getWadLoader(): IWadLoader?

    /**
     * Use this to "peg" visplane drawers (even parallel ones) to
     * the same set of visplane variables.
     *
     * @return
     */
    fun getVPVars(): Visplanes
    fun getSegVars(): SegVars
    fun getSpriteManager(): ISpriteManager?
    fun getBSPVars(): BSPVars
    fun getVisSpriteManager(): IVisSpriteManagement<V>
    fun getColFuncsHi(): ColFuncs<T, V>
    fun getColFuncsLow(): ColFuncs<T, V>
    fun getMaskedDCVars(): ColVars<T, V> //public subsector_t PointInSubsector(int x, int y);

    companion object {
        /**
         * Fineangles in the SCREENWIDTH wide window.
         */
        const val FIELDOFVIEW = Tables.FINEANGLES / 4
        val MINZ: Int = FRACUNIT * 4
        const val FUZZTABLE = 50

        /**
         * killough: viewangleoffset is a legacy from the pre-v1.2 days, when Doom
         * had Left/Mid/Right viewing. +/-ANG90 offsets were placed here on each
         * node, by d_net.c, to set up a L/M/R session.
         */
        const val viewangleoffset: Long = 0
    }
}