package m

import data.Defines
import data.dstrings
import data.sounds.sfxenum_t
import defines.GameMode
import defines.Language_t
import defines.gamestate_t
import defines.skill_t
import doom.*
import g.Signals.ScanCode
import g.Signals.ScanCode.*
import rr.patch_t
import s.*
import timing.DelegateTicker
import utils.C2JUtils
import v.renderers.DoomScreen
import w.DoomIO.readString
import java.io.*


class Menu<T, V>  ////////////////// CONSTRUCTOR ////////////////
    (DOOM: DoomMain<T, V>) : AbstractDoomMenu<T, V>(DOOM) {
    /** The fonts  ... must "peg" them to those from HU  */
    var hu_font = arrayOfNulls<patch_t>(Defines.HU_FONTSIZE)

    /** WTF?!  */
    var message_dontfuckwithme = false
    // int mouseSensitivity; // has default
    /** Show messages has default, 0 = off, 1 = on  */
    override var showMessages = false


    /**
     * showMessages can be read outside of Menu, but not modified. Menu has the
     * actual C definition (not declaration)
     */
    /*override fun getShowMessages(): Boolean {
        return _showMessages
    }

    override fun setShowMessages(`val`: Boolean) {
        _showMessages = `val`
    }*/

    /** Blocky mode, has default, 0 = high, 1 = normal  */
    override var detailLevel = 0
    override var screenBlocks = 10 // has default

    /** temp for screenblocks (0-9)  */
    var screenSize = 0

    /** -1 = no quicksave slot picked!  */
    var quickSaveSlot = 0

    /** 1 = message to be printed  */
    var messageToPrint = false

    /** ...and here is the message string!  */
    var messageString: String? = null

    /** message x & y  */
    var messx = 0
    var messy = 0
    var messageLastMenuActive = false

    /** timed message = no input from user  */
    var messageNeedsInput = false

    /** Probably I need some MessageRoutine interface at this point?  */
    var messageRoutine: MenuRoutine? = null

    /** we are going to be entering a savegame string  */
    var saveStringEnter = false
    var saveSlot // which slot to save in
            = 0
    var saveCharIndex // which char we're editing
            = 0

    /** old save description before edit  */
    var saveOldString = CharArray(Defines.SAVESTRINGSIZE)
    var inhelpscreens = false
    var savegamestrings = Array(10) { CharArray(Defines.SAVESTRINGSIZE) }
    var endstring = String()
    //
    // MENU TYPEDEFS
    //
    /** menu item skull is on  */
    var itemOn: Short = 0

    /** skull animation counter  */
    var skullAnimCounter: Short = 0

    /** which skull to draw  */
    var whichSkull: Short = 0

    /** current menudef  */ // MAES: pointer? array?
    var currentMenu: menu_t? = null
    //
    // DOOM MENU
    //
    // MAES: was an enum called "main_e" used purely as numerals. No need for
    // strong typing.
    /**
     * MenuRoutine class definitions, replacing "function pointers".
     */
    lateinit var ChangeDetail: MenuRoutine
    lateinit var ChangeMessages: MenuRoutine
    lateinit var ChangeSensitivity: MenuRoutine
    lateinit var ChooseSkill: MenuRoutine
    lateinit var EndGame: MenuRoutine
    lateinit var EndGameResponse: MenuRoutine
    lateinit var Episode: MenuRoutine
    lateinit var FinishReadThis: MenuRoutine
    lateinit var LoadGame: MenuRoutine
    lateinit var LoadSelect: MenuRoutine
    lateinit var MusicVol: MenuRoutine
    lateinit var NewGame: MenuRoutine
    lateinit var Options: MenuRoutine
    lateinit var VerifyNightmare: MenuRoutine
    lateinit var SaveSelect: MenuRoutine
    lateinit var SfxVol: MenuRoutine
    lateinit var SizeDisplay: MenuRoutine
    lateinit var SaveGame: MenuRoutine
    lateinit var Sound: MenuRoutine
    lateinit var QuitDOOM: MenuRoutine
    lateinit var QuitResponse: MenuRoutine
    lateinit var QuickLoadResponse: MenuRoutine
    lateinit var QuickSaveResponse: MenuRoutine
    lateinit var ReadThis: MenuRoutine
    lateinit var ReadThis2: MenuRoutine

    /** DrawRoutine class definitions, replacing "function pointers".  */
    lateinit var DrawEpisode: DrawRoutine
    lateinit var DrawLoad: DrawRoutine
    lateinit var DrawMainMenu: DrawRoutine
    lateinit var DrawNewGame: DrawRoutine
    lateinit var DrawOptions: DrawRoutine
    lateinit var DrawReadThis1: DrawRoutine
    lateinit var DrawReadThis2: DrawRoutine
    lateinit var DrawSave: DrawRoutine
    lateinit var DrawSound: DrawRoutine

    /** Initialize menu routines first  */
    private fun initMenuRoutines() {
        ChangeMessages = M_ChangeMessages()
        ChangeDetail = M_ChangeDetail()
        ChangeSensitivity = M_ChangeSensitivity()
        ChooseSkill = M_ChooseSkill()
        EndGame = M_EndGame()
        EndGameResponse = M_EndGameResponse()
        Episode = M_Episode()
        FinishReadThis = M_FinishReadThis()
        LoadGame = M_LoadGame()
        LoadSelect = M_LoadSelect()
        MusicVol = M_MusicVol()
        NewGame = M_NewGame()
        Options = M_Options()
        QuitDOOM = M_QuitDOOM()
        QuickLoadResponse = M_QuickLoadResponse()
        QuickSaveResponse = M_QuickSaveResponse()
        QuitResponse = M_QuitResponse()
        ReadThis = M_ReadThis()
        ReadThis2 = M_ReadThis2()
        SaveGame = M_SaveGame()
        SaveSelect = M_SaveSelect()
        SfxVol = M_SfxVol()
        SizeDisplay = M_SizeDisplay()
        Sound = M_Sound()
        VerifyNightmare = M_VerifyNightmare()
    }

    /** Then drawroutines  */
    private fun initDrawRoutines() {
        DrawEpisode = M_DrawEpisode()
        DrawNewGame = M_DrawNewGame()
        DrawReadThis1 = M_DrawReadThis1()
        DrawReadThis2 = M_DrawReadThis2()
        DrawOptions = M_DrawOptions()
        DrawLoad = M_DrawLoad()
        DrawSave = M_DrawSave()
        DrawSound = M_DrawSound()
        DrawMainMenu = M_DrawMainMenu()
    }

    /** Menuitem definitions. A "menu" can consist of multiple menuitems  */
    lateinit var MainMenu: Array<menuitem_t>
    lateinit var EpisodeMenu: Array<menuitem_t>
    lateinit var NewGameMenu: Array<menuitem_t>
    lateinit var OptionsMenu: Array<menuitem_t>
    lateinit var ReadMenu1: Array<menuitem_t>
    lateinit var ReadMenu2: Array<menuitem_t>
    lateinit var SoundMenu: Array<menuitem_t>
    lateinit var LoadMenu: Array<menuitem_t>
    lateinit var SaveMenu: Array<menuitem_t>

    /** Actual menus. Each can point to an array of menuitems  */
    var MainDef: menu_t? = null
    var EpiDef: menu_t? = null
    var NewDef: menu_t? = null
    var OptionsDef: menu_t? = null
    var ReadDef1: menu_t? = null
    var ReadDef2: menu_t? = null
    var SoundDef: menu_t? = null
    var LoadDef: menu_t? = null
    var SaveDef: menu_t? = null

    /** First initialize those  */
    private fun initMenuItems() {
        MainMenu = arrayOf(
            menuitem_t(1, "M_NGAME", NewGame, SC_N),
            menuitem_t(1, "M_OPTION", Options, SC_O),
            menuitem_t(1, "M_LOADG", LoadGame, SC_L),
            menuitem_t(1, "M_SAVEG", SaveGame, SC_S),  // Another hickup with Special edition.
            menuitem_t(1, "M_RDTHIS", ReadThis, SC_R),
            menuitem_t(1, "M_QUITG", QuitDOOM, SC_Q)
        )
        MainDef = menu_t(main_end, null, MainMenu, DrawMainMenu, 97, 64, 0)

        //
        // EPISODE SELECT
        //
        EpisodeMenu = arrayOf(
            menuitem_t(1, "M_EPI1", Episode, SC_K),
            menuitem_t(1, "M_EPI2", Episode, SC_T),
            menuitem_t(1, "M_EPI3", Episode, SC_I),
            menuitem_t(1, "M_EPI4", Episode, SC_T)
        )
        EpiDef = menu_t(
            ep_end,  // # of menu items
            MainDef,  // previous menu
            EpisodeMenu,  // menuitem_t ->
            DrawEpisode,  // drawing routine ->
            48, 63,  // x,y
            ep1 // lastOn
        )

        //
        // NEW GAME
        //
        NewGameMenu = arrayOf(
            menuitem_t(1, "M_JKILL", ChooseSkill, ScanCode.SC_I),
            menuitem_t(1, "M_ROUGH", ChooseSkill, ScanCode.SC_H),
            menuitem_t(1, "M_HURT", ChooseSkill, ScanCode.SC_H),
            menuitem_t(1, "M_ULTRA", ChooseSkill, ScanCode.SC_U),
            menuitem_t(1, "M_NMARE", ChooseSkill, ScanCode.SC_N)
        )
        NewDef = menu_t(
            newg_end,  // # of menu items
            EpiDef,  // previous menu
            NewGameMenu,  // menuitem_t ->
            DrawNewGame,  // drawing routine ->
            48, 63,  // x,y
            hurtme // lastOn
        )

        //
        // OPTIONS MENU
        //
        OptionsMenu = arrayOf(
            menuitem_t(1, "M_ENDGAM", EndGame, ScanCode.SC_3),
            menuitem_t(1, "M_MESSG", ChangeMessages, ScanCode.SC_M),
            menuitem_t(1, "M_DETAIL", ChangeDetail, ScanCode.SC_G),
            menuitem_t(2, "M_SCRNSZ", SizeDisplay, ScanCode.SC_S),
            menuitem_t(-1, "", null),
            menuitem_t(2, "M_MSENS", ChangeSensitivity, ScanCode.SC_M),
            menuitem_t(-1, "", null),
            menuitem_t(1, "M_SVOL", Sound, ScanCode.SC_S)
        )
        OptionsDef = menu_t(opt_end, MainDef, OptionsMenu, DrawOptions, 60, 37, 0)

        // Read This! MENU 1 
        ReadMenu1 = arrayOf(menuitem_t(1, "", ReadThis2, ScanCode.SC_0))
        ReadDef1 = menu_t(read1_end, MainDef, ReadMenu1, DrawReadThis1, 280, 185, 0)

        // Read This! MENU 2
        ReadMenu2 = arrayOf(menuitem_t(1, "", FinishReadThis, ScanCode.SC_0))
        ReadDef2 = menu_t(read2_end, ReadDef1, ReadMenu2, DrawReadThis2, 330, 175, 0)

        //
        // SOUND VOLUME MENU
        //
        SoundMenu = arrayOf(
            menuitem_t(2, "M_SFXVOL", SfxVol, ScanCode.SC_S),
            menuitem_t(-1, "", null),
            menuitem_t(2, "M_MUSVOL", MusicVol, ScanCode.SC_M),
            menuitem_t(-1, "", null)
        )
        SoundDef = menu_t(sound_end, OptionsDef, SoundMenu, DrawSound, 80, 64, 0)

        //
        // LOAD GAME MENU
        //
        LoadMenu = arrayOf(
            menuitem_t(1, "", LoadSelect, ScanCode.SC_1),
            menuitem_t(1, "", LoadSelect, ScanCode.SC_2),
            menuitem_t(1, "", LoadSelect, ScanCode.SC_3),
            menuitem_t(1, "", LoadSelect, ScanCode.SC_4),
            menuitem_t(1, "", LoadSelect, ScanCode.SC_5),
            menuitem_t(1, "", LoadSelect, ScanCode.SC_6)
        )
        LoadDef = menu_t(load_end, MainDef, LoadMenu, DrawLoad, 80, 54, 0)

        //
        // SAVE GAME MENU
        //
        SaveMenu = arrayOf(
            menuitem_t(1, "", SaveSelect, ScanCode.SC_1),
            menuitem_t(1, "", SaveSelect, ScanCode.SC_2),
            menuitem_t(1, "", SaveSelect, ScanCode.SC_3),
            menuitem_t(1, "", SaveSelect, ScanCode.SC_4),
            menuitem_t(1, "", SaveSelect, ScanCode.SC_5),
            menuitem_t(1, "", SaveSelect, ScanCode.SC_6)
        )
        SaveDef = menu_t(load_end, MainDef, SaveMenu, DrawSave, 80, 54, 0)
    }

    /**
     * M_ReadSaveStrings
     * read the strings from the savegame files
     */
    fun ReadSaveStrings() {
        var handle: DataInputStream
        var count: Int
        var i: Int
        var name: String
        i = 0
        while (i < load_end) {
            name =
                if (DOOM.cVarManager.bool(CommandVariable.CDROM)) "c:\\doomdata\\" + dstrings.SAVEGAMENAME + i + ".dsg" else dstrings.SAVEGAMENAME + i + ".dsg"
            try {
                handle = DataInputStream(BufferedInputStream(FileInputStream(name)))
                savegamestrings[i] = readString(handle, Defines.SAVESTRINGSIZE)!!.toCharArray()
                handle.close()
                LoadMenu[i].status = 1
            } catch (e: IOException) {
                savegamestrings[i][0] = 0x00.toChar()
                LoadMenu[i].status = 0
                i++
                continue
            }
            i++
        }
    }

    /**
     * Draw border for the savegame description. This is special in that it's
     * not "invokable" like the other drawroutines, but standalone.
     */
    private fun DrawSaveLoadBorder(x: Int, y: Int) {
        var x = x
        var i: Int
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            DOOM.wadLoader.CachePatchName("M_LSLEFT"),
            DOOM.vs,
            x - 8,
            y + 7
        )
        i = 0
        while (i < 24) {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                DOOM.wadLoader.CachePatchName("M_LSCNTR"),
                DOOM.vs,
                x,
                y + 7
            )
            x += 8
            i++
        }
        DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName("M_LSRGHT"), DOOM.vs, x, y + 7)
    }

    /** Draws slider rail of a specified width (each notch is 8 base units wide)
     * and with a slider selector at position thermDot.
     *
     * @param x
     * @param y
     * @param thermWidth
     * @param thermDot
     */
    fun DrawThermo(x: Int, y: Int, thermWidth: Int, thermDot: Int) {
        var xx = x
        DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName("M_THERML"), DOOM.vs, xx, y)
        xx += 8
        for (i in 0 until thermWidth) {
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName("M_THERMM"), DOOM.vs, xx, y)
            xx += 8
        }
        DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName("M_THERMR"), DOOM.vs, xx, y)
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            DOOM.wadLoader.CachePatchName("M_THERMO"),
            DOOM.vs,
            x + 8 + thermDot * 8,
            y
        )
    }

    fun DrawEmptyCell(menu: menu_t, item: Int) {
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            DOOM.wadLoader.CacheLumpName("M_CELL1", Defines.PU_CACHE, patch_t::class.java),
            DOOM.vs,
            menu.x - 10,
            menu.y + item * LINEHEIGHT - 1
        )
    }

    fun DrawSelCell(menu: menu_t, item: Int) {
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG,
            DOOM.wadLoader.CacheLumpName("M_CELL2", Defines.PU_CACHE, patch_t::class.java),
            DOOM.vs,
            menu.x - 10,
            menu.y + item * LINEHEIGHT - 1
        )
    }

    //
    // M_SaveGame & Cie.
    //
    inner class M_DrawSave : DrawRoutine {
        override fun invoke() {
            var i: Int
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName("M_SAVEG"), DOOM.vs, 72, 28)
            i = 0
            while (i < load_end) {
                DrawSaveLoadBorder(LoadDef!!.x, LoadDef!!.y + LINEHEIGHT * i)
                WriteText(LoadDef!!.x, LoadDef!!.y + LINEHEIGHT * i, savegamestrings[i])
                i++
            }
            if (saveStringEnter) {
                i = StringWidth(savegamestrings[saveSlot])
                WriteText(LoadDef!!.x + i, LoadDef!!.y + LINEHEIGHT * saveSlot, "_")
            }
        }
    }

    /**
     * M_Responder calls this when user is finished
     *
     * @param slot
     */
    fun DoSave(slot: Int) {
        DOOM.SaveGame(slot, String(savegamestrings[slot]))
        ClearMenus()

        // PICK QUICKSAVE SLOT YET?
        if (quickSaveSlot == -2) quickSaveSlot = slot
    }

    /**
     * User wants to save. Start string input for M_Responder
     */
    internal inner class M_SaveSelect : MenuRoutine {
        override fun invoke(choice: Int) {
            // we are going to be intercepting all chars
            //System.out.println("ACCEPTING typing input");
            saveStringEnter = true
            saveSlot = choice
            C2JUtils.strcpy(saveOldString, savegamestrings[choice])
            if (C2JUtils.strcmp(savegamestrings[choice], englsh.EMPTYSTRING)) savegamestrings[choice][0] = 0.toChar()
            saveCharIndex = C2JUtils.strlen(savegamestrings[choice])
        }
    }

    /**
     * Selected from DOOM menu
     */
    internal inner class M_SaveGame : MenuRoutine {
        override fun invoke(choice: Int) {
            if (!DOOM.usergame) {
                StartMessage(englsh.SAVEDEAD, null, false)
                return
            }
            if (DOOM.gamestate != gamestate_t.GS_LEVEL) return
            SetupNextMenu(SaveDef)
            ReadSaveStrings()
        }
    }

    //
    // M_QuickSave
    //
    private var tempstring: String? = null

    internal inner class M_QuickSaveResponse : MenuRoutine {
        override fun invoke(ch: Int) {
            if (ch == 'y'.code) {
                DoSave(quickSaveSlot)
                DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchx)
            }
        }
    }

    private fun QuickSave() {
        if (!DOOM.usergame) {
            DOOM.doomSound.StartSound(null, sfxenum_t.sfx_oof)
            return
        }
        if (DOOM.gamestate != gamestate_t.GS_LEVEL) return
        if (quickSaveSlot < 0) {
            StartControlPanel()
            ReadSaveStrings()
            SetupNextMenu(SaveDef)
            quickSaveSlot = -2 // means to pick a slot now
            return
        }
        tempstring = String.format(englsh.QSPROMPT, C2JUtils.nullTerminatedString(savegamestrings[quickSaveSlot]))
        StartMessage(tempstring, QuickSaveResponse, true)
    }

    //
    // M_QuickLoad
    //
    internal inner class M_QuickLoadResponse : MenuRoutine {
        override fun invoke(ch: Int) {
            if (ch == 'y'.code) {
                LoadSelect!!.invoke(quickSaveSlot)
                DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchx)
            }
        }
    }

    internal inner class M_QuitResponse : MenuRoutine {
        override fun invoke(ch: Int) {
            if (ch != 'y'.code) return
            if (!DOOM.netgame) {
                if (DOOM.isCommercial()) DOOM.doomSound.StartSound(
                    null,
                    quitsounds2[DOOM.gametic shr 2 and 7]
                ) else DOOM.doomSound.StartSound(null, quitsounds[DOOM.gametic shr 2 and 7])
                // TI.WaitVBL(105);
            }
            DOOM.doomSystem.Quit()
        }
    }

    fun QuickLoad() {
        if (DOOM.netgame) {
            StartMessage(englsh.QLOADNET, null, false)
            return
        }
        if (quickSaveSlot < 0) {
            StartMessage(englsh.QSAVESPOT, null, false)
            return
        }
        tempstring = String.format(englsh.QLPROMPT, C2JUtils.nullTerminatedString(savegamestrings[quickSaveSlot]))
        StartMessage(tempstring, QuickLoadResponse, true)
    }

    internal inner class M_Sound : MenuRoutine {
        override fun invoke(choice: Int) {
            SetupNextMenu(SoundDef)
        }
    }

    internal inner class M_SfxVol : MenuRoutine {
        override fun invoke(choice: Int) {
            when (choice) {
                0 -> if (DOOM.snd_SfxVolume != 0) DOOM.snd_SfxVolume--
                1 -> if (DOOM.snd_SfxVolume < 15) DOOM.snd_SfxVolume++
            }
            DOOM.doomSound.SetSfxVolume(DOOM.snd_SfxVolume * 8)
        }
    }

    internal inner class M_MusicVol : MenuRoutine {
        override fun invoke(choice: Int) {
            when (choice) {
                0 -> if (DOOM.snd_MusicVolume != 0) DOOM.snd_MusicVolume--
                1 -> if (DOOM.snd_MusicVolume < 15) DOOM.snd_MusicVolume++
            }
            DOOM.doomSound.SetMusicVolume(DOOM.snd_MusicVolume * 8)
        }
    }

    //
    // M_Episode
    //
    private var epi = 0

    internal inner class M_VerifyNightmare : MenuRoutine {
        override fun invoke(ch: Int) {
            if (ch != 'y'.code) return
            DOOM.DeferedInitNew(skill_t.sk_nightmare, epi + 1, 1)
            ClearMenus()
        }
    }

    /**
     * M_ReadThis
     */
    internal inner class M_ReadThis : MenuRoutine {
        override fun invoke(choice: Int) {
            var choice = choice
            choice = 0
            SetupNextMenu(ReadDef1)
        }
    }

    internal inner class M_ReadThis2 : MenuRoutine {
        override fun invoke(choice: Int) {
            var choice = choice
            choice = 0
            SetupNextMenu(ReadDef2)
        }
    }

    internal inner class M_FinishReadThis : MenuRoutine {
        override fun invoke(choice: Int) {
            var choice = choice
            choice = 0
            SetupNextMenu(MainDef)
        }
    }

    //
    // M_QuitDOOM
    //
    internal inner class M_QuitDOOM : MenuRoutine {
        override fun invoke(choice: Int) {
            // We pick index 0 which is language sensitive,
            // or one at random, between 1 and maximum number.
            endstring =
                if (DOOM.language != Language_t.english) """
     ${dstrings.endmsg[0]}
     
     ${englsh.DOSY}
     """.trimIndent() else """
     ${dstrings.endmsg[DOOM.gametic % (dstrings.NUM_QUITMESSAGES - 2) + 1]}
     
     ${englsh.DOSY}
     """.trimIndent()
            StartMessage(endstring, QuitResponse, true)
        }
    }

    internal inner class M_QuitGame : MenuRoutine {
        override fun invoke(ch: Int) {
            if (ch != 'y'.code) return
            if (!DOOM.netgame) {
                if (DOOM.isCommercial()) DOOM.doomSound.StartSound(
                    null,
                    quitsounds2[DOOM.gametic shr 2 and 7]
                ) else DOOM.doomSound.StartSound(null, quitsounds[DOOM.gametic shr 2 and 7])
                DOOM.doomSystem.WaitVBL(105)
            }
            DOOM.doomSystem.Quit()
        }
    }

    internal inner class M_SizeDisplay : MenuRoutine {
        override fun invoke(choice: Int) {
            when (choice) {
                0 -> if (screenSize > 0) {
                    screenBlocks--
                    screenSize--
                }
                1 -> if (screenSize < 8) {
                    screenBlocks++
                    screenSize++
                }
            }
            DOOM.sceneRenderer.SetViewSize(screenBlocks, detailLevel)
        }
    }

    internal inner class M_Options : MenuRoutine {
        override fun invoke(choice: Int) {
            SetupNextMenu(OptionsDef)
        }
    }

    internal inner class M_NewGame : MenuRoutine {
        override fun invoke(choice: Int) {
            if (DOOM.netgame && !DOOM.demoplayback) {
                StartMessage(englsh.NEWGAME, null, false)
                return
            }
            if (DOOM.isCommercial()) SetupNextMenu(NewDef) else SetupNextMenu(EpiDef)
        }
    }

    fun StartMessage(string: String?, routine: MenuRoutine?, input: Boolean) {
        messageLastMenuActive = DOOM.menuactive
        messageToPrint = true
        messageString = string
        messageRoutine = routine
        messageNeedsInput = input
        DOOM.menuactive = true // "true"
    }

    fun StopMessage() {
        DOOM.menuactive = messageLastMenuActive
        messageToPrint = false
    }

    /**
     * Find string width from hu_font chars
     */
    fun StringWidth(string: CharArray): Int {
        var i: Int
        var w = 0
        var c: Int
        i = 0
        while (i < C2JUtils.strlen(string)) {
            c = string[i].uppercaseChar().code - Defines.HU_FONTSTART
            w += if (c < 0 || c >= Defines.HU_FONTSIZE) 4 else hu_font[c]!!.width.toInt()
            i++
        }
        return w
    }

    /**
     * Find string height from hu_font chars.
     *
     * Actually it just counts occurences of 'n' and adds height to height.
     */
    private fun StringHeight(string: CharArray): Int {
        var i: Int
        var h: Int
        val height = hu_font[0]!!.height.toInt()
        h = height
        i = 0
        while (i < string.size) {
            if (string[i] == '\n') h += height
            i++
        }
        return h
    }

    /**
     * Find string height from hu_font chars
     */
    private fun StringHeight(string: String?): Int {
        return this.StringHeight(string!!.toCharArray())
    }

    /**
     * Write a string using the hu_font
     */
    private fun WriteText(x: Int, y: Int, string: CharArray) {
        var w: Int
        val ch: CharArray
        var c: Int
        var cx: Int
        var cy: Int
        ch = string
        var chptr = 0
        cx = x
        cy = y
        while (chptr < ch.size) {
            c = ch[chptr].code
            chptr++
            if (c == 0) break
            if (c == '\n'.code) {
                cx = x
                cy += 12
                continue
            }
            c = c.toChar().uppercaseChar().code - Defines.HU_FONTSTART
            if (c < 0 || c >= Defines.HU_FONTSIZE) {
                cx += 4
                continue
            }
            w = hu_font[c]!!.width.toInt()
            if (cx + w > DOOM.vs.getScreenWidth()) break
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, hu_font[c]!!, DOOM.vs, cx, cy)
            cx += w
        }
    }

    private fun WriteText(x: Int, y: Int, string: String?) {
        if (string == null || string.length == 0) return
        var w: Int
        var cx: Int
        var cy: Int
        var chptr = 0
        var c: Char
        cx = x
        cy = y
        while (chptr < string.length) {
            c = string[chptr++]
            if (c.code == 0) break
            if (c == '\n') {
                cx = x
                cy += 12
                continue
            }
            c = (c.uppercaseChar().code - Defines.HU_FONTSTART).toChar()
            if (c.code < 0 || c.code >= Defines.HU_FONTSIZE) {
                cx += 4
                continue
            }
            w = hu_font[c.code]!!.width.toInt()
            if (cx + w > DOOM.vs.getScreenWidth()) break
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, hu_font[c.code]!!, DOOM.vs, cx, cy)
            cx += w
        }
    }

    // These belong to the responder.
    private var joywait = 0
    private var mousewait = 0
    private var mousey = 0
    private var lasty = 0
    private var mousex = 0
    private var lastx = 0
    @SourceCode.Compatible
    @SourceCode.M_Menu.C(SourceCode.M_Menu.M_Responder)
    override fun Responder(ev: event_t): Boolean {
        val sc: ScanCode
        if (ev.isType(evtype_t.ev_joystick) && joywait < DOOM.ticker.GetTime()) {
            // Joystick input
            sc = ev.mapByJoy { joyEvent ->
                val joyEvent = joyEvent!!
                var r = ScanCode.SC_NULL
                if (joyEvent.y == -1) {
                    r = ScanCode.SC_UP
                    joywait = DOOM.ticker.GetTime() + 5
                } else if (joyEvent.y == 1) {
                    r = ScanCode.SC_DOWN
                    joywait = DOOM.ticker.GetTime() + 5
                }
                if (joyEvent.x == -1) {
                    r = ScanCode.SC_LEFT
                    joywait = DOOM.ticker.GetTime() + 2
                } else if (joyEvent.x == 1) {
                    r = ScanCode.SC_RIGHT
                    joywait = DOOM.ticker.GetTime() + 2
                }
                if (joyEvent.isJoy(event_t.JOY_2)) {
                    r = ScanCode.SC_BACKSPACE
                    joywait = DOOM.ticker.GetTime() + 5
                } else if (joyEvent.isJoy(event_t.JOY_1)) {
                    r = ScanCode.SC_ENTER
                    joywait = DOOM.ticker.GetTime() + 5
                }
                r
            }
        } else if (ev.isType(evtype_t.ev_mouse) && mousewait < DOOM.ticker.GetTime()) {
            // Mouse input 
            if (ev.mapByMouse { mouseEvent ->
                    val mouseEvent = mouseEvent!!
                    var r = ScanCode.SC_NULL
                    mousey += mouseEvent.y
                    if (mousey < lasty - 30) {
                        r = ScanCode.SC_DOWN
                        mousewait = DOOM.ticker.GetTime() + 5
                        lasty -= 30
                        mousey = lasty
                    } else if (mousey > lasty + 30) {
                        r = ScanCode.SC_UP
                        mousewait = DOOM.ticker.GetTime() + 5
                        lasty += 30
                        mousey = lasty
                    }
                    mousex += mouseEvent.x
                    if (mousex < lastx - 30) {
                        r = ScanCode.SC_LEFT
                        mousewait = DOOM.ticker.GetTime() + 5
                        lastx -= 30
                        mousex = lastx
                    } else if (mousex > lastx + 30) {
                        r = ScanCode.SC_RIGHT
                        mousewait = DOOM.ticker.GetTime() + 5
                        lastx += 30
                        mousex = lastx
                    }
                    if (mouseEvent.isMouse(event_t.MOUSE_RIGHT)) {
                        r = ScanCode.SC_BACKSPACE
                        mousewait = DOOM.ticker.GetTime() + 15
                    } else if (mouseEvent.isMouse(event_t.MOUSE_LEFT)) {
                        r = ScanCode.SC_ENTER
                        mousewait = DOOM.ticker.GetTime() + 15
                    }
                    r
                }.also { sc = it } == ScanCode.SC_NULL) {
                return false
            }
        } else if (ev.isType(evtype_t.ev_keydown)) {
            sc = ev.getSC()
        } else return false

        // Save Game string input
        if (saveStringEnter) {
            when (sc) {
                ScanCode.SC_BACKSPACE -> if (saveCharIndex > 0) {
                    saveCharIndex--
                    savegamestrings[saveSlot][saveCharIndex] = 0.toChar()
                }
                ScanCode.SC_ESCAPE -> {
                    saveStringEnter = false
                    C2JUtils.strcpy(savegamestrings[saveSlot], saveOldString)
                }
                ScanCode.SC_ENTER -> {
                    saveStringEnter = false
                    if (savegamestrings[saveSlot][0].code != 0) DoSave(saveSlot)
                }
                else -> {
                    val ch = sc.c.uppercaseChar()
                    if (ch != ' ') {
                        if (ch.code.toByte() - Defines.HU_FONTSTART < 0 || ch.code.toByte() - Defines.HU_FONTSTART >= Defines.HU_FONTSIZE) {
                            return true
                        }
                    }
                    if (ch >= ' ' && ch.code <= 0x7F && saveCharIndex < Defines.SAVESTRINGSIZE - 1 && StringWidth(
                            savegamestrings[saveSlot]
                        ) < (Defines.SAVESTRINGSIZE - 2) * 8
                    ) {
                        savegamestrings[saveSlot][saveCharIndex++] = ch
                        savegamestrings[saveSlot][saveCharIndex] = 0.toChar()
                    }
                }
            }
            return true
        }

        // Take care of any messages that need input
        if (messageToPrint) {
            if (messageNeedsInput && !(sc == SC_SPACE || sc == SC_N || sc == SC_Y || sc != SC_ESCAPE)) {
                return false
            }
            DOOM.menuactive = messageLastMenuActive
            messageToPrint = false
            if (messageRoutine != null) messageRoutine!!.invoke(sc.c.code)
            DOOM.menuactive = false // "false"
            DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchx)
            return true
        }
        if (DOOM.devparm && sc == SC_F1 || sc == SC_PRTSCRN) {
            DOOM.ScreenShot()
            return true
        }

        // F-Keys
        if (!DOOM.menuactive) {
            when (sc) {
                ScanCode.SC_MINUS -> {
                    if (DOOM.automapactive || DOOM.headsUp.chat_on[0]) return false
                    SizeDisplay!!.invoke(0)
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_stnmov)
                    return true
                }
                ScanCode.SC_EQUALS -> {
                    if (DOOM.automapactive || DOOM.headsUp.chat_on[0]) return false
                    SizeDisplay!!.invoke(1)
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_stnmov)
                    return true
                }
                ScanCode.SC_F1 -> {
                    StartControlPanel()
                    currentMenu = if (DOOM.isRegistered() || DOOM.isShareware()) ReadDef2 else ReadDef1
                    itemOn = 0
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                    return true
                }
                ScanCode.SC_F2 -> {
                    StartControlPanel()
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                    SaveGame!!.invoke(0)
                    return true
                }
                ScanCode.SC_F3 -> {
                    StartControlPanel()
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                    LoadGame!!.invoke(0)
                    return true
                }
                ScanCode.SC_F4 -> {
                    StartControlPanel()
                    currentMenu = SoundDef
                    itemOn = sfx_vol.toShort()
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                    return true
                }
                ScanCode.SC_F5 -> {
                    ChangeDetail!!.invoke(0)
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                    return true
                }
                ScanCode.SC_F6 -> {
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                    QuickSave()
                    return true
                }
                ScanCode.SC_F7 -> {
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                    EndGame!!.invoke(0)
                    return true
                }
                ScanCode.SC_F8 -> {
                    ChangeMessages!!.invoke(0)
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                    return true
                }
                ScanCode.SC_F9 -> {
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                    QuickLoad()
                    return true
                }
                ScanCode.SC_F10 -> {
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                    QuitDOOM!!.invoke(0)
                    return true
                }
                ScanCode.SC_F11 -> {
                    var usegamma = DOOM.graphicSystem.getUsegamma()
                    usegamma++
                    if (usegamma > 4) usegamma = 0
                    DOOM.players[DOOM.consoleplayer].message = gammamsg[usegamma]
                    DOOM.graphicSystem.setUsegamma(usegamma)
                    DOOM.autoMap.Repalette()
                    return true
                }
                else -> {}
            }
        } else if (sc == ScanCode.SC_F5 && DOOM.ticker is DelegateTicker) { // Toggle ticker
            DOOM.ticker.changeTicker()
            System.err.println("Warning! Ticker changed; time reset")
            DOOM.doomSound.StartSound(null, sfxenum_t.sfx_radio)
            return true
        }

        // Pop-up menu?
        if (!DOOM.menuactive) {
            if (sc == ScanCode.SC_ESCAPE) {
                StartControlPanel()
                DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                return true
            }
            return false
        }
        when (sc) {
            ScanCode.SC_DOWN -> {
                do {
                    if (itemOn + 1 > currentMenu!!.numitems - 1) {
                        itemOn = 0
                    } else {
                        itemOn++
                    }
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_pstop)
                } while (currentMenu!!.menuitems[itemOn.toInt()].status == -1)
                return true
            }
            ScanCode.SC_UP -> {
                do {
                    if (itemOn.toInt() == 0) {
                        itemOn = (currentMenu!!.numitems - 1).toShort()
                    } else {
                        itemOn--
                    }
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_pstop)
                } while (currentMenu!!.menuitems[itemOn.toInt()].status == -1)
                return true
            }
            ScanCode.SC_LEFT -> {
                if (currentMenu!!.menuitems[itemOn.toInt()].routine != null && currentMenu!!.menuitems[itemOn.toInt()].status == 2) {
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_stnmov)
                    currentMenu!!.menuitems[itemOn.toInt()].routine!!.invoke(0)
                }
                return true
            }
            ScanCode.SC_RIGHT -> {
                if (currentMenu!!.menuitems[itemOn.toInt()].routine != null && currentMenu!!.menuitems[itemOn.toInt()].status == 2) {
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_stnmov)
                    currentMenu!!.menuitems[itemOn.toInt()].routine!!.invoke(1)
                }
                return true
            }
            ScanCode.SC_NPENTER, ScanCode.SC_ENTER -> {
                if (currentMenu!!.menuitems[itemOn.toInt()].routine != null && currentMenu!!.menuitems[itemOn.toInt()].status != 0) {
                    currentMenu!!.lastOn = itemOn.toInt()
                    if (currentMenu!!.menuitems[itemOn.toInt()].status == 2) {
                        currentMenu!!.menuitems[itemOn.toInt()].routine!!.invoke(1) // right
                        // arrow
                        DOOM.doomSound.StartSound(null, sfxenum_t.sfx_stnmov)
                    } else {
                        currentMenu!!.menuitems[itemOn.toInt()].routine!!.invoke(itemOn.toInt())
                        DOOM.doomSound.StartSound(null, sfxenum_t.sfx_pistol)
                    }
                }
                return true
            }
            ScanCode.SC_ESCAPE -> {
                currentMenu!!.lastOn = itemOn.toInt()
                ClearMenus()
                DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchx)
                return true
            }
            ScanCode.SC_BACKSPACE -> {
                currentMenu!!.lastOn = itemOn.toInt()
                if (currentMenu!!.prevMenu != null) {
                    currentMenu = currentMenu!!.prevMenu
                    itemOn = currentMenu!!.lastOn.toShort()
                    DOOM.doomSound.StartSound(null, sfxenum_t.sfx_swtchn)
                }
                return true
            }
            else -> {
                run {
                    var i = itemOn + 1
                    while (i < currentMenu!!.numitems) {
                        if (currentMenu!!.menuitems[i].alphaKey == sc) {
                            itemOn = i.toShort()
                            DOOM.doomSound.StartSound(null, sfxenum_t.sfx_pstop)
                            return true
                        }
                        i++
                    }
                }
                var i = 0
                while (i <= itemOn) {
                    if (currentMenu!!.menuitems[i].alphaKey == sc) {
                        itemOn = i.toShort()
                        DOOM.doomSound.StartSound(null, sfxenum_t.sfx_pstop)
                        return true
                    }
                    i++
                }
            }
        }
        return false
    }

    /**
     * M_StartControlPanel
     */
    @SourceCode.Exact
    @SourceCode.M_Menu.C(SourceCode.M_Menu.M_StartControlPanel)
    override fun StartControlPanel() {
        // intro might call this repeatedly
        if (DOOM.menuactive) {
            return
        }
        DOOM.menuactive = true
        currentMenu = MainDef // JDC
        itemOn = currentMenu!!.lastOn.toShort() // JDC
    }

    /**
     * M_Drawer Called after the view has been rendered, but before it has been
     * blitted.
     */
    override fun Drawer() {
        var x: Int
        var y: Int
        val max: Int
        val string = CharArray(40)
        val msstring: CharArray
        var start: Int
        inhelpscreens = false // Horiz. & Vertically center string and print
        // it.
        if (messageToPrint) {
            start = 0
            y = 100 - this.StringHeight(messageString) / 2
            msstring = messageString!!.toCharArray()
            while (start < messageString!!.length) {
                var i = 0
                i = 0
                while (i < messageString!!.length - start) {
                    if (msstring[start + i] == '\n') {
                        C2JUtils.memset(string, 0.toChar(), 40)
                        C2JUtils.strcpy(string, msstring, start, i)
                        start += i + 1
                        break
                    }
                    i++
                }
                if (i == messageString!!.length - start) {
                    C2JUtils.strcpy(string, msstring, start)
                    start += i
                }
                x = 160 - StringWidth(string) / 2
                this.WriteText(x, y, string)
                y += hu_font[0]!!.height.toInt()
            }
            return
        }
        if (!DOOM.menuactive) {
            return
        }
        if (currentMenu!!.routine != null) {
            currentMenu!!.routine.invoke() // call Draw routine
        }
        // DRAW MENU
        x = currentMenu!!.x
        y = currentMenu!!.y
        max = currentMenu!!.numitems
        for (i in 0 until max) {
            if (currentMenu!!.menuitems[i].name != null && "" != currentMenu!!.menuitems[i].name) {
                DOOM.graphicSystem.DrawPatchScaled(
                    DoomScreen.FG, DOOM.wadLoader.CachePatchName(
                        currentMenu!!.menuitems[i].name, Defines.PU_CACHE
                    ), DOOM.vs, x, y
                )
            }
            y += LINEHEIGHT
        }

        // DRAW SKULL
        DOOM.graphicSystem.DrawPatchScaled(
            DoomScreen.FG, DOOM.wadLoader.CachePatchName(
                skullName[whichSkull.toInt()],
                Defines.PU_CACHE
            ), DOOM.vs, x + SKULLXOFF, currentMenu!!.y - 5 + itemOn
                    * LINEHEIGHT
        )
    }

    //
    // M_ClearMenus
    //
    override fun ClearMenus() {
        DOOM.menuactive = false
        //Engine.getEngine().window.setMouseCaptured();
        DOOM.graphicSystem.forcePalette()

        // MAES: was commented out :-/
        //if (!DM.netgame && DM.usergame && DM.paused)
        //    DM.setPaused(true);
    }

    /**
     * M_SetupNextMenu
     */
    fun SetupNextMenu(menudef: menu_t?) {
        currentMenu = menudef
        itemOn = currentMenu!!.lastOn.toShort()
    }

    /**
     * M_Ticker
     */
    @SourceCode.Exact
    @SourceCode.M_Menu.C(SourceCode.M_Menu.M_Ticker)
    override fun Ticker() {
        if (--skullAnimCounter <= 0) {
            whichSkull = (whichSkull.toInt() xor 1).toShort()
            skullAnimCounter = 8
        }
    }

    /**
     * M_Init
     */
    override fun Init() {

        // Init menus.
        initMenuRoutines()
        initDrawRoutines()
        initMenuItems()
        hu_font = DOOM.headsUp.getHUFonts() as Array<patch_t?>
        currentMenu = MainDef
        DOOM.menuactive = false
        itemOn = currentMenu!!.lastOn.toShort()
        whichSkull = 0
        skullAnimCounter = 10
        screenSize = screenBlocks - 3
        messageToPrint = false
        messageString = null
        messageLastMenuActive = DOOM.menuactive
        quickSaveSlot = -1
        when (DOOM.getGameMode()) {
            GameMode.freedm, GameMode.freedoom2, GameMode.commercial, GameMode.pack_plut, GameMode.pack_tnt -> {
                // This is used because DOOM 2 had only one HELP
                // page. I use CREDIT as second page now, but
                // kept this hack for educational purposes.
                MainMenu[readthis] = MainMenu[quitdoom]
                MainDef!!.numitems--
                MainDef!!.y += 8
                NewDef!!.prevMenu = MainDef
                ReadDef1!!.routine = DrawReadThis1
                ReadDef1!!.x = 330
                ReadDef1!!.y = 165
                ReadMenu1[0].routine = FinishReadThis
            }
            GameMode.shareware, GameMode.registered -> EpiDef!!.numitems--
            GameMode.freedoom1, GameMode.retail -> {}
            else -> {}
        }
    }

    /**
     * M_DrawText Returns the final X coordinate HU_Init must have been called
     * to init the font. Unused?
     *
     * @param x
     * @param y
     * @param direct
     * @param string
     * @return
     */
    fun DrawText(x: Int, y: Int, direct: Boolean, string: String): Int {
        var x = x
        var c: Int
        var w: Int
        var ptr = 0
        while (string[ptr].also { c = it.toInt() }.code > 0) {
            c = c.toChar().uppercaseChar().code - Defines.HU_FONTSTART
            ptr++
            if (c < 0 || c > Defines.HU_FONTSIZE) {
                x += 4
                continue
            }
            w = hu_font[c]!!.width.toInt()
            if (x + w > DOOM.vs.getScreenWidth()) break
            if (direct) DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                hu_font[c]!!,
                DOOM.vs,
                x,
                y
            ) else DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, hu_font[c]!!, DOOM.vs, x, y)
            x += w
        }
        return x
    }

    // ////////////////////////// DRAWROUTINES
    // //////////////////////////////////
    internal inner class M_DrawEpisode : DrawRoutine {
        override fun invoke() {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                DOOM.wadLoader.CachePatchName("M_EPISOD"),
                DOOM.vs,
                54,
                38
            )
        }
    }

    /**
     * M_LoadGame & Cie.
     */
    internal inner class M_DrawLoad : DrawRoutine {
        override fun invoke() {
            var i: Int
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName("M_LOADG"), DOOM.vs, 72, 28)
            i = 0
            while (i < load_end) {
                DrawSaveLoadBorder(LoadDef!!.x, LoadDef!!.y + LINEHEIGHT * i)
                WriteText(
                    LoadDef!!.x, LoadDef!!.y + LINEHEIGHT * i,
                    savegamestrings[i]
                )
                i++
            }
        }
    }

    internal inner class M_DrawMainMenu : DrawRoutine {
        override fun invoke() {
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName("M_DOOM"), DOOM.vs, 94, 2)
        }
    }

    internal inner class M_DrawNewGame : DrawRoutine {
        override fun invoke() {
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName("M_NEWG"), DOOM.vs, 96, 14)
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName("M_SKILL"), DOOM.vs, 54, 38)
        }
    }

    internal inner class M_DrawOptions : DrawRoutine {
        private val detailNames = arrayOf("M_GDHIGH", "M_GDLOW")
        private val msgNames = arrayOf("M_MSGOFF", "M_MSGON")
        override fun invoke() {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                DOOM.wadLoader.CachePatchName("M_OPTTTL"),
                DOOM.vs,
                108,
                15
            )
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                DOOM.wadLoader.CachePatchName(detailNames[detailLevel]),
                DOOM.vs,
                OptionsDef!!.x + 175,
                OptionsDef!!.y + LINEHEIGHT * detail
            )
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                DOOM.wadLoader.CachePatchName(msgNames[if (showMessages) 1 else 0]),
                DOOM.vs,
                OptionsDef!!.x + 120,
                OptionsDef!!.y + LINEHEIGHT * messages
            )
            DrawThermo(
                OptionsDef!!.x, OptionsDef!!.y + LINEHEIGHT
                        * (mousesens + 1), 10, DOOM.mouseSensitivity
            )
            DrawThermo(
                OptionsDef!!.x,
                OptionsDef!!.y + LINEHEIGHT * (scrnsize + 1), 9, screenSize
            )
        }
    }

    /**
     * Read This Menus
     * Had a "quick hack to fix romero bug"
     */
    internal inner class M_DrawReadThis1 : DrawRoutine {
        override fun invoke() {
            val lumpname: String
            val skullx: Int
            val skully: Int
            inhelpscreens = true
            when (DOOM.getGameMode()) {
                GameMode.commercial, GameMode.freedm, GameMode.freedoom2, GameMode.pack_plut, GameMode.pack_tnt -> {
                    skullx = 330
                    skully = 165
                    lumpname = "HELP"
                }
                GameMode.shareware -> {
                    lumpname = "HELP2"
                    skullx = 280
                    skully = 185
                }
                else -> {
                    lumpname = "CREDIT"
                    skullx = 330
                    skully = 165
                }
            }
            ReadDef1!!.x = skullx
            ReadDef1!!.y = skully
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName(lumpname), DOOM.vs, 0, 0)
            // Maes: we need to do this here, otherwide the status bar appears "dirty"
            DOOM.statusBar.forceRefresh()
        }
    }

    /**
     * Read This Menus - optional second page.
     */
    internal inner class M_DrawReadThis2 : DrawRoutine {
        override fun invoke() {
            val lumpname: String
            val skullx: Int
            val skully: Int
            inhelpscreens = true
            lumpname = "HELP1"
            skullx = 330
            skully = 175
            ReadDef2!!.x = skullx
            ReadDef2!!.y = skully
            DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, DOOM.wadLoader.CachePatchName(lumpname), DOOM.vs, 0, 0)
            // Maes: we need to do this here, otherwide the status bar appears "dirty"
            DOOM.statusBar.forceRefresh()
        }
    }

    /**
     * Change Sfx & Music volumes
     */
    internal inner class M_DrawSound : DrawRoutine {
        override fun invoke() {
            DOOM.graphicSystem.DrawPatchScaled(
                DoomScreen.FG,
                DOOM.wadLoader.CacheLumpName("M_SVOL", Defines.PU_CACHE, patch_t::class.java),
                DOOM.vs,
                60,
                38
            )
            DrawThermo(
                SoundDef!!.x, SoundDef!!.y + LINEHEIGHT * (sfx_vol + 1), 16,
                DOOM.snd_SfxVolume
            )
            DrawThermo(
                SoundDef!!.x, SoundDef!!.y + LINEHEIGHT * (music_vol + 1),
                16, DOOM.snd_MusicVolume
            )
        }
    }

    // /////////////////////////// MENU ROUTINES
    // ///////////////////////////////////
    internal inner class M_ChangeDetail : MenuRoutine {
        override fun invoke(choice: Int) {
            var choice = choice
            choice = 0
            detailLevel = 1 - detailLevel

            // FIXME - does not work. Remove anyway?
            //System.err.print("M_ChangeDetail: low detail mode n.a.\n");

            //return;
            DOOM.sceneRenderer.SetViewSize(screenBlocks, detailLevel)
            if (detailLevel == 0) DOOM.players[DOOM.consoleplayer].message =
                englsh.DETAILHI else DOOM.players[DOOM.consoleplayer].message = englsh.DETAILLO
        }
    }

    /**
     * Toggle messages on/off
     */
    internal inner class M_ChangeMessages : MenuRoutine {
        override fun invoke(choice: Int) {
            // warning: unused parameter `int choice'
            //choice = 0;
            showMessages = !showMessages
            if (!showMessages) DOOM.players[DOOM.consoleplayer].message =
                englsh.MSGOFF else DOOM.players[DOOM.consoleplayer].message = englsh.MSGON
            message_dontfuckwithme = true
        }
    }

    internal inner class M_ChangeSensitivity : MenuRoutine {
        override fun invoke(choice: Int) {
            when (choice) {
                0 -> if (DOOM.mouseSensitivity != 0) DOOM.mouseSensitivity--
                1 -> if (DOOM.mouseSensitivity < 9) DOOM.mouseSensitivity++
            }
        }
    }

    internal inner class M_ChooseSkill : MenuRoutine {
        override fun invoke(choice: Int) {
            if (choice == nightmare) {
                StartMessage(englsh.NIGHTMARE, VerifyNightmare, true)
                return
            }
            DOOM.DeferedInitNew(skill_t.values()[choice], epi + 1, 1)
            ClearMenus()
        }
    }

    /**
     * M_EndGame
     */
    internal inner class M_EndGame : MenuRoutine {
        override fun invoke(choice: Int) {
            var choice = choice
            choice = 0
            if (!DOOM.usergame) {
                DOOM.doomSound.StartSound(null, sfxenum_t.sfx_oof)
                return
            }
            if (DOOM.netgame) {
                StartMessage(englsh.NETEND, null, false)
                return
            }
            StartMessage(englsh.ENDGAME, EndGameResponse, true)
        }
    }

    internal inner class M_EndGameResponse : MenuRoutine {
        override fun invoke(ch: Int) {
            if (ch != 'y'.code) return
            currentMenu!!.lastOn = itemOn.toInt()
            ClearMenus()
            DOOM.StartTitle()
        }
    }

    internal inner class M_Episode : MenuRoutine {
        override fun invoke(choice: Int) {
            var choice = choice
            if (DOOM.isShareware() && choice != 0) {
                StartMessage(englsh.SWSTRING, null, false)
                SetupNextMenu(ReadDef2)
                return
            }

            // Yet another hack...
            if (!DOOM.isRetail() && choice > 2) {
                System.err
                    .print("M_Episode: 4th episode requires UltimateDOOM\n")
                choice = 0
            }
            epi = choice
            SetupNextMenu(NewDef)
        }
    }

    /**
     * User wants to load this game
     */
    internal inner class M_LoadSelect : MenuRoutine {
        override fun invoke(choice: Int) {
            val name: String
            if (DOOM.cVarManager.bool(CommandVariable.CDROM)) name =
                "c:\\doomdata\\" + dstrings.SAVEGAMENAME + choice + ".dsg" else name =
                dstrings.SAVEGAMENAME + choice + ".dsg"
            DOOM.LoadGame(name)
            ClearMenus()
        }
    }

    /**
     * Selected from DOOM menu
     */
    internal inner class M_LoadGame : MenuRoutine {
        override fun invoke(choice: Int) {
            if (DOOM.netgame) {
                StartMessage(englsh.LOADNET, null, false)
                return
            }
            SetupNextMenu(LoadDef)
            ReadSaveStrings()
        }
    }


    companion object {
        //int menuactive;
        protected const val SKULLXOFF = -32
        protected const val LINEHEIGHT = 16

        /**
         * graphic name of skulls warning: initializer-string for array of chars is
         * too long
         */
        private val skullName = arrayOf("M_SKULL1", "M_SKULL2")

        // ////////////////////// VARIOUS CONSTS //////////////////////
        private val quitsounds = arrayOf(
            sfxenum_t.sfx_pldeth, sfxenum_t.sfx_dmpain, sfxenum_t.sfx_popain,
            sfxenum_t.sfx_slop, sfxenum_t.sfx_telept, sfxenum_t.sfx_posit1,
            sfxenum_t.sfx_posit3, sfxenum_t.sfx_sgtatk
        )
        private val quitsounds2 = arrayOf(
            sfxenum_t.sfx_vilact, sfxenum_t.sfx_getpow, sfxenum_t.sfx_boscub,
            sfxenum_t.sfx_slop, sfxenum_t.sfx_skeswg, sfxenum_t.sfx_kntdth,
            sfxenum_t.sfx_bspact, sfxenum_t.sfx_sgtatk
        )

        /** episodes_e enum  */
        private const val ep1 = 0
        private const val ep2 = 1
        private const val ep3 = 2
        private const val ep4 = 3
        private const val ep_end = 4

        /** load_e enum  */
        private const val load1 = 0
        private const val load2 = 1
        private const val load3 = 2
        private const val load4 = 3
        private const val load5 = 4
        private const val load6 = 5
        private const val load_end = 6

        /** options_e enum;  */
        private const val endgame = 0
        private const val messages = 1
        private const val detail = 2
        private const val scrnsize = 3
        private const val option_empty1 = 4
        private const val mousesens = 5
        private const val option_empty2 = 6
        private const val soundvol = 7
        private const val opt_end = 8

        /** main_e enum;  */
        private const val newgame = 0
        private const val options = 1
        private const val loadgam = 2
        private const val savegame = 3
        private const val readthis = 4
        private const val quitdoom = 5
        private const val main_end = 6

        /** read_e enum  */
        private const val rdthsempty1 = 0
        private const val read1_end = 1

        /** read_2 enum  */
        private const val rdthsempty2 = 0
        private const val read2_end = 1

        /**  newgame_e enum; */
        const val killthings = 0
        const val toorough = 1
        const val hurtme = 2
        const val violence = 3
        const val nightmare = 4
        const val newg_end = 5
        private val gammamsg = arrayOf(
            englsh.GAMMALVL0,
            englsh.GAMMALVL1, englsh.GAMMALVL2, englsh.GAMMALVL3, englsh.GAMMALVL4
        )

        /** sound_e enum  */
        const val sfx_vol = 0
        const val sfx_empty1 = 1
        const val music_vol = 2
        const val sfx_empty2 = 3
        const val sound_end = 4
    }
}