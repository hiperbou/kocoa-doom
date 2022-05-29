package doom


enum class gameaction_t {
    ga_nothing, ga_loadlevel, ga_newgame, ga_loadgame, ga_savegame, ga_playdemo, ga_completed, ga_victory, ga_worlddone, ga_screenshot, ga_failure // HACK: communicate failures silently
}