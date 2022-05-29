package rr

import data.Defines
import doom.DoomMain
import m.fixed_t.Companion.FRACBITS
import utils.C2JUtils
import utils.GenericCopy

/** An stand-alone sprite loader. Surprisingly, it is quite a
 * separate concern from the renderer, and only needs to communicate
 * occasionally through its getters with the rest of the stuff.
 *
 * Helped lighten up the rendering code a lot, too.
 *
 * @author Maes
 */
class SpriteManager<T, V>(DOOM: DoomMain<T, V>) : ISpriteManager {
    private val DOOM: DoomMain<T, V>

    // Temporarily contains the frames of a given sprite before they are
    // registered with the rendering system. Apparently, a maximum of 29 frames
    // per sprite is allowed.
    protected var sprtemp = arrayOfNulls<spriteframe_t>(29)
    protected var maxframe = 0
    protected var spritename: String? = null

    // MAES: Shit taken from things
    protected var firstspritelump = 0
    protected var lastspritelump = 0
    protected var numspritelumps = 0

    // variables used to look up and range check thing_t sprites patches
    //
    protected lateinit var _sprites: Array<spritedef_t>
    protected var numsprites = 0

    /** needed for pre rendering (fixed_t[])  */
    protected lateinit var spritewidth: IntArray
    protected lateinit var spriteoffset: IntArray
    protected lateinit var spritetopoffset: IntArray

    init {
        sprtemp =
            GenericCopy.malloc({ spriteframe_t() }, SpriteManager.MAX_SPRITE_FRAMES)
        this.DOOM = DOOM
    }

