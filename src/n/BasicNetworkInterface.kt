package n

import data.Limits
import doom.*
import w.DoomBuffer
import java.io.IOException
import java.net.*
import java.nio.channels.IllegalBlockingModeException


//-----------------------------------------------------------------------------
//
// $Id: BasicNetworkInterface.java,v 1.5 2011/05/26 13:39:06 velktron Exp $
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
// $Log: BasicNetworkInterface.java,v $
// Revision 1.5  2011/05/26 13:39:06  velktron
// Now using ICommandLineManager
//
// Revision 1.4  2011/05/18 16:54:31  velktron
// Changed to DoomStatus
//
// Revision 1.3  2011/05/17 16:53:42  velktron
// _D_'s version.
//
// Revision 1.2  2010/12/20 17:15:08  velktron
// Made the renderer more OO -> TextureManager and other changes as well.
//
// Revision 1.1  2010/11/17 23:55:06  velktron
// Kind of playable/controllable.
//
// Revision 1.2  2010/11/11 15:31:28  velktron
// Fixed "warped floor" error.
//
// Revision 1.1  2010/10/22 16:22:43  velktron
// Renderer works stably enough but a ton of bleeding. Started working on netcode.
//
//
// DESCRIPTION:
//
//-----------------------------------------------------------------------------
class BasicNetworkInterface(protected var DOOM: DoomMain<*, *>) : DoomSystemNetworking {
    // Bind it to the ones inside DN and DM;  
    //doomdata_t netbuffer;
    var doomcom: doomcom_t? = null

    //void    NetSend ();
    //boolean NetListen ();
    //
    // NETWORKING
    //
    // Maes: come on, we all know it's 666.
    var DOOMPORT = 666 //(IPPORT_USERRESERVED +0x1d );

    //_D_: for testing purposes. If testing on the same machine, we can't have two UDP servers on the same port
    var RECVPORT = DOOMPORT
    var SENDPORT = DOOMPORT

    //DatagramSocket         sendsocket;
    var insocket: DatagramSocket? = null

    // MAES: closest java equivalent
    var   /*InetAddress*/sendaddress = arrayOfNulls<DatagramSocket>(Limits.MAXNETNODES)

    interface NetFunction {
        operator fun invoke()
    }

    // To use inside packetsend. Declare once and reuse to save on heap costs.
    private val sendData: doomdata_t
    private val recvData: doomdata_t

    // We also reuse always the same DatagramPacket, "peged" to sw's byte buffer.
    private val recvPacket: DatagramPacket
    private val sendPacket: DatagramPacket
    @Throws(IOException::class)
    fun sendSocketPacket(ds: DatagramSocket?, dp: DatagramPacket?) {
        ds!!.send(dp)
    }

    var packetSend = PacketSend()

    inner class PacketSend : NetFunction {
        override fun invoke() {
            var c: Int
            val netbuffer = DOOM.netbuffer!!

            // byte swap: so this is transferred as little endian? Ugh
            /*sendData.checksum = htonl(netbuffer.checksum);
          sendData.player = netbuffer.player;
          sendData.retransmitfrom = netbuffer.retransmitfrom;
          sendData.starttic = netbuffer.starttic;
          sendData.numtics = netbuffer.numtics;
          for (c=0 ; c< netbuffer.numtics ; c++)
          {
              sendData.cmds[c].forwardmove = netbuffer.cmds[c].forwardmove;
              sendData.cmds[c].sidemove = netbuffer.cmds[c].sidemove;
              sendData.cmds[c].angleturn = htons(netbuffer.cmds[c].angleturn);
              sendData.cmds[c].consistancy = htons(netbuffer.cmds[c].consistancy);
              sendData.cmds[c].chatchar = netbuffer.cmds[c].chatchar;
              sendData.cmds[c].buttons = netbuffer.cmds[c].buttons;
          }
             */
            //printf ("sending %i\n",gametic);      
            sendData.copyFrom(netbuffer)
            // MAES: This will force the buffer to be refreshed.
            val bytes = sendData.pack()

            /*System.out.print("SEND >> Thisplayer: "+DM.consoleplayer+" numtics: "+sendData.numtics+" consistency: ");
          for (doom.ticcmd_t t: sendData.cmds)
              System.out.print(t.consistancy+",");
          System.out.println();*/
            // The socket already contains the address it needs,
            // and the packet's buffer is already modified. Send away.
            sendPacket.setData(bytes, 0, doomcom!!.datalength.toInt())
            val sendsocket: DatagramSocket?
            try {
                sendsocket = sendaddress[doomcom!!.remotenode.toInt()]
                sendPacket.socketAddress = sendsocket!!.remoteSocketAddress
                sendSocketPacket(sendsocket, sendPacket)
            } catch (e: Exception) {
                e.printStackTrace()
                DOOM.doomSystem.Error("SendPacket error: %s", e.message)
            }

            //  if (c == -1)
            //      I_Error ("SendPacket error: %s",strerror(errno));
        }
    }

