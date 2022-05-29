package data


import data.sounds.sfxenum_t
import defines.statenum_t

class mobjinfo_t(
    var doomednum: Int, var spawnstate: statenum_t, var spawnhealth: Int,
    var seestate: statenum_t, var seesound: sfxenum_t, var reactiontime: Int,
    var attacksound: sfxenum_t, var painstate: statenum_t,
    var painchance: Int, var painsound: sfxenum_t,
    var meleestate: statenum_t, var missilestate: statenum_t,
    var deathstate: statenum_t, var xdeathstate: statenum_t,
    var deathsound: sfxenum_t, var speed: Int, var radius: Int, var height: Int,
    var mass: Int, var damage: Int, var activesound: sfxenum_t, var flags: Int, //TODO: was flags: Long
    var raisestate: statenum_t
)