    //
    // R_InitSpriteDefs
    // Pass a null terminated list of sprite names
    // (4 chars exactly) to be used.
    //
    // Builds the sprite rotation matrixes to account
    // for horizontally flipped sprites.
    //
    // Will report an error if the lumps are inconsistent.
    // Only called at startup.
    //
    // Sprite lump names are 4 characters for the actor,
    // a letter for the frame, and a number for the rotation.
    //
    // A sprite that is flippable will have an additional
    // letter/number appended.
    //
    // The rotation character can be 0 to signify no rotations.
    //
    // 1/25/98, 1/31/98 killough : Rewritten for performance
    //
    // Empirically verified to have excellent hash
    // properties across standard Doom sprites:
    protected fun InitSpriteDefs(namelist: Array<String>?) {
        val numentries = lastspritelump - firstspritelump + 1
        val hash: HashMap<Int, MutableList<Int>>
        var i: Int
        if (numentries == 0 || namelist == null) return

        // count the number of sprite names
        i = namelist.size
        numsprites = i
        _sprites = GenericCopy.malloc({ spritedef_t() }, numsprites)

        // Create hash table based on just the first four letters of each
        // sprite
        // killough 1/31/98
        // Maes: the idea is to have a chained hastable which can handle
        // multiple entries (sprites) on the same primary key (the 4 first chars of
        // the sprite name)
        hash = HashMap(numentries) // allocate hash table

        // We have to trasverse this in the opposite order, so that later
        // lumps
        // trump previous ones in order.
        i = numentries - 1
        while (i >= 0) {
            val hashcode = SpriteNameHash(DOOM.wadLoader.GetLumpInfo(i + firstspritelump)!!.name!!)
            // Create chain list for each sprite class (e.g. TROO, POSS,
            // etc.)
            //
            if (!hash.containsKey(hashcode)) {
                hash[hashcode] = ArrayList()
            }

            // Store (yet another) lump index for this sprite.
            hash[hashcode]!!.add(i)
            i--
        }

        // scan all the lump names for each of the names,
        // noting the highest frame letter.
        i = 0
        while (i < numsprites) {


            // We only look for entries that are known to be sprites.
            // The hashtable may contain a lot of other shit, at this point
            // which will be hopefully ignored.
            val spritename = namelist[i]
            val list: List<Int>? = hash[SpriteNameHash(spritename)]

            // Well, it may have been something else. Fuck it.
            if (list != null && !list.isEmpty()) {

                // Maes: the original code actually set everything to "-1"
                // here, including the
                // "boolean" rotate value. The idea was to create a
                // "tristate" of sorts, where -1
                // means a sprite of uncertain status. Goto
                // InstallSpriteLumps for more.
                for (sprtemp1 in sprtemp) {
                    C2JUtils.memset(sprtemp1!!.flip, (-1).toByte(), sprtemp1.flip.size)
                    C2JUtils.memset(sprtemp1!!.lump, (-1).toShort().toInt(), sprtemp1.lump.size) //TODO: short?
                    // This should be INDETERMINATE at this point.
                    sprtemp1.rotate = -1
                }
                maxframe = -1

                // What is stored in the lists are all actual lump numbers
                // relative
                // to e.g. TROO. In coalesced lumps, there will be overlap.
                // This procedure should, in theory, trump older ones.
                list.forEach({ j: Int ->
                    val lump = DOOM.wadLoader.GetLumpInfo(j + firstspritelump)!!
                    val name = lump.name!!
                    // We don't know a-priori which frames exist.
                    // However, we do know how to interpret existing ones,
                    // and have an implicit maximum sequence of 29 Frames.
                    // A frame can also hame multiple rotations.
                    if (name.substring(0, 4).equals(
                            spritename.substring(0, 4), ignoreCase = true
                        )
                    ) {
                        var frame = name[4].code - 'A'.code
                        var rotation = name[5].code - '0'.code
                        if (sprtemp[frame]!!.rotate != -1) {
                            // We already encountered this sprite, but we
                            // may need to trump it with something else
                        }
                        InstallSpriteLump(
                            j + firstspritelump, frame,
                            rotation, false
                        )
                        if (name.length >= 7) {
                            frame = name[6].code - 'A'.code
                            rotation = name[7].code - '0'.code
                            InstallSpriteLump(
                                j + firstspritelump, frame,
                                rotation, true
                            )
                        }
                    }
                })

                // check the frames that were found for completeness
                if ((++maxframe).also { _sprites[i].numframes = it } != 0) // killough
                // 1/31/98
                {
                    var frame: Int
                    frame = 0
                    while (frame < maxframe) {
                        when (sprtemp[frame]!!.rotate) {
                            -1 ->                                 // no rotations were found for that frame at all
                                DOOM.doomSystem.Error(
                                    "R_InitSprites: No patches found for %s frame %c",
                                    namelist[i], frame + 'A'.code
                                )
                            0 -> {}
                            1 ->                                 // must have all 8 frames
                            {
                                var rotation: Int
                                rotation = 0
                                while (rotation < 8) {
                                    if (sprtemp[frame]!!.lump[rotation] == -1) DOOM.doomSystem.Error(
                                        "R_InitSprites: Sprite %s frame %c is missing rotations",
                                        namelist[i], frame + 'A'.code
                                    )
                                    rotation++
                                }
                            }
                        }
                        frame++
                    }
                    // allocate space for the frames present and copy
                    // sprtemp to it
                    // MAES: we can do that elegantly in one line.
                    _sprites[i].copy(sprtemp as Array<spriteframe_t>, maxframe)
                }
            }
            i++
        }
    }

