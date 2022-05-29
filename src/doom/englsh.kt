package doom


// Emacs style mode select   -*- C++ -*-
//-----------------------------------------------------------------------------
//
// $Id: englsh.java,v 1.5 2011/05/31 21:46:20 velktron Exp $
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// DESCRIPTION:
//  Printed strings for translation.
//  English language support (default).
//
//-----------------------------------------------------------------------------
//
//  Printed strings for translation
//
//
// D_Main.C
//
object englsh {
    const val D_DEVSTR = "Development mode ON.\n"
    const val D_CDROM = "CD-ROM Version: default.cfg from c:\\doomdata\n"

    //
    // M_Misc.C
    //
    const val SCREENSHOT = "screen shot"

    //
    //  M_Menu.C
    //
    const val PRESSKEY = "press a key."
    const val PRESSYN = "press y or n."
    const val QUITMSG = "are you sure you want to\nquit this great game?"
    const val LOADNET = "you can't do load while in a net game!\n\n" + englsh.PRESSKEY
    const val QLOADNET = "you can't quickload during a netgame!\n\n" + englsh.PRESSKEY
    const val QSAVESPOT = "you haven't picked a quicksave slot yet!\n\n" + englsh.PRESSKEY
    const val SAVEDEAD = "you can't save if you aren't playing!\n\n" + englsh.PRESSKEY
    const val QSPROMPT = "quicksave over your game named\n\n'%s'?\n\n" + englsh.PRESSYN
    const val QLPROMPT = "do you want to quickload the game named\n\n'%s'?\n\n" + englsh.PRESSYN
    const val NEWGAME = "you can't start a new game\nwhile in a network game.\n\n" + englsh.PRESSKEY
    const val NIGHTMARE = "are you sure? this skill level\nisn't even remotely fair.\n\n" + englsh.PRESSYN
    const val SWSTRING =
        "this is the shareware version of doom.\n\nyou need to order the entire trilogy.\n\n" + englsh.PRESSKEY
    const val MSGOFF = "Messages OFF"
    const val MSGON = "Messages ON"
    const val NETEND = "you can't end a netgame!\n\n" + englsh.PRESSKEY
    const val ENDGAME = "are you sure you want to end the game?\n\n" + englsh.PRESSYN
    const val DOSY = "(press y to quit)"
    const val DETAILHI = "High detail"
    const val DETAILLO = "Low detail"
    const val GAMMALVL0 = "Gamma correction OFF"
    const val GAMMALVL1 = "Gamma correction level 1"
    const val GAMMALVL2 = "Gamma correction level 2"
    const val GAMMALVL3 = "Gamma correction level 3"
    const val GAMMALVL4 = "Gamma correction level 4"
    const val EMPTYSTRING = "empty slot"

