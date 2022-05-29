package doom


import defines.ammotype_t
import defines.statenum_t

//
// PSPRITE ACTIONS for waepons.
// This struct controls the weapon animations.
//
// Each entry is:
//   ammo/amunition type
//  upstate
//  downstate
// readystate
// atkstate, i.e. attack/fire/hit frame
// flashstate, muzzle flash
//
class weaponinfo_t(/*    
    public weaponinfo_t(ammotype_t ammo, int upstate, int downstate,
            int readystate, int atkstate, int flashstate) {
        super();
        this.ammo = ammo;
        this.upstate = upstate;
        this.downstate = downstate;
        this.readystate = readystate;
        this.atkstate = atkstate;
        this.flashstate = flashstate;
    }*/
    var ammo: ammotype_t, var upstate: statenum_t,
    var downstate: statenum_t, var readystate: statenum_t,
    var atkstate: statenum_t, /*
        public int     upstate;
        public int     downstate;
        public int     readystate;
        public int     atkstate;
        public int     flashstate;
        */var flashstate: statenum_t
)