    /**
     * R_InitSpriteLumps Finds the width and hoffset of all sprites in the wad,
     * so the sprite does not need to be cached completely just for having the
     * header info ready during rendering.
     */
    override fun InitSpriteLumps() {
        var i: Int
        var patch: patch_t
        firstspritelump = DOOM.wadLoader.GetNumForName("S_START") + 1
        lastspritelump = DOOM.wadLoader.GetNumForName("S_END") - 1
        numspritelumps = lastspritelump - firstspritelump + 1
        spritewidth = IntArray(numspritelumps)
        spriteoffset = IntArray(numspritelumps)
        spritetopoffset = IntArray(numspritelumps)
        i = 0
        while (i < numspritelumps) {
            if (i and 63 == 0) {
                print(".")
            }
            patch = DOOM.wadLoader.CacheLumpNum(
                firstspritelump + i, Defines.PU_CACHE,
                patch_t::class.java
            )!!
            spritewidth[i] = patch.width.toInt() shl FRACBITS
            spriteoffset[i] = patch.leftoffset.toInt() shl FRACBITS
            spritetopoffset[i] = patch.topoffset.toInt() shl FRACBITS
            i++
        }
    }

    /**
     * R_InstallSpriteLump Local function for R_InitSprites.
     *
     * Boom function, more suited to resource coalescing.
     *
     */
    fun InstallSpriteLump(
        lump: Int, frame: Int,
        rotation: Int, flipped: Boolean
    ) {
        var rotation = rotation
        if (frame >= SpriteManager.MAX_SPRITE_FRAMES || rotation > 8) DOOM.doomSystem.Error(
            "R_InstallSpriteLump: Bad frame characters in lump %d",
            lump
        )
        if (frame > maxframe) {
            maxframe = frame
        }
        if (rotation == 0) { // the lump should be used for all rotations
            var r: Int
            r = 0
            while (r < 8) {
                if (sprtemp[frame]!!.lump[r] == -1) {
                    sprtemp[frame]!!.lump[r] = lump - firstspritelump
                    sprtemp[frame]!!.flip[r] = (if (flipped) 1 else 0).toByte()
                    sprtemp[frame]!!.rotate = 0 // jff 4/24/98 if any subbed,
                    // rotless
                }
                r++
            }
            return
        }

        // the lump is only used for one rotation
        if (sprtemp[frame]!!.lump[--rotation] == -1) {
            sprtemp[frame]!!.lump[rotation] = lump - firstspritelump
            sprtemp[frame]!!.flip[rotation] = (if (flipped) 1 else 0).toByte()
            sprtemp[frame]!!.rotate = 1 // jff 4/24/98 only change if rot
            // used
        }
    }

    /**
     * R_InitSprites Called at program start.
     *
     */
    override fun InitSprites(namelist: Array<String>?) {
        InitSpriteDefs(namelist)
    }

    protected fun SpriteNameHash(ss: String): Int {
        return ss.substring(0, 4).hashCode()
    }

    // GETTERS
    override fun getFirstSpriteLump(): Int {
        return firstspritelump
    }

    override fun getNumSprites(): Int {
        return numsprites
    }

    override fun getSprites(): Array<spritedef_t> {
        return _sprites
    }

    override fun getSprite(index: Int): spritedef_t {
        return _sprites[index]
    }

    override fun getSpriteWidth(): IntArray {
        return spritewidth
    }

    override fun getSpriteOffset(): IntArray {
        return spriteoffset
    }

    override fun getSpriteTopOffset(): IntArray {
        return spritetopoffset
    }

    override fun getSpriteWidth(index: Int): Int {
        return spritewidth[index]
    }

    override fun getSpriteOffset(index: Int): Int {
        return spriteoffset[index]
    }

    override fun getSpriteTopOffset(index: Int): Int {
        return spritetopoffset[index]
    } // Some unused shit

