package doom


class doomcom_t {
    // Supposed to be DOOMCOM_ID?
    // Maes: was "long", but they intend 32-bit "int" here. Hurray for C's consistency!
    var id = 0

    // DOOM executes an int to execute commands.
    var intnum: Short = 0

    // Communication between DOOM and the driver.
    // Is CMD_SEND or CMD_GET.
    var command: Short = 0

    // Is dest for send, set by get (-1 = no packet).
    var remotenode: Short = 0

    // Number of bytes in doomdata to be sent
    var datalength: Short = 0

    // Info common to all nodes.
    // Console is allways node 0.
    var numnodes: Short = 0

    // Flag: 1 = no duplication, 2-5 = dup for slow nets.
    var ticdup: Short = 0

    // Flag: 1 = send a backup tic in every packet.
    var extratics: Short = 0

    // Flag: 1 = deathmatch.
    var deathmatch: Short = 0

    // Flag: -1 = new game, 0-5 = load savegame
    var savegame: Short = 0
    var episode // 1-3
            : Short = 0
    var map // 1-9
            : Short = 0
    var skill // 1-5
            : Short = 0

    // Info specific to this node.
    var consoleplayer: Short = 0
    var numplayers: Short = 0

    // These are related to the 3-display mode,
    //  in which two drones looking left and right
    //  were used to render two additional views
    //  on two additional computers.
    // Probably not operational anymore.
    // 1 = left, 0 = center, -1 = right
    var angleoffset: Short = 0

    // 1 = drone
    var drone: Short = 0

    // The packet data to be sent.
    var data: doomdata_t

    init {
        data = doomdata_t()
    }
}