    //
    //  P_inter.C
    //
    const val GOTARMOR = "Picked up the armor."
    const val GOTMEGA = "Picked up the MegaArmor!"
    const val GOTHTHBONUS = "Picked up a health bonus."
    const val GOTARMBONUS = "Picked up an armor bonus."
    const val GOTSTIM = "Picked up a stimpack."
    const val GOTMEDINEED = "Picked up a medikit that you REALLY need!"
    const val GOTMEDIKIT = "Picked up a medikit."
    const val GOTSUPER = "Supercharge!"
    const val GOTBLUECARD = "Picked up a blue keycard."
    const val GOTYELWCARD = "Picked up a yellow keycard."
    const val GOTREDCARD = "Picked up a red keycard."
    const val GOTBLUESKUL = "Picked up a blue skull key."
    const val GOTYELWSKUL = "Picked up a yellow skull key."
    const val GOTREDSKULL = "Picked up a red skull key."
    const val GOTINVUL = "Invulnerability!"
    const val GOTBERSERK = "Berserk!"
    const val GOTINVIS = "Partial Invisibility"
    const val GOTSUIT = "Radiation Shielding Suit"
    const val GOTMAP = "Computer Area Map"
    const val GOTVISOR = "Light Amplification Visor"
    const val GOTMSPHERE = "MegaSphere!"
    const val GOTCLIP = "Picked up a clip."
    const val GOTCLIPBOX = "Picked up a box of bullets."
    const val GOTROCKET = "Picked up a rocket."
    const val GOTROCKBOX = "Picked up a box of rockets."
    const val GOTCELL = "Picked up an energy cell."
    const val GOTCELLBOX = "Picked up an energy cell pack."
    const val GOTSHELLS = "Picked up 4 shotgun shells."
    const val GOTSHELLBOX = "Picked up a box of shotgun shells."
    const val GOTBACKPACK = "Picked up a backpack full of ammo!"
    const val GOTBFG9000 = "You got the BFG9000!  Oh, yes."
    const val GOTCHAINGUN = "You got the chaingun!"
    const val GOTCHAINSAW = "A chainsaw!  Find some meat!"
    const val GOTLAUNCHER = "You got the rocket launcher!"
    const val GOTPLASMA = "You got the plasma gun!"
    const val GOTSHOTGUN = "You got the shotgun!"
    const val GOTSHOTGUN2 = "You got the super shotgun!"

    //
    // P_Doors.C
    //
    const val PD_BLUEO = "You need a blue key to activate this object"
    const val PD_REDO = "You need a red key to activate this object"
    const val PD_YELLOWO = "You need a yellow key to activate this object"
    const val PD_BLUEK = "You need a blue key to open this door"
    const val PD_REDK = "You need a red key to open this door"
    const val PD_YELLOWK = "You need a yellow key to open this door"

    //
    //  G_game.C
    //
    const val GGSAVED = "game saved."

