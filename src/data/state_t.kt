package data

import defines.statenum_t
//import w.statenum_t
import p.ActiveStates


class state_t {
    constructor() {}
    constructor(
        sprite: spritenum_t?,
        frame: Int,
        tics: Int,
        nextstate: statenum_t?,
        misc1: Int,
        misc2: Int,
        _action: ActiveStates = ActiveStates.NOP
    ) {
        this.sprite = sprite
        this.frame = frame
        this.tics = tics * Defines.TIC_MUL
        this.action = _action
        this.nextstate = nextstate
        this.misc1 = misc1
        this.misc2 = misc2
    }

    var sprite: spritenum_t? = null

    /**
     * The frame should indicate which one of the frames available in the
     * available spritenum should be used. This can also be flagged with
     * 0x8000 indicating bright sprites.
     */
    var frame = 0
    var tics = 0
    //TODO: proper implementation of (*action)
    // MAES: was actionp_t... which is typedeffed to ActionFunction anyway,
    // and this is the only place it's invoked explicitly.
    /**
     * OK...this is the most infamous part of Doom to implement in Java.
     * We can't have proper "function pointers" in java without either losing a LOT
     * of speed (through reflection) or cluttering syntax and heap significantly
     * (callback objects, which also need to be aware of context).
     * Therefore, I decided to implement an "action dispatcher".
     * This a
     *
     */
    var action: ActiveStates/*<ParamClassDani>*/? = null
    var nextstate: statenum_t? = null
    var misc1 = 0
    var misc2 = 0

    /**
     * relative index in state array. Needed sometimes.
     */
    var id = 0
    override fun toString(): String {
        state_t.sb.setLength(0)
        state_t.sb.append(this.javaClass.name)
        state_t.sb.append(" sprite ")
        state_t.sb.append(sprite!!.name)
        state_t.sb.append(" frame ")
        state_t.sb.append(frame)
        return state_t.sb.toString()
    }

    companion object {
        protected var sb = StringBuilder() /*@Override
    public void read(DoomFile f) throws IOException {
        this.sprite = spritenum_t.values()[f.readLEInt()];
        this.frame = f.readLEInt();
        this.tics = f.readLong();
        this.action = ActionFunction.values()[f.readInt()];
        this.nextstate = statenum_t.values()[f.readInt()];
        this.misc1 = f.readInt();
        this.misc2 = f.readInt();
    } */
    }
}