    @Throws(IOException::class)
    fun socketGetPacket(ds: DatagramSocket?, dp: DatagramPacket?) {
        ds!!.receive(dp)
    }

    // Used inside PacketGet
    private var first = true
    var packetGet = PacketGet()

    inner class PacketGet : NetFunction {
        override fun invoke() {
            var i: Int
            var c: Int

            // Receive back into swp.
            try {
                //recvPacket.setSocketAddress(insocket.getLocalSocketAddress());
                socketGetPacket(insocket, recvPacket)
            } catch (e: SocketTimeoutException) {
                doomcom!!.remotenode = -1 // no packet
                return
            } catch (e: Exception) {
                if (e.javaClass != IllegalBlockingModeException::class.java) {
                    DOOM.doomSystem.Error("GetPacket: %s", *e.stackTrace as Array<Any?>)
                }
            }
            recvData.unpack(recvPacket.data)
            val fromaddress = recvPacket.address

            /*System.out.print("RECV << Thisplayer: "+DM.consoleplayer+" numtics: "+recvData.numtics+" consistency: ");
          for (doom.ticcmd_t t: recvData.cmds)
              System.out.print(t.consistancy+",");
          System.out.println();*/run {
                //static int first=1;
                if (first) {
                    sb.setLength(0)
                    sb.append("(").append(DOOM.consoleplayer).append(") PacketRECV len=")
                    sb.append(recvPacket.length)
                    sb.append(":p=[0x")
                    sb.append(Integer.toHexString(recvData.checksum))
                    sb.append(" 0x")
                    sb.append(
                        DoomBuffer.getBEInt(
                            recvData.retransmitfrom,
                            recvData.starttic,
                            recvData.player,
                            recvData.numtics
                        )
                    )
                    sb.append("numtics: ").append(recvData.numtics.toInt())
                    println(sb.toString())
                    first = false
                }
            }

            // find remote node number
            i = 0
            while (i < doomcom!!.numnodes) {
                if (sendaddress[i] != null) {
                    if (fromaddress == sendaddress[i]!!.inetAddress) {
                        break
                    }
                }
                i++
            }
            if (i == doomcom!!.numnodes.toInt()) {
                // packet is not from one of the players (new game broadcast)
                doomcom!!.remotenode = -1 // no packet
                return
            }
            doomcom!!.remotenode = i.toShort() // good packet from a game player
            doomcom!!.datalength = recvPacket.length.toShort()

            //_D_: temporary hack to test two player on single machine
            //doomcom.remotenode = (short)(RECVPORT-DOOMPORT);
            // byte swap
            /*doomdata_t netbuffer = DM.netbuffer;
          netbuffer.checksum = ntohl(recvData.checksum);
          netbuffer.player = recvData.player;
          netbuffer.retransmitfrom = recvData.retransmitfrom;
          netbuffer.starttic = recvData.starttic;
          netbuffer.numtics = recvData.numtics;

          for (c=0 ; c< netbuffer.numtics ; c++)
          {
              netbuffer.cmds[c].forwardmove = recvData.cmds[c].forwardmove;
              netbuffer.cmds[c].sidemove = recvData.cmds[c].sidemove;
              netbuffer.cmds[c].angleturn = ntohs(recvData.cmds[c].angleturn);
              netbuffer.cmds[c].consistancy = ntohs(recvData.cmds[c].consistancy);
              netbuffer.cmds[c].chatchar = recvData.cmds[c].chatchar;
              netbuffer.cmds[c].buttons = recvData.cmds[c].buttons;
          } */DOOM.netbuffer!!.copyFrom(recvData)
        }
    }

    // Maes: oh great. More function pointer "fun".
    var netget: NetFunction = packetGet
    var netsend: NetFunction = packetSend