    //
    //  HU_stuff.C
    //
    const val HUSTR_MSGU = "[Message unsent]"
    const val HUSTR_E1M1 = "E1M1: Hangar"
    const val HUSTR_E1M2 = "E1M2: Nuclear Plant"
    const val HUSTR_E1M3 = "E1M3: Toxin Refinery"
    const val HUSTR_E1M4 = "E1M4: Command Control"
    const val HUSTR_E1M5 = "E1M5: Phobos Lab"
    const val HUSTR_E1M6 = "E1M6: Central Processing"
    const val HUSTR_E1M7 = "E1M7: Computer Station"
    const val HUSTR_E1M8 = "E1M8: Phobos Anomaly"
    const val HUSTR_E1M9 = "E1M9: Military Base"
    const val HUSTR_E2M1 = "E2M1: Deimos Anomaly"
    const val HUSTR_E2M2 = "E2M2: Containment Area"
    const val HUSTR_E2M3 = "E2M3: Refinery"
    const val HUSTR_E2M4 = "E2M4: Deimos Lab"
    const val HUSTR_E2M5 = "E2M5: Command Center"
    const val HUSTR_E2M6 = "E2M6: Halls of the Damned"
    const val HUSTR_E2M7 = "E2M7: Spawning Vats"
    const val HUSTR_E2M8 = "E2M8: Tower of Babel"
    const val HUSTR_E2M9 = "E2M9: Fortress of Mystery"
    const val HUSTR_E3M1 = "E3M1: Hell Keep"
    const val HUSTR_E3M2 = "E3M2: Slough of Despair"
    const val HUSTR_E3M3 = "E3M3: Pandemonium"
    const val HUSTR_E3M4 = "E3M4: House of Pain"
    const val HUSTR_E3M5 = "E3M5: Unholy Cathedral"
    const val HUSTR_E3M6 = "E3M6: Mt. Erebus"
    const val HUSTR_E3M7 = "E3M7: Limbo"
    const val HUSTR_E3M8 = "E3M8: Dis"
    const val HUSTR_E3M9 = "E3M9: Warrens"
    const val HUSTR_E4M1 = "E4M1: Hell Beneath"
    const val HUSTR_E4M2 = "E4M2: Perfect Hatred"
    const val HUSTR_E4M3 = "E4M3: Sever The Wicked"
    const val HUSTR_E4M4 = "E4M4: Unruly Evil"
    const val HUSTR_E4M5 = "E4M5: They Will Repent"
    const val HUSTR_E4M6 = "E4M6: Against Thee Wickedly"
    const val HUSTR_E4M7 = "E4M7: And Hell Followed"
    const val HUSTR_E4M8 = "E4M8: Unto The Cruel"
    const val HUSTR_E4M9 = "E4M9: Fear"
    const val HUSTR_1 = "level 1: entryway"
    const val HUSTR_2 = "level 2: underhalls"
    const val HUSTR_3 = "level 3: the gantlet"
    const val HUSTR_4 = "level 4: the focus"
    const val HUSTR_5 = "level 5: the waste tunnels"
    const val HUSTR_6 = "level 6: the crusher"
    const val HUSTR_7 = "level 7: dead simple"
    const val HUSTR_8 = "level 8: tricks and traps"
    const val HUSTR_9 = "level 9: the pit"
    const val HUSTR_10 = "level 10: refueling base"
    const val HUSTR_11 = "level 11: 'o' of destruction!"
    const val HUSTR_12 = "level 12: the factory"
    const val HUSTR_13 = "level 13: downtown"
    const val HUSTR_14 = "level 14: the inmost dens"
    const val HUSTR_15 = "level 15: industrial zone"
    const val HUSTR_16 = "level 16: suburbs"
    const val HUSTR_17 = "level 17: tenements"
    const val HUSTR_18 = "level 18: the courtyard"
    const val HUSTR_19 = "level 19: the citadel"
    const val HUSTR_20 = "level 20: gotcha!"
    const val HUSTR_21 = "level 21: nirvana"
    const val HUSTR_22 = "level 22: the catacombs"
    const val HUSTR_23 = "level 23: barrels o' fun"
    const val HUSTR_24 = "level 24: the chasm"
    const val HUSTR_25 = "level 25: bloodfalls"
    const val HUSTR_26 = "level 26: the abandoned mines"
    const val HUSTR_27 = "level 27: monster condo"
    const val HUSTR_28 = "level 28: the spirit world"
    const val HUSTR_29 = "level 29: the living end"
    const val HUSTR_30 = "level 30: icon of sin"
    const val HUSTR_31 = "level 31: wolfenstein"
    const val HUSTR_32 = "level 32: grosse"
    const val HUSTR_33 = "level 33: betray"
    const val PHUSTR_1 = "level 1: congo"
    const val PHUSTR_2 = "level 2: well of souls"
    const val PHUSTR_3 = "level 3: aztec"
    const val PHUSTR_4 = "level 4: caged"
    const val PHUSTR_5 = "level 5: ghost town"
    const val PHUSTR_6 = "level 6: baron's lair"
    const val PHUSTR_7 = "level 7: caughtyard"
    const val PHUSTR_8 = "level 8: realm"
    const val PHUSTR_9 = "level 9: abattoire"
    const val PHUSTR_10 = "level 10: onslaught"
    const val PHUSTR_11 = "level 11: hunted"
    const val PHUSTR_12 = "level 12: speed"
    const val PHUSTR_13 = "level 13: the crypt"
    const val PHUSTR_14 = "level 14: genesis"
    const val PHUSTR_15 = "level 15: the twilight"
    const val PHUSTR_16 = "level 16: the omen"
    const val PHUSTR_17 = "level 17: compound"
    const val PHUSTR_18 = "level 18: neurosphere"
    const val PHUSTR_19 = "level 19: nme"
    const val PHUSTR_20 = "level 20: the death domain"
    const val PHUSTR_21 = "level 21: slayer"
    const val PHUSTR_22 = "level 22: impossible mission"
    const val PHUSTR_23 = "level 23: tombstone"
    const val PHUSTR_24 = "level 24: the final frontier"
    const val PHUSTR_25 = "level 25: the temple of darkness"
    const val PHUSTR_26 = "level 26: bunker"
    const val PHUSTR_27 = "level 27: anti-christ"
    const val PHUSTR_28 = "level 28: the sewers"
    const val PHUSTR_29 = "level 29: odyssey of noises"
    const val PHUSTR_30 = "level 30: the gateway of hell"
    const val PHUSTR_31 = "level 31: cyberden"
    const val PHUSTR_32 = "level 32: go 2 it"
    const val THUSTR_1 = "level 1: system control"
    const val THUSTR_2 = "level 2: human bbq"
    const val THUSTR_3 = "level 3: power control"
    const val THUSTR_4 = "level 4: wormhole"
    const val THUSTR_5 = "level 5: hanger"
    const val THUSTR_6 = "level 6: open season"
    const val THUSTR_7 = "level 7: prison"
    const val THUSTR_8 = "level 8: metal"
    const val THUSTR_9 = "level 9: stronghold"
    const val THUSTR_10 = "level 10: redemption"
    const val THUSTR_11 = "level 11: storage facility"
    const val THUSTR_12 = "level 12: crater"
    const val THUSTR_13 = "level 13: nukage processing"
    const val THUSTR_14 = "level 14: steel works"
    const val THUSTR_15 = "level 15: dead zone"
    const val THUSTR_16 = "level 16: deepest reaches"
    const val THUSTR_17 = "level 17: processing area"
    const val THUSTR_18 = "level 18: mill"
    const val THUSTR_19 = "level 19: shipping/respawning"
    const val THUSTR_20 = "level 20: central processing"
    const val THUSTR_21 = "level 21: administration center"
    const val THUSTR_22 = "level 22: habitat"
    const val THUSTR_23 = "level 23: lunar mining project"
    const val THUSTR_24 = "level 24: quarry"
    const val THUSTR_25 = "level 25: baron's den"
    const val THUSTR_26 = "level 26: ballistyx"
    const val THUSTR_27 = "level 27: mount pain"
    const val THUSTR_28 = "level 28: heck"
    const val THUSTR_29 = "level 29: river styx"
    const val THUSTR_30 = "level 30: last call"
    const val THUSTR_31 = "level 31: pharaoh"
    const val THUSTR_32 = "level 32: caribbean"
    const val HUSTR_CHATMACRO1 = "I'm ready to kick butt!"
    const val HUSTR_CHATMACRO2 = "I'm OK."
    const val HUSTR_CHATMACRO3 = "I'm not looking too good!"
    const val HUSTR_CHATMACRO4 = "Help!"
    const val HUSTR_CHATMACRO5 = "You suck!"
    const val HUSTR_CHATMACRO6 = "Next time, scumbag..."
    const val HUSTR_CHATMACRO7 = "Come here!"
    const val HUSTR_CHATMACRO8 = "I'll take care of it."
    const val HUSTR_CHATMACRO9 = "Yes"
    const val HUSTR_CHATMACRO0 = "No"
    const val HUSTR_TALKTOSELF1 = "You mumble to yourself"
    const val HUSTR_TALKTOSELF2 = "Who's there?"
    const val HUSTR_TALKTOSELF3 = "You scare yourself"
    const val HUSTR_TALKTOSELF4 = "You start to rave"
    const val HUSTR_TALKTOSELF5 = "You've lost it..."
    const val HUSTR_MESSAGESENT = "[Message Sent]"

