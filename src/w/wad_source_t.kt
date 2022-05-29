package w


// CPhipps - defined enum in wider scope
// Ty 08/29/98 - add source field to identify where this lump came from
enum class wad_source_t {
    // CPhipps - define elements in order of 'how new/unusual'
    source_iwad,  // iwad file load 
    source_pre,  // predefined lump
    source_auto_load,  // lump auto-loaded by config file
    source_pwad,  // pwad file load
    source_lmp,  // lmp file load
    source_net // CPhipps
    ,
    source_deh, source_err
}