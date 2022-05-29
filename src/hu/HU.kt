package hu

import data.Defines
import data.Limits
import data.sounds.sfxenum_t
import defines.GameMode
import defines.Language_t
import doom.*
import doom.SourceCode.*
import g.Signals.ScanCode
import rr.patch_t
import utils.C2JUtils
import v.renderers.DoomScreen
import java.awt.Rectangle
import java.util.*


// -----------------------------------------------------------------------------
//
// $Id: HU.java,v 1.32 2012/09/24 17:16:23 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
// Copyright (C) 2022 hiperbou
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// DESCRIPTION: Heads-up displays
//
// -----------------------------------------------------------------------------
class HU(  // MAES: Status and wad data.
    val DOOM: DoomMain<*, *>
) : IHeadsUp {
    //
    // Locally used constants, shortcuts.
    // MAES: Some depend on STATE, so moved into constructor.
    var HU_TITLE: String? = null
    var HU_TITLE2: String? = null
    var HU_TITLEP: String? = null
    var HU_TITLET: String? = null
    protected var HU_TITLEY // = (167 - Swap.SHORT(hu_font[0].height));
            = 0
    protected var HU_INPUTY // = (HU_MSGY +
            = 0
    var chat_macros = arrayOf(
        englsh.HUSTR_CHATMACRO0, englsh.HUSTR_CHATMACRO1, englsh.HUSTR_CHATMACRO2,
        englsh.HUSTR_CHATMACRO3, englsh.HUSTR_CHATMACRO4, englsh.HUSTR_CHATMACRO5,
        englsh.HUSTR_CHATMACRO6, englsh.HUSTR_CHATMACRO7, englsh.HUSTR_CHATMACRO8,
        englsh.HUSTR_CHATMACRO9
    )

    override fun setChatMacro(i: Int, s: String) {
        chat_macros[i] = s
    }

    var chat_char // remove later.
            = 0.toChar()
    var plr: player_t? = null

    // MAES: a whole lot of "static" stuff which really would be HU instance
    // status.
    var hu_font = arrayOfNulls<patch_t>(Defines.HU_FONTSIZE)
    var chat_dest = CharArray(Limits.MAXPLAYERS)

    // MAES: these used to be defined in hu_lib. We're going 100% OO here...
    var w_inputbuffer: Array<hu_itext_t?>
    var w_title: hu_textline_t
    var w_chat: hu_itext_t
    var always_off = booleanArrayOf(false)

    // Needs to be referenced by one of the widgets.
    var chat_on = BooleanArray(1)

    // MAES: Ugly hack which allows it to be passed as reference. Sieg heil!
    var message_on = booleanArrayOf(true)
    var message_dontfuckwithme = false
    var message_nottobefuckedwith = false
    var w_message: hu_stext_t
    var message_counter = 0

    // This is actually an "extern" pointing inside m_menu (Menu.java). So we
    // need to share Menu context.
    // int showMessages;
    // MAES: I think this is supposed to be visible by the various hu_ crap...
    // boolean automapactive;
    var headsupactive = false

    //
    // Builtin map names.
    // The actual names can be found in DStrings.h.
    //
    protected var mapnames = arrayOf(
        englsh.HUSTR_E1M1,
        englsh.HUSTR_E1M2,
        englsh.HUSTR_E1M3,
        englsh.HUSTR_E1M4,
        englsh.HUSTR_E1M5,
        englsh.HUSTR_E1M6,
        englsh.HUSTR_E1M7,
        englsh.HUSTR_E1M8,
        englsh.HUSTR_E1M9,
        englsh.HUSTR_E2M1,
        englsh.HUSTR_E2M2,
        englsh.HUSTR_E2M3,
        englsh.HUSTR_E2M4,
        englsh.HUSTR_E2M5,
        englsh.HUSTR_E2M6,
        englsh.HUSTR_E2M7,
        englsh.HUSTR_E2M8,
        englsh.HUSTR_E2M9,
        englsh.HUSTR_E3M1,
        englsh.HUSTR_E3M2,
        englsh.HUSTR_E3M3,
        englsh.HUSTR_E3M4,
        englsh.HUSTR_E3M5,
        englsh.HUSTR_E3M6,
        englsh.HUSTR_E3M7,
        englsh.HUSTR_E3M8,
        englsh.HUSTR_E3M9,
        englsh.HUSTR_E4M1,
        englsh.HUSTR_E4M2,
        englsh.HUSTR_E4M3,
        englsh.HUSTR_E4M4,
        englsh.HUSTR_E4M5,
        englsh.HUSTR_E4M6,
        englsh.HUSTR_E4M7,
        englsh.HUSTR_E4M8,
        englsh.HUSTR_E4M9,
        "NEWLEVEL",
        "NEWLEVEL",
        "NEWLEVEL",
        "NEWLEVEL",
        "NEWLEVEL",
        "NEWLEVEL",
        "NEWLEVEL",
        "NEWLEVEL",
        "NEWLEVEL"
    )
    protected var mapnames2 = arrayOf(
        englsh.HUSTR_1,
        englsh.HUSTR_2,
        englsh.HUSTR_3,
        englsh.HUSTR_4,
        englsh.HUSTR_5,
        englsh.HUSTR_6,
        englsh.HUSTR_7,
        englsh.HUSTR_8,
        englsh.HUSTR_9,
        englsh.HUSTR_10,
        englsh.HUSTR_11,
        englsh.HUSTR_12,
        englsh.HUSTR_13,
        englsh.HUSTR_14,
        englsh.HUSTR_15,
        englsh.HUSTR_16,
        englsh.HUSTR_17,
        englsh.HUSTR_18,
        englsh.HUSTR_19,
        englsh.HUSTR_20,
        englsh.HUSTR_21,
        englsh.HUSTR_22,
        englsh.HUSTR_23,
        englsh.HUSTR_24,
        englsh.HUSTR_25,
        englsh.HUSTR_26,
        englsh.HUSTR_27,
        englsh.HUSTR_28,
        englsh.HUSTR_29,
        englsh.HUSTR_30,
        englsh.HUSTR_31,
        englsh.HUSTR_32,
        englsh.HUSTR_33
    )
    protected var mapnamesp = arrayOf(
        englsh.PHUSTR_1,
        englsh.PHUSTR_2,
        englsh.PHUSTR_3,
        englsh.PHUSTR_4,
        englsh.PHUSTR_5,
        englsh.PHUSTR_6,
        englsh.PHUSTR_7,
        englsh.PHUSTR_8,
        englsh.PHUSTR_9,
        englsh.PHUSTR_10,
        englsh.PHUSTR_11,
        englsh.PHUSTR_12,
        englsh.PHUSTR_13,
        englsh.PHUSTR_14,
        englsh.PHUSTR_15,
        englsh.PHUSTR_16,
        englsh.PHUSTR_17,
        englsh.PHUSTR_18,
        englsh.PHUSTR_19,
        englsh.PHUSTR_20,
        englsh.PHUSTR_21,
        englsh.PHUSTR_22,
        englsh.PHUSTR_23,
        englsh.PHUSTR_24,
        englsh.PHUSTR_25,
        englsh.PHUSTR_26,
        englsh.PHUSTR_27,
        englsh.PHUSTR_28,
        englsh.PHUSTR_29,
        englsh.PHUSTR_30,
        englsh.PHUSTR_31,
        englsh.PHUSTR_32
    )
    protected var mapnamest = arrayOf(
        englsh.THUSTR_1,
        englsh.THUSTR_2,
        englsh.THUSTR_3,
        englsh.THUSTR_4,
        englsh.THUSTR_5,
        englsh.THUSTR_6,
        englsh.THUSTR_7,
        englsh.THUSTR_8,
        englsh.THUSTR_9,
        englsh.THUSTR_10,
        englsh.THUSTR_11,
        englsh.THUSTR_12,
        englsh.THUSTR_13,
        englsh.THUSTR_14,
        englsh.THUSTR_15,
        englsh.THUSTR_16,
        englsh.THUSTR_17,
        englsh.THUSTR_18,
        englsh.THUSTR_19,
        englsh.THUSTR_20,
        englsh.THUSTR_21,
        englsh.THUSTR_22,
        englsh.THUSTR_23,
        englsh.THUSTR_24,
        englsh.THUSTR_25,
        englsh.THUSTR_26,
        englsh.THUSTR_27,
        englsh.THUSTR_28,
        englsh.THUSTR_29,
        englsh.THUSTR_30,
        englsh.THUSTR_31,
        englsh.THUSTR_32
    )
    lateinit var shiftxform: CharArray

    // Maes: char?
    var frenchKeyMap = charArrayOf(
        0.toChar(),
        1.toChar(),
        2.toChar(),
        3.toChar(),
        4.toChar(),
        5.toChar(),
        6.toChar(),
        7.toChar(),
        8.toChar(),
        9.toChar(),
        10.toChar(),
        11.toChar(),
        12.toChar(),
        13.toChar(),
        14.toChar(),
        15.toChar(),
        16.toChar(),
        17.toChar(),
        18.toChar(),
        19.toChar(),
        20.toChar(),
        21.toChar(),
        22.toChar(),
        23.toChar(),
        24.toChar(),
        25.toChar(),
        26.toChar(),
        27.toChar(),
        28.toChar(),
        29.toChar(),
        30.toChar(),
        31.toChar(),
        ' ',
        '!',
        '"',
        '#',
        '$',
        '%',
        '&',
        '%',
        '(',
        ')',
        '*',
        '+',
        ';',
        '-',
        ':',
        '!',
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        ':',
        'M',
        '<',
        '=',
        '>',
        '?',
        '@',
        'Q',
        'B',
        'C',
        'D',
        'E',
        'F',
        'G',
        'H',
        'I',
        'J',
        'K',
        'L',
        ',',
        'N',
        'O',
        'P',
        'A',
        'R',
        'S',
        'T',
        'U',
        'V',
        'Z',
        'X',
        'Y',
        'W',
        '^',
        '\\',
        '$',
        '^',
        '_',
        '@',
        'Q',
        'B',
        'C',
        'D',
        'E',
        'F',
        'G',
        'H',
        'I',
        'J',
        'K',
        'L',
        ',',
        'N',
        'O',
        'P',
        'A',
        'R',
        'S',
        'T',
        'U',
        'V',
        'Z',
        'X',
        'Y',
        'W',
        '^',
        '\\',
        '$',
        '^',
        127.toChar()
    )

    protected fun ForeignTranslation(ch: Char): Char {
        return if (ch.code < 128) frenchKeyMap[ch.code] else ch
    }

    /**
     * Loads a bunch of STCFNx fonts from WAD, and sets some of the remaining
     * constants.
     *
     * @throws Exception
     */
    override fun Init() {
        shiftxform = if (DOOM.language == Language_t.french) {
            HU.french_shiftxform
        } else {
            HU.english_shiftxform
        }

        // load the heads-up font
        var j = Defines.HU_FONTSTART.toInt()

        // So it basically loads a bunch of patch_t's from memory.
        Arrays.setAll(hu_font) { i: Int -> patch_t() }
        for (i in 0 until Defines.HU_FONTSIZE) {
            val buffer = String.format("STCFN%03d", j++)
            // hu_font[i] = ((patch_t[]) wd.CacheLumpName(buffer, PU_STATIC);
            hu_font[i] = DOOM.wadLoader.CachePatchName(buffer, Defines.PU_STATIC)
        }

        // MAES: Doom's SC had a really fucked up endianness change for height.
        // I don't really see the point in that, as in the WAD patches appear
        // to be all Little Endian... mystery :-S
        // HU_TITLEY = (167 - Swap.SHORT(hu_font[0].height));
        HU_TITLEY = 167 - hu_font[0]!!.height
        HU_INPUTY = Defines.HU_MSGY.code + Defines.HU_MSGHEIGHT.code.toShort() * hu_font[0]!!.height + 1
    }

    override fun Stop() {
        headsupactive = false
    }

    @Suspicious(CauseOfDesyncProbability.LOW)
    override fun Start() {
        var i: Int
        val s: String?

        // MAES: fugly hax. These were compile-time inlines,
        // so they can either work as functions, or be set whenever the HU is started
        // (typically once per level). They need to be aware of game progress,
        // and episode numbers <1 will cause it to bomb.
        // MAES: hack to handle Betray in XBLA 31/5/2011
        if (DOOM.gamemap > 32 && DOOM.getGameMode() == GameMode.pack_xbla) {
            HU_TITLE = mapnames[(DOOM.gameepisode - 1) * 9 + DOOM.gamemap - 2]
            HU_TITLE2 = mapnames2[DOOM.gamemap - 1]
            HU_TITLEP = mapnamesp[DOOM.gamemap - 2] // fixed from HU_TITLEPw
            HU_TITLET = mapnamest[DOOM.gamemap - 2]
        } else {
            HU_TITLE = mapnames[(DOOM.gameepisode - 1) * 9 + DOOM.gamemap - 1]
            HU_TITLE2 = mapnames2[DOOM.gamemap - 1]
            HU_TITLEP = mapnamesp[DOOM.gamemap - 1] // fixed from HU_TITLEP
            HU_TITLET = mapnamest[DOOM.gamemap - 1]
        }
        if (headsupactive) Stop()
        plr = DOOM.players[DOOM.consoleplayer]
        message_on[0] = false
        message_dontfuckwithme = false
        message_nottobefuckedwith = false
        chat_on[0] = false

        // create the message widget
        w_message.initSText(
            Defines.HU_MSGX.code, Defines.HU_MSGY.code, Defines.HU_MSGHEIGHT.code, hu_font,
            Defines.HU_FONTSTART.toInt(), message_on
        )

        // create the map title widget
        w_title.initTextLine(HU.HU_TITLEX, HU_TITLEY, hu_font, Defines.HU_FONTSTART.toInt())
        s = when (DOOM.getGameMode()) {
            GameMode.shareware, GameMode.registered, GameMode.retail, GameMode.freedoom1 -> HU_TITLE
            GameMode.pack_plut -> HU_TITLEP
            GameMode.pack_tnt -> HU_TITLET
            GameMode.commercial, GameMode.freedoom2, GameMode.freedm -> HU_TITLE2
            else -> HU_TITLE2
        }

        // MAES: oh great, more pointer-char magic... oh no you don't, you ugly
        // cow horse and reindeer lover.

        // while (*s) this.w_title.addCharToTextLine(*(s++));
        var ptr = 0
        while (ptr < s!!.length) {
            w_title.addCharToTextLine(s[ptr++])
        }
        // create the chat widget
        w_chat.initIText(
            HU.HU_INPUTX, HU_INPUTY, hu_font, Defines.HU_FONTSTART.toInt(),
            chat_on
        )

        // create the inputbuffer widgets
        i = 0
        while (i < Limits.MAXPLAYERS) {
            w_inputbuffer[i] = hu_itext_t()
            w_inputbuffer[i]!!.initIText(0, 0, null, 0, always_off)
            i++
        }
        headsupactive = true
    }

    override fun Drawer() {
        w_message.drawSText()
        w_chat.drawIText()
        if (DOOM.automapactive) w_title.drawTextLine(false)
    }

    override fun Erase() {
        w_message.eraseSText()
        w_chat.eraseIText()
        w_title.eraseTextLine()
    }

    override fun Ticker() {
        var i: Int
        var rc: Boolean
        var c: Char = Char(0) //TODO: default initialization?

        // tick down message counter if message is up
        if (message_counter != 0 && --message_counter == 0) {
            message_on[0] = false
            message_nottobefuckedwith = false
        }
        if (DOOM.menu.showMessages || message_dontfuckwithme) {

            // display message if necessary
            if (plr!!.message != null && !message_nottobefuckedwith || plr!!.message != null && message_dontfuckwithme) {
                w_message.addMessageToSText(null, plr!!.message!!)
                plr!!.message = null
                message_on[0] = true
                message_counter = Defines.HU_MSGTIMEOUT
                message_nottobefuckedwith = message_dontfuckwithme
                message_dontfuckwithme = false
            }
        } // else message_on = false;

        // check for incoming chat characters
        if (DOOM.netgame) {
            i = 0
            while (i < Limits.MAXPLAYERS) {
                if (!DOOM.playeringame[i]) {
                    i++
                    continue
                }
                if (i != DOOM.consoleplayer && DOOM.players[i].cmd.chatchar.also { c = it }.code != 0) {
                    if (c <= Defines.HU_BROADCAST) chat_dest[i] = c else {
                        if (c >= 'a' && c <= 'z') c = shiftxform[c.code]
                        rc = w_inputbuffer[i]!!.keyInIText(c)
                        if (rc && c == ScanCode.SC_ENTER.c) {
                            if (w_inputbuffer[i]!!.l!!.len != 0 && chat_dest[i].code == DOOM.consoleplayer + 1 || chat_dest[i] == Defines.HU_BROADCAST) {
                                w_message.addMessageToSText(
                                    HU.player_names.get(i),
                                    w_inputbuffer[i]!!.l!!.text.toString()
                                )
                                message_nottobefuckedwith = true
                                message_on[0] = true
                                message_counter = Defines.HU_MSGTIMEOUT
                                if (DOOM.isCommercial()) DOOM.doomSound.StartSound(
                                    null,
                                    sfxenum_t.sfx_radio
                                ) else DOOM.doomSound.StartSound(null, sfxenum_t.sfx_tink)
                            }
                            w_inputbuffer[i]!!.resetIText()
                        }
                    }
                    DOOM.players[i].cmd.chatchar = 0.toChar()
                }
                i++
            }
        }
    }

    protected val QUEUESIZE = 128
    protected var chatchars = CharArray(QUEUESIZE)
    protected var head = 0
    protected var tail = 0
    @SourceCode.Exact
    @HU_Stuff.C(HU_Stuff.HU_queueChatChar)
    protected fun queueChatChar(c: Char) {
        if (head + 1 and QUEUESIZE - 1 == tail) {
            plr!!.message = englsh.HUSTR_MSGU
        } else {
            chatchars[head] = c
            head = head + 1 and QUEUESIZE - 1
        }
    }

    override fun dequeueChatChar(): Char {
        val c: Char
        if (head != tail) {
            c = chatchars[tail]
            tail = tail + 1 and QUEUESIZE - 1
        } else {
            c = 0.toChar()
        }
        return c
    }

    // MAES: These were "static" inside HU_Responder, since they were meant to
    // represent state.
    protected var lastmessage = StringBuilder(Defines.HU_MAXLINELENGTH + 1)

    // protected char[] lastmessage=new char[HU_MAXLINELENGTH+1];
    protected var shiftdown = false
    protected var altdown = false
    protected var destination_keys =
        charArrayOf(englsh.HUSTR_KEYGREEN, englsh.HUSTR_KEYINDIGO, englsh.HUSTR_KEYBROWN, englsh.HUSTR_KEYRED)
    protected var num_nobrainers = 0

    init {
        w_message = hu_stext_t()
        w_inputbuffer = arrayOfNulls(Limits.MAXPLAYERS)
        for (i in 0 until Limits.MAXPLAYERS) {
            w_inputbuffer[i] = hu_itext_t()
        }
        w_title = hu_textline_t()
        w_chat = hu_itext_t()
    }

    @Compatible
    @HU_Stuff.C(HU_Stuff.HU_Responder)
    override fun Responder(ev: event_t): Boolean {

        //System.out.println("Player "+DM.players[0].mo.x);
        var numplayers = 0
        // MAES: Adding BOOLEANS to ints, are we ?!
        for (i in 0 until Limits.MAXPLAYERS) {
            numplayers += if (DOOM.playeringame[i]) 1 else 0
        }
        if (ev.isKey(ScanCode.SC_LSHIFT) || ev.isKey(ScanCode.SC_RSHIFT)) {
            shiftdown = ev.isType(evtype_t.ev_keydown)
            return false
        } else if (ev.isKey(ScanCode.SC_LALT) || ev.isKey(ScanCode.SC_RALT)) {
            altdown = ev.isType(evtype_t.ev_keydown)
            return false
        }
        if (!ev.isType(evtype_t.ev_keydown)) return false
        val eatkey: Boolean
        if (!chat_on[0]) {
            if (ev.isKey(Defines.HU_MSGREFRESH)) {
                message_on[0] = true
                message_counter = Defines.HU_MSGTIMEOUT
                eatkey = true
            } else if (DOOM.netgame && ev.isKey(HU.HU_INPUTTOGGLE)) {
                chat_on[0] = true
                eatkey = chat_on.get(0)
                //HUlib_resetIText@ run {
                    w_chat.resetIText()
                //}
                //HU_queueChatChar@ run {
                    this.queueChatChar(Defines.HU_BROADCAST)
                //}
            } else if (DOOM.netgame && numplayers > 2) {
                eatkey = ev.ifKey { sc: ScanCode? ->
                    var r = false
                    for (i in 0 until Limits.MAXPLAYERS) {
                        if (sc!!.c == destination_keys[i]) {
                            if (DOOM.playeringame[i] && i != DOOM.consoleplayer) {
                                chat_on[0] = true
                                r = chat_on.get(0)
                                //HUlib_resetIText@ run {
                                    w_chat.resetIText()
                                //}
                                //HU_queueChatChar@ run {
                                    this.queueChatChar((i + 1).toChar())
                                //}
                                break
                            } else if (i == DOOM.consoleplayer) {
                                num_nobrainers++
                                if (num_nobrainers < 3) plr!!.message =
                                    englsh.HUSTR_TALKTOSELF1 else if (num_nobrainers < 6) plr!!.message =
                                    englsh.HUSTR_TALKTOSELF2 else if (num_nobrainers < 9) plr!!.message =
                                    englsh.HUSTR_TALKTOSELF3 else if (num_nobrainers < 32) plr!!.message =
                                    englsh.HUSTR_TALKTOSELF4 else plr!!.message = englsh.HUSTR_TALKTOSELF5
                            }
                        }
                    }
                    r
                }
            } else eatkey = false
        } else eatkey = ev.ifKey { sc: ScanCode? ->
            var ret: Boolean
            var c = sc!!.c
            // send a macro
            if (altdown) {
                c = (c.code - '0'.code).toChar()
                if (c.code > 9) return@ifKey false

                // fprintf(stderr, "got here\n");
                val macromessage = chat_macros[c.code].toCharArray()

                // kill last message with a '\n'
                //HU_queueChatChar@ run {
                    queueChatChar(ScanCode.SC_ENTER.c)
                //} // DEBUG!!!

                // send the macro message
                val index = 0
                while (macromessage[index].code != 0) {
                    //HU_queueChatChar@ run {
                        queueChatChar(macromessage[index])
                    //}
                }
                //HU_queueChatChar@ run {
                    queueChatChar(ScanCode.SC_ENTER.c)
                //}

                // leave chat mode and notify that it was sent
                chat_on[0] = false
                lastmessage.setLength(0)
                lastmessage.append(chat_macros[c.code])
                plr!!.message = lastmessage.toString()
                ret = true
            } else {
                if (DOOM.language == Language_t.french) {
                    c = ForeignTranslation(c)
                }
                if ((shiftdown || c >= 'a') && c <= 'z') {
                    c = shiftxform[c.code]
                }
                //HUlib_keyInIText@ run {
                    ret = w_chat.keyInIText(c)
                //}
                if (ret) {
                    // static unsigned char buf[20]; // DEBUG
                    //HU_queueChatChar@ run {
                        queueChatChar(c)
                    //}

                    // sprintf(buf, "KEY: %d => %d", ev->data1, c);
                    // plr->message = buf;
                }
                if (c == ScanCode.SC_ENTER.c) {
                    chat_on[0] = false
                    if (w_chat.l!!.len != 0) {
                        lastmessage.setLength(0)
                        lastmessage.append(w_chat.l!!.text)
                        plr!!.message = String(lastmessage)
                    }
                } else if (c == ScanCode.SC_ESCAPE.c) {
                    chat_on[0] = false
                }
            }
            ret
        }
        return eatkey
    }
    // ///////////////////////////////// STRUCTS
    // ///////////////////////////////////
    /**
     * Input Text Line widget
     * (child of Text Line widget)
     */
    inner class hu_itext_t {
        var l // text line to input on
                : hu_textline_t? = null

        // left margin past which I am not to delete characters
        var lm = 0

        // pointer to boolean stating whether to update window
        lateinit var on: BooleanArray
        var laston // last value of *->on;
                = false

        fun initIText(
            x: Int, y: Int, font: Array<patch_t?>?, startchar: Int,
            on: BooleanArray
        ) {
            lm = 0 // default left margin is start of text
            this.on = on
            laston = true
            l = hu_textline_t(x, y, font, startchar)
        }

        // The following deletion routines adhere to the left margin restriction
        @SourceCode.Exact
        @HU_Lib.C(HU_Lib.HUlib_delCharFromIText)
        fun delCharFromIText() {
            if (l!!.len != lm) {
                HUlib_delCharFromTextLine@ run {
                    l!!.delCharFromTextLine()
                }
            }
        }

        fun eraseLineFromIText() {
            while (lm != l!!.len) l!!.delCharFromTextLine()
        }

        // Resets left margin as well
        @SourceCode.Exact
        @HU_Lib.C(HU_Lib.HUlib_resetIText)
        fun resetIText() {
            lm = 0
            l!!.clearTextLine()
        }

        fun addPrefixToIText(str: CharArray) {
            var ptr = 0
            while (str[ptr].code > 0) {
                l!!.addCharToTextLine(str[ptr++])
                lm = l!!.len
            }
        }

        // Maes: String overload
        fun addPrefixToIText(str: String) {
            var ptr = 0
            while (str[ptr].code > 0) {
                l!!.addCharToTextLine(str[ptr++])
                lm = l!!.len
            }
        }

        // wrapper function for handling general keyed input.
        // returns true if it ate the key
        @SourceCode.Exact
        @HU_Lib.C(HU_Lib.HUlib_keyInIText)
        fun keyInIText(ch: Char): Boolean {
            if (ch >= ' ' && ch <= '_') {
                HUlib_addCharToTextLine@ run {
                    l!!.addCharToTextLine(ch)
                }
            } else if (ch == ScanCode.SC_BACKSPACE.c) {
                HUlib_delCharFromIText@ run {
                    delCharFromIText()
                }
            } else if (ch != ScanCode.SC_ENTER.c) {
                return false // did not eat key
            }
            return true // ate the key
        }

        fun drawIText() {
            if (!on[0]) return
            l!!.drawTextLine(true) // draw the line w/ cursor
        }

        fun eraseIText() {
            if (laston && !on[0]) l!!.needsupdate = 4
            l!!.eraseTextLine()
            laston = on[0]
        }
    }

    /** Scrolling Text window widget
     * (child of Text Line widget)
     */
    inner class hu_stext_t {
        var lines = arrayOfNulls<hu_textline_t>(Defines.HU_MAXLINES) // text lines to draw
        var height // height in lines
                = 0
        var currline // current line number
                = 0

        // pointer to boolean stating whether to update window
        lateinit var on: BooleanArray
        var laston // last value of *->on.
                = false

        constructor() {}
        constructor(
            x: Int, y: Int, h: Int, font: Array<patch_t?>, startchar: Int,
            on: BooleanArray
        ) {
            initSText(x, y, h, font, startchar, on)
        }

        fun initSText(
            x: Int, y: Int, h: Int, font: Array<patch_t?>,
            startchar: Int, on: BooleanArray
        ) {
            for (i in 0 until Defines.HU_MAXLINES) {
                lines[i] = hu_textline_t()
            }
            height = h
            this.on = on
            laston = true
            currline = 0
            for (i in 0 until h) lines[i]!!.initTextLine(
                x, y - i
                        * (font[0]!!.height + 1), font, startchar
            )
        }

        fun addLineToSText() {

            // add a clear line
            if (++currline == height) currline = 0
            lines[currline]!!.clearTextLine()

            // everything needs updating
            for (i in 0 until height) lines[i]!!.needsupdate = 4
        }

        fun addMessageToSText(prefix: CharArray?, msg: CharArray) {
            addLineToSText()
            var ptr = 0
            if (prefix != null && prefix.size > 0) {
                while (ptr < prefix.size && prefix[ptr].code > 0) lines[currline]!!.addCharToTextLine(prefix[ptr++])
            }
            ptr = 0
            while (ptr < msg.size && msg[ptr].code > 0) lines[currline]!!.addCharToTextLine(msg[ptr++])
        }

        fun addMessageToSText(prefix: String?, msg: String) {
            addLineToSText()
            if (prefix != null && prefix.length > 0) {
                for (i in 0 until prefix.length) lines[currline]!!.addCharToTextLine(prefix[i])
            }
            for (i in 0 until msg.length) lines[currline]!!.addCharToTextLine(msg[i])
        }

        fun drawSText() {
            var i: Int
            var idx: Int
            var l: hu_textline_t?
            if (!on[0]) return  // if not on, don't draw


            // draw everything
            i = 0
            while (i < height) {
                idx = currline - i
                if (idx < 0) idx += height // handle queue of lines
                l = lines[idx]

                // need a decision made here on whether to skip the draw
                l!!.drawTextLine(false) // no cursor, please
                i++
            }
        }

        fun eraseSText() {
            for (i in 0 until height) {
                if (laston && !on[0]) lines[i]!!.needsupdate = 4
                lines[i]!!.eraseTextLine()
            }
            laston = on[0]
        }

        /**
         * MAES: this was the only variable in HUlib.c, and only instances of
         * hu_textline_t ever use it. For this reason, it makes sense to have it
         * common (?) between all instances of hu_textline_t and set it
         * somewhere else. Of course, if could be made an instance method or a
         * HUlib object could be defined.
         */
        protected var _automapactive // in AM_map.c
                = false

        fun isAutomapactive(): Boolean {
            return _automapactive
        }

        fun setAutomapactive(automapactive: Boolean) {
            this._automapactive = automapactive
        }

        /**
         * Same here.
         */
        // TODO: boolean : whether the screen is always erased
        protected var _noterased // =viewwindowx;
                = false

        fun isNoterased(): Boolean {
            return _noterased
        }

        fun setNoterased(noterased: Boolean) {
            this._noterased = noterased
        }

        var sb = StringBuilder()
        override fun toString(): String {
            sb.setLength(0)
            sb.append(lines[0]!!.text)
            sb.append(lines[1]!!.text)
            sb.append(lines[2]!!.text)
            sb.append(lines[3]!!.text)
            return sb.toString()
        }
    }

    // Text Line widget
    // (parent of Scrolling Text and Input Text widgets)
    inner class hu_textline_t {
        // left-justified position of scrolling text window
        var x = 0
        var y = 0

        // MAES: was **
        var f // font
                : Array<patch_t?>? = null
        var sc // start character
                = 0
        var text = CharArray(Defines.HU_MAXLINELENGTH + 1) // line of text
        var len // current line length
                = 0

        // whether this line needs to be udpated
        var needsupdate = 0

        constructor() {}

        @Compatible
        @HU_Lib.C(HU_Lib.HUlib_clearTextLine)
        fun clearTextLine() {
            len = 0
            C2JUtils.memset(text, 0.toChar(), text.size)
            // It's actually used as a status, go figure.
            needsupdate = 1
        }

        // Maes: this could as well be the contructor
        fun initTextLine(x: Int, y: Int, f: Array<patch_t?>?, sc: Int) {
            this.x = x
            this.y = y
            this.f = f
            this.sc = sc
            clearTextLine()
        }

        constructor(x: Int, y: Int, f: Array<patch_t?>?, sc: Int) {
            this.x = x
            this.y = y
            this.f = f
            this.sc = sc
            clearTextLine()
        }

        @SourceCode.Exact
        @HU_Lib.C(HU_Lib.HUlib_addCharToTextLine)
        fun addCharToTextLine(ch: Char): Boolean {
            return if (len == Defines.HU_MAXLINELENGTH) false else {
                text[len++] = ch
                text[len] = 0.toChar()
                // this.l[this.len] = 0;
                // MAES: for some reason this is set as "4", so this is a status
                // rather than a boolean.
                needsupdate = 4
                true
            }
        }

        /**
         * MAES: This is much better than cluttering up the syntax everytime a
         * STRING must be added.
         *
         * @param s
         * @return
         */
        /*
        public boolean addStringToTextLine(String s) {
            int index = 0;
            if (this.len == HU_MAXLINELENGTH)
                return false;
            else
                while ((index<s.length())&&(this.len < HU_MAXLINELENGTH)) {

                    this.l[len]append(s.charAt(index++));
                    this.len++;
                }
            this.l.append((char) 0);// final padding.

            // MAES: for some reason this is set as "4", so this is a
            // status rather than a boolean.

            this.needsupdate = 4;
            return true;
        } */
        @SourceCode.Exact
        @HU_Lib.C(HU_Lib.HUlib_delCharFromTextLine)
        fun delCharFromTextLine(): Boolean {
            return if (len == 0) false else {
                text[--len] = 0.toChar()
                needsupdate = 4
                true
            }
        }

        fun drawTextLine(drawcursor: Boolean) {
            var i: Int
            var w: Int
            var x: Int
            var c: Char

            // draw the new stuff
            x = this.x
            i = 0
            while (i < len) {
                c = text[i].uppercaseChar()
                if (c != ' ' && c.code >= sc && c <= '_') {
                    // MAES: fixed a FUCKING STUPID bug caused by SWAP.SHORT
                    w = f!![c.code - sc]!!.width.toInt()
                    if (x + w > DOOM.vs.getScreenWidth()) break
                    DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, f!![c.code - sc]!!, DOOM.vs, x, y)
                    x += w
                } else {
                    // Leave a space
                    x += 4
                    if (x >= DOOM.vs.getScreenWidth()) break
                }
                i++
            }

            // draw the cursor if requested
            if (drawcursor
                && x + f!!['_'.code - sc]!!.width <= DOOM.vs.getScreenWidth()
            ) {
                DOOM.graphicSystem.DrawPatchScaled(DoomScreen.FG, f!!['_'.code - sc]!!, DOOM.vs, x, y)
            }
        }

        // MAES: was "static" in C within HUlib. Which may mean it's instance
        // specific or global-ish. Or both.
        protected var lastautomapactive = true

        /**
         * Erases as little as possible to remove text messages
         * Only erases when NOT in automap and the screen is reduced,
         * and the text must either need updating or refreshing
         * (because of a recent change back from the automap)
         *
         * Rewritten by Good Sign 2017/04/06
         */
        fun eraseTextLine() {
            if (!DOOM.automapactive && DOOM.sceneRenderer.getView().windowx != 0 && needsupdate > 0) {
                val active = DOOM.sceneRenderer.getView()
                val   // active part of the screen
                        activeEndX = active.x + active.width
                val activeEndY = active.y + active.height
                // scaled text ranges
                val dupY = DOOM.graphicSystem.getScalingY()
                val lineY = y * dupY
                val lineHeight = (f!![0]!!.height + 1) * dupY
                val lineEndY = lineY + lineHeight
                val rect = Rectangle(0, lineY, DOOM.vs.getScreenWidth(), lineHeight)

                // TOP FULL WIDTH
                if (lineY < active.y) {
                    if (lineEndY >= active.y) {
                        rect.height = active.y - lineY
                    }
                    DOOM.graphicSystem.CopyRect(DoomScreen.BG, rect, DoomScreen.FG)
                }
                // CENTER SIDES
                if (lineEndY >= active.y && lineEndY < activeEndY || lineY >= active.y && lineY < activeEndY) {
                    if (lineY < active.y) {
                        rect.y = active.y
                        rect.height = lineHeight - rect.height // = lineHeight - (active.y - lineY);
                    } else {
                        rect.y = lineY
                        if (lineEndY >= activeEndY) {
                            rect.height = activeEndY - lineY
                        } else {
                            rect.height = lineHeight
                        }
                    }
                    // LEFT
                    rect.width = active.x
                    DOOM.graphicSystem.CopyRect(DoomScreen.BG, rect, DoomScreen.FG)
                    // RIGHT
                    rect.width = DOOM.vs.getScreenWidth() - activeEndX
                    DOOM.graphicSystem.CopyRect(DoomScreen.BG, rect, DoomScreen.FG)
                    rect.width = DOOM.vs.getScreenWidth() // restore, don't need to bother later
                }
                // BOTTOM FULL WIDTH
                if (lineEndY >= activeEndY) {
                    if (lineY >= activeEndY) {
                        rect.y = lineY
                    } else {
                        rect.y = activeEndY
                        rect.height = lineHeight - rect.height // = lineHeight - (activeEndY - lineY);
                    }
                    DOOM.graphicSystem.CopyRect(DoomScreen.BG, rect, DoomScreen.FG)
                }
            }
            lastautomapactive = DOOM.automapactive
            if (needsupdate != 0) needsupdate--
        }
    }

    override fun getHUFonts(): Array<patch_t> {
        return hu_font as  Array<patch_t>
    }

    companion object {
        protected const val HU_TITLEHEIGHT = 1
        protected const val HU_TITLEX = 0
        protected val HU_INPUTTOGGLE = ScanCode.SC_T
        protected const val HU_INPUTX = Defines.HU_MSGX.code

        // HU_MSGHEIGHT*(Swap.SHORT(hu_font[0].height) +1));
        protected const val HU_INPUTWIDTH = 64
        protected const val HU_INPUTHEIGHT = 1

        /** Needs to be seen by DoomGame  */
        val player_names =
            arrayOf(englsh.HUSTR_PLRGREEN, englsh.HUSTR_PLRINDIGO, englsh.HUSTR_PLRBROWN, englsh.HUSTR_PLRRED)
        val french_shiftxform = charArrayOf(
            0.toChar(),
            1.toChar(),
            2.toChar(),
            3.toChar(),
            4.toChar(),
            5.toChar(),
            6.toChar(),
            7.toChar(),
            8.toChar(),
            9.toChar(),
            10.toChar(),
            11.toChar(),
            12.toChar(),
            13.toChar(),
            14.toChar(),
            15.toChar(),
            16.toChar(),
            17.toChar(),
            18.toChar(),
            19.toChar(),
            20.toChar(),
            21.toChar(),
            22.toChar(),
            23.toChar(),
            24.toChar(),
            25.toChar(),
            26.toChar(),
            27.toChar(),
            28.toChar(),
            29.toChar(),
            30.toChar(),
            31.toChar(),
            ' ',
            '!',
            '"',
            '#',
            '$',
            '%',
            '&',
            '"',  // shift-'
            '(',
            ')',
            '*',
            '+',
            '?',  // shift-,
            '_',  // shift--
            '>',  // shift-.
            '?',  // shift-/
            '0',  // shift-0
            '1',  // shift-1
            '2',  // shift-2
            '3',  // shift-3
            '4',  // shift-4
            '5',  // shift-5
            '6',  // shift-6
            '7',  // shift-7
            '8',  // shift-8
            '9',  // shift-9
            '/',
            '.',  // shift-;
            '<',
            '+',  // shift-=
            '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
            'V', 'W',
            'X',
            'Y',
            'Z',
            '[',  // shift-[
            '!',  // shift-backslash - OH MY GOD DOES WATCOM SUCK
            ']',  // shift-]
            '"',
            '_',
            '\'',  // shift-`
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', '{', '|', '}', '~', 127
                .toChar()
        )
        val english_shiftxform = charArrayOf(
            0.toChar(),
            1.toChar(),
            2.toChar(),
            3.toChar(),
            4.toChar(),
            5.toChar(),
            6.toChar(),
            7.toChar(),
            8.toChar(),
            9.toChar(),
            10.toChar(),
            11.toChar(),
            12.toChar(),
            13.toChar(),
            14.toChar(),
            15.toChar(),
            16.toChar(),
            17.toChar(),
            18.toChar(),
            19.toChar(),
            20.toChar(),
            21.toChar(),
            22.toChar(),
            23.toChar(),
            24.toChar(),
            25.toChar(),
            26.toChar(),
            27.toChar(),
            28.toChar(),
            29.toChar(),
            30.toChar(),
            31.toChar(),
            ' ',
            '!',
            '"',
            '#',
            '$',
            '%',
            '&',
            '"',  // shift-'
            '(',
            ')',
            '*',
            '+',
            '<',  // shift-,
            '_',  // shift--
            '>',  // shift-.
            '?',  // shift-/
            ')',  // shift-0
            '!',  // shift-1
            '@',  // shift-2
            '#',  // shift-3
            '$',  // shift-4
            '%',  // shift-5
            '^',  // shift-6
            '&',  // shift-7
            '*',  // shift-8
            '(',  // shift-9
            ':',
            ':',  // shift-;
            '<',
            '+',  // shift-=
            '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
            'V', 'W',
            'X',
            'Y',
            'Z',
            '[',  // shift-[
            '!',  // shift-backslash - OH MY GOD DOES WATCOM SUCK
            ']',  // shift-]
            '"',
            '_',
            '\'',  // shift-`
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', '{', '|', '}', '~', 127.toChar()
        )
    }
}