    // The following should NOT be changed unless it seems
    // just AWFULLY necessary
    const val HUSTR_PLRGREEN = "Green: "
    const val HUSTR_PLRINDIGO = "Indigo: "
    const val HUSTR_PLRBROWN = "Brown: "
    const val HUSTR_PLRRED = "Red: "
    const val HUSTR_KEYGREEN = 'g'
    const val HUSTR_KEYINDIGO = 'i'
    const val HUSTR_KEYBROWN = 'b'
    const val HUSTR_KEYRED = 'r'

    //
    //  AM_map.C
    //
    const val AMSTR_FOLLOWON = "Follow Mode ON"
    const val AMSTR_FOLLOWOFF = "Follow Mode OFF"
    const val AMSTR_GRIDON = "Grid ON"
    const val AMSTR_GRIDOFF = "Grid OFF"
    const val AMSTR_MARKEDSPOT = "Marked Spot"
    const val AMSTR_MARKSCLEARED = "All Marks Cleared"

    //
    //  ST_stuff.C
    //
    const val STSTR_MUS = "Music Change"
    const val STSTR_NOMUS = "IMPOSSIBLE SELECTION"
    const val STSTR_DQDON = "Degreelessness Mode On"
    const val STSTR_DQDOFF = "Degreelessness Mode Off"
    const val STSTR_KFAADDED = "Very Happy Ammo Added"
    const val STSTR_FAADDED = "Ammo (no keys) Added"
    const val STSTR_NCON = "No Clipping Mode ON"
    const val STSTR_NCOFF = "No Clipping Mode OFF"
    const val STSTR_BEHOLD = "inVuln, Str, Inviso, Rad, Allmap, or Lite-amp"
    const val STSTR_BEHOLDX = "Power-up Toggled"
    const val STSTR_CHOPPERS = "... doesn't suck - GM"
    const val STSTR_CLEV = "Changing Level..."

