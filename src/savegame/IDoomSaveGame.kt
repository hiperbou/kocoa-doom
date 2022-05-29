package savegame


import p.ThinkerList
import java.io.DataInputStream
import java.io.DataOutputStream

interface IDoomSaveGame {
    fun setThinkerList(li: ThinkerList?)
    fun doLoad(f: DataInputStream): Boolean
    fun getHeader(): IDoomSaveGameHeader?
    fun setHeader(header: IDoomSaveGameHeader?)
    fun doSave(f: DataOutputStream): Boolean
}