    //
    // I_InitNetwork
    //
    override fun InitNetwork() {
        //struct hostent* hostentry;  // host information entry
        doomcom = doomcom_t()
        //netbuffer = new doomdata_t();
        DOOM.setDoomCom(doomcom)
        //DM.netbuffer = netbuffer;

        // set up for network
        if (!DOOM.cVarManager.with(CommandVariable.DUP, 0) { c: Char? ->
                doomcom!!.ticdup = (c!!.code - '0'.code).toShort()
                if (doomcom!!.ticdup < 1) {
                    doomcom!!.ticdup = 1
                }
                if (doomcom!!.ticdup > 9) {
                    doomcom!!.ticdup = 9
                }
            }) {
            doomcom!!.ticdup = 1
        }
        if (DOOM.cVarManager.bool(CommandVariable.EXTRATIC)) {
            doomcom!!.extratics = 1
        } else {
            doomcom!!.extratics = 0
        }
        DOOM.cVarManager.with(CommandVariable.PORT, 0) { port: Int? ->
            DOOMPORT = port!!
            println("using alternate port $DOOMPORT")
        }

        // parse network game options,
        //  -net <consoleplayer> <host> <host> ...
        if (!DOOM.cVarManager.present(CommandVariable.NET)) {
            // single player game
            DOOM.netgame = false
            doomcom!!.id = NetConsts.DOOMCOM_ID
            doomcom!!.numnodes = 1
            doomcom!!.numplayers = doomcom!!.numnodes
            doomcom!!.deathmatch = 0 // false
            doomcom!!.consoleplayer = 0
            return
        }
        DOOM.netgame = true

        // parse player number and host list
        doomcom!!.consoleplayer =
            (DOOM.cVarManager.get(CommandVariable.NET, Char::class.java, 0).get().code - '1'.code).toShort()
        SENDPORT = DOOMPORT
        RECVPORT = SENDPORT
        if (doomcom!!.consoleplayer.toInt() == 0) {
            SENDPORT++
        } else {
            RECVPORT++
        }
        doomcom!!.numnodes = 1 // this node for sure
        val hosts = DOOM.cVarManager.get(CommandVariable.NET, Array<String>::class.java, 1).get()
        for (host in hosts) {
            try {
                val addr = InetAddress.getByName(host)
                val ds = DatagramSocket(null)
                ds.reuseAddress = true
                ds.connect(addr, SENDPORT)
                sendaddress[doomcom!!.numnodes.toInt()] = ds
            } catch (e: SocketException) {
                e.printStackTrace()
            } catch (e: UnknownHostException) {
                e.printStackTrace()
            }
            doomcom!!.numnodes++
        }
        doomcom!!.id = NetConsts.DOOMCOM_ID
        doomcom!!.numplayers = doomcom!!.numnodes

        // build message to receive
        try {
            insocket = DatagramSocket(null)
            insocket!!.reuseAddress = true
            insocket!!.soTimeout = 1
            insocket!!.bind(InetSocketAddress(RECVPORT))
        } catch (e1: SocketException) {
            e1.printStackTrace()
        }
    }

    override fun NetCmd() {
        if (insocket == null) //HACK in case "netgame" is due to "addbot"
        {
            return
        }
        if (DOOM.doomcom!!.command == NetConsts.CMD_SEND) {
            netsend.invoke()
        } else if (doomcom!!.command == NetConsts.CMD_GET) {
            netget.invoke()
        } else {
            DOOM.doomSystem.Error("Bad net cmd: %i\n", doomcom!!.command)
        }
    }

    // Instance StringBuilder
    private val sb = StringBuilder()

    init {
        //this.myargv=DM.myargv;
        //this.myargc=DM.myargc;
        sendData = doomdata_t()
        recvData = doomdata_t()
        // We can do that since the buffer is reused.
        // Note: this will effectively tie doomdata and the datapacket.
        recvPacket = DatagramPacket(recvData.cached(), recvData.cached().size)
        sendPacket = DatagramPacket(sendData.cached(), sendData.cached().size)
    }

    companion object {
        // For some odd reason...
        /**
         * Changes endianness of a number
         */
        fun ntohl(x: Int): Int {
            return (x and 0x000000ff shl 24
                    or (x and 0x0000ff00 shl 8)
                    or (x and 0x00ff0000 ushr 8)
                    or (x and -0x1000000 ushr 24))
        }

        fun ntohs(x: Short): Short {
            return (x.toInt() and 0x00ff shl 8 or (x.toInt() and 0xff00 ushr 8)).toShort()
        }

        fun htonl(x: Int): Int {
            return ntohl(x)
        }

        fun htons(x: Short): Short {
            return ntohs(x)
        }
    }
}