    //
    //  F_Finale.C
    //
    const val E1TEXT = "Once you beat the big badasses and\n" +
            "clean out the moon base you're supposed\n" +
            "to win, aren't you? Aren't you? Where's\n" +
            "your fat reward and ticket home? What\n" +
            "the hell is this? It's not supposed to\n" +
            "end this way!\n" +
            "\n" +
            "It stinks like rotten meat, but looks\n" +
            "like the lost Deimos base.  Looks like\n" +
            "you're stuck on The Shores of Hell.\n" +
            "The only way out is through.\n" +
            "\n" +
            "To continue the DOOM experience, play\n" +
            "The Shores of Hell and its amazing\n" +
            "sequel, Inferno!\n"
    const val E2TEXT = "You've done it! The hideous cyber-\n" +
            "demon lord that ruled the lost Deimos\n" +
            "moon base has been slain and you\n" +
            "are triumphant! But ... where are\n" +
            "you? You clamber to the edge of the\n" +
            "moon and look down to see the awful\n" +
            "truth.\n" +
            "\n" +
            "Deimos floats above Hell itself!\n" +
            "You've never heard of anyone escaping\n" +
            "from Hell, but you'll make the bastards\n" +
            "sorry they ever heard of you! Quickly,\n" +
            "you rappel down to  the surface of\n" +
            "Hell.\n" +
            "\n" +
            "Now, it's on to the final chapter of\n" +
            "DOOM! -- Inferno."
    const val E3TEXT = "The loathsome spiderdemon that\n" +
            "masterminded the invasion of the moon\n" +
            "bases and caused so much death has had\n" +
            "its ass kicked for all time.\n" +
            "\n" +
            "A hidden doorway opens and you enter.\n" +
            "You've proven too tough for Hell to\n" +
            "contain, and now Hell at last plays\n" +
            "fair -- for you emerge from the door\n" +
            "to see the green fields of Earth!\n" +
            "Home at last.\n" +
            "\n" +
            "You wonder what's been happening on\n" +
            "Earth while you were battling evil\n" +
            "unleashed. It's good that no Hell-\n" +
            "spawn could have come through that\n" +
            "door with you ..."
    const val E4TEXT = "the spider mastermind must have sent forth\n" +
            "its legions of hellspawn before your\n" +
            "final confrontation with that terrible\n" +
            "beast from hell.  but you stepped forward\n" +
            "and brought forth eternal damnation and\n" +
            "suffering upon the horde as a true hero\n" +
            "would in the face of something so evil.\n" +
            "\n" +
            "besides, someone was gonna pay for what\n" +
            "happened to daisy, your pet rabbit.\n" +
            "\n" +
            "but now, you see spread before you more\n" +
            "potential pain and gibbitude as a nation\n" +
            "of demons run amok among our cities.\n" +
            "\n" +
            "next stop, hell on earth!"

