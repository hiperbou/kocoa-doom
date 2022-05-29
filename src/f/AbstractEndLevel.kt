package f


import data.Defines
import w.animenum_t

abstract class AbstractEndLevel {
    companion object {
        //NET GAME STUFF
        const val NG_STATSY = 50
        const val NG_SPACINGX = 64

        //DEATHMATCH STUFF
        const val DM_MATRIXX = 42
        const val DM_MATRIXY = 68
        const val DM_SPACINGX = 40
        const val DM_TOTALSX = 269
        const val DM_KILLERSX = 10
        const val DM_KILLERSY = 100
        const val DM_VICTIMSX = 5
        const val DM_VICTIMSY = 50

        // static point_t lnodes[NUMEPISODES][NUMMAPS] 
        val lnodes = arrayOf(
            arrayOf(
                point_t(185, 164),  // location of level 0 (CJ)
                point_t(148, 143),  // location of level 1 new point_t(CJ)
                point_t(69, 122),  // location of level 2 new point_t(CJ)
                point_t(209, 102),  // location of level 3 new point_t(CJ)
                point_t(116, 89),  // location of level 4 new point_t(CJ)
                point_t(166, 55),  // location of level 5 new point_t(CJ)
                point_t(71, 56),  // location of level 6 new point_t(CJ)
                point_t(135, 29),  // location of level 7 new point_t(CJ)
                point_t(71, 24) // location of level 8 new point_t(CJ)
            ), arrayOf(
                point_t(254, 25),  // location of level 0 new point_t(CJ)
                point_t(97, 50),  // location of level 1 new point_t(CJ)
                point_t(188, 64),  // location of level 2 new point_t(CJ)
                point_t(128, 78),  // location of level 3 new point_t(CJ)
                point_t(214, 92),  // location of level 4 new point_t(CJ)
                point_t(133, 130),  // location of level 5 new point_t(CJ)
                point_t(208, 136),  // location of level 6 new point_t(CJ)
                point_t(148, 140),  // location of level 7 new point_t(CJ)
                point_t(235, 158) // location of level 8 new point_t(CJ)
            ), arrayOf(
                point_t(156, 168),  // location of level 0 new point_t(CJ)
                point_t(48, 154),  // location of level 1 new point_t(CJ)
                point_t(174, 95),  // location of level 2 new point_t(CJ)
                point_t(265, 75),  // location of level 3 new point_t(CJ)
                point_t(130, 48),  // location of level 4 new point_t(CJ)
                point_t(279, 23),  // location of level 5 new point_t(CJ)
                point_t(198, 48),  // location of level 6 new point_t(CJ)
                point_t(140, 25),  // location of level 7 new point_t(CJ)
                point_t(281, 136) // location of level 8 new point_t(CJ)
            )
        )

        //
        //Animation locations for episode 0 (1).
        //Using patches saves a lot of space,
        //as they replace 320x200 full screen frames.
        //
        val epsd0animinfo = arrayOf(
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(224, 104)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(184, 160)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(112, 136)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(72, 112)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(88, 96)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(64, 48)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(192, 40)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(136, 16)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(80, 16)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(64, 24))
        )
        val epsd1animinfo = arrayOf(
            anim_t(animenum_t.ANIM_LEVEL, Defines.TICRATE / 3, 1, point_t(128, 136), 1),
            anim_t(animenum_t.ANIM_LEVEL, Defines.TICRATE / 3, 1, point_t(128, 136), 2),
            anim_t(animenum_t.ANIM_LEVEL, Defines.TICRATE / 3, 1, point_t(128, 136), 3),
            anim_t(animenum_t.ANIM_LEVEL, Defines.TICRATE / 3, 1, point_t(128, 136), 4),
            anim_t(animenum_t.ANIM_LEVEL, Defines.TICRATE / 3, 1, point_t(128, 136), 5),
            anim_t(animenum_t.ANIM_LEVEL, Defines.TICRATE / 3, 1, point_t(128, 136), 6),
            anim_t(animenum_t.ANIM_LEVEL, Defines.TICRATE / 3, 1, point_t(128, 136), 7),
            anim_t(animenum_t.ANIM_LEVEL, Defines.TICRATE / 3, 3, point_t(192, 144), 8),
            anim_t(animenum_t.ANIM_LEVEL, Defines.TICRATE / 3, 1, point_t(128, 136), 8)
        )
        val epsd2animinfo = arrayOf(
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(104, 168)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(40, 136)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(160, 96)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(104, 80)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 3, 3, point_t(120, 32)),
            anim_t(animenum_t.ANIM_ALWAYS, Defines.TICRATE / 4, 3, point_t(40, 0))
        )

        /*static int NUMANIMS[NUMEPISODES] =
	{
	 sizeof(epsd0animinfo)/sizeof(anim_t),
	 sizeof(epsd1animinfo)/sizeof(anim_t),
	 sizeof(epsd2animinfo)/sizeof(anim_t)
	};*/
        // MAES: cute, but we can do it in a more Java-friendly way :-p
        val NUMANIMS = intArrayOf(
            AbstractEndLevel.epsd0animinfo.size,
            AbstractEndLevel.epsd1animinfo.size,
            AbstractEndLevel.epsd2animinfo.size
        )

        /** ATTENTION: there's a difference between these "anims" and those used in p_spec.c  */
        val anims = arrayOf<Array<anim_t>>(
            AbstractEndLevel.epsd0animinfo,
            AbstractEndLevel.epsd1animinfo,
            AbstractEndLevel.epsd2animinfo
        )
    }
}