    /*
         * R_InstallSpriteLump Local function for R_InitSprites.
         * 
         * Older function, closer to linuxdoom. Using Boom-derived one instead.
         */
    /*
        protected final void InstallSpriteLump(int lump, int frame,
                int rotation, boolean flipped) {

            // System.out.println("Trying to install "+spritename+" Frame "+
            // (char)('A'+frame)+" rot "+(rotation)
            // +" . Should have rotations: "+sprtemp[frame].rotate);
            int r;

            if (frame >= 29 || rotation > 8)
                I.Error("R_InstallSpriteLump: Bad frame characters in lump %i",
                        lump);

            if ((int) frame > maxframe)
                maxframe = frame;

            // A rotation value of 0 means that we are either checking the first
            // frame of a sprite that HAS rotations, or something that has no
            // rotations at all. The value of rotate doesn't really help us
            // discern here, unless set to "false" a-priori...which can't happen
            // ?!

            if (rotation == 0) {
                
                 // MAES: notice how comparisons are done with strict literals
                 // (true and false) which are actually defined to be 0 and 1,
                 // rather than assuming that true is "any nonzero value". This
                 // happens because rotate's value could be -1 at this point (!),
                 // if a series of circumstances occur. Therefore it's actually a
                 // "tri-state", and the comparison 0==false and
                 // "anything else"==true was not good enough in this case. A
                 // value of -1 doesn't yield either true or false here.
                
                // the lump should be used for all rotations
                if (sprtemp[frame].rotate == 0) {
                    
                     // MAES: Explanation: we stumbled upon this lump before, and
                     // decided that this frame should have no more rotations,
                     // hence we found an error and we bomb everything.
                    
                    I.Error("R_InitSprites: Sprite %s frame %c has multiple rot=0 lump",
                            spritename, 'A' + frame);
                }

                // This should NEVER happen!
                if (sprtemp[frame].rotate == 1) {
                    
                     // MAES: This can only happen if we decided that a sprite's
                     // frame was already decided to have rotations, but now we
                     // stumble upon another occurence of "rotation 0". Or if you
                     // use naive true/false evaluation for .rotate ( -1 is also
                     // an admissible value).

                    I.Error("R_InitSprites: Sprite %s frame %c has rotations and a rot=0 lump",
                            spritename, 'A' + frame);
                }

                // Rotation is acknowledged to be totally false at this point.
                sprtemp[frame].rotate = 0;
                for (r = 0; r < 8; r++) {
                    sprtemp[frame].lump[r] = (short) (lump - firstspritelump);
                    sprtemp[frame].flip[r] = (byte) (flipped ? 1 : 0);
                }
                return;
            }

            // the lump is only used for one rotation
            if (sprtemp[frame].rotate == 0)
                I.Error("R_InitSprites: Sprite %s frame %c has rotations and a rot=0 lump",
                        spritename, 'A' + frame);

            sprtemp[frame].rotate = 1;

            // make 0 based
            rotation--;
            if (sprtemp[frame].lump[rotation] == -1) {
                // FUN FACT: with resource coalesing, this is no longer an
                // error.
                // I.Error
                // ("R_InitSprites: Sprite %s : %c : %c has two lumps mapped to it",
                // spritename, 'A'+frame, '1'+rotation);

                // Everything is OK, we can bless the temporary sprite's frame's
                // rotation.
                sprtemp[frame].lump[rotation] = (short) (lump - firstspritelump);
                sprtemp[frame].flip[rotation] = (byte) (flipped ? 1 : 0);
                sprtemp[frame].rotate = 1; // jff 4/24/98 only change if rot
                                            // used
            }
        }
        */
    /*
         * OLDER, UNUSED VERSION
         * 
         * R_InitSpriteDefs Pass a null terminated list of sprite names (4 chars
         * exactly) to be used. Builds the sprite rotation matrixes to account
         * for horizontally flipped sprites. Will report an error if the lumps
         * are inconsistent. Only called at startup.
         * 
         * Sprite lump names are 4 characters for the actor, a letter for the
         * frame, and a number for the rotation. A sprite that is flippable will
         * have an additional letter/number appended. The rotation character can
         * be 0 to signify no rotations.
         */
    /*  public void InitSpriteDefs2(String[] namelist) {

            int intname;
            int frame;
            int rotation;
            int start;
            int end;
            int patched;

            if (namelist == null)
                return;
            numsprites = namelist.length;

            if (numsprites == 0)
                return;

            sprites = new spritedef_t[numsprites];
            C2JUtils.initArrayOfObjects(sprites);

            start = firstspritelump - 1;
            end = lastspritelump + 1;

            // scan all the lump names for each of the names,
            // noting the highest frame letter.
            // Just compare 4 characters as ints
            for (int i = 0; i < numsprites; i++) {
                // System.out.println("Preparing sprite "+i);
                spritename = namelist[i];

                // The original code actually set everything to "-1"
                // here, including the "boolean" rotate value. The idea was 
                // to create a "tristate" of sorts, where -1 means a 
                // sprite of uncertain status. Goto InstallSpriteLumps
                // for more.
                for (int j = 0; j < sprtemp.length; j++) {
                    Arrays.fill(sprtemp[j].flip, (byte) -1);
                    Arrays.fill(sprtemp[j].lump, (short) -1);
                    // This should be INDETERMINATE at this point.
                    sprtemp[j].rotate = -1;
                }

                maxframe = -1;
                intname = name8.getIntName(namelist[i].toUpperCase());

                // scan the lumps,
                // filling in the frames for whatever is found
                for (int l = start + 1; l < end; l++) {
                    // We HOPE it has 8 characters.
                    char[] cname = W.GetLumpInfo(l).name.toCharArray();
                    if (cname.length == 6 || cname.length == 8) // Sprite names
                                                                // must be this
                                                                // way

                        // If the check is successful, we keep looking for more
                        // frames
                        // for a particular sprite e.g. TROOAx, TROOHxHy etc.
                        //
                        if (W.GetLumpInfo(l).intname == intname) {
                            frame = cname[4] - 'A';
                            rotation = cname[5] - '0';

                            if (DM.modifiedgame)
                                patched = W
                                        .GetNumForName(W.GetLumpInfo(l).name);
                            else
                                patched = l;

                            InstallSpriteLump2(patched, frame, rotation, false);

                            // Second set of rotations?
                            if (cname.length > 6 && cname[6] != 0) {
                                frame = cname[6] - 'A';
                                rotation = cname[7] - '0';
                                InstallSpriteLump2(l, frame, rotation, true);
                            }
                        }
                }

                // check the frames that were found for completeness
                // This can only be -1 at this point if we didn't install
                // a single frame successfuly.
                //
                if (maxframe == -1) {
                    // System.out.println("Sprite "+spritename+" has no frames!");
                    getSprites()[i].numframes = 0;
                    // We move on to the next sprite with this one.
                    continue;
                }

                maxframe++;

                for (frame = 0; frame < maxframe; frame++) {
                    switch ((int) sprtemp[frame].rotate) {
                    case -1:
                        // no rotations were found for that frame at all
                        I.Error("R_InitSprites: No patches found for %s frame %c",
                                namelist[i], frame + 'A');
                        break;

                    case 0:
                        // only the first rotation is needed
                        break;

                    case 1:
                        // must have all 8 frames
                        for (rotation = 0; rotation < 8; rotation++)
                            if (sprtemp[frame].lump[rotation] == -1)
                                I.Error("R_InitSprites: Sprite %s frame %c is missing rotations",
                                        namelist[i], frame + 'A');
                        break;
                    }
                }

                // allocate space for the frames present and copy sprtemp to it
                // MAES: we can do that elegantly in one line.

                sprites[i].copy(sprtemp, maxframe);

                // sprites[i].numframes = maxframe;
                // sprites[i].spriteframes = new spriteframe_t[maxframe];
                // C2JUtils.initArrayOfObjects(sprites[i].spriteframes,spriteframe_t.class);

                // for (int j=0;j<)
                // System.arraycopy(src, srcPos, dest, destPos, length)
                // memcpy (sprites[i].spriteframes, sprtemp,
                // maxframe*sizeof(spriteframe_t));
            }

        }
        */
    companion object {
        /** There seems to be an arbitrary limit of 29 distinct frames per THING  */
        const val MAX_SPRITE_FRAMES = 29
    }
}