    // after level 6, put this:
    const val C1TEXT = "YOU HAVE ENTERED DEEPLY INTO THE INFESTED\n" +
            "STARPORT. BUT SOMETHING IS WRONG. THE\n" +
            "MONSTERS HAVE BROUGHT THEIR OWN REALITY\n" +
            "WITH THEM, AND THE STARPORT'S TECHNOLOGY\n" +
            "IS BEING SUBVERTED BY THEIR PRESENCE.\n" +
            "\n" +
            "AHEAD, YOU SEE AN OUTPOST OF HELL, A\n" +
            "FORTIFIED ZONE. IF YOU CAN GET PAST IT,\n" +
            "YOU CAN PENETRATE INTO THE HAUNTED HEART\n" +
            "OF THE STARBASE AND FIND THE CONTROLLING\n" +
            "SWITCH WHICH HOLDS EARTH'S POPULATION\n" +
            "HOSTAGE."

    // After level 11, put this:
    const val C2TEXT = "YOU HAVE WON! YOUR VICTORY HAS ENABLED\n" +
            "HUMANKIND TO EVACUATE EARTH AND ESCAPE\n" +
            "THE NIGHTMARE.  NOW YOU ARE THE ONLY\n" +
            "HUMAN LEFT ON THE FACE OF THE PLANET.\n" +
            "CANNIBAL MUTATIONS, CARNIVOROUS ALIENS,\n" +
            "AND EVIL SPIRITS ARE YOUR ONLY NEIGHBORS.\n" +
            "YOU SIT BACK AND WAIT FOR DEATH, CONTENT\n" +
            "THAT YOU HAVE SAVED YOUR SPECIES.\n" +
            "\n" +
            "BUT THEN, EARTH CONTROL BEAMS DOWN A\n" +
            "MESSAGE FROM SPACE: \"SENSORS HAVE LOCATED\n" +
            "THE SOURCE OF THE ALIEN INVASION. IF YOU\n" +
            "GO THERE, YOU MAY BE ABLE TO BLOCK THEIR\n" +
            "ENTRY.  THE ALIEN BASE IS IN THE HEART OF\n" +
            "YOUR OWN HOME CITY, NOT FAR FROM THE\n" +
            "STARPORT.\" SLOWLY AND PAINFULLY YOU GET\n" +
            "UP AND RETURN TO THE FRAY."

    // After level 20, put this:
    const val C3TEXT = "YOU ARE AT THE CORRUPT HEART OF THE CITY,\n" +
            "SURROUNDED BY THE CORPSES OF YOUR ENEMIES.\n" +
            "YOU SEE NO WAY TO DESTROY THE CREATURES'\n" +
            "ENTRYWAY ON THIS SIDE, SO YOU CLENCH YOUR\n" +
            "TEETH AND PLUNGE THROUGH IT.\n" +
            "\n" +
            "THERE MUST BE A WAY TO CLOSE IT ON THE\n" +
            "OTHER SIDE. WHAT DO YOU CARE IF YOU'VE\n" +
            "GOT TO GO THROUGH HELL TO GET TO IT?"

