package rr

import doom.SourceCode.R_Data
import rr.parallel.IGetSmpColumn
import rr.patch_t
import java.io.IOException

/** All texture, flat and sprite management operations should be handled
 * by an implementing class. As of now, the renderer does both, though it's
 * not really the most ideal.
 *
 * @author Velktron
 */
interface TextureManager<T> : IGetColumn<T>, IGetCachedColumn<T>, IGetSmpColumn {
    fun TextureNumForName(texname: String): Int

    /**The "num" expected here is the internal flat number,
     * not the absolute lump number. So impement accordingly.
     *
     * @param flatname
     * @return
     */
    fun FlatNumForName(flatname: String): Int

    @R_Data.C(R_Data.R_PrecacheLevel)
    @Throws(IOException::class)
    fun PrecacheLevel()
    fun GenerateComposite(tex: Int)
    fun getTextureheight(texnum: Int): Int
    fun getTextureTranslation(texnum: Int): Int
    fun getFlatTranslation(flatnum: Int): Int
    fun setTextureTranslation(texnum: Int, amount: Int)
    fun setFlatTranslation(flatnum: Int, amount: Int)
    fun CheckTextureNumForName(texnamem: String): Int
    fun CheckTextureNameForNum(texnum: Int): String?
    fun getTexturewidthmask(tex: Int): Int
    fun getTextureColumnLump(tex: Int, col: Int): Int
    fun getTextureColumnOfs(tex: Int, col: Int): Char
    fun getTextureComposite(tex: Int): Array<T>?
    fun getTextureComposite(tex: Int, col: Int): T
    fun InitFlats()

    @Throws(IOException::class)
    fun InitTextures()

    //int getFirstFlat();
    fun getSkyTextureMid(): Int
    fun getSkyFlatNum(): Int
    fun getSkyTexture(): Int
    fun setSkyTexture(skytexture: Int)
    fun InitSkyMap(): Int
    fun setSkyFlatNum(skyflatnum: Int)

    @Throws(IOException::class)
    fun GenerateLookup(texnum: Int)
    fun getFlatLumpNum(flatnum: Int): Int
    fun getRogueColumn(lump: Int, column: Int): T
    fun getMaskedComposite(tex: Int): patch_t?
    fun GenerateMaskedComposite(texnum: Int)

    /** Return a "sanitized" patch. If data is insufficient, return
     * a default patch or attempt a partial draw.
     *
     * @param patchnum
     * @return
     */
    fun getSafeFlat(flatnum: Int): T
    fun GetColumnStruct(tex: Int, col: Int): column_t?
    fun setSMPVars(nUMMASKEDTHREADS: Int)

    companion object {
        val texturelumps = arrayOf("TEXTURE1", "TEXTURE2")
        val NUMTEXLUMPS: Int = TextureManager.texturelumps.size
        const val TEXTURE1 = 0
        const val TEXTURE2 = 1
    }
}