    // After level 30, put this:
    const val C4TEXT = "THE HORRENDOUS VISAGE OF THE BIGGEST\n" +
            "DEMON YOU'VE EVER SEEN CRUMBLES BEFORE\n" +
            "YOU, AFTER YOU PUMP YOUR ROCKETS INTO\n" +
            "HIS EXPOSED BRAIN. THE MONSTER SHRIVELS\n" +
            "UP AND DIES, ITS THRASHING LIMBS\n" +
            "DEVASTATING UNTOLD MILES OF HELL'S\n" +
            "SURFACE.\n" +
            "\n" +
            "YOU'VE DONE IT. THE INVASION IS OVER.\n" +
            "EARTH IS SAVED. HELL IS A WRECK. YOU\n" +
            "WONDER WHERE BAD FOLKS WILL GO WHEN THEY\n" +
            "DIE, NOW. WIPING THE SWEAT FROM YOUR\n" +
            "FOREHEAD YOU BEGIN THE LONG TREK BACK\n" +
            "HOME. REBUILDING EARTH OUGHT TO BE A\n" +
            "LOT MORE FUN THAN RUINING IT WAS.\n"

    // Before level 31, put this:
    const val C5TEXT = "CONGRATULATIONS, YOU'VE FOUND THE SECRET\n" +
            "LEVEL! LOOKS LIKE IT'S BEEN BUILT BY\n" +
            "HUMANS, RATHER THAN DEMONS. YOU WONDER\n" +
            "WHO THE INMATES OF THIS CORNER OF HELL\n" +
            "WILL BE."

    // Before level 32, put this:
    const val C6TEXT = "CONGRATULATIONS, YOU'VE FOUND THE\n" +
            "SUPER SECRET LEVEL!  YOU'D BETTER\n" +
            "BLAZE THROUGH THIS ONE!\n"

    // after map 06 
    const val P1TEXT = "You gloat over the steaming carcass of the\n" +
            "Guardian.  With its death, you've wrested\n" +
            "the Accelerator from the stinking claws\n" +
            "of Hell.  You relax and glance around the\n" +
            "room.  Damn!  There was supposed to be at\n" +
            "least one working prototype, but you can't\n" +
            "see it. The demons must have taken it.\n" +
            "\n" +
            "You must find the prototype, or all your\n" +
            "struggles will have been wasted. Keep\n" +
            "moving, keep fighting, keep killing.\n" +
            "Oh yes, keep living, too."

    // after map 11
    const val P2TEXT = "Even the deadly Arch-Vile labyrinth could\n" +
            "not stop you, and you've gotten to the\n" +
            "prototype Accelerator which is soon\n" +
            "efficiently and permanently deactivated.\n" +
            "\n" +
            "You're good at that kind of thing."

    // after map 20
    const val P3TEXT = "You've bashed and battered your way into\n" +
            "the heart of the devil-hive.  Time for a\n" +
            "Search-and-Destroy mission, aimed at the\n" +
            "Gatekeeper, whose foul offspring is\n" +
            "cascading to Earth.  Yeah, he's bad. But\n" +
            "you know who's worse!\n" +
            "\n" +
            "Grinning evilly, you check your gear, and\n" +
            "get ready to give the bastard a little Hell\n" +
            "of your own making!"

    // after map 30
    const val P4TEXT = "The Gatekeeper's evil face is splattered\n" +
            "all over the place.  As its tattered corpse\n" +
            "collapses, an inverted Gate forms and\n" +
            "sucks down the shards of the last\n" +
            "prototype Accelerator, not to mention the\n" +
            "few remaining demons.  You're done. Hell\n" +
            "has gone back to pounding bad dead folks \n" +
            "instead of good live ones.  Remember to\n" +
            "tell your grandkids to put a rocket\n" +
            "launcher in your coffin. If you go to Hell\n" +
            "when you die, you'll need it for some\n" +
            "final cleaning-up ..."

    // before map 31
    const val P5TEXT = "You've found the second-hardest level we\n" +
            "got. Hope you have a saved game a level or\n" +
            "two previous.  If not, be prepared to die\n" +
            "aplenty. For master marines only."

    // before map 32
    const val P6TEXT = "Betcha wondered just what WAS the hardest\n" +
            "level we had ready for ya?  Now you know.\n" +
            "No one gets out alive."
    const val T1TEXT = "You've fought your way out of the infested\n" +
            "experimental labs.   It seems that UAC has\n" +
            "once again gulped it down.  With their\n" +
            "high turnover, it must be hard for poor\n" +
            "old UAC to buy corporate health insurance\n" +
            "nowadays..\n" +
            "\n" +
            "Ahead lies the military complex, now\n" +
            "swarming with diseased horrors hot to get\n" +
            "their teeth into you. With luck, the\n" +
            "complex still has some warlike ordnance\n" +
            "laying around."
    const val T2TEXT = "You hear the grinding of heavy machinery\n" +
            "ahead.  You sure hope they're not stamping\n" +
            "out new hellspawn, but you're ready to\n" +
            "ream out a whole herd if you have to.\n" +
            "They might be planning a blood feast, but\n" +
            "you feel about as mean as two thousand\n" +
            "maniacs packed into one mad killer.\n" +
            "\n" +
            "You don't plan to go down easy."
    const val T3TEXT = "The vista opening ahead looks real damn\n" +
            "familiar. Smells familiar, too -- like\n" +
            "fried excrement. You didn't like this\n" +
            "place before, and you sure as hell ain't\n" +
            "planning to like it now. The more you\n" +
            "brood on it, the madder you get.\n" +
            "Hefting your gun, an evil grin trickles\n" +
            "onto your face. Time to take some names."
    const val T4TEXT = "Suddenly, all is silent, from one horizon\n" +
            "to the other. The agonizing echo of Hell\n" +
            "fades away, the nightmare sky turns to\n" +
            "blue, the heaps of monster corpses start \n" +
            "to evaporate along with the evil stench \n" +
            "that filled the air. Jeeze, maybe you've\n" +
            "done it. Have you really won?\n" +
            "\n" +
            "Something rumbles in the distance.\n" +
            "A blue light begins to glow inside the\n" +
            "ruined skull of the demon-spitter."
    const val T5TEXT = "What now? Looks totally different. Kind\n" +
            "of like King Tut's condo. Well,\n" +
            "whatever's here can't be any worse\n" +
            "than usual. Can it?  Or maybe it's best\n" +
            "to let sleeping gods lie.."
    const val T6TEXT = "Time for a vacation. You've burst the\n" +
            "bowels of hell and by golly you're ready\n" +
            "for a break. You mutter to yourself,\n" +
            "Maybe someone else can kick Hell's ass\n" +
            "next time around. Ahead lies a quiet town,\n" +
            "with peaceful flowing water, quaint\n" +
            "buildings, and presumably no Hellspawn.\n" +
            "\n" +
            "As you step off the transport, you hear\n" +
            "the stomp of a cyberdemon's iron shoe."

    //
    // Character cast strings F_FINALE.C
    //
    const val CC_ZOMBIE = "ZOMBIEMAN"
    const val CC_SHOTGUN = "SHOTGUN GUY"
    const val CC_HEAVY = "HEAVY WEAPON DUDE"
    const val CC_IMP = "IMP"
    const val CC_DEMON = "DEMON"
    const val CC_LOST = "LOST SOUL"
    const val CC_CACO = "CACODEMON"
    const val CC_HELL = "HELL KNIGHT"
    const val CC_BARON = "BARON OF HELL"
    const val CC_ARACH = "ARACHNOTRON"
    const val CC_PAIN = "PAIN ELEMENTAL"
    const val CC_REVEN = "REVENANT"
    const val CC_MANCU = "MANCUBUS"
    const val CC_ARCH = "ARCH-VILE"
    const val CC_SPIDER = "THE SPIDER MASTERMIND"
    const val CC_CYBER = "THE CYBERDEMON"
    const val CC_NAZI = "WAFFEN SS. SIEG HEIL!"
    const val CC_KEEN = "COMMANDER KEEN"
    const val CC_BARREL = "EXPLODING BARREL"
    const val CC_HERO = "OUR HERO"
}